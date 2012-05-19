package field.core.plugins.python;

import javax.swing.JEditorPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;

import org.eclipse.swt.custom.StyledText;

public class UnitTestHelper {

	private static final String prefix = "#->";

	static public String expectedOutputForExpression(String expression) {
		String[] lines = expression.split("\n");
		for (String ll : lines) {
			if (ll.startsWith(prefix)) {
				return ll.substring(prefix.length());
			}
		}
		return null;
	}

	public static void reviseOutputForExpression(StyledText ed, int p1,
			String expression, String oldOutput, String newOutput) {
		if (oldOutput == null) {
			int expressionStartsAt = ed.getText().indexOf(expression, p1)
					+ expression.length();
				ed.getContent().replaceTextRange(expressionStartsAt, 0,
						"\n" + (prefix + newOutput));

		} else {
			int expressionStartsAt = ed.getText().indexOf(prefix + oldOutput,
					p1);
				ed.getContent().replaceTextRange(expressionStartsAt,
						(prefix + oldOutput).length(), (prefix + newOutput));
		}
	}

}
