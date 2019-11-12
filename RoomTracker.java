package Reika.ArchiSections;

import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;

import Reika.DragonAPI.Instantiable.Data.Immutable.BlockBox;
import Reika.DragonAPI.Instantiable.Data.Immutable.Coordinate;

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

	public Room getActiveRoom() {
		//this.clear();
		return this.getRoomForEntity(Minecraft.getMinecraft().thePlayer);
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
