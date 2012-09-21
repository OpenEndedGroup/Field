/*
 * Copyright (c) 2009 Armando Blancas. All rights reserved.
 * 
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file epl-v10.html at the root of this distribution.
 * 
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * 
 * You must not remove this notice, or any other, from this software.
 */
package field.extras.jsrscripting.clojure;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Map;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import clojure.lang.Compiler;
import clojure.lang.LineNumberingPushbackReader;
import clojure.lang.Namespace;
import clojure.lang.RT;
import clojure.lang.Repl;
import clojure.lang.Symbol;
import clojure.lang.Var;

/**
 * Implementation of a {@code ScriptEngine} for Clojure.
 * 
 * @author Armando Blancas (+ modifications Marc)
 * @version 1.0
 */
public class ClojureScriptEngine extends AbstractScriptEngine implements Invocable, Compilable {

	private static final Symbol USER_SYM = Symbol.create("user");
	private static final Namespace USER_NS = Namespace.findOrCreate(USER_SYM);
	private static final Var IN_NS = RT.var("clojure.core", "in-ns");
	private static final String SOURCE_PATH_KEY = "clojure.source.path";
	private static final String COMPILE_PATH_KEY = "clojure.compile.path";
	private static final String WARN_REFLECTION_KEY = "clojure.compile.warn-on-reflection";
	private static final String CLASSPATH = System.getProperty("java.class.path");

	/**
	 * Default Constructor.
	 * 
	 * @param sef
	 *                The Script Engine Factory that created this instance.
	 */
	public ClojureScriptEngine() {

		Bindings engineScope = getBindings(ScriptContext.ENGINE_SCOPE);
		engineScope.put(ENGINE, "Clojure Scripting Engine");
		engineScope.put(ENGINE_VERSION, "1.0");
		engineScope.put(NAME, "Clojure");
		engineScope.put(LANGUAGE, "Clojure");
		engineScope.put(LANGUAGE_VERSION, "1.0");

		// Defaults used for compiling Clojure sources.
		engineScope.put(SOURCE_PATH_KEY, null);
		engineScope.put(COMPILE_PATH_KEY, "classes");
		engineScope.put(WARN_REFLECTION_KEY, Boolean.valueOf(false));
	}

