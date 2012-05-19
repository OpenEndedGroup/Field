package field.core.ui.text.util;

import java.net.URLEncoder;

import field.core.util.ExecuteCommand;
import field.launch.SystemProperties;

public class OpenInTrac {

	static public String openendedgroupTracURL = SystemProperties.getProperty("openendedgroupTracURL", "http://openendedgroup.com:8000/field");


	static public void searchFor(String text)
	{
		String t = URLEncoder.encode(text);
		new ExecuteCommand(".", new String[]{"open", openendedgroupTracURL+"/search?q="+t+"&noquickjump=1&ticket=on&discussion=on&changeset=on&wiki=on"}, false);
	}
}

