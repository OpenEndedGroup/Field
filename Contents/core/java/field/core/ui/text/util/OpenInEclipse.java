package field.core.ui.text.util;

import field.core.util.AppleScript;

public class OpenInEclipse {

	public OpenInEclipse(String name)
	{
		String keys = "";
		for(int i=0;i<name.length();i++)
		{
			char c = name.charAt(i);
			keys += "keystroke \""+c+"\"\n";
		}
		String command = "tell application \"System Events\"\n" +
			"	tell application \"Eclipse\" to activate\n" +
			"	tell application \"Eclipse\" to tell window 2 to activate\n" +
			"	keystroke \"t\" using {command down, shift down}\n" +
			keys+
			"      delay 0.75\n"+
				"	key code 36\n" +
				"end tell\n" +
				"";
		;//System.out.println(" command is <"+command+">");
		AppleScript script = new AppleScript(command, false);
	}

	public OpenInEclipse(String name, int line)
	{
		String keys = "";
		for(int i=0;i<name.length();i++)
		{
			char c = name.charAt(i);
			keys += "keystroke \""+c+"\"\n";
		}

		String lines= "";
		String ll = ""+line;
		for(int i=0;i<ll.length();i++)
		{
			char c = ll.charAt(i);
			lines+= "keystroke \""+c+"\"\n";
		}
		
		String command = "tell application \"System Events\"\n" +
			"	tell application \"Eclipse\" to activate\n" +
			"	tell application \"Eclipse\" to tell window 2 to activate\n" +
				"	keystroke \"r\" using {command down, shift down}\n" +
				"      delay 0.25\n"+
				keys+
				"      delay 0.5\n"+
				"	key code 36\n" +
				"delay 1\n"+
				" keystroke \"l\" using command down\n"+
				" delay 0.5\n"+
				lines+
				"	key code 36\n" +
				"end tell\n" +
				"";
		;//System.out.println(" command is <"+command+">");
		AppleScript script = new AppleScript(command, false);

		//script.execute(null);
	}

}
