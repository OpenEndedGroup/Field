package field.core.ui.text;

import org.eclipse.swt.custom.StyledText;

public class SWTSwingCompatibility {

	private StyledText out;

	public class Document {
		public String getText(int start, int length) {
			try{
				return out.getTextRange(start, length);
			}
			catch(IllegalArgumentException e)
			{
				e.printStackTrace();
				return "";
			}
		}

		public void insertString(int start, String text, Object ignored) {
			int c = out.getCaretOffset();
			out.replaceTextRange(start, 0, text);
			if (c == start)
				out.setCaretOffset(c + text.length());
		}

		public void remove(int start, int length) {
			out.replaceTextRange(start, length, "");
		}

		public int getLength() {
			return out.getText().length();
		}
	}

	public SWTSwingCompatibility(StyledText out) {
		this.out = out;
	}

	public int getLineStartOffset(int line) {
		try {
			return out.getOffsetAtLine(line);
		} catch (IllegalArgumentException e) {
			return 0;
		}
	}

	public int getLineEndOffset(int line) {
		try {
			return out.getOffsetAtLine(line + 1) - 1;
		} catch (IllegalArgumentException e) {
			return 0;
		}
	}

	public int getLineOfOffset(int ofset) {
		try {
			return out.getLineAtOffset(ofset);
		} catch (IllegalArgumentException e) {
			return 0;
		}
	}

	public String getText() {
		return out.getText();
	}

	public int getCaretPosition() {
		return out.getCaretOffset();
	}

	public Document getDocument() {
		return new Document();
	}

}
