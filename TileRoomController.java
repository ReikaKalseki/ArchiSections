package Reika.ArchiSections;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import Reika.DragonAPI.Base.TileEntityBase;
import Reika.DragonAPI.Instantiable.Data.Immutable.BlockBox;
import Reika.DragonAPI.Libraries.Registry.ReikaParticleHelper;
import Reika.RotaryCraft.API.Interfaces.Screwdriverable;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;


public class TileRoomController extends TileEntityBase implements Screwdriverable {

	private static final int MAX_SIZE = 40;

	private BlockBox bounds;
	private int ticksSinceRoomSet;
	//private UUID currentRoom;

	@Override
	public boolean allowTickAcceleration() {
		return false;
	}

	@Override
	public void updateEntity(World world, int x, int y, int z, int meta) {
		if (!world.isRemote && bounds == null) {
			this.getDimensions(world, x, y, z);
		}
		if (world.isRemote && bounds != null && (ticksSinceRoomSet < 40 || this.hasRedstoneSignal())) {
			this.doParticles(world, x, y, z);
		}
		ticksSinceRoomSet++;
	}

	@Override
	protected void onAdjacentBlockUpdate() {
		this.getDimensions(worldObj, xCoord, yCoord, zCoord);
	}

	@Override
	protected void onPositiveRedstoneEdge() {
		this.getDimensions(worldObj, xCoord, yCoord, zCoord);
	}

	private void doParticles(World world, int x, int y, int z) {
		double f = (this.getTileEntityAge()%20)/20D;
		for (int i = 0; i < 6; i++) {
			ForgeDirection dir = ForgeDirection.VALID_DIRECTIONS[i];
			int d = 0;
			int dx = x+dir.offsetX*d;
			int dy = y+dir.offsetY*d;
			int dz = z+dir.offsetZ*d;
			while (bounds.isBlockInside(dx, dy, dz)) {
				int x2 = x+dir.offsetX*(d+2);
				int y2 = y+dir.offsetY*(d+2);
				int z2 = z+dir.offsetZ*(d+2);
				if (bounds.isBlockInside(x2, y2, z2)) {
					double px = dx+0.5;
					double py = dy+0.5;
					double pz = dz+0.5;
					switch(dir) {
						case DOWN:
							py = dy-f;
							break;
						case UP:
							py = dy+1+f;
							break;
						case WEST:
							px = dx-f;
							break;
						case EAST:
							px = dx+1+f;
							break;
						case NORTH:
							pz = dz-f;
							break;
						case SOUTH:
							pz = dz+1+f;
							break;
						default:
							break;
					}
					ReikaParticleHelper.REDSTONE.spawnAt(world, px, py, pz);
				}
				d++;
				dx = x+dir.offsetX*d;
				dy = y+dir.offsetY*d;
				dz = z+dir.offsetZ*d;
			}
		}
	}

	private void getDimensions(World world, int x, int y, int z) {
		if (world.isRemote)
			return;
		int[] dists = new int[6];
		for (int i = 0; i < 6; i++) {
			dists[i] = MAX_SIZE;
			ForgeDirection dir = ForgeDirection.VALID_DIRECTIONS[i];
			for (int d = 1; d <= MAX_SIZE; d++) {
				int dx = x+dir.offsetX*d;
				int dy = y+dir.offsetY*d;
				int dz = z+dir.offsetZ*d;
				if (ArchiSections.isOpaqueForRoom(world, dx, dy, dz)) {
					//bounds = bounds.clamp(dir, xCoord, yCoord, zCoord, d-1);
					dists[i] = d-1;
					//ReikaJavaLibrary.pConsole("Found limit "+d+" at "+dir);
					break;
				}
			}
		}
		int minx = x-1-dists[ForgeDirection.WEST.ordinal()];
		int miny = y-1-dists[ForgeDirection.DOWN.ordinal()];
		int minz = z-1-dists[ForgeDirection.NORTH.ordinal()];
		int maxx = x+1+dists[ForgeDirection.EAST.ordinal()];
		int maxy = y+1+dists[ForgeDirection.UP.ordinal()];
		int maxz = z+1+dists[ForgeDirection.SOUTH.ordinal()];
		bounds = new BlockBox(minx, miny, minz, maxx, maxy, maxz);
		if (world.isRemote)
			RoomTracker.instance.addRoom(this, bounds);
		else
			this.triggerBlockUpdate();
	}

	public void setRoom(Room r) {
		//currentRoom = r != null ? r.id : null;
		bounds = r != null ? r.volume : null;
		ticksSinceRoomSet = 0;
	}

	@Override
	protected void writeSyncTag(NBTTagCompound NBT) {
		super.writeSyncTag(NBT);
		if (bounds != null) {
			NBTTagCompound tag = new NBTTagCompound();
			bounds.writeToNBT(tag);
			NBT.setTag("room", tag);
		}
	}

	@Override
	protected void readSyncTag(NBTTagCompound NBT) {
		super.readSyncTag(NBT);
		if (NBT.hasKey("room")) {
			NBTTagCompound tag = NBT.getCompoundTag("room");
			bounds = BlockBox.readFromNBT(tag);
			if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT)
				RoomTracker.instance.addRoom(this, bounds);
		}
	}

	@Override
	public Block getTileEntityBlockID() {
		return ArchiSections.roomBlock;
	}

	@Override
	protected void animateWithTick(World world, int x, int y, int z) {

	}

	@Override
	public int getRedstoneOverride() {
		return 0;
	}

	@Override
	protected String getTEName() {
		return "Room Controller";
	}

	@Override
	public boolean shouldRenderInPass(int pass) {
		return false;
	}

	@Override
	public boolean onShiftRightClick(World world, int x, int y, int z, ForgeDirection side) {
		bounds = null;
		return true;
	}

	@Override
	public boolean onRightClick(World world, int x, int y, int z, ForgeDirection side) {
		this.getDimensions(world, x, y, z);
		return true;
	}

}
