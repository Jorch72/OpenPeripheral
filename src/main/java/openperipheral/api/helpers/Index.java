package openperipheral.api.helpers;

import com.google.common.primitives.Ints;

/**
 * Simple class for easy handling of not-zero indexed collections.
 * If used, OpenPeripheral will convert any index to zero-base value with script language specific offset.
 * For example, in Lua value 3 will become {@code Index(value = 2, offset = 1}.
 */
public class Index extends Number implements Comparable<Index> {
	private static final long serialVersionUID = 1L;

	public int offset;

	public final int value;

	public Index(int value, int offset) {
		this.value = value - offset;
		this.offset = offset;
	}

	public Index(int value) {
		this.value = value;
	}

	public Integer box() {
		return Integer.valueOf(value);
	}

	public int unbox() {
		return value;
	}

	@Override
	public int intValue() {
		return value + offset;
	}

	@Override
	public long longValue() {
		return intValue();
	}

	@Override
	public float floatValue() {
		return intValue();
	}

	@Override
	public double doubleValue() {
		return intValue();
	}

	@Override
	public boolean equals(Object obj) {
		return (obj instanceof Index) && (value == ((Index)obj).value);
	}

	@Override
	public int compareTo(Index other) {
		return Integer.compare(this.value, other.value);
	}

	@Override
	public int hashCode() {
		return Ints.hashCode(value);
	}

	@Override
	public String toString() {
		return Integer.toString(value + offset);
	}
	
	public void checkElementIndex(String name, int size) {
		if (size < 0) throw new IllegalArgumentException("Negative size: " + size);

		if (value < 0) throw new IndexOutOfBoundsException(String.format("%s (%d) must be at least %d", name, value + offset, offset));
		if (value >= size) throw new IndexOutOfBoundsException(String.format("%s (%d) must be less than %d", name, value + offset, size + offset));
	}

	public void checkElementIndex(int size) {
		checkElementIndex("index", size);
	}

}
