package openperipheral.common.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import openperipheral.OpenPeripheral;
import openperipheral.api.IAttachable;
import openperipheral.api.IMethodDefinition;
import openperipheral.api.IRestriction;
import openperipheral.common.converter.TypeConversionRegistry;
import openperipheral.common.definition.DefinitionManager;
import openperipheral.common.postchange.PostChangeRegistry;
import openperipheral.common.util.StringUtils;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ModContainer;
import dan200.computer.api.IComputerAccess;
import dan200.computer.api.IHostedPeripheral;

public class HostedPeripheral implements IHostedPeripheral {

	static class MySecurityManager extends SecurityManager {
		public String getCallerClassName(int callStackDepth) {
			return getClassContext()[callStackDepth].getName();
		}
	}

	private final static MySecurityManager mySecurityManager = new MySecurityManager();

	private World worldObj;
	private Class klass;
	private int x;
	private int y;
	private int z;
	private String name;
	private ArrayList<IMethodDefinition> methods;
	private String[] methodNames;

	public HostedPeripheral(TileEntity tile) {
		klass = tile.getClass();

		worldObj = tile.worldObj;
		x = tile.xCoord;
		y = tile.yCoord;
		z = tile.zCoord;
		methods = DefinitionManager.getMethodsForTile(tile);
		ArrayList<String> mNames = new ArrayList<String>();

		mNames.add("listMethods");
		for (IMethodDefinition method : methods) {
			mNames.add(method.getLuaName());
		}
		methodNames = mNames.toArray(new String[mNames.size()]);

		Block blockType = tile.getBlockType();
		
		ItemStack is = new ItemStack(blockType, 1, blockType.getDamageValue(tile.worldObj, tile.xCoord, tile.yCoord, tile.zCoord));

		try {
			name = is.getDisplayName();
		} catch (Exception e) {
			try {
				name = is.getItemName();
			} catch (Exception e2) {
			}
		}

		if (name == null) {
			name = tile.getClass().getName();
		}

		name = name.replace(".", "_");
		name = name.replace(" ", "_");
		name = name.toLowerCase();
	}

	@Override
	public String getType() {
		return name;
	}

	@Override
	public String[] getMethodNames() {
		return methodNames;
	}

	@Override
	public Object[] callMethod(IComputerAccess computer, int methodId, Object[] arguments) throws Exception {

		if (methodId == 0) {
			return new Object[] { StringUtils.join(getMethodNames(), "\n") };
		}

		methodId--;

		String callerClass = mySecurityManager.getCallerClassName(2);

		boolean isCableCall = callerClass.equals("dan200.computer.shared.TileEntityCable$RemotePeripheralWrapper")
				|| callerClass.equals("openperipheral.common.tileentity.TileEntityProxy");

		final IMethodDefinition methodDefinition = methods.get(methodId);
		if (methodDefinition != null) {

			if (!methodDefinition.needsSanitize()) {
				final TileEntity tile = worldObj.getBlockTileEntity(x, y, z);
				return executeMethod(isCableCall || methodDefinition.isInstant(), methodDefinition, tile, arguments);
			}

			ArrayList<Object> args = new ArrayList(Arrays.asList(arguments));

			Class[] requiredParameters = methodDefinition.getRequiredParameters();
			
			if (requiredParameters != null) {

				replaceArguments(args, methodDefinition.getReplacements());
				
				if (args.size() != requiredParameters.length) {
					int replacements = 0;
					if (methodDefinition.getReplacements() != null) {
						replacements = methodDefinition.getReplacements().size();
					}
					throw new Exception("Invalid number of parameters. Expected " + (requiredParameters.length - replacements));
				}
	
				for (int i = 0; i < requiredParameters.length; i++) {
					Object converted = TypeConversionRegistry.fromLua(args.get(i), requiredParameters[i]);
					if (converted == null) {
						throw new Exception("Invalid parameter number " + (i + 1));
					}
					args.set(i, converted);
				}
			}

			for (int i = 0; i < args.size(); i++) {
				ArrayList<IRestriction> restrictions = methodDefinition.getRestrictions(i);
				if (restrictions != null) {
					for (IRestriction restriction : restrictions) {
						if (!restriction.isValid(args.get(i))) {
							throw new Exception(restriction.getErrorMessage(i + 1));
						}
					}
				}
			}

			final TileEntity tile = worldObj.getBlockTileEntity(x, y, z);

			final Object[] argsToUse = args.toArray(new Object[args.size()]);

			return executeMethod(isCableCall || methodDefinition.isInstant(), methodDefinition, tile, argsToUse);
		}

		return null;

	}

	private Object[] executeMethod(boolean isInstant, final IMethodDefinition methodDefinition, final TileEntity tile, final Object[] argsToUse) throws Exception {
		if (isInstant) {
			Object response = TypeConversionRegistry.toLua(methodDefinition.execute(tile, argsToUse));
			PostChangeRegistry.onPostChange(tile, methodDefinition, argsToUse);
			return new Object[] { response };
		} else {
			Future callback = TickHandler.addTickCallback(tile.worldObj, new Callable() {
				@Override
				public Object call() throws Exception {
					Object response = TypeConversionRegistry.toLua(methodDefinition.execute(tile, argsToUse));
					PostChangeRegistry.onPostChange(tile, methodDefinition, argsToUse);
					return response;
				}
			});
			return new Object[] { callback.get() };
		}
	}

	private void replaceArguments(ArrayList<Object> args, HashMap<Integer, String> replacements) {
		if (replacements == null) {
			return;
		}
		for (Entry<Integer, String> replacement : replacements.entrySet()) {
			String r = replacement.getValue();
			Object v = null;
			if (r.equals("x")) {
				v = x;
			} else if (r.equals("y")) {
				v = y;
			} else if (r.equals("z")) {
				v = z;
			} else if (r.equals("world")) {
				v = worldObj;
			}
			if (v != null) {
				args.add(replacement.getKey(), v);
			}
		}
	}

	@Override
	public boolean canAttachToSide(int side) {
		return true;
	}

	@Override
	public void attach(final IComputerAccess computer) {
		ModContainer container = FMLCommonHandler.instance().findContainerFor(OpenPeripheral.instance);
		try {
			computer.unmount("openp");
		} catch (Exception e) {
		}
		computer.mountFixedDir("openp", String.format("openperipheral/lua", container.getVersion()), true, 0);
		try {
			TickHandler.addTickCallback(worldObj, new Callable() {
				@Override
				public Object call() throws Exception {
					TileEntity tile = worldObj.getBlockTileEntity(x, y, z);
					if (tile != null && tile instanceof IAttachable) {
						((IAttachable) tile).addComputer(computer);
					}
					return null;
				}
			});
		} catch (InterruptedException e) {
		}
	}

	@Override
	public void detach(final IComputerAccess computer) {

		try {
			TickHandler.addTickCallback(worldObj, new Callable() {
				@Override
				public Object call() throws Exception {
					TileEntity tile = worldObj.getBlockTileEntity(x, y, z);
					if (tile != null && tile instanceof IAttachable) {
						((IAttachable) tile).removeComputer(computer);
					}
					return null;
				}
			});
		} catch (InterruptedException e) {
		}

	}

	@Override
	public void update() {
	}

	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound) {
	}

	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound) {
	}

}
