package Reika.ArchiSections;

import java.util.ArrayList;

import net.minecraft.command.ICommandSender;

import Reika.DragonAPI.Command.DragonCommandBase;
import Reika.DragonAPI.Libraries.Java.ReikaJavaLibrary;


public class DumpOpacityDataCommand extends DragonCommandBase {

	@Override
	public void processCommand(ICommandSender ics, String[] args) {
		ArrayList<String> li = TransparencyRules.instance.getData();
		for (String s : li) {
			this.sendChatToSender(ics, s);
			ReikaJavaLibrary.pConsole(s);
		}
	}

	@Override
	public String getCommandString() {
		return "dumpopacitycalc";
	}

	@Override
	protected boolean isAdminOnly() {
		return false;
	}

}
