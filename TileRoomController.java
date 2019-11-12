package Reika.ArchiSections;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import Reika.DragonAPI.Instantiable.Data.Immutable.BlockBox;
import Reika.DragonAPI.Libraries.Registry.ReikaParticleHelper;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;


public class TileRoomController extends TileEntity {

	private static final int MAX_SIZE = 40;

	private BlockBox bounds;
	//private UUID currentRoom;

	@Override
	public void updateEntity() {
		boolean red = worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord);
		if (!worldObj.isRemote && (bounds == null || (red && worldObj.getTotalWorldTime()%128 == 0))) {
			this.getDimensions();
		}
		if (worldObj.isRemote && bounds != null && red) {
			this.doParticles();
		}
	}

	private void doParticles() {
		double f = (worldObj.getTotalWorldTime()%20)/20D;
		for (int i = 0; i < 6; i++) {
			ForgeDirection dir = ForgeDirection.VALID_DIRECTIONS[i];
			int d = 0;
			int x = xCoord+dir.offsetX*d;
			int y = yCoord+dir.offsetY*d;
			int z = zCoord+dir.offsetZ*d;
			while (bounds.isBlockInside(x, y, z)) {
				int x2 = xCoord+dir.offsetX*(d+2);
				int y2 = yCoord+dir.offsetY*(d+2);
				int z2 = zCoord+dir.offsetZ*(d+2);
				if (bounds.isBlockInside(x2, y2, z2)) {
					double px = x+0.5;
					double py = y+0.5;
					double pz = z+0.5;
					switch(dir) {
						case DOWN:
							py = y-f;
							break;
						case UP:
							py = y+1+f;
							break;
						case WEST:
							px = x-f;
							break;
						case EAST:
							px = x+1+f;
							break;
						case NORTH:
							pz = z-f;
							break;
						case SOUTH:
							pz = z+1+f;
							break;
						default:
							break;
					}
					ReikaParticleHelper.REDSTONE.spawnAt(worldObj, px, py, pz);
				}
				d++;
				x = xCoord+dir.offsetX*d;
				y = yCoord+dir.offsetY*d;
				z = zCoord+dir.offsetZ*d;
			}
		}
	}

	private void getDimensions() {
		int[] dists = new int[6];
		for (int i = 0; i < 6; i++) {
			dists[i] = MAX_SIZE;
			ForgeDirection dir = ForgeDirection.VALID_DIRECTIONS[i];
			for (int d = 1; d <= MAX_SIZE; d++) {
				int dx = xCoord+dir.offsetX*d;
				int dy = yCoord+dir.offsetY*d;
				int dz = zCoord+dir.offsetZ*d;
				if (ArchiSections.isOpaqueForRoom(worldObj, dx, dy, dz)) {
					//bounds = bounds.clamp(dir, xCoord, yCoord, zCoord, d-1);
					dists[i] = d-1;
					//ReikaJavaLibrary.pConsole("Found limit "+d+" at "+dir);
					break;
				}
			}
		}
		int minx = xCoord-1-dists[ForgeDirection.WEST.ordinal()];
		int miny = yCoord-1-dists[ForgeDirection.DOWN.ordinal()];
		int minz = zCoord-1-dists[ForgeDirection.NORTH.ordinal()];
		int maxx = xCoord+1+dists[ForgeDirection.EAST.ordinal()];
		int maxy = yCoord+1+dists[ForgeDirection.UP.ordinal()];
		int maxz = zCoord+1+dists[ForgeDirection.SOUTH.ordinal()];
		bounds = new BlockBox(minx, miny, minz, maxx, maxy, maxz);
		if (worldObj.isRemote)
			RoomTracker.instance.addRoom(this, bounds);
		else
			worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
	}

	public void setRoom(Room r) {
		//currentRoom = r != null ? r.id : null;
		bounds = r != null ? r.volume : null;
	}

	@Override
	public void writeToNBT(NBTTagCompound NBT) {
		if (bounds != null) {
			NBTTagCompound tag = new NBTTagCompound();
			bounds.writeToNBT(tag);
			NBT.setTag("room", tag);
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound NBT) {
		if (NBT.hasKey("room")) {
			NBTTagCompound tag = NBT.getCompoundTag("room");
			bounds = BlockBox.readFromNBT(tag);
			if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT)
				RoomTracker.instance.addRoom(this, bounds);
		}
	}

	@Override
	public Packet getDescriptionPacket() {
		NBTTagCompound NBT = new NBTTagCompound();
		this.writeToNBT(NBT);
		S35PacketUpdateTileEntity pack = new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, NBT);
		return pack;
	}

	@Override
	public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity p)  {
		this.readFromNBT(p.field_148860_e);
		worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
	}

}
