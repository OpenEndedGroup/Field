package field.core.ui.text.syntax;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import field.bytecode.protect.Woven;


@Woven
public class PythonScanner {

	public class Cache {
		int advance;

		int token;

		public Cache(int advance, int token) {
			super();
			this.advance = advance;
			this.token = token;
		}
	}

	public class Token {
		private final Pattern pattern;

		int num;

		public Token(String expression, int num) {
			pattern = Pattern.compile(expression);
			this.num = num;
		}
	}

	public enum TokenTypes {
		comment, decorator, localTemp, localPersistant, self, keyword, string, identifier, operator, number, tab, whitespace, multlineStringBeginEnd, embedded_control;
	}

	public int token;

	public int end;

	public int start;

	public int offset;

	private String string;

	List<Token> tokens = new ArrayList<Token>();

	LinkedHashMap<String, Cache> cache = new LinkedHashMap<String, Cache>(1000, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(java.util.Map.Entry<String, field.core.ui.text.syntax.PythonScanner.Cache> eldest) {
			if (this.size() > 1000)
				return true;
			return false;
		}
	};

	public PythonScanner() {

		// an embedded widget
		tokens.add(new Token("^[\uf800-\uff00]", TokenTypes.embedded_control.ordinal()));

		// a decorator
		tokens.add(new Token("^@\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*", TokenTypes.decorator.ordinal()));

		// a # comment
		tokens.add(new Token("^#.*", TokenTypes.comment.ordinal()));

		// special field
		// conventions
		tokens.add(new Token("^_self.\\p{Alpha}\\p{Alnum}*_", TokenTypes.localTemp.ordinal()));
		tokens.add(new Token("^_self.\\p{Alpha}\\p{Alnum}*", TokenTypes.localPersistant.ordinal()));
		tokens.add(new Token("^_self.\\p{Alpha}\\p{Alnum}*_v", TokenTypes.localPersistant.ordinal()));

		tokens.add(new Token("^_self", TokenTypes.self.ordinal()));


		// a multiline string start / end
		tokens.add(new Token("^\"\"\"", TokenTypes.multlineStringBeginEnd.ordinal()));

		// a string
		tokens.add(new Token("^\".*?\"", TokenTypes.string.ordinal()));

		// an operator
		tokens.add(new Token("^[\\p{Punct}&&[^\"]]+", TokenTypes.operator.ordinal()));

		// an number
		tokens.add(new Token("^\\p{Digit}+", TokenTypes.number.ordinal()));

		// tabspace
		tokens.add(new Token("^\\t+", TokenTypes.tab.ordinal()));

		// whitespace
		tokens.add(new Token("^\\p{Space}+", TokenTypes.whitespace.ordinal()));

		// a keyword
		tokens.add(new Token("(^and|^del|^from|^not|^while|^as|^elif|^global|^or|^with|^assert|^else|^if|^pass|^yield|^break|^except|^import|^print|^class|^exec|^in|^raise|^continue|^finally|^is|^return|^def|^for|^lambda|^try)[\\p{Space}$]", TokenTypes.keyword.ordinal()));
		
		// an identifier
		tokens.add(new Token("^\\p{Alpha}\\p{Alnum}*", TokenTypes.identifier.ordinal()));

	}

	public int getEndOffset() {
		return offset + end;
	}

	public int getStartOffset() {
		return offset + start;
	}

	public void scan() {
		
		
		if (end >= string.length()) {
			token = 0;
			return;
		}

		String currentString = string.substring(end);

		Cache mm = cache.get(currentString);
		if (mm != null) {
			this.token = mm.token;
			this.start = end;
			this.end = end + mm.advance;
			return;
		}

		//long timeIn = System.currentTimeMillis();
		try {

			for (Token t : tokens) {
				Matcher m = t.pattern.matcher(currentString);

				if (m.find()) {
					int startAt = m.start();
					assert startAt == 0 : startAt;
					int endAt = m.end();
					this.start = end;
					this.end = end + endAt;
					this.token = t.num;

					cache.put(currentString, new Cache(endAt, t.num));

					return;
				}
			}
			//System.err.println(" string <" + currentString + "> matched by nothing");
			start++;
			end++;
			token = 0;
		} finally {
			//long timeOut = System.currentTimeMillis();
		}
	}

	public void setString(String on, int offset) {
		string = on;
		this.offset = offset;
		start = 0;
		end =   0;
	}
}
