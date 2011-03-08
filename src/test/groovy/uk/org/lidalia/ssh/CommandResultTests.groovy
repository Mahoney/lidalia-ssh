package uk.org.lidalia.ssh

import org.junit.Test;

import uk.org.lidalia.ssh.CommandResult;
import uk.org.lidalia.ssh.ExitStatus;

class CommandResultTests {

	@Test void isSuccessReturnsTrueIfExitStatusIsSuccess() {
		def result = commandResult(new ExitStatus(0))
		assert result.success
	}
	
	@Test void isSuccessReturnsFalseIfExitStatusIsFailure() {
		def result = commandResult(new ExitStatus(1))
		assert !result.success
	}
	
	@Test void isSuccessReturnsFalseIfExitStatusIsNull() {
		def result = commandResult(null)
		assert !result.success
	}

	@Test void isFailureReturnsFalseIfExitStatusIsSuccess() {
		def result = commandResult(new ExitStatus(0))
		assert !result.failure
	}
	
	@Test void isFailureReturnsTrueIfExitStatusIsFailure() {
		def result = commandResult(new ExitStatus(1))
		assert result.failure
	}
	
	@Test void isFailureReturnsTrueIfExitStatusIsNull() {
		def result = commandResult(null)
		assert result.failure
	}
	
	private CommandResult commandResult(ExitStatus exitStatus) {
		return new CommandResult(null, exitStatus, null, null, 1L)
	}
}
