package Reika.ArchiSections;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;


public class RoomBlock extends Block {

	public RoomBlock() {
		super(Material.iron);
		this.setLightLevel(0);
		this.setLightOpacity(0);
		this.setResistance(6000);
		this.setHardness(1.5F);

		this.setBlockName("roomcontrol");

		this.setCreativeTab(CreativeTabs.tabRedstone);
	}

	@Override
	public int damageDropped(int meta) {
		return meta;
	}

	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public boolean hasTileEntity(int meta) {
		return meta == 0;
	}

	public TileEntity createTileEntity(World world, int meta) {
		switch(meta) {
			case 0:
				return new TileRoomController();
			default:
				return null;
		}
	}

}
