package openperipheral.adapter;

import java.util.Map;

public interface IMethodExecutor {
	public IMethodDescription description();

	public IMethodCall startCall(Object target);

	public boolean isAsynchronous();

	public boolean canInclude(String architecture);

	public Map<String, Class<?>> requiredEnv();
}
