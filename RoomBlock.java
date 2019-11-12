package Reika.ArchiSections;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;


public class RoomBlock extends Block {

	private IIcon iconBottom;
	private IIcon iconTop;

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
	public IIcon getIcon(int s, int meta) {
		switch(s) {
			case 0:
				return iconBottom;
			case 1:
				return iconTop;
			default:
				return blockIcon;
		}
	}

	@Override
	public void registerBlockIcons(IIconRegister ico) {
		blockIcon = ico.registerIcon("archisections:controller");
		iconTop = ico.registerIcon("archisections:controller_top");
		iconBottom = ico.registerIcon("archisections:controller_bottom");
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

	@Override
	public TileEntity createTileEntity(World world, int meta) {
		switch(meta) {
			case 0:
				return new TileRoomController();
			default:
				return null;
		}
	}

}
