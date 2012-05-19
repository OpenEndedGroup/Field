package field.graphics.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

/**
 * aguments a .glslang file with additions and inclusions. Makes it easy to require variables and functions (attributes, varyings and local variables etc.) without duplication
 *
 * syntax:
 *
 * variable use is $use{variable[, default]} variable addition is $add{variable}{ ... } variable conditional addition $if_add{variable, condition}{ ... } (will only add something to that variable if that condition has been set on it) $if_nadd{variable, condition} (if not been set) $set{variable, condition} sets a condition
 *
 * $import{filename} imports
 *
 * @author marc
 *
 */
public class ShaderPreprocessor {

	public class AddExpression implements iExpressionHandler {
		public String handle(ShaderPreprocessor here, StringStream r, String expression, String[] parameters) throws PreprocessorException {
			if (parameters.length != 1) throw new PreprocessorException(" bad parameters for add <" + Arrays.asList(parameters) + ">");
			iVariable v = here.getVariable(parameters[0]);
			String b = here.readBlock(r);
			v.add(b);
			return "";
		}
	}

	public class ConditionalAddExpression implements iExpressionHandler {
		public String handle(ShaderPreprocessor here, StringStream r, String expression, String[] parameters) throws PreprocessorException {
			if (parameters.length != 2) throw new PreprocessorException(" bad parameters for conditional add <" + Arrays.asList(parameters) + ">");
			iVariable v = here.getVariable(parameters[0]);
			String t = here.readBlock(r);
			v.conditionalAdd(parameters[1], t);
			return "";
		}
	}

	public class ConditionalNAddExpression implements iExpressionHandler {
		public String handle(ShaderPreprocessor here, StringStream r, String expression, String[] parameters) throws PreprocessorException {
			if (parameters.length != 2) throw new PreprocessorException(" bad parameters for conditional Nadd <" + Arrays.asList(parameters) + ">");
			iVariable v = here.getVariable(parameters[0]);
			String t = here.readBlock(r);
			v.conditionalNAdd(parameters[1], t);
			return "";
		}
	}

	public class DefaultVariable implements iVariable {

		String text = "";

		HashSet<String> conditions = new HashSet<String>();

		public void add(String add) throws PreprocessorException {
			text += read(add) + "\n";
		}

		public void conditionalAdd(String condition, String add) throws PreprocessorException {
			if (conditions.contains(condition)) add(read(add));
		}

		public void conditionalNAdd(String condition, String add) throws PreprocessorException {
			if (!conditions.contains(condition)) add(read(add));
		}

		public String getText() {
			return text;
		}

		public void setCondition(String condition) {
			conditions.add(condition);
		}

	}

	public class HandleExpression implements iExpressionHandler {
		public String handle(ShaderPreprocessor here, StringStream r, String expression, String[] parameters) throws PreprocessorException {
			if (parameters.length != 2) throw new PreprocessorException(" bad parameters for handle <" + Arrays.asList(parameters) + ">");

			Exception e2 = null;
			Exception e3 = null;

			try {
				iExpressionHandler e = (iExpressionHandler) this.getClass().getClassLoader().loadClass(parameters[1]).newInstance();
				handlers.put(parameters[0], e);
				return "";
			} catch (ClassNotFoundException e) {
				e2 = e;
			} catch (InstantiationException e) {
				e2 = e;
			} catch (IllegalAccessException e) {
				e2 = e;
			}
			try {
				iExpressionHandler e = (iExpressionHandler) this.getClass().getClassLoader().loadClass("innards.graphics.basic." + parameters[1]).newInstance();
				handlers.put(parameters[0], e);
				return "";
			} catch (ClassNotFoundException e) {
				e3 = e;
			} catch (InstantiationException e) {
				e3 = e;
			} catch (IllegalAccessException e) {
				e3 = e;
			}

			throw new PreprocessorException(" couldn't instantiate <" + parameters[1] + "> exceptions are <" + e2 + "> <" + e3 + ">");
		}
	}

	public interface iExpressionHandler {
		public String handle(ShaderPreprocessor here, StringStream r, String expression, String[] parameters) throws PreprocessorException;
	}

	public class IncludeExpression implements iExpressionHandler {
		public String handle(ShaderPreprocessor here, StringStream r, String expression, String[] parameters) throws PreprocessorException {
			if (parameters.length != 1) throw new PreprocessorException(" bad parameters for include<" + Arrays.asList(parameters) + ">");
			return filenameToString(textFileRoot + "/" + parameters[0]);
		}
	}

	public interface iVariable {
		public void add(String add) throws PreprocessorException;

		public void conditionalAdd(String condition, String add) throws PreprocessorException;

		public void conditionalNAdd(String condition, String add) throws PreprocessorException;

		public String getText();

