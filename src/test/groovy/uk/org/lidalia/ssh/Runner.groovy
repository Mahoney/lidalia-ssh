package uk.org.lidalia.ssh

import java.util.concurrent.TimeUnit;

interface Runner {
	
	CommandResult run(Command command, int timeout, TimeUnit timeUnit) throws CommandFailedException, CommandInterruptedException, CommandTimeoutException
	
	void put(String src, String target)

}
