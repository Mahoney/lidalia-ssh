package uk.org.lidalia.ssh

class CommandResult {
    final Command command
    final ExitStatus exitStatus
    final String out
    final String err
	final long elapsedTimeMillis

    CommandResult(Command command, ExitStatus exitStatus, String out, String err, long elapsedTimeMillis) {
        this.command = command
        this.exitStatus = exitStatus
        this.out = out
        this.err = err
		this.elapsedTimeMillis = elapsedTimeMillis
    }

    @Override
    String toString() {
        StringBuilder command = new StringBuilder("${exitStatus ?: 'Did not exit'} running [${command}] over $elapsedTimeMillis milliseconds")
        if (out) {
            command << "\nout = $out"
        }
        if (err) {
            command << "\nerr = $err"
        }
        return command.toString()
    }

	boolean isSuccess() {
		return exitStatus?.success
	}

    boolean isFailure() {
        return !success
    }
}
