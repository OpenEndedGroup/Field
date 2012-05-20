package field.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.lang.ref.Reference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyFrame;
import org.python.core.PyFunction;
import org.python.core.PyGenerator;
import org.python.core.PyInstance;
import org.python.core.PyIterator;
import org.python.core.PyObject;
import org.python.core.PyReflectedFunction;
import org.python.core.PySequence;
import org.python.core.PyTuple;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.Sun14ReflectionProvider;
import com.thoughtworks.xstream.core.ReferenceByIdMarshallingStrategy;
import com.thoughtworks.xstream.core.ReferenceByXPathMarshallingStrategy;

import field.bytecode.protect.DeferedInQueue.iRegistersUpdateable;
import field.bytecode.protect.FastEntry;
import field.bytecode.protect.Trampoline2;
import field.bytecode.protect.dispatch.Cont;
import field.bytecode.protect.dispatch.Cont.ReturnCode;
import field.bytecode.protect.dispatch.Cont.aRun;
import field.core.dispatch.iVisualElement;
import field.core.execution.PythonGeneratorStack;
import field.core.execution.PythonInterface;
import field.core.execution.PythonScriptingSystem;
import field.core.execution.PythonScriptingSystem.Promise;
import field.core.plugins.PythonOverridden;
import field.core.plugins.PythonOverridden.Callable;
import field.core.plugins.drawing.opengl.CachedLineCompression;
import field.core.plugins.python.PythonPlugin.CapturedEnvironment;
import field.launch.Launcher;
import field.launch.iUpdateable;
import field.math.abstraction.iAcceptor;
import field.math.abstraction.iDoubleProvider;
import field.math.abstraction.iFilter;
import field.math.abstraction.iFloatProvider;
import field.math.abstraction.iPhasicAcceptor;
import field.math.abstraction.iProvider;
import field.math.util.ComplexCubicFloat;
import field.math.util.CubicInterpolatorDynamic;
import field.namespace.generic.Adaptation;
import field.namespace.generic.Bind.iFunction;
import field.namespace.generic.Generics.Triple;
import field.namespace.generic.ReflectionTools;
import field.namespace.key.FKey;
import field.namespace.key.Key;
import field.namespace.key.OKey;

/**
 * @author marc Created on Nov 30, 2003
 */
public class PythonUtils {

	static public class FKeyByName implements iFloatProvider {
		private final String name;

		private final float def;

		public FKeyByName(String name, float def) {
			this.name = name;
			this.def = def;
		}

		public float evaluate() {
			Reference<FKey> k = (Reference<FKey>) Key.internedKeys.get(name);
			if (k == null || k.get() == null)
				return def;
			return k.get().evaluate();
		}
	}

	static public class OKeyByName<T> implements iProvider<T> {
		private final String name;

		private final T def;

		public OKeyByName(String name, T def) {
			this.name = name;
			this.def = def;
		}

		public T get() {
			Reference<OKey> k = (Reference<OKey>) Key.internedKeys.get(name);

			;//System.out.println(" looking up key <" + name + "> got <" + k + " / " + (k != null ? k.get() : null) + "> of class <"+k+">");

			if (k == null || k.get() == null)
				return def;

			Object oo = k.get().evaluate();
			;//System.out.println(" evaluating to <" + oo + ">");

			return (T) oo;
		}

		public PyObject __call__(PyObject[] args, String[] keywords) {
			System.err.println(" inside call for okeyby name <" + get() + "> <" + def + ">");
			return Py.java2py(get());
		}

	}

