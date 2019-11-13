package Reika.ArchiSections;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import Reika.DragonAPI.Exception.UnreachableCodeException;
import Reika.DragonAPI.IO.ReikaFileReader;
import Reika.DragonAPI.Instantiable.LogicalCombination;
import Reika.DragonAPI.Instantiable.Data.Immutable.BlockKey;
import Reika.DragonAPI.Instantiable.IO.LuaBlock;
import Reika.DragonAPI.Instantiable.IO.LuaBlock.LuaBlockDatabase;
import Reika.DragonAPI.Instantiable.IO.SimpleConfig;
import Reika.DragonAPI.Libraries.Java.ReikaJavaLibrary;
import Reika.DragonAPI.Libraries.Java.ReikaStringParser;
import Reika.DragonAPI.Libraries.Logic.LogicalOperators;
import Reika.DragonAPI.Libraries.World.ReikaWorldHelper;

public class TransparencyRules {

	public static final TransparencyRules instance = new TransparencyRules();

	private final HashMap<BlockKey, TransparencyRule> data = new HashMap();
	private final HashMap<BlockKey, Boolean> overrides = new HashMap();

	private LogicalCombination opacityRules;

	private TransparencyRules() {

	}

	public void loadSettings(SimpleConfig cfg) {
		File f = new File(cfg.getConfigFolder(), "ArchiSections_Opacity_Calculation.cfg");
		opacityRules = new LogicalCombination(LogicalOperators.AND);
		try {
			if (!f.exists()) {
				f.createNewFile();
				InputStream in = ArchiSections.class.getResourceAsStream("OpacityRules.txt");
				OutputStream out = new FileOutputStream(f);
				ReikaFileReader.copyFile(in, out, 256);
			}
			LuaBlockDatabase data = new LuaBlockDatabase();
			data.hasDuplicateKeys = true;
			ArrayList<String> li = ReikaFileReader.getFileAsLines(f, true);
			li.remove(0);
			String s = li.remove(li.size()-1);
			while (s.isEmpty() || s.charAt(0) != '=') {
				s = li.remove(li.size()-1);
			}
			data.loadFromLines(li);
			StringBuilder sb = new StringBuilder();
			for (String s2 : data.getRootBlock().writeToStrings()) {
				sb.append(s2);
				sb.append("\n");
			}
			for (LuaBlock b : data.getRootBlock().getChildren()) {

			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		f = new File(cfg.getConfigFolder(), "ArchiSections_Opacity_Overrides.cfg");
		try {
			if (f.exists()) {
				ArrayList<String> li = ReikaFileReader.getFileAsLines(f, true);
				for (String s : li) {
					try {
						s = ReikaStringParser.stripSpaces(s);
						if (s.isEmpty())
							continue;
						char c = s.charAt(0);
						if (c == '#' || c == '=' || c == '-')
							continue;
						String[] parts = s.split("=");
						if (parts.length != 2)
							ArchiSections.logger.logError("Could not parse specified block opacity '"+s+"' - malformed");
						BlockKey bk = this.tryParse(parts[0]);
						if (bk != null) {
							if (bk.blockID == Blocks.air)
								ArchiSections.logger.logError("Could not parse specified block entry - air cannot be overridden");
							else
								overrides.put(bk, Boolean.parseBoolean(parts[1]));
						}
						else {
							ArchiSections.logger.logError("Could not parse specified block entry - no such block '"+parts[0]+"'");
						}
					}
					catch (Exception e) {
						ArchiSections.logger.logError("Could not parse specified block opacity '"+s+": "+e.getMessage());
						e.printStackTrace();
					}
				}
			}
			else {
				f.createNewFile();
				ArrayList<String> li = new ArrayList();
				li.add(ReikaStringParser.getNOf("=", 60));
				li.add(ReikaStringParser.getNOf("-", 45));
				li.add("# Block Opacity Overrides for Room Boundaries");
				li.add(ReikaStringParser.getNOf("-", 45));
				li.add("# Add blocks to this file, one per line, to override the default opacity calculations.");
				li.add("# Blocks not in this list will use the default calculations.");
				li.add(ReikaStringParser.getNOf("-", 45));
				li.add("# Add blocks in one of four forms:");
				li.add("# <blockID> = <opacity>");
				li.add("# <blockID>:<metadata> = <opacity>");
				li.add("# <blockName> = <opacity>");
				li.add("# <blockName>:<metadata> = <opacity>");
				li.add(ReikaStringParser.getNOf("-", 45));
				li.add("# Examples:");
				li.add("# 46 = TRUE //Marks block ID 46 (TNT) as opaque");
				li.add("# 35:15 = FALSE //Marks block ID 35 with meta 15 (wool, black) as NOT opaque");
				li.add("# RotaryCraft:blastglass = TRUE //Marks RotaryCraft's 'blastglass' block as opaque");
				li.add("# ChromatiCraft:crystalglass:0 = TRUE //Marks meta 0 (black) of 'crystalglass' block from ChromatiCraft as opaque");
				li.add(ReikaStringParser.getNOf("-", 45));
				li.add("# Add new entries below the following line");
				li.add(ReikaStringParser.getNOf("=", 60));
				ReikaFileReader.writeLinesToFile(f, li, true);
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	private BlockKey tryParse(String s) {
		if (s.contains(":")) {
			String[] parts = s.split("$");
			Block b = this.tryParseBlock(parts[0]);
			int meta = Integer.parseInt(parts[1]);
			return b != null ? new BlockKey(b, meta) : null;
		}
		else {
			Block b = this.tryParseBlock(s);
			return b != null ? new BlockKey(b) : null;
		}
	}

	private Block tryParseBlock(String s) {
		Block b = Block.getBlockFromName(s);
		if (b != null)
			return b;
		int id = ReikaJavaLibrary.safeIntParse(s);
		return id < 0 ? null : Block.getBlockById(id);
	}

	public void load() {
		for (Object k : Block.blockRegistry.getKeys()) {
			Block b = (Block)Block.blockRegistry.getObject(k);
			boolean def = this.calculateDefaultOpacity(b);
			for (int i = 0; i < 16; i++) {
				BlockKey bk = new BlockKey(b, i);
				TransparencyRule tr = new TransparencyRule(bk, def);
				tr.isOpaque = tr.isDefaultOpaque;
				Boolean override = overrides.get(bk);
				if (override != null)
					tr.isOpaque = override.booleanValue();
				data.put(bk, tr);
			}
		}
	}

	private boolean calculateDefaultOpacity(Block b) {
		try {
			return opacityRules.call();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public TransparencyRule getForBlock(Block b, int meta) {
		return this.getForBlock(new BlockKey(b, meta));
	}

	public TransparencyRule getForBlock(BlockKey b) {
		return data.get(b);
	}

	public boolean isOpaqueForRoom(World world, int x, int y, int z) {
		Block b = world.getBlock(x, y, z);
		int meta = world.getBlockMetadata(x, y, z);
		if (b.isAir(world, x, y, z) || ReikaWorldHelper.softBlocks(world, x, y, z))
			return false;
		return this.getForBlock(b, meta).isOpaque();
	}

	private static enum OpacityChecks {
		OPAQUECUBE("Block isOpaqueCube() returns true"),
		NORMALRENDER("Block renderAsNormalBlock() returns true"),
		RENDERTYPEZERO("Block has render type of zero (standard cube)"),
		FULLLIGHTOPACITY("Block has total light opacity"),
		ZEROLIGHTOPACITY("Block has zero light opacity"),
		OPAQUEMATERIAL("Block material is marked as opaque"),
		AIRMATERIAL("Block has air material type"),
		LIQUIDMATERIAL("Block has liquid material type");

		public final String desc;
		//private OpacityCondition defaultValue;

		private OpacityChecks(String s/*, OpacityCondition val*/) {
			desc = s;
			//defaultValue = val;
		}

		public boolean evaluate(Block b) {
			switch(this) {
				case AIRMATERIAL:
					return b.getMaterial() == Material.air;
				case FULLLIGHTOPACITY:
					return b.getLightOpacity() == 255;
				case LIQUIDMATERIAL:
					return b.getMaterial().isLiquid();
				case NORMALRENDER:
					return b.renderAsNormalBlock();
				case OPAQUECUBE:
					return b.isOpaqueCube();
				case OPAQUEMATERIAL:
					return b.getMaterial().isOpaque();
				case RENDERTYPEZERO:
					return b.getRenderType() == 0;
				case ZEROLIGHTOPACITY:
					return b.getLightOpacity() == 0;
			}
			throw new UnreachableCodeException();
		}
	}

	private static class OpacityProperty implements Callable<Boolean> {

		private final Block block;
		private final OpacityChecks call;

		private OpacityProperty(Block b, OpacityChecks o) {
			call = o;
			block = b;
		}

		@Override
		public Boolean call() throws Exception {
			return call.evaluate(block);
		}

	}

	public static class TransparencyRule {

		public final BlockKey block;
		public final boolean isDefaultOpaque;
		private boolean isOpaque;

		private TransparencyRule(BlockKey b, boolean def) {
			block = b;
			isDefaultOpaque = def;
		}

		public boolean isOpaque() {
			return isOpaque;
		}

		public boolean isDefault() {
			return isOpaque == isDefaultOpaque;
		}

	}

}
