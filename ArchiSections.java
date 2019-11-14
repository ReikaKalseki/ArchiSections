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

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.ClientCommandHandler;

import Reika.DragonAPI.DragonAPICore;
import Reika.DragonAPI.DragonOptions;
import Reika.DragonAPI.Auxiliary.Trackers.CommandableUpdateChecker;
import Reika.DragonAPI.Base.DragonAPIMod;
import Reika.DragonAPI.Base.DragonAPIMod.LoadProfiler.LoadPhase;
import Reika.DragonAPI.Extras.ThrottleableEffectRenderer;
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
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@Mod( modid = "ArchiSections", name="ArchiSections", version = "v@MAJOR_VERSION@@MINOR_VERSION@", certificateFingerprint = "@GET_FINGERPRINT@", dependencies="required-after:DragonAPI")

public class ArchiSections extends DragonAPIMod {

	@Instance("ArchiSections")
	public static ArchiSections instance = new ArchiSections();

	public static ModLogger logger;

	public static final SimpleConfig config = new SimpleConfig(instance);

	public static RoomBlock roomBlock;

	public static boolean requireSolidWalls;

	@Override
	@EventHandler
	public void preload(FMLPreInitializationEvent evt) {
		this.startTiming(LoadPhase.PRELOAD);
		this.verifyInstallation();

		logger = new ModLogger(instance, false);
		if (DragonOptions.FILELOG.getState())
			logger.setOutput("**_Loading_Log.log");

		config.loadSubfolderedConfigFile(evt);
		config.loadDataFromFile(evt);
		requireSolidWalls = config.getBoolean("Options", "Require Solid Walls", false);
		config.finishReading();

		roomBlock = new RoomBlock();
		GameRegistry.registerBlock(roomBlock, "room");
		LanguageRegistry.addName(roomBlock, "Room Controller");
		GameRegistry.registerTileEntity(TileRoomController.class, "RoomController");

		FMLCommonHandler.instance().bus().register(this);
		this.basicSetup(evt);
		this.finishTiming();
	}

	@Override
	@EventHandler
	public void load(FMLInitializationEvent event) {
		this.startTiming(LoadPhase.LOAD);
		TransparencyRules.instance.loadSettings(config);
		GameRegistry.addShapedRecipe(new ItemStack(roomBlock, 2, 0),
				"rgr", "sis", "srs", 'i', Items.iron_ingot, 's', Blocks.cobblestone, 'r', Items.redstone, 'g', Items.glowstone_dust);
		this.finishTiming();
	}

	@Override
	@EventHandler
	public void postload(FMLPostInitializationEvent evt) {
		this.startTiming(LoadPhase.POSTLOAD);
		TransparencyRules.instance.load();

		if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
			this.doClientSetup();
		}

		this.finishTiming();
	}

	private void doClientSetup() {
		ClientCommandHandler.instance.registerCommand(new ChunkRoomToggleCommand());
		ThrottleableEffectRenderer.getRegisteredInstance().addSpawnHandler(RoomTracker.instance);
	}

	@EventHandler
	public void registerCommands(FMLServerStartingEvent evt) {
		evt.registerServerCommand(new DumpOpacityDataCommand());
	}

	@Override
	public String getDisplayName() {
		return "ArchiSections";
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
	@SideOnly(Side.CLIENT)
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
	@SideOnly(Side.CLIENT)
	public void interceptTileRender(TileEntityRenderEvent evt) {
		if (evt.tileEntity.worldObj == null || StructureRenderer.isRenderingTiles())
			return;
		Room r = RoomTracker.instance.getRoomForTile(evt.tileEntity);
		if (!RoomTracker.instance.isActiveRoom(r))
			evt.setCanceled(true);
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void interceptEntityRender(EntityRenderEvent evt) {
		if (evt.entity.worldObj == null)
			return;
		Room r = RoomTracker.instance.getRoomForEntity(evt.entity);
		if (!RoomTracker.instance.isActiveRoom(r))
			evt.setCanceled(true);
	}

	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void clearCache(SinglePlayerLogoutEvent evt) {
		RoomTracker.instance.clear();
	}

	@SubscribeEvent
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

}