	public static iDoubleProvider doubleFor(final Object in, String name) {
		try {
			final Field declaredField = in.getClass().getDeclaredField(name);
			declaredField.setAccessible(true);
			return new iDoubleProvider() {
				public double evaluate() {
					try {
						return declaredField.getDouble(in);
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
					return 0;
				}
			};
		} catch (SecurityException e) {
			throw new IllegalArgumentException(in + " " + name);
		} catch (NoSuchFieldException e) {
			throw new IllegalArgumentException(in + " " + name);
		}
	}

	static public iFloatProvider floatFor(final Object in, String name) {
		try {

			final Field declaredField = ReflectionTools.getFirstFIeldCalled(in.getClass(), name);

			// final Field declaredField =
			// in.getClass().getDeclaredField
			// (name);
			declaredField.setAccessible(true);
			return new iFloatProvider() {
				public float evaluate() {
					try {
						return declaredField.getFloat(in);
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
					return 0;
				}
			};
		} catch (SecurityException e) {
			throw new IllegalArgumentException(in + " " + name);
		}
	}

	public float t;

	static HashMap registeredFunctions = new HashMap();

	static HashMap<String, Triple<aRun, Method, Class>> contMap = new HashMap<String, Triple<aRun, Method, Class>>();

	public void also(String name, PyReflectedFunction o, Class c, final PyFunction function) {
		if (contMap.containsKey(name)) {
			unalso(name);
		}
		Method m = (Method) ReflectionTools.illegalGetObject(Array.get(o.argslist, 0), "data");
		aRun r = new aRun() {
			PyObject[] a = null;

			@Override
			public ReturnCode head(Object calledOn, Object[] args) {

				if (a == null || a.length != args.length)
					a = new PyObject[args.length];
				for (int i = 0; i < a.length; i++) {
					a[i] = Py.java2py(args[i]);
				}

				function.__call__(a);

				return super.head(calledOn, args);
			}
		};
		FastEntry.linkWith(m, c, r);

		contMap.put(name, new Triple<aRun, Method, Class>(r, m, c));
	}

	public iFilter<Float, Float> asFilter(final float mul, final CubicInterpolatorDynamic<ComplexCubicFloat> blender) {
		return new iFilter<Float, Float>() {
			public Float filter(Float value) {
				return blender.get(value).value * mul;
			}
		};
	}

	public iFunction<?, ?> asFunction(final PyObject o) {
		return new iFunction<Object, Object>() {

			public Object f(Object in) {
				return o.__call__(Py.java2py(in)).__tojava__(Object.class);
			}
		};

	}

	public iUpdateable asUpdateable(final PyObject o) {
		final CapturedEnvironment env = (CapturedEnvironment) PythonInterface.getPythonInterface().getVariable("_environment");
		return new iUpdateable() {

			public void update() {
				env.enter();
				try {
					o.__call__();
				} finally {
					env.exit();
				}
			}
		};

	}

	public iUpdateable toUpdateable(Object o) {
		if (o instanceof iUpdateable)
			return ((iUpdateable) o);
		if (o instanceof PyObject)
			return asUpdateable((PyObject) o);
		return null;
	}

	public Object call(iVisualElement forElement, Object function, Object arg) {

		PythonScriptingSystem pss = PythonScriptingSystem.pythonScriptingSystem.get(forElement);
		Promise p = pss.promiseForKey(forElement);
		p.beginExecute();
		try {
			if (function instanceof iFilter)
				return ((iFilter) function).filter(arg);
			if (function instanceof PyObject) {
				Object m = ((PyObject) function).__call__(Py.java2py(arg));
				if (m instanceof PyObject) {
					return ((PyObject) m).__tojava__(Object.class);
				}
				return m;
			}
			System.err.println(" can't call a <" + function + " / " + function.getClass() + ">");
			return null;
		} finally {

			p.endExecute();
		}
	}

	public byte castByte(int a) {
		return (byte) a;
	}

	public void cont(String name, PyReflectedFunction o, Class c, final PyFunction function) {
		if (contMap.containsKey(name)) {
			uncont(name);
		}
		Method m = (Method) ReflectionTools.illegalGetObject(Array.get(o.argslist, 0), "data");
		aRun r = new aRun() {
			PyObject[] a = null;

			@Override
			public ReturnCode head(Object calledOn, Object[] args) {

				if (a == null || a.length != args.length)
					a = new PyObject[args.length];
				for (int i = 0; i < a.length; i++) {
					a[i] = Py.java2py(args[i]);
				}

				function.__call__(a);

				return super.head(calledOn, args);
			}
		};
		Cont.linkWith_static(m, c, r);

		contMap.put(name, new Triple<aRun, Method, Class>(r, m, c));
	}

	public Map executeMaybeGenerator(PyFunction function, Map map) {
		if (map == null)
			map = new HashMap();

		if (map.containsKey(function)) {
			if (map.get(function) != null) {
				PyObject po = ((PyGenerator) map.get(function)).__iternext__();
				if (po == null) {
					map.put(function, null);
				}
			}
			;
		} else {
			PyObject r = function.__call__();
			if (r instanceof PyGenerator) {
				map.put(function, r);
				PyObject po = ((PyGenerator) map.get(function)).__iternext__();
				if (po == null) {
					map.put(function, null);
				}
			}
		}

		return map;
	}

	public Object fromXML(String s) {
		XStream stream = new XStream(new Sun14ReflectionProvider());
		stream.registerConverter(new ChannelSerializer());
		stream.registerConverter(new MarkerSerializer(stream.getClassMapper()));
		stream.setMarshallingStrategy(new ReferenceByIdMarshallingStrategy());
		return stream.fromXML(s);
	}

	public iFunction function(final PyFunction f) {
		return new iFunction() {
			public Object f(Object in) {
				PyObject ins = Py.java2py(in);
				PyObject g = f.__call__(new PyObject[] { ins }, new String[0]);
				return g.__tojava__(Object.class);
			}
		};
	}

	// doesn't compile against jython 2.5
	// public Object getLocalInGenerator(PyGenerator g, String
	// local) {
	// PyObject o = g.gi_frame.getlocal(local.intern());
	// if (o == null)
	// return null;
	// return o.__tojava__(Object.class);
	// }

	static public PythonUtils installed = null;

	public void install() {
		if (installed != null)
			return;

		PythonInterface.getPythonInterface().setVariable("u", this);
		PythonInterface.getPythonInterface().importJava("java.lang.System", "*");
		// PythonInterface.getPythonInterface().importJava("field.util",
		// "*");
		PythonInterface.getPythonInterface().importJava("field.util.PythonUtils", "*");
		PythonInterface.getPythonInterface().importJava("field.core.ui.text.referencealgorithms", "*");

		Launcher.getLauncher().registerUpdateable(new iUpdateable() {

			/** @see innards.iUpdateable#update() */
			public void update() {
				t++;
			}
		});

		installed = this;
	}

	public Object loadAsSerialization(String filename) {
		;//System.out.println(" reading from <" + filename + ">");
		try {
			ObjectInputStream input = new ObjectInputStream(new BufferedInputStream(new FileInputStream(new File(filename)))) {
				@Override
				protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
					String name = desc.getName();
					;//System.out.println(" resolving <" + name + ">");
					try {
						Class<?> c = Class.forName(name, false, Trampoline2.trampoline.getClassLoader());
						;//System.out.println(" found <" + c + ">");
						return c;
					} catch (ClassNotFoundException ex) {
						;//System.out.println(" didn't find <" + name + ">");
						return super.resolveClass(desc);
					}
				}
			};
			Object o = input.readObject();
			input.close();
			;//System.out.println(" read ");
			return o;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	public Object loadAsXML(String filename) {
		XStream stream = new XStream(new Sun14ReflectionProvider());
		stream.registerConverter(new ChannelSerializer());
		stream.registerConverter(new FloatBufferSerializer());
		stream.registerConverter(CachedLineCompression.converter);
		stream.registerConverter(new MarkerSerializer(stream.getClassMapper()));
		stream.setMarshallingStrategy(new ReferenceByIdMarshallingStrategy());

		try {
			ObjectInputStream input = stream.createObjectInputStream(new BufferedReader(new FileReader(new File(filename))));
			Object o = input.readObject();
			input.close();
			return o;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String pad(int n, int with) {
		String s = "" + n;
		while (s.length() < with)
			s = "0" + s;
		return s;
	}

	public void persistAsSerialization(Object o, String filename) {
		;//System.out.println(" writing to <" + filename + ">");
		try {
			ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(new File(filename))));
			oos.writeObject(o);
			oos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		;//System.out.println(" written ");
	}

	public void persistAsXML(Object o, String filename) {
		XStream stream = new XStream(new Sun14ReflectionProvider());
		stream.registerConverter(new ChannelSerializer());
		stream.registerConverter(new FloatBufferSerializer());
		stream.registerConverter(CachedLineCompression.converter);
		stream.registerConverter(new MarkerSerializer(stream.getClassMapper()));
		stream.setMarshallingStrategy(new ReferenceByIdMarshallingStrategy());

		try {
			ObjectOutputStream out = stream.createObjectOutputStream(new BufferedWriter(new FileWriter(new File(filename))));
			out.writeObject(o);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public iFloatProvider provider(final PyFunction f, final float def) {
		return new iFloatProvider() {
			/** @see innards.provider.iFloatProvider#evaluate() */
			public float evaluate() {
				PyObject o = f.__call__();
				Object r = o.__tojava__(Number.class);
				if (r == null) {
					return def;
				}
				return ((Number) r).floatValue();
			}
		};
	}

	public iFloatProvider provider(final PyGenerator f, final float def) {
		return new iFloatProvider() {
			/** @see innards.provider.iFloatProvider#evaluate() */
			public float evaluate() {
				PyObject o = f.__iternext__();
				if (o == null) {
					return def;
				}
				Object r = o.__tojava__(Number.class);
				if (r == null)
					return def;
				return ((Number) r).floatValue();
			}
		};
	}

	public iFloatProvider provider(final PyObject varname) {
		return new iFloatProvider() {
			public float evaluate() {
				return ((Number) varname.__tojava__(Number.class)).floatValue();
			}
		};
	}

	public iFloatProvider provider(final PyTuple varname) {
		return new iFloatProvider() {
			public float evaluate() {
				return ((Number) varname.__getitem__(0).__tojava__(Number.class)).floatValue();
			}
		};
	}

	public iFloatProvider provider(final String varname) {
		return new iFloatProvider() {
			public float evaluate() {
				return ((Number) PythonInterface.getPythonInterface().getVariable(varname)).floatValue();
			}
		};
	}

	public iDoubleProvider providerDouble(final PyFunction f, final float def) {
		return new iDoubleProvider() {
			/** @see innards.provider.iFloatProvider#evaluate() */
			public double evaluate() {
				PyObject o = f.__call__();
				Object r = o.__tojava__(Number.class);
				if (r == null) {
					return def;
				}
				return ((Number) r).doubleValue();
			}
		};
	}

	public iProvider<Object> providerObject(final PyFunction f, final CapturedEnvironment inside) {
		
		;//System.out.println(" creating provider object with environment <"+inside+">");
		
		return new iProvider<Object>() {
			/** @see innards.provider.iFloatProvider#evaluate() */
			public Object get() {
				inside.enter();
				try {
					PyObject o = f.__call__();
					Object r = o.__tojava__(Object.class);
					return r;
				} finally {
					inside.exit();
				}
			}
		};
	}

	public iProvider<Object> providerObject(final PyFunction f, final Object def) {
		return new iProvider<Object>() {
			/** @see innards.provider.iFloatProvider#evaluate() */
			public Object get() {
				PyObject o = f.__call__();
				Object r = o.__tojava__(Object.class);
				if (r == null)
					return def;
				return r;
			}
		};
	}

	public String regexp(String pattern, CharSequence match) {

		Matcher m = Pattern.compile(pattern).matcher(match);
		if (m.matches()) {
			String s = "matches: ";
			for (int i = 0; i < m.groupCount() + 1; i++) {
				s += "(" + m.group(i) + "), ";
			}
			return s;
		}
		Matcher f = Pattern.compile(pattern).matcher(match);
		if (f.find()) {
			String s = "finds: ";
			for (int i = 0; i < f.groupCount() + 1; i++) {
				s += "(" + f.group(i) + "), ";
			}
			return s;
		}
		return "no match";
	}

	public String regexpAll(String pattern, CharSequence match) {

		Matcher f = Pattern.compile(pattern).matcher(match);
		String r = "";
		while (f.find()) {
			String s = "finds: ";
			for (int i = 0; i < f.groupCount() + 1; i++) {
				s += "(" + f.group(i) + "), ";
			}
			r += s + "\n";
		}
		return r;
	}

	public void registerAdaptations(Adaptation adapt) {
		adapt.declare(PyDictionary.class, Map.class, new Adaptation.iAdaptor<PyDictionary, Map>() {
			public Map adapt(Class<PyDictionary> from, Class<Map> to, PyDictionary object) {
				PyObject p = object.iteritems();
				PyObject c = p.__iternext__();
				Map r = new HashMap();

				while (c != null) {
					r.put(c.__getitem__(0).__tojava__(Object.class), c.__getitem__(1).__tojava__(Object.class));
					c = p.__iternext__();
				}
				return r;
			}
		}, new iFloatProvider.Constant(0.5f));
	}

	public void salso(final String name, PyReflectedFunction o, Class c, final PyFunction function) {
		if (contMap.containsKey(name)) {
			unalso(name);
		}
		Method m = (Method) ReflectionTools.illegalGetObject(Array.get(o.argslist, 0), "data");
		aRun r = new aRun() {
			PyObject[] a = null;

			@Override
			public ReturnCode head(Object calledOn, Object[] args) {

				try {
					if (a == null || a.length != args.length)
						a = new PyObject[args.length];
					for (int i = 0; i < a.length; i++) {
						a[i] = Py.java2py(args[i]);
					}

					function.__call__(a);

					return super.head(calledOn, args);
				} catch (Exception e) {
					System.err.println(" exception thrown in safe also block:");
					e.printStackTrace();
					System.err.println(" deactivating also <" + name + ">");
					unalso(name);
					return ReturnCode.cont;
				}
			}
		};
		FastEntry.linkWith(m, c, r);

		contMap.put(name, new Triple<aRun, Method, Class>(r, m, c));
	}

	public void setLocalInGenerator(PyGenerator g, String local, Object o) {
		PyFrame f = (PyFrame) ReflectionTools.illegalGetObject(g, "gi_frame");
		String[] v = f.f_code.co_varnames;
		int i = 0;
		for (String s : v) {
			if (s.equals(local)) {
				f.setlocal(i, Py.java2py(o));
			}
			i++;
		}
	}

	public String split(String pattern, String match) {
		String[] s = match.split(pattern);
		return Arrays.asList(s) + "";
	}

	public void stack(final PyGenerator f, final CapturedEnvironment env) {
		PythonGeneratorStack x = new PythonGeneratorStack(f) {
			@Override
			protected void preamble() {
				super.preamble();
				env.enter();
			}

			@Override
			protected void postamble() {
				env.exit();
				super.postamble();
			}
		};
		registeredFunctions.put(f, x);
	}

	public void stack(final PyGenerator f, final CapturedEnvironment env, TaskQueue queue) {
		final PythonGeneratorStack x = new PythonGeneratorStack(f, null) {
			@Override
			protected void preamble() {
				super.preamble();
				env.enter();
			}

			@Override
			protected void postamble() {
				env.exit();
				super.postamble();
			}
		};
		
		queue.new Task() {
			
			@Override
			protected void run() {
				x.update();
				if (!x.isOver())
					recur();
			}
		};
	}
	public void stack(final PyGenerator f, TaskQueue queue) {
		stack(f, (CapturedEnvironment) PythonInterface.getPythonInterface().getVariable("_environment"), queue);
	}

	public PythonGeneratorStack mostRecentStack;

	public PythonGeneratorStack stackPrePost(final PyFunction pre, final PyGenerator g, final iAcceptor<Object> progress, final PyFunction post) {
		if (g == null)
			return null;

		PythonGeneratorStack x = new PythonGeneratorStack(g) {

			@Override
			protected void evaluatedTo(Object to) {
				if (progress != null)
					progress.set(to);
			}

			@Override
			protected void preamble() {
				if (pre != null)
					pre.__call__();
			}

			@Override
			protected void postamble() {
				if (post != null)
					post.__call__();
			}

			@Override
			protected void finished() {
				if (progress instanceof iPhasicAcceptor)
					((iPhasicAcceptor) progress).end();
			}

			@Override
			protected void first() {
				if (progress instanceof iPhasicAcceptor)
					((iPhasicAcceptor) progress).begin();
			}
		};
		registeredFunctions.put(g, x);

		mostRecentStack = x;

		return x;
	}

	public void stackPrePost(final PyFunction pre, final PySequence g, final iAcceptor<PyObject> progress, final PyFunction post) {
		if (g == null)
			return;

		final PyIterator gi = (PyIterator) g.__iter__();

		PythonGeneratorStack x = new PythonGeneratorStack(new iProvider<Object>() {
			public Object get() {
				PyObject a = gi.__iternext__();
				if (progress != null)
					progress.set(a);

				return a;
			}
		}) {

			@Override
			protected void preamble() {
				if (pre != null)
					pre.__call__();
			}

			@Override
			protected void postamble() {
				if (post != null)
					post.__call__();
			}

			@Override
			protected void finished() {
				if (progress instanceof iPhasicAcceptor)
					((iPhasicAcceptor) progress).end();
			}

			@Override
			protected void first() {
				if (progress instanceof iPhasicAcceptor)
					((iPhasicAcceptor) progress).begin();
			}
		};

		mostRecentStack = x;

		registeredFunctions.put(g, x);
	}

	public void stackPrePost(final PyGenerator g, final iAcceptor<Object> progress, final CapturedEnvironment env) {
		if (g == null)
			return;

		final PyIterator gi = (PyIterator) g.__iter__();

		PythonGeneratorStack x = new PythonGeneratorStack(g) {

			@Override
			protected void evaluatedTo(Object to) {
				iAcceptor<Object> set = progress.set(to);
			}

			@Override
			protected void preamble() {
				if (env != null)
					env.enter();
			}

			@Override
			protected void postamble() {
				if (env != null)
					env.exit();
			}

			@Override
			protected void finished() {
				if (progress instanceof iPhasicAcceptor)
					((iPhasicAcceptor) progress).end();
			}

			@Override
			protected void first() {
				if (progress instanceof iPhasicAcceptor)
					((iPhasicAcceptor) progress).begin();
			}
		};

		mostRecentStack = x;

		registeredFunctions.put(g, x);
	}

	public void stackPrePost(final PyFunction pre, final PyGenerator g, final PyFunction post) {
		if (g == null)
			return;
		PythonGeneratorStack x = new PythonGeneratorStack(new iProvider<Object>() {
			public Object get() {
				PyObject a = g.__iternext__();

				return a;
			}
		}) {
			@Override
			protected void preamble() {
				if (pre != null)
					pre.__call__();
			}

			@Override
			protected void postamble() {
				if (post != null)
					post.__call__();
			}
		};

		mostRecentStack = x;

		registeredFunctions.put(g, x);
	}

	public iUpdateable start(iUpdateable u) {
		Launcher.getLauncher().registerUpdateable(u);
		return u;
	}

	public void start(final PyFunction f) {
		iUpdateable up = new iUpdateable() {
			/** @see innards.iUpdateable#update() */
			public void update() {
				f.__call__();
			}
		};
		registeredFunctions.put(f, up);
		Launcher.getLauncher().registerUpdateable(up);
	}

	public void start(final PyFunction f, iRegistersUpdateable r) {
		iUpdateable up = new iUpdateable() {
			/** @see innards.iUpdateable#update() */
			public void update() {
				f.__call__();
			}
		};
		registeredFunctions.put(f, up);
		r.registerUpdateable(up);
	}

	public void start(final PyGenerator f) {

		start(f, (CapturedEnvironment)PythonInterface.getPythonInterface().getVariable("_environment"));
	}

	public void start(final PyGenerator f, final CapturedEnvironment e) {

		iUpdateable up = new iUpdateable() {
			public void update() {
				if (e!=null) e.enter();
				try {
					PyObject aa = f.__iternext__();
					if (aa == null)
						stop(f);
				} catch (Throwable t) {
					t.printStackTrace();
					stop(f);
				} finally {
					if (e!=null) e.exit();
				}
			}
		};
		registeredFunctions.put(f, up);
		Launcher.getLauncher().registerUpdateable(up);
	}

	public iUpdateable stop(iUpdateable u) {
		Launcher.getLauncher().deregisterUpdateable(u);
		return u;
	}

	public void stop(final PyFunction f) {
		iUpdateable up = (iUpdateable) registeredFunctions.remove(f);
		if (up != null) {
			try {
				Launcher.getLauncher().deregisterUpdateable(up);
			} catch (Throwable t) {
			}

		}
	}

	public void stop(final PyFunction f, iRegistersUpdateable r) {
		iUpdateable up = (iUpdateable) registeredFunctions.remove(f);
		if (up != null) {
			try {
				r.deregisterUpdateable(up);
			} catch (Throwable t) {
			}

		}
	}

	public void stop(final PyGenerator f) {
		iUpdateable up = (iUpdateable) registeredFunctions.remove(f);
		if (up != null) {
			try {
				Launcher.getLauncher().deregisterUpdateable(up);
			} catch (Throwable t) {
			}

		}
	}

	public Object stringToKeyOrString(String s) {
		Object ic = Key.internedKeys.get(s);
		if (ic instanceof Reference)
			ic = ((Reference) ic).get();

		return ic == null ? s : ic;
	}

	public Number toNumber(Object o) {
		if (o instanceof iFloatProvider)
			return ((iFloatProvider) o).evaluate();
		if (o instanceof iDoubleProvider)
			return ((iDoubleProvider) o).evaluate();
		if (o instanceof iProvider)
			return toNumber(((iProvider) o).get());

		if (o instanceof Number)
			return (Number) o;
		if (o instanceof PyIterator) {
			PyObject m = ((PyIterator) o).__iternext__();
			return toNumber(m);
		}
		if (o instanceof PyFunction) {
			return toNumber(((PyFunction) o).__call__());
		}
		if (o instanceof PySequence) {
			return toNumber(((PyObject) o).__getitem__(0));
		}
		if (o instanceof PyObject) {
			Object m = ((PyObject) o).__tojava__(Number.class);
			if (m instanceof Number) {
				return ((Number) m);
			}
			PyObject r = ((PyObject) o).__call__();
			return toNumber(r);
		}
		return null;
	}

	public String toXML(Object o, boolean noreturns) {
		XStream stream = new XStream(new Sun14ReflectionProvider());
		stream.registerConverter(new ChannelSerializer());
		stream.registerConverter(new MarkerSerializer(stream.getClassMapper()));
		stream.setMarshallingStrategy(new ReferenceByXPathMarshallingStrategy());

		String s = stream.toXML(o);
		if (noreturns)
			s = s.replace("\n", "");
		return s;
	}

	public void unalso(String name) {
		Triple<aRun, Method, Class> n = contMap.get(name);
		if (n != null) {
			FastEntry.unlinkWith(n.middle, n.right, n.left);
		}
	}

	public void uncont(String name) {
		Triple<aRun, Method, Class> n = contMap.get(name);
		if (n != null) {
			Cont.unlinkWith_static(n.middle, n.right, n.left);
		}
	}

	public String whatIs(Object o) {
		String r = o + " " + o.getClass();
		if (o instanceof PyInstance) {
			if (((PyInstance) o).instclass.__name__.equals("ct")) {
				r += " is context ";
			}
			r += ((PyInstance) o).__getattr__("where");
		}
		return r;
	}

	// beta1
	static public Object maybeToJava(Object instance) {
		if (instance instanceof PyObject)
			try {
				Object o = ((PyObject) instance).__tojava__(Object.class);
				if (o == Py.NoConversion)
					return instance;
				if (o == null)
					return instance;
				return o;
			} catch (Exception e) {
				// beta1
				e.printStackTrace();
				return instance;
			}
		return instance;
	}

	static public String info(Object o) {
		if (o == null)
			return "null reference";
		if (o instanceof Class)
			return "class <" + o + "> <" + ((Class) o).getClassLoader() + ">";
		String x = o.toString() + " class " + o.getClass() + " " + o.getClass().getSuperclass();
		x += "\n" + Arrays.asList(o.getClass().getDeclaredMethods());

		System.err.println(" info is <" + x + ">");
		return x;
	}

	static public Class getCallerClass() {
		try {
			Method m = System.class.getDeclaredMethod("getCallerClass");
			m.setAccessible(true);
			Class cc = (Class) m.invoke(null);
			;//System.out.println(cc.getClassLoader());
			return cc;
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		return null;
	}

	static public void printStackTraceNow() {
		new Exception().printStackTrace();
	}

	public void callLater(PyFunction f) {
		final Callable c = PythonOverridden.callableForFunction(f, (CapturedEnvironment) PythonInterface.getPythonInterface().getVariable("_environment"));
		Launcher.getLauncher().registerUpdateable(new iUpdateable() {
			public void update() {
				c.call(null, new Object[] {});
				Launcher.getLauncher().deregisterUpdateable(this);
			}
		});
	}

	public void callLater(PyFunction f, final int n) {
		final Callable c = PythonOverridden.callableForFunction(f, (CapturedEnvironment) PythonInterface.getPythonInterface().getVariable("_environment"));
		Launcher.getLauncher().registerUpdateable(new iUpdateable() {
			int y = 0;

			public void update() {
				if (y++ == n) {
					c.call(null, new Object[] {});
					Launcher.getLauncher().deregisterUpdateable(this);
				}
			}
		});
	}

	public void callLater(PyFunction f, TaskQueue q) {
		final Callable c = PythonOverridden.callableForFunction(f, (CapturedEnvironment) PythonInterface.getPythonInterface().getVariable("_environment"));
		q.new Task() {
			@Override
			protected void run() {
				c.call(null, new Object[] {});
			}
		};
	}

}