package uk.org.lidalia.ssh

public abstract class WrappedValue<E> {

	protected final E wrappedValue;

	public WrappedValue(E wrappedValue) {
		assert wrappedValue != null
		this.wrappedValue = wrappedValue
	}

	@Override
	public String toString() {
		return "${getClass().simpleName}: [${wrappedValue.toString()}]"
	}

	@Override
	public final int hashCode() {
		final int prime = 31
		int result = 1
		result = prime * result + getClass().hashCode()
		result = prime * result + wrappedValue.hashCode()
		return result
	}

	@Override
	public final boolean equals(Object other) {
        if (other == null)
            return false
        if (this.is(other))
            return true
        if (!(other.getClass() == this.getClass()))
			return false
		WrappedValue<?> otherWrappedValue = (WrappedValue<?>) other
		return wrappedValue == otherWrappedValue.wrappedValue
	}
}
