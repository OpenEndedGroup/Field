package field.core.ui.text.util;

public class IndentationUtils {

	static public String deindent(String a) {
		StringBuffer out = new StringBuffer();
		String[] lines = a.split("\n");
		for (int i = 0; i < lines.length; i++) {
			int n = numTabIndent(lines[i]);
			if (n > 1) {
				lines[i] = lines[i].substring(1, lines[i].length());
			}
		}
		for (int i = 0; i < lines.length; i++) {
			out.append(lines[i]);
			if (i < lines.length - 1)
				out.append("\n");
		}
		return out.toString();
	}

	static public String indent(String a) {
		StringBuffer out = new StringBuffer();
		String[] lines = a.split("\n");
		for (int i = 0; i < lines.length; i++) {
			lines[i] = "\t" + lines[i];
		}
		for (int i = 0; i < lines.length; i++) {
			out.append(lines[i]);
			if (i < lines.length - 1)
				out.append("\n");
		}
		return out.toString();
	}

	static public String indentTo(int to, String lines) {
		
		int l = numTabIndent(lines);

		if (l == to)
			return lines;
		int lastl = to;
		while (l > to) {
			lines = deindent(lines);
			l = numTabIndent(lines);
			if (l == lastl)
				break;
			lastl = l;
		}
		lastl = to;
		while (l < to) {
			lines = indent(lines);
			l = numTabIndent(lines);
			if (l == lastl)
				break;
			lastl = l;
		}
		return lines;
	}

	static public int numTabIndent(String a) {
		int l = 0;
		boolean found = false;
		for (int i = 0; i < a.length(); i++) {
			if (a.charAt(i) == '\t')
				l++;
			else if (a.charAt(i) == '\n') {
				l = 0;
			} else {
				found = true;
				break;
			}
		}
		if (!found)
			return 0;
		return l;
	}

}
