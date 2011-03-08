package uk.org.lidalia.ssh

import java.util.concurrent.TimeoutException
import java.util.concurrent.TimeUnit

class CommandTimeoutException extends TimeoutException {

    final CommandResult result
    final int timeout
    final TimeUnit timeUnit

    CommandTimeoutException(CommandResult result, int timeout, TimeUnit timeUnit) {
        super("Command timed out after $timeout $timeUnit; state when timed out: $result".toString())
        this.result = result
        this.timeout = timeout
        this.timeUnit = timeUnit
    }
}
