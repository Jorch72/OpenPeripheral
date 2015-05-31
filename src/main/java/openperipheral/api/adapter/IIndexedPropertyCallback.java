package openperipheral.api.adapter;

import java.lang.reflect.Field;

/**
 *
 * Interface used to receive callback from generated Lua accessors.
 *
 * @see CallbackProperty
 */
public interface IIndexedPropertyCallback {
	public void setField(Field field, Object index, Object value);

	public Object getField(Field field, Object index);
}
