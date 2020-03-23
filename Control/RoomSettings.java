package Reika.ArchiSections.Control;

import java.util.EnumSet;
import java.util.HashSet;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;

import Reika.ArchiSections.ArchiSections;
import Reika.DragonAPI.Libraries.ReikaNBTHelper.NBTTypes;

public class RoomSettings {

	private final HashSet<CullingTypes> culling;
	private int chunkBufferXZ = ArchiSections.chunkBufferXZ;
	private int chunkBufferY = ArchiSections.chunkBufferY;

	public RoomSettings() {
		culling = new HashSet(EnumSet.allOf(CullingTypes.class));
		if (!ArchiSections.interceptSounds)
			culling.remove(CullingTypes.SOUND);
	}

	public void toggleCullingType(CullingTypes type) {
		if (this.isCulling(type)) {
			culling.remove(type);
		}
		else {
			culling.add(type);
		}
	}

	public boolean isCulling(CullingTypes type) {
		return culling.contains(type);
	}

	public void readFromNBT(NBTTagCompound tag) {
		if (tag.hasKey("types")) {
			culling.clear();
			NBTTagList li = tag.getTagList("types", NBTTypes.STRING.ID);
			for (Object o : li.tagList) {
				String s = ((NBTTagString)o).func_150285_a_();
				culling.add(CullingTypes.valueOf(s));
			}
		}

		if (tag.hasKey("bxz"))
			chunkBufferXZ = tag.getInteger("bxz");
		if (tag.hasKey("by"))
			chunkBufferY = tag.getInteger("by");
	}

	public void writeToNBT(NBTTagCompound tag) {
		NBTTagList li = new NBTTagList();
		for (CullingTypes c : culling) {
			li.appendTag(new NBTTagString(c.toString()));
		}
		tag.setTag("types", li);

		tag.setInteger("bxz", chunkBufferXZ);
		tag.setInteger("by", chunkBufferY);
	}

}
