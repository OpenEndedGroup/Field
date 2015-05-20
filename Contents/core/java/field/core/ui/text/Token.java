package field.core.ui.text;

import org.joni.constants.TokenType;

import field.core.ui.text.syntax.PythonScanner.TokenTypes;

public class Token {

	public TokenTypes[] translation = { TokenTypes.keyword, TokenTypes.comment, 
			TokenTypes.comment, TokenTypes.comment, TokenTypes.comment, 
			TokenTypes.comment, TokenTypes.keyword, TokenTypes.keyword, 
			TokenTypes.operator, TokenTypes.number, TokenTypes.number, 
			TokenTypes.number, TokenTypes.string, TokenTypes.string,
			TokenTypes.string, TokenTypes.identifier, TokenTypes.identifier, 
			TokenTypes.identifier, TokenTypes.identifier, TokenTypes.identifier, 
			TokenTypes.identifier, TokenTypes.whitespace, TokenTypes.operator, 
			TokenTypes.keyword, TokenTypes.keyword, TokenTypes.keyword, 
			TokenTypes.keyword, TokenTypes.keyword, TokenTypes.keyword, 
			TokenTypes.keyword, TokenTypes.keyword, TokenTypes.keyword, 
			TokenTypes.keyword, TokenTypes.keyword, TokenTypes.identifier, 
			TokenTypes.number,
			TokenTypes.string, TokenTypes.string, TokenTypes.string, TokenTypes.embedded_control };

	public static final int NULL = 0;

	public static final int COMMENT_EOL = 1;
	public static final int COMMENT_MULTILINE = 2;
	public static final int COMMENT_DOCUMENTATION = 3;
	public static final int COMMENT_KEYWORD = 4;
	public static final int COMMENT_MARKUP = 5;

	public static final int RESERVED_WORD = 6;
	public static final int RESERVED_WORD_2 = 7;

	public static final int FUNCTION = 8;

	public static final int LITERAL_BOOLEAN = 9;
	public static final int LITERAL_NUMBER_DECIMAL_INT = 10;
	public static final int LITERAL_NUMBER_FLOAT = 11;
	public static final int LITERAL_NUMBER_HEXADECIMAL = 12;
	public static final int LITERAL_STRING_DOUBLE_QUOTE = 13;
	public static final int LITERAL_CHAR = 14;
	public static final int LITERAL_BACKQUOTE = 15;

	public static final int DATA_TYPE = 16;

	public static final int VARIABLE = 17;

	public static final int REGEX = 18;

	public static final int ANNOTATION = 19;

	public static final int IDENTIFIER = 20;

	public static final int WHITESPACE = 21;

	public static final int SEPARATOR = 22;

	public static final int OPERATOR = 23;

	public static final int PREPROCESSOR = 24;

	public static final int MARKUP_TAG_DELIMITER = 25;
	public static final int MARKUP_TAG_NAME = 26;
	public static final int MARKUP_TAG_ATTRIBUTE = 27;
	public static final int MARKUP_TAG_ATTRIBUTE_VALUE = 28;
	public static final int MARKUP_COMMENT = 29;
	public static final int MARKUP_DTD = 30;
	public static final int MARKUP_PROCESSING_INSTRUCTION = 31;
	public static final int MARKUP_CDATA_DELIMITER = 32;
	public static final int MARKUP_CDATA = 33;
	public static final int MARKUP_ENTITY_REFERENCE = 34;

	public static final int ERROR_IDENTIFIER = 35;
	public static final int ERROR_NUMBER_FORMAT = 36;
	public static final int ERROR_STRING_DOUBLE = 37;
	public static final int ERROR_CHAR = 38;

	public static final int DEFAULT_NUM_TOKEN_TYPES = 39;

	public static final int EMBEDDED_CONTROL = 39;

	int type = NULL;
	char[] array;
	int start;
	int end;

	public String toString() {
		return start + "->" + end + "(" + type + "/" + toToken() + ") -" + (array == null ? "" : new String(array).substring(start, end + 1)) + "-";
	}

	public TokenTypes toToken() {
		return translation[type];
	}
}