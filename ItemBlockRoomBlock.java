/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.ArchiSections;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

public class ItemBlockRoomBlock extends ItemBlock {

	public ItemBlockRoomBlock(Block b) {
		super(b);
		hasSubtypes = true;
	}

	@Override
	public void getSubItems(Item item, CreativeTabs c, List li) {
		super.getSubItems(item, c, li);
	}

	@Override
	public void addInformation(ItemStack is, EntityPlayer ep, List li, boolean vb) {
		super.addInformation(is, ep, li, vb);
	}

	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer ep, World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ) {
		return super.onItemUse(stack, ep, world, x, y, z, side, hitX, hitY, hitZ);
	}

	@Override
	public final String getItemStackDisplayName(ItemStack is) {
		String s = super.getItemStackDisplayName(is);
		if (is.getItemDamage() == 1) {
			s = s+" (Mini)";
		}
		return s;
	}

	@Override
	public boolean placeBlockAt(ItemStack stack, EntityPlayer ep, World world, int x, int y, int z, int side, float a, float b, float c, int metadata) {
		int placedmeta = 0;
		if (stack.getItemDamage() == 1) {
			placedmeta = ForgeDirection.VALID_DIRECTIONS[side].ordinal()+1;
		}
		if (!world.setBlock(x, y, z, field_150939_a, placedmeta, 3))
			return false;

		if (world.getBlock(x, y, z) == field_150939_a) {
			field_150939_a.onBlockPlacedBy(world, x, y, z, ep, stack);
			field_150939_a.onPostBlockPlaced(world, x, y, z, placedmeta);
		}

		return true;
	}

	@Override
	public int getMetadata(int meta) {
		return meta;
	}

}
