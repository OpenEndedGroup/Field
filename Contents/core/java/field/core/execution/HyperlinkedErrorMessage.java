package field.core.execution;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import field.core.ui.text.embedded.MinimalButton;

public class HyperlinkedErrorMessage {

	public HyperlinkedErrorMessage() {
	}

	static public String error(String uidDesc, int lineNo) {
		return "";
		//return "###errorLink###" + uidDesc + "###" + lineNo + "###";
	}

	public void filterOutput(String output, DefaultStyledDocument outputTo, MutableAttributeSet es) throws BadLocationException {
		int a = 0;
		try {
			Pattern p = Pattern.compile("###errorLink###(.*?)###(.*?)###");
			Matcher m = p.matcher(output);
			boolean found = m.find();

			;//System.out.println(" found ? <" + found + "> in <" + output + ">");

			while (found) {
				;//System.out.println(" matched <" + m.group() + ">");
				outputTo.insertString(outputTo.getEndPosition().getOffset() - 1, output.substring(a, m.start()), es);

				JComponent component = makeButton(m.group(1), m.group(2));

				outputTo.insertString(outputTo.getEndPosition().getOffset() - 1, "((printed component))", getAttributeSetForComponent(component));
				a = m.end();
				found = m.find();
			}
		} finally {
			outputTo.insertString(outputTo.getEndPosition().getOffset() - 1, output.substring(a, output.length()), es);
		}
	}

	private JComponent makeButton(final String uidDesc, final String line) {

		return new MinimalButton("\u2014", 20) {
			@Override
			public void execute() {
				goButton(uidDesc, Integer.parseInt(line));
			}

		};
	}

	protected void goButton(String uidDesc, int parseInt) {
	}

	private AttributeSet getAttributeSetForComponent(JComponent component) {
		Style style = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
		StyleConstants.setComponent(style, component);
		return style;
	}

}