package field.core.ui.text.util;

import java.net.URLEncoder;

import com.sun.jna.Platform;

import field.core.util.ExecuteCommand;
import field.launch.SystemProperties;

public class OpenInTrac {

	static public void searchFor(String text)
	{
	
		String t = URLEncoder.encode(text);
		if (Platform.isMac())
			new ExecuteCommand(".", new String[]{"open", "https://www.google.com/#hl=en&output=search&sclient=psy-ab&q=site:openendedgroup.com%2Ffield+"+t}, false);
		else
			new ExecuteCommand(".", new String[]{"xdg-open", "https://www.google.com/#hl=en&output=search&sclient=psy-ab&q=site:openendedgroup.com%2Ffield+"+t}, false);

	}
}

