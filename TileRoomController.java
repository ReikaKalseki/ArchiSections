package Reika.ArchiSections;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import Reika.ArchiSections.Control.RoomSettings;
import Reika.ArchiSections.Control.TransparencyRules;
import Reika.DragonAPI.Base.TileEntityBase;
import Reika.DragonAPI.Instantiable.Data.Immutable.BlockBox;
import Reika.DragonAPI.Libraries.Registry.ReikaParticleHelper;
import Reika.RotaryCraft.API.Interfaces.Screwdriverable;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;


public class TileRoomController extends TileEntityBase implements Screwdriverable {

	private static final int MAX_SIZE = 40;

	private BlockBox bounds;
	private int ticksSinceRoomSet;
	//private UUID currentRoom;

	private final RoomSettings settings = new RoomSettings();

	@Override
	public boolean allowTickAcceleration() {
		return false;
	}

	@Override
	public void updateEntity(World world, int x, int y, int z, int meta) {
		if (!world.isRemote && this.getTicksExisted()%8 == 0) {
			if (bounds == null) {
				this.getDimensions(world, x, y, z);
			}
			else if (this.getTicksExisted()%128 == 0) {
				this.validate(world);
			}
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

	@SideOnly(Side.CLIENT)
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
					RoomTracker.instance.forceAllowParticles = true;
					ReikaParticleHelper.REDSTONE.spawnAt(world, px, py, pz);
					RoomTracker.instance.forceAllowParticles = false;
				}
				d++;
				dx = x+dir.offsetX*d;
				dy = y+dir.offsetY*d;
				dz = z+dir.offsetZ*d;
			}
		}
	}

	private void getDimensions(World world, int x, int y, int z) {
		BlockBox old = bounds;
		bounds = null;
		int[] dists = new int[6];
		for (int i = 0; i < 6; i++) {
			dists[i] = MAX_SIZE;
			ForgeDirection dir = ForgeDirection.VALID_DIRECTIONS[i];
			for (int d = 1; d <= MAX_SIZE; d++) {
				int dx = x+dir.offsetX*d;
				int dy = y+dir.offsetY*d;
				int dz = z+dir.offsetZ*d;
				if (TransparencyRules.instance.isOpaqueForRoom(world, dx, dy, dz)) {
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
		if (ArchiSections.requireSolidWalls) {
			if (!this.checkForSolidWalls(world)) {
				bounds = null;
			}
		}
		if (bounds.equals(old))
			return;
		if (world.isRemote) {
			this.assignRoom();
		}
		else {
			this.triggerBlockUpdate();
		}
	}

	@SideOnly(Side.CLIENT)
	private void assignRoom() {
		if (bounds != null)
			RoomTracker.instance.addRoom(this, bounds);
		else
			RoomTracker.instance.removeRoom(this);
	}

	private boolean checkForSolidWalls(World world) {
		BlockBox box2 = bounds.contract(1, 1, 1);
		for (int x = bounds.minX; x <= bounds.maxX; x++) {
			for (int y = bounds.minY; y <= bounds.maxY; y++) {
				for (int z = bounds.minZ; z <= bounds.maxZ; z++) {
					if (box2.isBlockInside(x, y, z))
						continue;
					if (y == bounds.minY || y == bounds.maxY) {
						if (x == bounds.minX || x == bounds.maxX)
							continue;
						if (z == bounds.minZ || z == bounds.maxZ)
							continue;
					}
					if (x == bounds.minX || x == bounds.maxX) {
						if (z == bounds.minZ || z == bounds.maxZ)
							continue;
					}
					//ReikaJavaLibrary.pConsole(x+","+y+","+z);
					if (!TransparencyRules.instance.isOpaqueForRoom(world, x, y, z)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private void validate(World world) {
		this.getDimensions(world, xCoord, yCoord, zCoord);
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

		NBTTagCompound tag = new NBTTagCompound();
		settings.writeToNBT(tag);
		NBT.setTag("settings", tag);
	}

	@Override
	protected void readSyncTag(NBTTagCompound NBT) {
		super.readSyncTag(NBT);
		bounds = null;
		if (NBT.hasKey("room")) {
			NBTTagCompound tag = NBT.getCompoundTag("room");
			bounds = BlockBox.readFromNBT(tag);
		}

		if (NBT.hasKey("settings")) {
			NBTTagCompound tag = NBT.getCompoundTag("settings");
			settings.readFromNBT(tag);
		}

		if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT)
			this.assignRoom();
	}

	@Override
	public void writeToNBT(NBTTagCompound NBT) {
		super.writeToNBT(NBT);
	}

	@Override
	public void readFromNBT(NBTTagCompound NBT) {
		super.readFromNBT(NBT);
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

	public RoomSettings getSettings() {
		return settings;
	}

}
