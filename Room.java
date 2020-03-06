package Reika.ArchiSections;

import java.util.HashSet;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;

import Reika.ArchiSections.Control.RoomSettings;
import Reika.DragonAPI.Instantiable.Data.Immutable.BlockBox;
import Reika.DragonAPI.Instantiable.Data.Immutable.Coordinate;

public class Room {

	public final int dimensionID;
	public final UUID id;
	public final BlockBox volume;
	public final AxisAlignedBB bounds;

	private final HashSet<Coordinate> chunkSet = new HashSet();

	private Coordinate controller;

	private RoomSettings settings = new RoomSettings();

	Room(int dim, BlockBox box) {
		this(dim, UUID.randomUUID(), box);
	}

	private Room(int dim, UUID uid, BlockBox box) {
		dimensionID = dim;
		id = uid;
		volume = box;
		bounds = box.asAABB();

		int x0 = volume.minX >> 4;
		int x1 = volume.maxX >> 4;
		int y0 = volume.minY >> 4;
		int y1 = volume.maxY >> 4;
		int z0 = volume.minZ >> 4;
		int z1 = volume.maxZ >> 4;
		x0 -= ArchiSections.chunkBufferXZ;
		y0 -= ArchiSections.chunkBufferY;
		z0 -= ArchiSections.chunkBufferXZ;
		x1 += ArchiSections.chunkBufferXZ;
		y1 += ArchiSections.chunkBufferY;
		z1 += ArchiSections.chunkBufferXZ;
		for (int x = x0; x <= x1; x++) {
			for (int y = y0; y <= y1; y++) {
				for (int z = z0; z <= z1; z++) {
					chunkSet.add(new Coordinate(x << 4, y << 4, z << 4));
				}
			}
		}

		//ReikaJavaLibrary.pConsole(this);
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Room && ((Room)o).id.equals(id);
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	public boolean contains(WorldRenderer wr) {
		Coordinate c = new Coordinate(wr);
		return chunkSet.contains(c);
	}

	@Override
	public String toString() {
		return id.toString()+" = "+bounds.toString();
	}

	public NBTTagCompound writeToNBT() {
		NBTTagCompound ret = new NBTTagCompound();
		ret.setInteger("dim", dimensionID);
		ret.setString("id", id.toString());
		NBTTagCompound tag = new NBTTagCompound();
		volume.writeToNBT(tag);
		ret.setTag("box", tag);
		return ret;
	}

	public static Room readFromNBT(NBTTagCompound tag) {
		UUID id = UUID.fromString(tag.getString("id"));
		BlockBox box = BlockBox.readFromNBT(tag.getCompoundTag("box"));
		return new Room(tag.getInteger("dim"), id, box);
	}

	void setController(TileRoomController te) {
		controller = new Coordinate(te);
		settings = te.getSettings();
	}

	public TileRoomController getController() {
		TileEntity te = controller != null ? controller.getTileEntity(Minecraft.getMinecraft().theWorld) : null;
		return te instanceof TileRoomController ? (TileRoomController)te : null;
	}

	public void reloadSettings() {
		TileRoomController te = this.getController();
		if (te != null)
			settings = te.getSettings();
	}

	public Coordinate getControllerLocation() {
		return controller;
	}

	public boolean isValid() {
		return this.getController() != null;
	}

	public RoomSettings getSettings() {
		return settings;
	}

}
