package field.core.ui.text;

import java.util.List;

import javax.swing.text.Segment;

public interface TokenMaker {

	/**
	 * Returns the first token in the linked list of tokens generated from
	 * <code>text</code>. This method must be implemented by subclasses so
	 * they can correctly implement syntax highlighting.
	 * 
	 * @param text
	 *                The text from which to get tokens.
	 * @param initialTokenType
	 *                The token type we should start with.
	 * @param startOffset
	 *                The offset into the document at which
	 *                <code>text</code> starts.
	 * @return The first <code>Token</code> in a linked list representing
	 *         the syntax highlighted text.
	 */
	public abstract List<Token> getTokenList(Segment text, int initialTokenType, int startOffset);

}