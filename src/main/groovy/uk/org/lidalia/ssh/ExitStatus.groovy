package uk.org.lidalia.ssh

import uk.org.lidalia.lang.WrappedValue

class ExitStatus extends WrappedValue {

    private final int exitStatus

    ExitStatus(int exitStatus) {
        super(exitStatus)
        assert exitStatus >= 0 && exitStatus <= 255
        this.exitStatus = exitStatus
    }

    Integer toInteger() {
        return exitStatus
    }

    boolean isSuccess() {
        return exitStatus == 0
    }

    boolean isFailure() {
        return !success
    }
}
