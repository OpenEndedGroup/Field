package field.graphics.dynamic;

import field.graphics.dynamic.iLineOutput.iLineIdentifier;
import field.namespace.context.SimpleContextTopology;

public class LineIdentifier implements iLineIdentifier {

	protected String path;

	protected String[] parts;

	public LineIdentifier(iLineIdentifier identifier) {
		this.path = identifier.getName();
	}

	public LineIdentifier(String path) {
		this.path = path;
	}

	protected LineIdentifier() {
	}

	public LineIdentifier append(String unit) {
		LineIdentifier i = new LineIdentifier();
		i.path = this.path + "/" + unit;
		if (this.parts != null) {
			i.parts = new String[this.parts.length + 1];
			System.arraycopy(this.parts, 0, i.parts, 0, this.parts.length);
			this.parts[this.parts.length - 1] = unit;
		}
		return i;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof iLineIdentifier ? ((iLineIdentifier) obj).getName().equals(this.path) : false;
	}

	public String getName() {
		return path;
	}

	public String[] getParts() {
		if (parts != null) return parts;
		return parts = path.split("/");
	}

	@Override
	public int hashCode() {
		return path.hashCode();
	}

	public void intoContext(SimpleContextTopology here) {
		String[] p = getParts();
		for (String s : p)
			here.begin(s);
	}

	public void outOfContext(SimpleContextTopology here) {
		String[] p = getParts();
		for (int i = 0; i < p.length; i++)
		{
			here.end(p[p.length - 1 - i]);
		}
	}

	public LineIdentifier prepend(String unit) {
		LineIdentifier i = new LineIdentifier();
		i.path = unit + "/" + this.path;
		if (this.parts != null) {
			i.parts = new String[this.parts.length + 1];
			System.arraycopy(this.parts, 0, i.parts, 1, this.parts.length);
			this.parts[0] = unit;
		}
		return i;
	}

	public LineIdentifier stripFirst() {
		LineIdentifier i = new LineIdentifier();
		int index = this.path.indexOf("/");
		if (index != -1) {
			i.path = this.path.substring(this.path.indexOf("/") + 1, this.path.length());
			if (this.parts != null) {
				i.parts = new String[this.parts.length - 1];
				System.arraycopy(this.parts, 1, i.parts, 0, i.parts.length);
			}
		} else {
			i.path = path;
			i.parts = parts;
		}
		return i;
	}

	@Override
	public String toString() {
		return "line:<" + path + ">";
	}

}
