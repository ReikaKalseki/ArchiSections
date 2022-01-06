package Reika.ArchiSections;

import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import Reika.ArchiSections.Command.ChunkRoomToggleCommand;
import Reika.ArchiSections.Control.CullingTypes;
import Reika.DragonAPI.Extras.ThrottleableEffectRenderer.ParticleSpawnHandler;
import Reika.DragonAPI.Instantiable.Data.Immutable.BlockBox;
import Reika.DragonAPI.Instantiable.Data.Immutable.Coordinate;
import Reika.DragonAPI.Libraries.ReikaAABBHelper;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RoomTracker implements ParticleSpawnHandler {

	public static final RoomTracker instance = new RoomTracker();

	private final HashMap<UUID, Room> rooms = new HashMap();
	private final HashMap<Coordinate, UUID> cache = new HashMap();

	public boolean forceAllowParticles = false;

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

	public void removeRoom(TileRoomController te) {
		this.removeRoom(cache.get(new Coordinate(te)));
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
		return this.getRoomForEntity(Minecraft.getMinecraft().thePlayer);
	}

	@SideOnly(Side.CLIENT)
	private Room getDynamicChunkRoom() {
		EntityPlayer ep = Minecraft.getMinecraft().thePlayer;
		int r = ChunkRoomToggleCommand.dynamicChunkRadius;
		int ry = ChunkRoomToggleCommand.dynamicChunkRadiusY;
		int x = MathHelper.floor_double(ep.posX) >> 4;
		int y = MathHelper.floor_double(ep.posY) >> 4;
		int z = MathHelper.floor_double(ep.posZ) >> 4;
		int x0 = x-r;
		int y0 = y-ry;
		int z0 = z-r;
		int x1 = x+r+1;
		int y1 = y+ry+1;
		int z1 = z+r+1;
		return new Room(ep.worldObj.provider.dimensionId, new BlockBox(x0 << 4, y0 << 4, z0 << 4, (x1 << 4) - 1, (y1 << 4) - 1, (z1 << 4) - 1));
	}

	@Deprecated
	private Room getRoomForEntity(Entity e) {
		return this.getRoomForPos(e.worldObj.provider.dimensionId, e.posX, e.posY, e.posZ);
	}

	@Deprecated
	private Room getRoomForTile(TileEntity e) {
		return this.getRoomForPos(e.worldObj.provider.dimensionId, e.xCoord, e.yCoord, e.zCoord);
	}

	public Room getRoomForPos(int dim, double x, double y, double z) {
		Vec3 pos = Vec3.createVectorHelper(x, y, z);
		if (ChunkRoomToggleCommand.dynamicChunkRadius >= 0) {
			Room r = this.getDynamicChunkRoom();
			if (r.dimensionID == dim && r.bounds.isVecInside(pos)) {
				return r;
			}
		}
		Iterator<Room> it = rooms.values().iterator();
		while (it.hasNext()) {
			Room r = it.next();
			if (r.dimensionID == dim) {
				if (r.isValid()) {
					if (r.bounds.isVecInside(pos)) {
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

	public boolean isInActiveRoom(TileEntity e) {
		//return this.isInActiveRoom(e.worldObj, e.xCoord, e.yCoord, e.zCoord, CullingTypes.TILE);
		Room act = this.getActiveRoom();
		if (act != null) {
			return act.bounds.intersectsWith(e.getRenderBoundingBox());
		}
		else {
			return this.existsOutsideRooms(e.worldObj.provider.dimensionId, e.getRenderBoundingBox());
		}
	}

	private boolean existsOutsideRooms(int dim, AxisAlignedBB aabb) {
		Iterator<Room> it = rooms.values().iterator();
		while (it.hasNext()) {
			Room r = it.next();
			if (r.dimensionID == dim) {
				if (r.isValid()) {
					if (ReikaAABBHelper.fullyContains(r.bounds, aabb)) {
						return false;
					}
				}
				else {
					it.remove();
				}
			}
		}
		return true;
	}

	public boolean isInActiveRoom(Entity e) {
		return this.isInActiveRoom(e.worldObj, e.posX, e.posY, e.posZ, CullingTypes.ENTITY);
	}

	public boolean isInActiveRoom(World world, double x, double y, double z, CullingTypes type) {
		Room act = this.getActiveRoom();
		if (act != null) {
			if (!act.getSettings().isCulling(type))
				return true;
			return act.dimensionID == world.provider.dimensionId && act.bounds.isVecInside(Vec3.createVectorHelper(x, y, z));
		}
		else {
			return this.getRoomForPos(world.provider.dimensionId, x, y, z) == null;
		}
	}

	private void recalculateRoomCache() {
		cache.clear();
		for (Room r : rooms.values()) {
			cache.put(r.getControllerLocation(), r.id);
		}
	}

	public Room getRoom(UUID id) {
		return rooms.get(id);
	}

	@Override
	public boolean cancel(EntityFX fx) {
		if (forceAllowParticles)
			return false;
		return !this.isActiveRoom(this.getRoomForEntity(fx));
	}

}
