package uk.org.lidalia.ssh

import org.junit.Test

import uk.org.lidalia.ssh.WrappedValue;

class WrappedValueTests {

    @Test
    void toStringContainsValue() {
        WrappedInteger wrapped = new WrappedInteger(4)
        org.junit.Assert.assertEquals("WrappedInteger: [4]", wrapped.toString())
    }
}

class WrappedInteger extends WrappedValue<Integer> {
    WrappedInteger(Integer value) {
        super(value)
    }
}