package Reika.ArchiSections;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import Reika.DragonAPI.IO.ReikaFileReader;
import Reika.DragonAPI.Instantiable.Data.Immutable.BlockKey;
import Reika.DragonAPI.Instantiable.IO.SimpleConfig;
import Reika.DragonAPI.Libraries.Java.ReikaStringParser;
import Reika.DragonAPI.Libraries.World.ReikaWorldHelper;

public class TransparencyRules {

	public static final TransparencyRules instance = new TransparencyRules();

	private final HashMap<OpacityChecks, OpacityCondition> opacitySettings = new HashMap();

	private final HashMap<BlockKey, TransparencyRule> data = new HashMap();
	private final HashMap<BlockKey, Boolean> overrides = new HashMap();

	private TransparencyRules() {

	}

	public void loadSettings(SimpleConfig cfg) {
		for (OpacityChecks oc : OpacityChecks.values()) {
			String tag = oc.name()+" ["+oc.desc+"]";
			OpacityCondition val = cfg.getBoolean("Opacity Settings", tag, oc.defaultValue);
		}
		opaqueIfOpaque = cfg.getBoolean("Opacity Settings", "Opaque if isOpaqueCube", opaqueIfOpaque);
		opaqueIfNormalRender = cfg.getBoolean("Opacity Settings", "Opaque if renderAsNormalBlock", opaqueIfNormalRender);
		opaqueIfRenderTypeZero = cfg.getBoolean("Opacity Settings", "Opaque if Render Type Zero", opaqueIfRenderTypeZero);
		opaqueIfMaterialOpaque = cfg.getBoolean("Opacity Settings", "Opaque if Material isOpaque", opaqueIfMaterialOpaque);

		File f = new File(cfg.getConfigFolder(), "ArchiSections_Opacity_Overrides.cfg");
		try {
			if (f.exists()) {
				ArrayList<String> li = ReikaFileReader.getFileAsLines(f, true);
			}
			else {
				f.createNewFile();
				ArrayList<String> li = new ArrayList();
				li.add(ReikaStringParser.getNOf("#", 60));
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
				li.add(ReikaStringParser.getNOf("#", 60));
				ReikaFileReader.writeLinesToFile(f, li, true);
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
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
		boolean ret = false;
		if (b.isOpaqueCube() && opaqueIfOpaque)
			ret = true;
		if (b.renderAsNormalBlock() && opaqueIfNormalRender)
			ret = true;
		if (b.getRenderType() == 0 && opaqueIfRenderTypeZero)
			ret = true;
		if (b.getMaterial().isOpaque() && opaqueIfMaterialOpaque)
			ret = true;
		return ret;
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

	private static enum OpacityCondition {
		REQUIRED("Required for the block to be opaque"),
		ONEOF(""),
		IGNORED("Ignored; does not affect opacity"),
		DISALLOWED("Must not be true for the block to be opaque");

		public final String desc;

		private OpacityCondition(String s) {
			desc = s;
		}
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
