package openperipheral.common.integration.vanilla;

import java.util.ArrayList;
import java.util.HashMap;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import openperipheral.api.IMethodDefinition;
import openperipheral.api.IRestriction;

public class InventorySwapMethod implements IMethodDefinition {

	@Override
	public HashMap<Integer, String> getReplacements() {
		return null;
	}

	@Override
	public String getPostScript() {
		return null;
	}

	@Override
	public boolean getCauseTileUpdate() {
		return false;
	}

	@Override
	public Class[] getRequiredParameters() {
		return new Class[] { int.class, int.class };
	}

	@Override
	public boolean isInstant() {
		return false;
	}

	@Override
	public String getLuaName() {
		return "swapStacks";
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public boolean needsSanitize() {
		return true;
	}

	@Override
	public ArrayList<IRestriction> getRestrictions(int index) {
		return null;
	}

	@Override
	public Object execute(TileEntity tile, Object[] args) throws Exception {
		if (tile instanceof IInventory) {
			IInventory invent = (IInventory) tile;
			int from = (Integer)args[0];
			int to = (Integer)args[1];
			if (from >= 0 && from < invent.getSizeInventory() && to >= 0 && to < invent.getSizeInventory()) {
				
				ItemStack stack1 = invent.getStackInSlot(from);
				ItemStack stack2 = invent.getStackInSlot(to);
				
				if (stack1 != null) {
					stack1 = stack1.copy();
				}
				if (stack2 != null) {
					stack2 = stack2.copy();
				}
				
				invent.setInventorySlotContents(from, stack2);
				invent.setInventorySlotContents(to, stack1);
				return true;
			}
			
		}
		return false;
	}

}
