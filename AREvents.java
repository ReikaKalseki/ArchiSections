package Reika.ArchiSections;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.client.event.sound.PlaySoundEvent17;

import Reika.DragonAPI.Instantiable.Event.Client.ClientLogoutEvent;
import Reika.DragonAPI.Instantiable.Event.Client.EntityRenderEvent;
import Reika.DragonAPI.Instantiable.Event.Client.EntityRenderEvent.EntityRenderWatcher;
import Reika.DragonAPI.Instantiable.Event.Client.RenderItemStackEvent;
import Reika.DragonAPI.Instantiable.Event.Client.SinglePlayerLogoutEvent;
import Reika.DragonAPI.Instantiable.Event.Client.TileEntityRenderEvent;
import Reika.DragonAPI.Instantiable.Event.Client.TileEntityRenderEvent.TileRenderWatcher;
import Reika.DragonAPI.Instantiable.Rendering.StructureRenderer;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class AREvents implements EntityRenderWatcher, TileRenderWatcher {

	public static final AREvents instance = new AREvents();

	private AREvents() {
		EntityRenderEvent.addListener(this);
		TileEntityRenderEvent.addListener(this);
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void interceptSound(PlaySoundEvent17 evt) {
		if (!ArchiSections.interceptSounds)
			return;
		if (Minecraft.getMinecraft().theWorld == null)
			return;
		Room r = RoomTracker.instance.getRoomForPos(Minecraft.getMinecraft().theWorld.provider.dimensionId, evt.sound.getXPosF(), evt.sound.getYPosF(), evt.sound.getZPosF());
		if (!RoomTracker.instance.isActiveRoom(r)) {
			evt.result = null;
		}
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void clearCache(SinglePlayerLogoutEvent evt) {
		RoomTracker.instance.clear();
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void clearCache(ClientDisconnectionFromServerEvent evt) {
		RoomTracker.instance.clear();
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void clearCache(ClientLogoutEvent evt) {
		RoomTracker.instance.clear();
	}
	/*
	@SubscribeEvent
	public void clearCache(PlayerChangedDimensionEvent evt) {
		RoomTracker.instance.clear();
	}*/
	/*
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void interceptEntityRender(AddParticleEvent evt) {

	}*/

	@SideOnly(Side.CLIENT)
	public boolean preTileRender(TileEntitySpecialRenderer tesr, TileEntity te, double par2, double par4, double par6, float par8) {
		if (te.worldObj == null || StructureRenderer.isRenderingTiles())
			return false;
		if (RenderItemStackEvent.runningItemRender) //for items which call TESRs or entity renders
			return false;
		if (ArchiSections.disableOnSneak && Minecraft.getMinecraft().thePlayer.isSneaking())
			return false;
		if (!RoomTracker.instance.isInActiveRoom(te))
			return true;
		return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void postTileRender(TileEntitySpecialRenderer tesr, TileEntity te, double par2, double par4, double par6, float par8) {

	}

	@SideOnly(Side.CLIENT)
	public boolean tryRenderEntity(Render r, Entity e, double par2, double par4, double par6, float par8, float par9) {
		if (e.worldObj == null)
			return false;
		if (e instanceof EntityItem)
			return false;
		if (RenderItemStackEvent.runningItemRender) //for items which call TESRs or entity renders
			return false;
		if (ArchiSections.disableOnSneak && Minecraft.getMinecraft().thePlayer.isSneaking())
			return false;
		if (!RoomTracker.instance.isInActiveRoom(e))
			return true;
		return false;
	}

	@Override
	public int watcherSortIndex() {
		return Integer.MIN_VALUE;
	}

}
