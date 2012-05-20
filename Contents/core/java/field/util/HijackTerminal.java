package field.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

import field.core.util.ExecuteCommand;

public class HijackTerminal {

	static public final String ttypname = "/var/tmp/field_tty";

	static public void hijackTerminal(boolean forceNew) {
		hijackTerminal(forceNew, 0);
	}

	static protected void hijackTerminal(boolean forceNew, int c) {

		if (c>4)
			return;

		if (!forceNew)
			if (!new File(ttypname).exists())
				forceNew = true;
		if (!forceNew)
		{
			try {
				forceNew = true;

				BufferedReader b;
				b = new BufferedReader(new FileReader(new File(ttypname)));
				String line = b.readLine();
				b.close();


				File nf = new File(line.trim());
				if (nf.canWrite())
				{
					System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(nf))));
					System.setErr(new PrintStream((new FileOutputStream(nf))));
					return;
				}
				else
				{
					;//System.out.println(" can't write file ?");
				}

			} catch (FileNotFoundException e) {
			} catch (IOException e) {
			}
		}

		String cmd = "/usr/bin/open " + (new File(".").getAbsolutePath()) + "/whichtty_command.terminal";
		ExecuteCommand ec = new ExecuteCommand(".", cmd);
		int done = ec.waitFor();

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		hijackTerminal(false, c+1);
	}


}
