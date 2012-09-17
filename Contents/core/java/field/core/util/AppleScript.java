package field.core.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * replaces the NSAppleScript classes that we can't use in Java 1.6
 * 
 * @author marc
 * 
 */
public class AppleScript {

	private ExecuteCommand cc;

	public AppleScript(String command, boolean sync) {
		try {
			File tmp = File.createTempFile("field", "");
			tmp.deleteOnExit();

			BufferedWriter w = new BufferedWriter(new FileWriter(tmp));
			w.append(command + "\n");
			w.close();

			cc = new ExecuteCommand(".", new String[] { "/usr/bin/osascript", tmp.getAbsolutePath() }, true);

			if (sync) {
				cc.waitFor(true);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getOutput() {
		cc.waitFor(true);
		String o = cc.getOutput();
		
		
		return scrub(o);
	}

	private String scrub(String o) {
		StringBuffer b = new StringBuffer(o.length());
		for(int i=0;i<o.length();i++)
		{
			char c = o.charAt(i);
			if ((int)c<128)
			{
				b.append(c);
			}
		}
		return b.toString();
	}

	public String getError() {
		cc.waitFor(true);
		return scrub(cc.getOutput());
	}

}
