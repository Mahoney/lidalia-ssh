package uk.org.lidalia.ssh

import static java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.TimeUnit
import ch.ethz.ssh2.SCPClient
import ch.ethz.ssh2.Connection
import ch.ethz.ssh2.Session
import ch.ethz.ssh2.StreamGobbler
import java.util.concurrent.TimeoutException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SSHConnection implements uk.org.lidalia.ssh.Connection {

    private static final Logger log = LoggerFactory.getLogger(SSHConnection)

    final String serverURL
    private final String serverUsername
    private final String serverPassword
    private Connection conn

    SSHConnection(String serverURL, String serverUsername, String serverPassword) {
        this.serverURL = serverURL
        this.serverUsername = serverUsername
        this.serverPassword = serverPassword
    }

    void open() {
        assert !connected
        conn = new Connection(serverURL);
        conn.connect();
        boolean authenticated = conn.authenticateWithPassword(serverUsername, serverPassword);
        if (!authenticated) {
			close()
            throw new IOException("Authentication failed for $serverUsername on $serverURL");
        }
    }

    void close() {
        try {
            conn?.close()
        } finally {
            conn = null
        }
    }

    void put(String src, String target) {
        String mode = "0777"
        def scp = new SCPClient(conn)
        scp.put(src, target, mode)
    }

    boolean isConnected() {
        return conn != null
    }

    CommandResult run(Command command, int timeout = Constants.DEFAULT_TIMEOUT, TimeUnit timeUnit = Constants.DEFAULT_TIMEUNIT) throws CommandFailedException, CommandInterruptedException, CommandTimeoutException {
        Session session
		String stdOutput
        String errorOutput
        ExitStatus exitStatus = null
		
		long startTime = System.currentTimeMillis()
        try {
            session = conn.openSession()
            log.debug "executing cmd on server: $command"

            session.execCommand(command.command)

            def stdout = new StreamGobbler(session.getStdout());
            def stderr = new StreamGobbler(session.getStderr());

            long timeoutInMilliseconds = timeUnit.toMillis(timeout)
			try {
	            while (exitStatus == null && within(startTime, timeoutInMilliseconds)) {
	                def status = session.exitStatus
	                exitStatus = status != null ? new ExitStatus(status) : null
	                if (!exitStatus) {
	                    Thread.sleep(3)
	                }
	            }
			} catch (InterruptedException ie) {
		        Thread.currentThread().interrupt()
		    }
            if (exitStatus == null) {
                session.close()
            }
            errorOutput = stderr?.text?.trim()
            stdOutput = stdout?.text?.trim()
        } finally {
            session?.close()
        }
		long elapsedTime = System.currentTimeMillis() - startTime
        CommandResult result = new CommandResult(command, exitStatus, stdOutput, errorOutput, elapsedTime)
        if (Thread.currentThread().interrupted()) {
            throw new CommandInterruptedException(result)
        }
        if (exitStatus == null) {
            throw new CommandTimeoutException(result, timeout, timeUnit)
        }
        log.debug result.toString()
        if (result.failure) {
            throw new CommandFailedException(result)
        } else {
            return result
        }
    }

    private boolean within(long startTime, long timeoutInMilliseconds) {
        long elapsedTime = System.currentTimeMillis() - startTime
        return elapsedTime < timeoutInMilliseconds
    }
}
