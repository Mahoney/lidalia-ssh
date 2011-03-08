package uk.org.lidalia.ssh

class CommandInterruptedException extends InterruptedException {

    final CommandResult result

    CommandInterruptedException(CommandResult result) {
        super("Command interrupted; state when interrupted: $result".toString())
        this.result = result
    }

}
