package openperipheral.api.adapter.method;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import openperipheral.api.adapter.Asynchronous;
import openperipheral.api.adapter.IAdapter;

/**
 * Used to mark methods that should be visible in script.
 *
 * When used in inline adapters (defined inside class), accepted arguments are
 * <ul>
 * <li>Argument annotated with {@link Env} - for implementation specific details</li>
 * <li>Argument annotated with {@link Arg} - for Lua visible arguments</li>
 * </ul>
 *
 * When used in external adapters (defined in {@link IAdapter}, accepted arguments are
 * <ul>
 * <li>target - should be target class (see {@link IAdapter#getTargetClass()} or superclass</li>
 * <li>Argument annotated with {@link Env} - for implementation specific details</li>
 * <li>Argument annotated with {@link Arg} - for Lua visible arguments</li>
 * </ul>
 *
 * @see VarReturn
 * @see Asynchronous
 * @see Arg
 * @see Env
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ScriptCallable {
	public static final String USE_METHOD_NAME = "[none set]";

	/**
	 * Name visible in Lua. Default will use Java name. More names can be defined with {@link Alias}
	 */
	String name() default USE_METHOD_NAME;

	String description() default "";

}