	/*
	 * Bindings are interned in the user namespace.
	 */
	private void applyBindings(Bindings bindings) {

		HashSet<String> remove = new HashSet<String>();

		for (Map.Entry<String, Object> global : bindings.entrySet()) {
			String key = global.getKey();
			if (key.indexOf('.') == -1) {
				Object value = global.getValue();
				try {
					Var.intern(USER_NS, Symbol.create(key), value);
				} catch (IllegalStateException e) {
					remove.add(key);
				}
			}
		}
		for (String m : remove)
			bindings.remove(m);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Returns an instance of SimpleBindings.
	 */
	public Bindings createBindings() {
		return new SimpleBindings();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The Clojure runtime will keep its state between {@code eval()} calls.
	 * Previous to running the script:
	 * <p>
	 * 1- All Engine and Global bindings are applied to the {@code user}
	 * namespace.
	 * <p>
	 * 2- The Clojure standard streams {@code*in*}, {@code *out*} and
	 * {@code *err*} are redirected to their corresponding values in the
	 * passed {@code context}. The defaults are {@code System.in},
	 * {@code System.out} and {@code System.err}, respectively.
	 * <p>
	 * 3- The Clojure runtime is set to the {@code user} namespace in order
	 * to provide consistency with the REPL.
	 * <p>
	 * For consistency with the REPL, redirect {@code *err* } to a
	 * {@code PrintWriter}.
	 */
	public Object eval(String script, ScriptContext context) throws ScriptException {
		if (script == null)
			throw new NullPointerException("script is null");

		return eval(new StringReader(script), context);
	}

	public void pushTopLevelThreadBindings(ScriptContext c)
	{
		Bindings engineScope = context.getBindings(ScriptContext.ENGINE_SCOPE);
		if (engineScope != null)
			applyBindings(engineScope);

		Bindings globalScope = context.getBindings(ScriptContext.GLOBAL_SCOPE);
		if (globalScope != null)
			applyBindings(globalScope);

		Var.pushThreadBindings(RT.map(RT.CURRENT_NS, RT.CURRENT_NS.deref(), RT.IN, new LineNumberingPushbackReader(context.getReader()), RT.OUT, context.getWriter(), RT.ERR, context.getErrorWriter()));
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * The Clojure runtime will keep its state between {@code eval()} calls.
	 * Previous to running the script:
	 * <p>
	 * 1- All Engine and Global bindings are applied to the {@code user}
	 * namespace.
	 * <p>
	 * 2- The Clojure standard streams {@code*in*}, {@code *out*} and
	 * {@code *err*} are redirected to their corresponding values in the
	 * passed {@code context}. The defaults are {@code System.in},
	 * {@code System.out} and {@code System.err}, respectively.
	 * <p>
	 * 3- The Clojure runtime is set to the {@code user} namespace in order
	 * to provide consistency with the REPL.
	 * <p>
	 * For consistency with the REPL, redirect {@code *err* } to a
	 * {@code PrintWriter}.
	 */
	public Object eval(Reader reader, ScriptContext context) throws ScriptException {
		if (reader == null)
			throw new NullPointerException("reader is null");
		if (context == null)
			throw new NullPointerException("context is null");

		Object result = null;

		Bindings engineScope = context.getBindings(ScriptContext.ENGINE_SCOPE);
		if (engineScope != null)
			applyBindings(engineScope);

		Bindings globalScope = context.getBindings(ScriptContext.GLOBAL_SCOPE);
		if (globalScope != null)
			applyBindings(globalScope);
		try {

			Var.pushThreadBindings(RT.map(RT.CURRENT_NS, RT.CURRENT_NS.deref(), RT.IN, new LineNumberingPushbackReader(context.getReader()), RT.OUT, context.getWriter(), RT.ERR, context.getErrorWriter()));
			IN_NS.invoke(USER_SYM);
			
			result = Compiler.load(reader);
		} catch (Exception e) {

			throw new ScriptException(e);
		} finally {
			Var.popThreadBindings();
		}

		return result;
	}

	/******************************************************************
	 * * Implementation of interface Invocable. * *
	 ******************************************************************/

	/**
	 * {@inheritDoc}
	 * <p>
	 * To implement a Java interface use the {@code proxy} macro. This
	 * method expects a var in the {@code user} namespace named after the
	 * interface with the suffix "Impl". For example, to implement an
	 * {@code ActionListener} the Clojure code could be:
	 * 
	 * <pre>
	 * (import java.awt.event.ActionListener)
	 * 
	 * (def ActionListenerImpl 
	 *   (proxy [ActionListener] []
	 *     (actionPerformed [evt] (println "button pushed"))))
	 * </pre>
	 * 
	 * Then get that implementation by calling:
	 * <p>
	 * {@code engine.getInterface(EventListener.class)}
	 * <p>
	 */
	@SuppressWarnings("unchecked")
	public <T> T getInterface(Class<T> clasz) {
		if (clasz == null)
			throw new NullPointerException("clasz is null");

		String ns = "user";
		Var var = RT.var(ns, clasz.getSimpleName() + "Impl");
		return (var == null) ? null : (T) var.deref();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * To implement a Java interface use the {@code proxy} macro. This
	 * method looks for a var in the namespace named indicated by
	 * {@code thiz} and named after the interface with the suffix "Impl".
	 * For example, to implement an {@code ActionListener} in the
	 * {@code actions} namespace the Clojure code could be:
	 * 
	 * <pre>
	 * (ns actions
	 *  (:import java.awt.event.ActionListener))
	 *  
	 * (def ActionListenerImpl 
	 *   (proxy [ActionListener] []
	 *     (actionPerformed [evt] (println "button pushed"))))
	 * </pre>
	 * 
	 * Then get that implementation by calling:
	 * <p>
	 * {@code engine.getInterface("actions", EventListener.class)}
	 * <p>
	 */
	@SuppressWarnings("unchecked")
	public <T> T getInterface(Object thiz, Class<T> clasz) {
		if (thiz == null)
			throw new NullPointerException("thiz is null");
		if (clasz == null)
			throw new NullPointerException("clasz is null");
		if (!(thiz instanceof String))
			throw new IllegalArgumentException("thiz is not a string");

		String ns = (String) thiz;
		Var var = RT.var(ns, clasz.getSimpleName() + "Impl");
		return (var == null) ? null : (T) var.deref();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * If the function name is not qualified with a namespace, this method
	 * looks for it in the {@code user} namespace, thus {@code foo} is
	 * equivalent to {@code user/foo}. Functions in other namespaces must
	 * used their fully-qualified names.
	 * <p>
	 * As in the {@code eval()} calls, bindings and redirections are applied
	 * prior to invoking the function.
	 */
	public Object invokeFunction(String name, Object... args) throws ScriptException, NoSuchMethodException {
		if (name == null)
			throw new NullPointerException("name is null");

		Object result = null;
		String format = "Function %s not found in namespace %s";

		try {
			Bindings engineScope = context.getBindings(ScriptContext.ENGINE_SCOPE);
			if (engineScope != null)
				applyBindings(engineScope);

			Bindings globalScope = context.getBindings(ScriptContext.GLOBAL_SCOPE);
			if (globalScope != null)
				applyBindings(globalScope);

			Var.pushThreadBindings(RT.map(RT.CURRENT_NS, RT.CURRENT_NS.deref(), RT.IN, new LineNumberingPushbackReader(context.getReader()), RT.OUT, context.getWriter(), RT.ERR, context.getErrorWriter()));

			if (name.indexOf('/') == -1) {
				String ns = "user";
				Var var = RT.var(ns, name);
				if (var == null) {
					String msg = String.format(format, name, ns);
					throw new NoSuchMethodException(msg);
				}
				result = var.applyTo(RT.seq(args));
			} else {
				String[] names = name.split("/");
				Var var = RT.var(names[0], names[1]);
				if (var == null) {
					String msg = String.format(format, names[1], names[0]);
					throw new NoSuchMethodException(msg);
				}
				result = var.applyTo(RT.seq(args));
			}
		} catch (Exception e) {
			throw new ScriptException(e);
		} finally {
			Var.popThreadBindings();
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method works just like
	 * {@code invoke(String name, Object... args)}. The parameter
	 * {@code thiz} is ignored.
	 */
	public Object invokeMethod(Object thiz, String name, Object... args) throws ScriptException, NoSuchMethodException {
		return invokeFunction(name, args);
	}

	/******************************************************************
	 * * Implementation of interface Compilable. * *
	 ******************************************************************/

	/**
	 * {@inheritDoc}
	 * <p>
	 * Clojure code always runs compiled. This method will compile a Clojure
	 * library to .class files for AOT compilation. The argument is expected
	 * to be the namespace defined by the library. The filename and location
	 * should be as expected by the Clojure runtime.
	 * <p>
	 * This engine will recognize and pass on these properties to the
	 * Clojure compiler:
	 * <p>
	 * {@code clojure.source.path} Additional locations of Clojure source
	 * files, to be appended to the value of "java.class.path". This is an
	 * optional property and defaults to {@code null}.
	 * <p>
	 * {@code clojure.compile.path} The location for the generated .class
	 * files. Defaults to {@code "classes"}.
	 * <p>
	 * {@code clojure.compile.warn-on-reflection} Whether to get a warning
	 * when Clojure will use Java reflection. Defaults to
	 * {@code Boolean false}.
	 */
	public CompiledScript compile(String script) throws ScriptException {
		if (script == null)
			throw new NullPointerException("script is null");

		String library = script.trim();
		if (library.length() == 0)
			return null;

		try {
			/*
			 * Each compilation takes place in its own process with
			 * a clean slate in the Clojure RT. No bindings nor
			 * redirections are applied from the host Java code.
			 */
			StringBuffer classpath = new StringBuffer(CLASSPATH);
			String cmp = (String) get(COMPILE_PATH_KEY);
			if (cmp != null && cmp.length() > 0)
				classpath.append(File.pathSeparatorChar).append(cmp);
			String src = (String) get(SOURCE_PATH_KEY);
			if (src != null && src.length() > 0)
				classpath.append(File.pathSeparatorChar).append(src);

			String compile = String.format("java -D%s=%s -D%s=%b -cp %s clojure.lang.Compile %s", COMPILE_PATH_KEY, (String) get(COMPILE_PATH_KEY), WARN_REFLECTION_KEY, (Boolean) get(WARN_REFLECTION_KEY), classpath.toString(), library);

			Process process = Runtime.getRuntime().exec(compile);

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			StringBuffer buffer = new StringBuffer();
			String line = reader.readLine();
			while (line != null) {
				buffer.append(line).append('\n');
				line = reader.readLine();
			}
			reader.close();

			process.waitFor();
			if (process.exitValue() != 0)
				throw new ScriptException(buffer.toString());
		} catch (IOException e) {
			throw new ScriptException(e);
		} catch (InterruptedException e) {
			throw new ScriptException(e);
		}

		return null;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * <b>NOTE:</b> No Clojure code is expected from the {@code Reader}.
	 * <p>
	 * This method expects to read library names in separate lines from the
	 * passed reader. It will compile each library in turn. The actual
	 * Clojure code to compile should be in source files with the name and
	 * locations as expected by the Clojure compiler.
	 */
	public CompiledScript compile(Reader script) throws ScriptException {
		if (script == null)
			throw new NullPointerException("script is null");

		BufferedReader bf = new BufferedReader(script);

		try {
			String library = bf.readLine();
			while (library != null) {
				compile(library);
				library = bf.readLine();
			}
		} catch (IOException e) {
			throw new ScriptException(e);
		} finally {
			try {
				bf.close();
			} catch (IOException e) {
				throw new ScriptException(e);
			}
		}

		return null;
	}

	@Override
	public ScriptEngineFactory getFactory() {
		return null;
	}

}
