package openperipheral.interfaces.cc.executors;

import openperipheral.adapter.IMethodExecutor;
import openperipheral.interfaces.cc.ComputerCraftEnv;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;

public abstract class PeripheralExecutor<T> {
	protected final Object[] call(IMethodExecutor executor, T target, IComputerAccess computer, ILuaContext context, Object[] args) throws Exception {
		return ComputerCraftEnv.addPeripheralArgs(executor.startCall(target), computer, context).call(args);

	}

	public abstract Object[] execute(IMethodExecutor executor, T target, IComputerAccess computer, ILuaContext context, Object[] args) throws Exception;
}
