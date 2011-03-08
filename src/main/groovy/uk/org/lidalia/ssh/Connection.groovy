package uk.org.lidalia.ssh

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

interface Connection {

	static final TimeUnit DEFAULT_TIMEUNIT = TimeUnit.SECONDS
	static final int DEFAULT_TIMEOUT = 10
	
	CommandResult run(Command command, int timeout, TimeUnit timeUnit) throws CommandFailedException, CommandInterruptedException, CommandTimeoutException

    void open()

    void close()

    void put(String src, String target)

    boolean isConnected()
}
