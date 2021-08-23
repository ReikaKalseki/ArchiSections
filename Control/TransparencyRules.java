package Reika.ArchiSections.Control;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.function.Function;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLeavesBase;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import Reika.ArchiSections.ArchiSections;
import Reika.DragonAPI.Exception.UnreachableCodeException;
import Reika.DragonAPI.IO.ReikaFileReader;
import Reika.DragonAPI.Instantiable.LogicalCombination;
import Reika.DragonAPI.Instantiable.LogicalCombination.EvaluatorConstructor;
import Reika.DragonAPI.Instantiable.Data.Immutable.BlockKey;
import Reika.DragonAPI.Instantiable.IO.LuaBlock;
import Reika.DragonAPI.Instantiable.IO.LuaBlock.LuaBlockDatabase;
import Reika.DragonAPI.Instantiable.IO.SimpleConfig;
import Reika.DragonAPI.Libraries.Java.ReikaJavaLibrary;
import Reika.DragonAPI.Libraries.Java.ReikaStringParser;
import Reika.DragonAPI.Libraries.Logic.LogicalOperators;
import Reika.DragonAPI.Libraries.World.ReikaWorldHelper;
import Reika.DragonAPI.ModRegistry.InterfaceCache;

public class TransparencyRules implements EvaluatorConstructor<Block> {

	public static final TransparencyRules instance = new TransparencyRules();

	private final TreeMap<BlockKey, TransparencyRule> data = new TreeMap();
	private final HashMap<BlockKey, Boolean> overrides = new HashMap();

	private LogicalCombination<Block> opacityRules;

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
			ArrayList<String> li = ReikaFileReader.getFileAsLines(f, true, Charset.defaultCharset());
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
			LuaBlock lb = data.getRootBlock();
			if (lb.getChildren().size() != 1) {

			}
			opacityRules.populate(lb.getChildren().iterator().next(), this);
			ArchiSections.logger.log("Parsed opacity logic tree:\n"+opacityRules);
			//ReikaJavaLibrary.pConsole(opacityRules.toString());
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
			this.calculateForBlock(b);
		}
	}

	private void calculateForBlock(Block b) {
		boolean def = this.calculateDefaultOpacity(b);
		for (int i = 0; i < 16; i++) {
			BlockKey bk = new BlockKey(b, i);
			TransparencyRule tr = new TransparencyRule(bk, def);
			Boolean override = overrides.get(bk);
			if (override != null)
				tr.isOpaque = override.booleanValue();
			data.put(bk, tr);
		}
	}

	public ArrayList<String> getData() {
		ArrayList<String> ret = new ArrayList();
		for (TransparencyRule e : data.values()) {
			ret.add(e.toString());
		}
		return ret;
	}

	private boolean calculateDefaultOpacity(Block b) {
		try {
			return opacityRules.apply(b);
		}
		catch (Exception e) {
			ArchiSections.logger.logError("Block "+b+" threw exception computing transparency rule!");
			e.printStackTrace();
			return false;
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
		TransparencyRule tr = this.getForBlock(b, meta);
		if (tr == null) {
			ArchiSections.logger.logError("Block "+b+" with meta "+meta+" has null transparency rule!");
			this.calculateForBlock(b);
			return true;
		}
		return tr.isOpaque();
	}

	private static enum OpacityChecks {
		OPAQUECUBE("Block isOpaqueCube() returns true"),
		NORMALRENDER("Block renderAsNormalBlock() returns true"),
		RENDERTYPEZERO("Block has render type of zero (standard cube)"),
		FULLLIGHTOPACITY("Block has total light opacity"),
		ZEROLIGHTOPACITY("Block has zero light opacity"),
		OPAQUEMATERIAL("Block material is marked as opaque"),
		AIRMATERIAL("Block has air material type"),
		LIQUIDMATERIAL("Block has liquid material type"),
		ISLEAVES("Block is leaves"),
		ISPIPE("Block is pipe/conduit/cable/duct");

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
				case ISLEAVES:
					return b instanceof BlockLeavesBase;
				case ISPIPE:
					return InterfaceCache.BCPIPEBLOCK.instanceOf(b) || InterfaceCache.TDDUCTBLOCK.instanceOf(b) || InterfaceCache.AECABLEBLOCK.instanceOf(b) || InterfaceCache.EIOCONDUITBLOCK.instanceOf(b);
			}
			throw new UnreachableCodeException();
		}
	}

	private static class OpacityProperty implements Function<Block, Boolean> {

		private final OpacityChecks call;

		private OpacityProperty(OpacityChecks o) {
			call = o;
		}

		@Override
		public Boolean apply(Block b) {
			try {
				return call.evaluate(b);
			}
			catch (Exception e) {
				throw new RuntimeException("Error thrown when checking "+call+" property of block "+b, e);
			}
		}

		@Override
		public String toString() {
			return call.name();
		}

	}

	@Override
	public Function<Block, Boolean> create(String s) {
		return new OpacityProperty(OpacityChecks.valueOf(s));
	}

	public static class TransparencyRule {

		public final BlockKey block;
		public final boolean isDefaultOpaque;
		private boolean isOpaque;

		private TransparencyRule(BlockKey b, boolean def) {
			block = b;
			isDefaultOpaque = def;
			isOpaque = isDefaultOpaque;
		}

		public boolean isOpaque() {
			return isOpaque;
		}

		public boolean isDefault() {
			return isOpaque == isDefaultOpaque;
		}

		@Override
		public String toString() {
			String name = StatCollector.translateToLocal(block.blockID.getLocalizedName());
			String ret = block.toString()+" ["+Block.getIdFromBlock(block.blockID)+":"+block.metadata+"] ["+name+"] - "+(this.isOpaque() ? "Opaque" : "Transparent");
			if (!this.isDefault())
				ret = ret+" * OVERRIDE";
			return ret;
		}

	}

}
