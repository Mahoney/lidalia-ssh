package uk.org.lidalia.ssh

import java.util.concurrent.TimeUnit
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeoutException

class Command {

    private static final Logger log = LoggerFactory.getLogger(Command)

    final String command
    final PosixUser user

    Command(String command, PosixUser user = null) {
        this.user = user
        this.command = buildCommandString(command)
    }

    private String buildCommandString(String command) {
        if (user) {
            String escapedCommand = command.replace(/"/, /\"/)
            return """echo "$escapedCommand" | sudo -s -u $user"""
        } else {
            return command
        }
    }

    CommandResult run(Connection conn, int timeout = Constants.DEFAULT_TIMEOUT, TimeUnit timeUnit = Constants.DEFAULT_TIMEUNIT) throws CommandFailedException, TimeoutException {
        return conn.run(this, timeout, timeUnit)
    }

    @Override String toString() {
        return command
    }

    @Override boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.getClass()) return false

        Command command1 = (Command) o

        if (command != command1.command) return false
        if (user != command1.user) return false

        return true
    }

    @Override int hashCode() {
        int result
        result = command.hashCode()
        result = 31 * result + (user != null ? user.hashCode() : 0)
        return result
    }
}
