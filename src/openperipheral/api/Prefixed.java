package openperipheral.api;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PACKAGE, ElementType.TYPE, ElementType.METHOD })
public @interface Prefixed {

	public String[] value();
}
