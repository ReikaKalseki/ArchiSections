package Reika.ArchiSections;

import net.minecraft.command.ICommandSender;

import Reika.DragonAPI.Command.DragonClientCommand;


public class ChunkRoomToggleCommand extends DragonClientCommand {

	public static int dynamicChunkRadius;
	public static int dynamicChunkRadiusY;

	@Override
	public void processCommand(ICommandSender ics, String[] args) {
		int r = 0;
		int ry = r;
		switch(args.length) {
			case 1:
				r = ry = Integer.parseInt(args[0]);
				break;
			case 2:
				r = Integer.parseInt(args[0]);
				ry = Integer.parseInt(args[1]);
				break;
		}
		if (dynamicChunkRadius >= 0) {
			dynamicChunkRadius = -1;
			this.sendChatToSender(ics, "Dynamic chunk room disabled.");
		}
		else {
			dynamicChunkRadius = r;
			this.sendChatToSender(ics, "Dynamic chunk room enabled with radius "+r+" chunks horizontal and "+ry+" vertical.");
		}
	}

	@Override
	public String getCommandString() {
		return "chunkroom";
	}

}
