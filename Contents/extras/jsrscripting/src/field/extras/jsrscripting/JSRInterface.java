package field.extras.jsrscripting;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.NextUpdate;
import field.core.dispatch.iVisualElement;
import field.core.execution.PythonInterface;
import field.core.execution.ScriptingInterface;
import field.core.ui.text.embedded.MinimalTextField_blockMenu;
import field.namespace.generic.Generics.Pair;

@Woven
public abstract class JSRInterface implements ScriptingInterface {

	protected ScriptEngine engine;
	protected ScriptContext context;
	protected ScriptContext newContext;

	public JSRInterface() {
		engine = makeEngine();
		context = engine.getContext();
		newContext = new ScriptContext() {

			ScriptContext c = context;

			public Object getAttribute(String name, int scope) {
				return c.getAttribute(name, scope);
			}

			public Object getAttribute(String name) {
				return c.getAttribute(name);
			}

			public int getAttributesScope(String name) {
				return c.getAttributesScope(name);
			}

			public Bindings getBindings(int scope) {
				return c.getBindings(scope);
			}

			public Writer getErrorWriter() {
				return c.getErrorWriter();
			}

			public Reader getReader() {
				return c.getReader();
			}

			public List<Integer> getScopes() {
				return c.getScopes();
			}

			public Writer getWriter() {
				return c.getWriter();
			}

			public Object removeAttribute(String name, int scope) {
				return c.removeAttribute(name, scope);
			}

			public void setAttribute(String name, Object value, int scope) {
				;//;//System.out.println(" set attribute <" + name + " = " + value + " @ " + scope + ">");
				c.setAttribute(name, value, scope);
				if (!insideShared)
					propagateAttribute(name, value);
			}

			public void setBindings(Bindings bindings, int scope) {
				;//;//System.out.println("WARNING: unhandled set bindings <" + bindings.size() + ">");
				c.setBindings(bindings, scope);
			}

			public void setErrorWriter(Writer writer) {
				c.setErrorWriter(writer);
			}

			public void setReader(Reader reader) {
				c.setReader(reader);
			}

			public void setWriter(Writer writer) {
				c.setWriter(writer);
			}

		};
		engine.setContext(newContext);

		// engine.setBindings(engine.createBindings(),
		// ScriptContext.GLOBAL_SCOPE);

		PythonInterface.getPythonInterface().addSharedScriptingInterface(this);
		this.addSharedScriptingInterface(PythonInterface.getPythonInterface());

		Map<String, Object> m = PythonInterface.getPythonInterface().getVariables();
		for (Map.Entry<String, Object> e : m.entrySet()) {
			setVariable(e.getKey(), e.getValue());
		}

		PythonInterface.getPythonInterface().execString("from field.extras.jsrscripting import JSRPlugin");
		PythonInterface.getPythonInterface().execString("def " + transformName() + "(inside, text, glob):\n\treturn JSRPlugin.getInterface(\"" + transformName() + "\").eval(text)");

		MinimalTextField_blockMenu.knownTextTransforms.add(new Pair<String, String>(transformName(), description()));

		callFinishInstall();

	}

	@NextUpdate
	protected void callFinishInstall() {
		finishInstall();
	}

	abstract protected String description();

	abstract protected String transformName();

	abstract protected ScriptEngine makeEngine();

	private boolean insideShared;

	List<ScriptingInterface> shared = new ArrayList<ScriptingInterface>();

	public void addSharedScriptingInterface(ScriptingInterface s) {
		shared.add(s);
	}

	public Object eval(String eval) {
		try {
			String total = preamble(eval) + body(eval) + postamble(eval);

			Object evaluation = engine.eval(total);
			postBind(evaluation, eval);

			// Bindings bindings =
			// engine.getBindings(ScriptContext.ENGINE_SCOPE);
			// Bindings bindings2 =
			// engine.getBindings(ScriptContext.GLOBAL_SCOPE);

			return evaluation;
		} catch (ScriptException e) {
			;//;//System.out.println(e + " " + e.getLineNumber());

			// e.printStackTrace();

			// handleScriptException(e);
		}
		return null;
	}

	protected String body(String eval) {
		return eval;
	}

	protected void handleScriptException(ScriptException e) {

		;//;//System.out.println(" << stack trace begins >>");
		e.printStackTrace();
		;//;//System.out.println(" << stack trace ends>>");

		try {
			newContext.getErrorWriter().append(e + ":" + e.getMessage());
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// e.printStackTrace(new PrintStream(new OutputStream() {
		// Writer w = newContext.getErrorWriter();
		//
		// @Override
		// public void write(int b) throws IOException {
		// w.append((char) b);
		// }
		// }));
	}

	protected void postBind(Object evaluation, String eval) {
	}

	protected String postamble(String eval) {
		return "";
	}

	protected String preamble(String eval) {
		return "";
	}

	public void execString(String exec) {
		eval(exec);
	}

	public Object executeStringReturnRawValue(String script, String tag) {
		eval(script);
		return context.getBindings(ScriptContext.ENGINE_SCOPE).get(tag);
	}

	public Object executeStringReturnValue(String script, String tag) {
		return executeStringReturnRawValue(script, tag);
	}

	public Language getLanguage() {
		return Language.jsr;
	}

	public Object getVariable(String name) {
		return context.getBindings(ScriptContext.ENGINE_SCOPE).get(name);
	}

	public Map<String, Object> getVariables() {
		HashMap<String, Object> m = new HashMap<String, Object>();
		Bindings bindingskno = context.getBindings(ScriptContext.ENGINE_SCOPE);
		m.putAll(bindingskno);
		return m;
	}

	public void importJava(String pack, String clas) {
	}

	public void popGlobalTrap() {
	}

	public void popOutput() {
		if (insideShared)
			return;
		insideShared = true;
		try {
			if (outputStack.size() == 0)
				return;

			Pair<Writer, Writer> popped = outputStack.pop();
			newContext.setErrorWriter(popped.left);
			newContext.setWriter(popped.right);
		} finally {
			insideShared = false;
		}
	}

	public void pushGlobalTrap(iGlobalTrap gt) {
	}

	Stack<Pair<Writer, Writer>> outputStack = new Stack<Pair<Writer, Writer>>();

	public void pushOutput(Writer output, Writer error) {
		
		
		
		if (insideShared)
			return;
		insideShared = true;
		try {
			Writer oldError = newContext.getErrorWriter();
			Writer oldOutput = newContext.getWriter();
			outputStack.add(new Pair<Writer, Writer>(oldError, oldOutput));
			newContext.setErrorWriter(error);
			newContext.setWriter(output);
			for (ScriptingInterface s : shared)
				s.pushOutput(output, error);
		} finally {
			insideShared = false;
		}
	}

	public void setVariable(String name, Object value) {
		if (filterSetVariable(name, value) && !insideShared && name != null) {
			context.setAttribute(name, value, ScriptContext.ENGINE_SCOPE);
		}
	}

	protected boolean filterSetVariable(String name, Object value) {
		return true;
	}

	protected void propagateAttribute(String name, Object value) {
		insideShared = true;
		try {
			for (ScriptingInterface s : shared) {
				// ;//;//System.out.println(" set <"+name+"> to <"+value+"> on <"+s+">");
				s.setVariable(name, value);
			}
		} finally {
			;//;//System.out.println(" finished prop");
			insideShared = false;
		}
	}

	public void setRoot(iVisualElement root) {
	}

	@Override
	public void finishInstall() {

	}
}
