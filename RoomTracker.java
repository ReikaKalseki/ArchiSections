package Reika.ArchiSections;

import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import Reika.DragonAPI.Instantiable.Data.Immutable.BlockBox;
import Reika.DragonAPI.Instantiable.Data.Immutable.Coordinate;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class RoomTracker {

	public static final RoomTracker instance = new RoomTracker();

	private final HashMap<UUID, Room> rooms = new HashMap();
	private final HashMap<Coordinate, UUID> cache = new HashMap();

	private RoomTracker() {
		//this.addRoom(new BlockBox(-20, 50, -20, 20, 80, 20));
	}

	/*
	public void addRoom(TileRoomController te, Room r) {
		r = this.getDuplicateRoom(r, r.volume);
		rooms.put(r.id, r);
		this.recalculateRoomCache();
		this.setRoom(te, r);
	}
	 */
	public void addRoom(TileRoomController te, BlockBox box) {
		Room r = new Room(te.worldObj.provider.dimensionId, box);
		r = this.getDuplicateRoom(r, box);
		rooms.put(r.id, r);
		this.recalculateRoomCache();
		this.setRoom(te, r);
	}

	private Room getDuplicateRoom(Room r, BlockBox box) {
		for (Room rm : rooms.values()) {
			if (rm.volume.equals(box))
				return rm;
		}
		return r;
	}

	private void setRoom(TileRoomController te, Room r) {
		te.setRoom(r);
		r.setController(te);
	}

	public void removeRoom(Room r) {
		this.removeRoom(r.id);
	}

	public void removeRoom(UUID id) {
		Room r = rooms.remove(id);
		if (r != null) {
			TileRoomController te = r.getController();
			te.setRoom(null);
		}
		this.recalculateRoomCache();
	}

	public void clear() {
		rooms.clear();
		//this.addRoom(new BlockBox(-20, 50, -20, 20, 80, 20));
		this.recalculateRoomCache();
	}

	@SideOnly(Side.CLIENT)
	public Room getActiveRoom() {
		//this.clear();
		if (ChunkRoomToggleCommand.dynamicChunkRadius >= 0) {
			return this.getDynamicChunkRoom(Minecraft.getMinecraft().thePlayer);
		}
		return this.getRoomForEntity(Minecraft.getMinecraft().thePlayer);
	}

	private Room getDynamicChunkRoom(EntityPlayer ep) {
		int r = ChunkRoomToggleCommand.dynamicChunkRadius;
		int ry = ChunkRoomToggleCommand.dynamicChunkRadiusY;
		int x = MathHelper.floor_double(ep.posX) >> 4;
		int y = MathHelper.floor_double(ep.posY) >> 4;
		int z = MathHelper.floor_double(ep.posZ) >> 4;
		int x0 = x-r;
		int y0 = y-ry;
		int z0 = z-r;
		int x1 = x+r;
		int y1 = y+ry;
		int z1 = z+r;
		return new Room(ep.worldObj.provider.dimensionId, new BlockBox(x0, y0, z0, x1, y1, z1));
	}

	public Room getRoomForEntity(Entity e) {
		return this.getRoomForPos(e.worldObj.provider.dimensionId, e.posX, e.posY, e.posZ);
	}

	public Room getRoomForTile(TileEntity e) {
		return this.getRoomForPos(e.worldObj.provider.dimensionId, e.xCoord, e.yCoord, e.zCoord);
	}

	public Room getRoomForPos(int dim, double x, double y, double z) {
		Iterator<Room> it = rooms.values().iterator();
		while (it.hasNext()) {
			Room r = it.next();
			if (r.dimensionID == dim) {
				if (r.isValid()) {
					if (r.bounds.isVecInside(Vec3.createVectorHelper(x, y, z))) {
						return r;
					}
				}
				else {
					it.remove();
				}
			}
		}
		return null;
	}

	public boolean isActiveRoom(Room r) {
		Room act = this.getActiveRoom();
		return act == null ? r == null : act.equals(r);
	}

	private void recalculateRoomCache() {

	}

	public Room getRoom(UUID id) {
		return rooms.get(id);
	}

}
