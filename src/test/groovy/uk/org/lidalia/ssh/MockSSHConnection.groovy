package uk.org.lidalia.ssh

import java.util.concurrent.TimeUnit

import uk.org.lidalia.ssh.Command
import uk.org.lidalia.ssh.CommandResult
import uk.org.lidalia.ssh.Connection

class MockSSHConnection implements Connection {

    boolean connected = false
    def runner

    MockSSHConnection(def runner) {
        this.runner = runner
    }

    CommandResult run(Command command, int timeout = Constants.DEFAULT_TIMEOUT, TimeUnit timeUnit = Constants.DEFAULT_TIMEUNIT) {
        assert connected
        runner.run(command, timeout, timeUnit)
    }

    void open() {
        assert !connected
        connected = true
    }

    void close() {
        connected = false
    }

    void put(String src, String target) {
        assert connected
        runner.put(src, target)
    }
}