		public void setCondition(String condition) throws PreprocessorException;
	}

	static public class PreprocessorException extends Exception {
		public PreprocessorException() {
		}

		public PreprocessorException(IOException e) {
			initCause(e);
		}

		public PreprocessorException(String string) {
			super(string);
		}
	}

	public class SetExpression implements iExpressionHandler {
		public String handle(ShaderPreprocessor here, StringStream r, String expression, String[] parameters) throws PreprocessorException {
			if (parameters.length != 2) throw new PreprocessorException(" bad parameters for set <" + Arrays.asList(parameters) + ">");
			iVariable v = here.getVariable(parameters[0]);
			v.setCondition(parameters[1]);
			return "";
		}
	}

	public class UseExpression implements iExpressionHandler {
		public String handle(ShaderPreprocessor here, StringStream r, String expression, String[] parameters) throws PreprocessorException {
			if (parameters.length != 1 && parameters.length != 2) throw new PreprocessorException(" bad parameters for use <" + Arrays.asList(parameters) + ">");
			iVariable v = here.variables.get(parameters[0]);
			if (v == null) return parameters.length == 2 ? parameters[1] : "\n";
			return v.getText();
		}
	}

	class StringStream {
		String a;

		int at = 0;

		public StringStream(String a) {
			this.a = a;
		}

		public char read() {
			return a.charAt(at++);
		}

		public boolean ready() {
			return at < a.length();
		}
	}

	private final String textFileRoot;

	HashMap<String, iVariable> variables = new HashMap<String, iVariable>();

	HashMap<String, iExpressionHandler> handlers = new HashMap<String, iExpressionHandler>();

	public ShaderPreprocessor(String textFileRoot) {
		this.textFileRoot = textFileRoot;

		// the standard set

		handlers.put("use", new UseExpression());
		handlers.put("add", new AddExpression());
		handlers.put("if_add", new ConditionalAddExpression());
		handlers.put("if_nadd", new ConditionalNAddExpression());
		handlers.put("set", new SetExpression());
		handlers.put("include", new IncludeExpression());
		handlers.put("handle", new HandleExpression());
	}

	public String filenameToString(String filename) throws PreprocessorException {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(new File(textFileRoot+"/"+filename)));
			StringBuilder b = new StringBuilder();
			while (reader.ready())
				b.append(reader.readLine()+"\n");
			return b.toString();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new PreprocessorException(e);
		} catch (IOException e) {
			e.printStackTrace();
			throw new PreprocessorException(e);
		}
	}

	public iVariable getVariable(String name) {
		iVariable v = variables.get(name);
		if (v == null) {
			variables.put(name, v = newDefaultVariable(name));
		}
		return v;
	}

	public iVariable newDefaultVariable(String string) {
		return new DefaultVariable();
	}

	public String read(String text) throws PreprocessorException {
		StringStream reader = new StringStream(text);
		String output = "";
		try {
			while (reader.ready()) {
				String soFar = nextExpression(reader);
				output += soFar;

				if (reader.ready()) {
					String written = handleExpression(reader);
					output += written;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new PreprocessorException(e);
		}
		return output;
	}

	private String handleExpression(StringStream reader) throws IOException, PreprocessorException {
		String opcode = "";
		while (reader.ready()) {
			char c = reader.read();
			if (c != '{') {
				opcode += c;
			} else
				break;
		}

		String expression = "";
		while (reader.ready()) {
			char c = reader.read();
			if (c != '}') {
				expression += c;
			} else
				break;
		}

		String[] parameters = expression.split(",");

		iExpressionHandler handler = handlers.get(opcode);
		if (handler == null) throw new PreprocessorException("unknown handler <" + opcode + "> <" + expression + "> <" + Arrays.asList(parameters) + ">");

		String s = handler.handle(this, reader, expression, parameters);
		return s;
	}

	private String nextExpression(StringStream reader) throws IOException {
		String skipped = "";
		while (reader.ready()) {
			char c = reader.read();
			if (c != '$') {
				skipped += c;
			} else {
				break;
			}
		}
		return skipped;
	}

	protected String readBlock(StringStream reader) throws PreprocessorException {
		String skipped = "";
		while (reader.ready()) {
			char c = reader.read();
			if (c != '{') {
				skipped += c;
			} else {
				break;
			}
		}

		if (!skipped.trim().equals("{")) throw new PreprocessorException(" parse error, junk characters <" + skipped + ">");

		int openCount = 1;

		String block = "";
		while (reader.ready()) {
			char c = reader.read();
			if (c == '}') {
				openCount--;
				if (openCount == 0) break;
			} else
				block += c;
		}

		if (openCount == 0) return block;
		throw new PreprocessorException(" no end brace ? starting at <" + block + ">");
	}

}
