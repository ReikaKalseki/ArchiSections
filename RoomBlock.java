package Reika.ArchiSections;

import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import Reika.ArchiSections.Control.CullingTypes;
import Reika.ArchiSections.Control.RoomSettings;
import Reika.DragonAPI.Base.BlockTEBase;
import Reika.DragonAPI.Instantiable.Data.Immutable.BlockBounds;
import Reika.DragonAPI.Libraries.IO.ReikaSoundHelper;


public class RoomBlock extends BlockTEBase {

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
		switch(meta) {
			case 1: //ceiling
				if (s <= 1)
					s = 1-s;
				break;
			case 3:
				if (s == 2)
					s = 1;
				else if (s == 1)
					s = 2;
				break;
			case 4:
				if (s == 3)
					s = 1;
				else if (s == 1)
					s = 3;
				break;
			case 5:
				if (s == 4)
					s = 1;
				else if (s == 1)
					s = 4;
				break;
			case 6:
				if (s == 5)
					s = 1;
				else if (s == 1)
					s = 5;
				break;
		}
		if (meta > 2 && s > 1) {
			s = 0;
		}
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
		return 0;
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
		return true;
	}

	@Override
	public TileEntity createTileEntity(World world, int meta) {
		return new TileRoomController();
	}

	@Override
	public final void setBlockBoundsForItemRender() {
		this.setFullBlockBounds();
	}

	@Override
	public final void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
		AxisAlignedBB box = this.getBoundingBox(world, x, y, z);
		this.setBounds(box, x, y, z);
	}

	@Override
	public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
		AxisAlignedBB box = this.getCollisionBoundingBoxFromPool(world, x, y, z);
		this.setBounds(box, x, y, z);
		return box;
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z)  {
		AxisAlignedBB box = this.getBoundingBox(world, x, y, z);
		this.setBounds(box, x, y, z);
		return box;
	}

	private AxisAlignedBB getBoundingBox(IBlockAccess world, int x, int y, int z) {
		BlockBounds bounds = BlockBounds.block();
		int meta = world.getBlockMetadata(x, y, z);
		if (meta > 0) {
			ForgeDirection dir = ForgeDirection.VALID_DIRECTIONS[meta-1];
			bounds = bounds.cut(dir, 0.8125);
			for (int i = 0; i < 6; i++) {
				ForgeDirection dir2 = ForgeDirection.VALID_DIRECTIONS[i];
				if (dir != dir2 && dir != dir2.getOpposite()) {
					bounds = bounds.cut(dir2, 0.125);
				}
			}
		}
		return bounds.asAABB(x, y, z);
	}

	@Override
	public final boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer ep, int par6, float a, float b, float c) {
		if (world.isRemote)
			return true;
		TileRoomController te = (TileRoomController)world.getTileEntity(x, y, z);
		RoomSettings rs = te.getSettings();
		rs.toggleCullingType(ep.isSneaking() ? CullingTypes.TILE : CullingTypes.CHUNK);
		ReikaSoundHelper.playSoundFromServerAtBlock(world, x, y, z, "random.click", 0.5F, 0.66F, true);
		return true;
	}

}
