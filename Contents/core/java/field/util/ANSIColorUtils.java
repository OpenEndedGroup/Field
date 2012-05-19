package field.util;

import field.core.Platform;

/**
 * old skhool.
 * 
 * @author marc <I>Created on Mar 19, 2003</I>
 */
public class ANSIColorUtils {

	static public final char esc = (char) 0x1b;

	static public String red(String s) {
		if (Platform.getOS() != Platform.OS.mac)
			return s;
		return esc + "[31m" + s + esc + "[0m";
	}

	static public String blue(String s) {
		if (Platform.getOS() != Platform.OS.mac)
			return s;
		return esc + "[34m" + s + esc + "[0m";
	}

	static public String green(String s) {
		if (Platform.getOS() != Platform.OS.mac)
			return s;
		return esc + "[32m" + s + esc + "[0m";
	}

	static public String yellow(String s) {
		if (Platform.getOS() != Platform.OS.mac)
			return s;
		return esc + "[33m" + s + esc + "[0m";
	}

	/**
	 * for use with \r. for example
	 * System.out.print(eraseLine()+" status = "+i+" \r");
	 * 
	 * @return
	 */
	static public String eraseLine() {
		if (Platform.getOS() != Platform.OS.mac)
			return "\n";
		return esc + "[K";
	}

	static public String eraseScreen() {
		if (Platform.getOS() != Platform.OS.mac)
			return "\n";
		return esc + "[2J";
	}
}
