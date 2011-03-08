package uk.org.lidalia.ssh

abstract class WrappedString extends WrappedValue<String> {

	WrappedString(String wrappedValue) {
		super(wrappedValue);
	}

	@Override
	public String toString() {
		return wrappedValue;
	}
}
