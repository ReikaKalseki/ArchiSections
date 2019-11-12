/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.ArchiSections;

import java.io.File;
import java.net.URL;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import Reika.DragonAPI.DragonAPICore;
import Reika.DragonAPI.DragonOptions;
import Reika.DragonAPI.Auxiliary.Trackers.CommandableUpdateChecker;
import Reika.DragonAPI.Base.DragonAPIMod;
import Reika.DragonAPI.Base.DragonAPIMod.LoadProfiler.LoadPhase;
import Reika.DragonAPI.Instantiable.Event.Client.ChunkWorldRenderEvent;
import Reika.DragonAPI.Instantiable.Event.Client.ClientLogoutEvent;
import Reika.DragonAPI.Instantiable.Event.Client.EntityRenderEvent;
import Reika.DragonAPI.Instantiable.Event.Client.SinglePlayerLogoutEvent;
import Reika.DragonAPI.Instantiable.Event.Client.TileEntityRenderEvent;
import Reika.DragonAPI.Instantiable.IO.ModLogger;
import Reika.DragonAPI.Instantiable.IO.SimpleConfig;
import Reika.DragonAPI.Instantiable.Rendering.StructureRenderer;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;

@Mod( modid = "ArchiSections", name="ArchiSections", version = "v@MAJOR_VERSION@@MINOR_VERSION@", certificateFingerprint = "@GET_FINGERPRINT@", dependencies="required-after:DragonAPI")

public class ArchiSections extends DragonAPIMod {

	@Instance("ArchiSections")
	public static ArchiSections instance = new ArchiSections();

	public static ModLogger logger;

	public static final SimpleConfig config = new SimpleConfig(instance);

	public static RoomBlock roomBlock;

	@Override
	@EventHandler
	public void preload(FMLPreInitializationEvent evt) {
		this.startTiming(LoadPhase.PRELOAD);
		this.verifyInstallation();
		logger = new ModLogger(instance, false);
		if (DragonOptions.FILELOG.getState())
			logger.setOutput("**_Loading_Log.log");

		roomBlock = new RoomBlock();
		GameRegistry.registerBlock(roomBlock, "room");
		LanguageRegistry.addName(roomBlock, "Room Controller");

		FMLCommonHandler.instance().bus().register(this);
		this.basicSetup(evt);
		this.finishTiming();
	}

	@Override
	@EventHandler
	public void load(FMLInitializationEvent event) {
		this.startTiming(LoadPhase.LOAD);
		GameRegistry.addShapedRecipe(new ItemStack(roomBlock, 2, 0),
				"rgr", "sis", "srs", 'i', Items.iron_ingot, 's', Blocks.stone, 'r', Items.redstone, 'g', Items.glowstone_dust);
		this.finishTiming();
	}

	@Override
	@EventHandler
	public void postload(FMLPostInitializationEvent evt) {
		this.startTiming(LoadPhase.POSTLOAD);
		this.finishTiming();
	}

	@Override
	public String getDisplayName() {
		return "RenderRooms";
	}

	@Override
	public String getModAuthorName() {
		return "Reika";
	}

	@Override
	public URL getDocumentationSite() {
		return DragonAPICore.getReikaForumPage();
	}

	@Override
	public String getWiki() {
		return null;
	}

	@Override
	public String getUpdateCheckURL() {
		return CommandableUpdateChecker.reikaURL;
	}

	@Override
	public ModLogger getModLogger() {
		return logger;
	}

	@Override
	public File getConfigFolder() {
		return config.getConfigFolder();
	}

	@SubscribeEvent
	public void interceptChunkRender(ChunkWorldRenderEvent evt) {
		Room r = RoomTracker.instance.getActiveRoom();
		if (r == null) { //do not bother culling if you are not in a room

		}
		else {
			if (!r.contains(evt.renderer)) {
				evt.setCanceled(true);
			}
		}
	}

	@SubscribeEvent
	public void interceptTileRender(TileEntityRenderEvent evt) {
		Room r = RoomTracker.instance.getRoomForTile(evt.tileEntity);
		if (evt.tileEntity.worldObj == null || StructureRenderer.isRenderingTiles())
			return;
		if (!RoomTracker.instance.isActiveRoom(r))
			evt.setCanceled(true);
	}

	@SubscribeEvent
	public void interceptEntityRender(EntityRenderEvent evt) {
		Room r = RoomTracker.instance.getRoomForEntity(evt.entity);
		if (evt.entity.worldObj == null)
			return;
		if (!RoomTracker.instance.isActiveRoom(r))
			evt.setCanceled(true);
	}

	@SubscribeEvent
	public void clearCache(SinglePlayerLogoutEvent evt) {
		RoomTracker.instance.clear();
	}

	@SubscribeEvent
	public void clearCache(ClientDisconnectionFromServerEvent evt) {
		RoomTracker.instance.clear();
	}

	@SubscribeEvent
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
	public void interceptEntityRender(AddParticleEvent evt) {

	}*/

	public static boolean isOpaqueForRoom(World world, int x, int y, int z) {
		Block b = world.getBlock(x, y, z);
		int meta = world.getBlockMetadata(x, y, z);
		if (b.isAir(world, x, y, z))
			return false;
		return b.isOpaqueCube() || b.renderAsNormalBlock();
	}

}
