package uk.org.lidalia.ssh

import static java.util.concurrent.TimeUnit.SECONDS

import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.concurrent.TimeoutException

class RichConnection implements Connection {

    private static final def log = LoggerFactory.getLogger(RichConnection)

    private final Connection connection
    private final String serverURL
    private SSHConnection conn
	
	int defaultTimeout = DEFAULT_TIMEOUT
	TimeUnit defaultTimeUnit = DEFAULT_TIMEUNIT

    RichConnection(String serverURL, String serverUsername, String serverPassword) {
        this.connection = new SSHConnection(serverURL, serverUsername, serverPassword)
        this.serverURL = serverURL
    }

    RichConnection(Connection connection, String serverUrl) {
        this.connection = connection
        this.serverURL = serverUrl
    }

    def remote(Closure work) throws IOException {
        final def result
        try {
            MDC.put("hostname", serverURL)
            if (connected) {
                result = work.call()
            } else {
                try {
                    open()
                    result = work.call()
                } finally {
                    close()
                }
            }
        } finally {
            MDC.remove("hostname")
        }
        return result
    }

    CommandResult run(Command command, int timeout = defaultTimeout, TimeUnit timeUnit = defaultTimeUnit) throws CommandFailedException, CommandInterruptedException, CommandTimeoutException {
        return (CommandResult) remote {
            return connection.run(command, timeout, timeUnit)
        }
    }

    String run(String cmd, String user = null, int timeout = defaultTimeout, TimeUnit timeUnit = defaultTimeUnit) throws CommandFailedException, CommandInterruptedException, CommandTimeoutException {
        PosixUser posixUser = user ? new PosixUser(user) : null
        Command command = new Command(cmd, posixUser)
        return run(command, timeout, timeUnit).out
    }

    @Override void open() {
        connection.open()
    }

    @Override void close() {
        connection.close()
    }

    @Override void put(String src, String target) {
        remote {
            connection.put(src, target)
        }
    }

    void put(String from, String to, String user) {
        remote {
            String filename = from.substring(from.lastIndexOf("/") + 1)
            put(from, "/tmp")
            run("cp /tmp/$filename $to", user)
            run("rm /tmp/$filename")
        }
    }

    @Override boolean isConnected() {
        return connection.connected
    }
}
