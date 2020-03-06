package Reika.ArchiSections.Control;

import java.util.EnumSet;

import net.minecraft.nbt.NBTTagCompound;

import Reika.ArchiSections.ArchiSections;

public class RoomSettings {

	private final EnumSet<CullingTypes> culling = EnumSet.allOf(CullingTypes.class);
	private int chunkBufferXZ = ArchiSections.chunkBufferXZ;
	private int chunkBufferY = ArchiSections.chunkBufferY;

	public RoomSettings() {
		if (!ArchiSections.interceptSounds)
			culling.remove(CullingTypes.SOUND);
	}

	public void readFromNBT(NBTTagCompound tag) {

	}

	public void writeToNBT(NBTTagCompound tag) {

	}

}
