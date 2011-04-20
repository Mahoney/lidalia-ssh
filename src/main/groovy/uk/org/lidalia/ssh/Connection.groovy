package uk.org.lidalia.ssh

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

interface Connection {

	CommandResult run(Command command, int timeout, TimeUnit timeUnit) throws CommandFailedException, CommandInterruptedException, CommandTimeoutException

    void open()

    void close()

    void put(String src, String target)

    boolean isConnected()
}
