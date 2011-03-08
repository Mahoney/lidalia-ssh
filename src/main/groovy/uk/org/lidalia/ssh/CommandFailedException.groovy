package uk.org.lidalia.ssh

class CommandFailedException extends Exception {

    final CommandResult result

    CommandFailedException(CommandResult result) {
        super(result.toString())
        this.result = result
    }
}
