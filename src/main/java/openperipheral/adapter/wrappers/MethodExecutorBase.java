package openperipheral.adapter.wrappers;

import com.google.common.base.Optional;
import java.lang.reflect.Method;
import java.util.Map;
import openperipheral.adapter.AnnotationMetaExtractor;
import openperipheral.adapter.IMethodCall;
import openperipheral.adapter.IMethodDescription;
import openperipheral.adapter.RestrictedMethodExecutor;
import openperipheral.adapter.method.MethodDeclaration;

public abstract class MethodExecutorBase extends RestrictedMethodExecutor {

	private final MethodDeclaration decl;

	private final boolean isAsynchronous;

	private final Optional<String> returnSignal;

	public MethodExecutorBase(MethodDeclaration decl, Method method, AnnotationMetaExtractor info) {
		super(info.getExcludedArchitectures(method), info.getFeatureGroups(method));
		this.decl = decl;
		this.isAsynchronous = info.isAsync(method);
		this.returnSignal = info.getReturnSignal(method);
	}

	@Override
	public IMethodDescription description() {
		return decl;
	}

	@Override
	public boolean isAsynchronous() {
		return isAsynchronous;
	}

	@Override
	public Optional<String> getReturnSignal() {
		return returnSignal;
	}

	@Override
	public Map<String, Class<?>> requiredEnv() {
		return decl.getOptionalArgs();
	}

	@Override
	public IMethodCall startCall(Object target) {
		return decl.startCall(target);
	}

}
