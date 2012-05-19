package field.core.ui.text;

import java.util.Iterator;
import java.util.TreeSet;

public class PasteTabHelper {

	static public String entab(String text) {
		if (text.trim().length() == 0)
			return text;
		
		text = text.replace("\r", "\n");
		String[] lines = text.split("\n");
		if (lines.length < 2)
			return text;

		TreeSet<Integer> leadings = new TreeSet<Integer>();

		for (int i = 0; i < lines.length; i++) {
			int leading = leadingFor(lines[i]);
			if (leading > 0)
				leadings.add(leading);
		}

		Integer unit = null;

		if (leadings.size() == 0)
			return text;

		if (leadings.size() == 1) {
			unit = leadings.iterator().next();
		} else {
			Iterator<Integer> m = leadings.iterator();
			while (m.hasNext()) {
				Integer g = m.next();
				if (unit == null)
					unit = g;
				else {
					unit = gcd(unit, g);
				}
			}
		}
		
		if (unit == null) return text;
		if (unit == 1) unit = 2;
		

		StringBuilder b = new StringBuilder(unit);
		for (int i = 0; i < unit; i++)
			b.append(" ");

		StringBuilder o = new StringBuilder(text.length());

		for (String s : lines) {
			o.append(s.replaceAll(b.toString(), "\t") + "\n");
		}

		return o.toString();

	}

	private static int leadingFor(String string) {
		int index = 0;
		while (index < string.length() && string.charAt(index) == ' ')
			index++;
		return index;
	}

	static protected int gcd(int x, int y) {
		if (y == 0)
			return x;
		return gcd(y, x % y);
	}

}
