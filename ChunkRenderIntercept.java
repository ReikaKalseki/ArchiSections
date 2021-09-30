package Reika.ArchiSections;

import net.minecraft.client.renderer.WorldRenderer;

import Reika.ArchiSections.Control.CullingTypes;
import Reika.DragonAPI.Instantiable.Event.Client.ChunkWorldRenderEvent.ChunkWorldRenderWatcher;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ChunkRenderIntercept implements ChunkWorldRenderWatcher {

	public static final ChunkRenderIntercept instance = new ChunkRenderIntercept();

	private ChunkRenderIntercept() {

	}

	@Override
	public boolean interceptChunkRender(WorldRenderer wr, int renderPass, int GLListID) {
		Room r = RoomTracker.instance.getActiveRoom();
		if (r == null) { //do not bother culling if you are not in a room

		}
		else {
			if (!r.getSettings().isCulling(CullingTypes.CHUNK))
				return false;
			if (!r.contains(wr)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int chunkRenderSortIndex() {
		return 0;
	}

}
