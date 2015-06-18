package openperipheral.api.peripheral;

import java.lang.annotation.*;

/**
 * Peripheral created for types marked with this annotation will not be cached (i.e. one will be created on any call).
 *
 * @deprecated No longer implemented
 */
@Deprecated
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Volatile {

}
