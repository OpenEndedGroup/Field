package field.extras.max;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.python.apache.xerces.impl.dv.util.Base64;
import org.python.core.CompileMode;
import org.python.core.CompilerFlags;
import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyFile;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PySystemState;

import com.cycling74.jitter.JitterMatrix;
import com.cycling74.max.Atom;
import com.cycling74.max.Executable;
import com.cycling74.max.MaxClock;
import com.cycling74.max.MaxObject;

import field.extras.max.FieldMaxPyObjectAdaptor.iHandlesAttributes;

public class MaxField extends MaxObject implements iHandlesAttributes {
	{
		System.out.println(" working around jffi issues B");
		try {
			Method m = MaxField.class.getClassLoader().getClass().getDeclaredMethod("definedPackage", String.class, String.class, String.class, String.class, String.class, String.class, String.class, URL.class);
			m.invoke("com.kenai.jffi", null, null, null, null, null, null, null);
		} catch (Exception e) {
		}
	}
	static public MaxFieldRoot thisRoot;

	public PyDictionary globals = new PyDictionary();
	static public PySystemState state = new PySystemState();
	static public CompilerFlags cflags = new CompilerFlags();

	private PyDictionary locals = new PyDictionary();

	private OutputStream outputStream;

	public Set<MaxClock> ongoingClocks = new LinkedHashSet<MaxClock>();

	public MaxField(Atom[] args) {

		Py.setSystemState(state);

		state.setClassLoader(this.getClass().getClassLoader());
		Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
		FieldMaxPyObjectAdaptor.initialize();
		globals.put(new PyString("_"), Py.java2py(this));

		lateInit();
		declareIO(4, 4);
	}

	boolean inited = false;

	protected void lateInit() {
		if (inited)
			return;
		inited = true;
		try {
			FileInputStream fis = new FileInputStream(new File(getBootDir() + "../python/maxfield/__init__.py"));

			Py.getSystemState().path.add(getBootDir());
			Py.getSystemState().path.add(getBootDir() + "../../../lib/python");

			Py.exec(Py.compile_flags(fis, "__boot__", CompileMode.exec, cflags), globals, globals);
			fis.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String getBootDir() {
		Object m = System.getProperties().get("java.class.path");
		String[] mm = m.toString().split(":");
		for (String mmm : mm) {

			;// ;//System.out.println(" checking <" + mmm + ">");

			if (new File(mmm + "/../python/maxfield/__init__.py").exists()) {
				return mmm + "/";
			}
		}

		;// ;//System.out.println(" code source is :"+this.getClass().getProtectionDomain().getCodeSource().getLocation());
		if (new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath() + "/../python/maxfield/__init__.py").exists()) {
			return this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath() + "/../python/";
		}

		System.err.println(" warning: couldn't find Field python classes");
		return null;
	}

	@Override
	protected void loadbang() {
		super.loadbang();
		lateInit();
	}

	@Override
	protected void anything(String message, Atom[] args) {
		lateInit();

		if (message.equals("__data__")) {

			String called = args[0].getString();
			byte[] decoded = new Base64().decode(args[1].getString());
			try {
				Object d = new ObjectInputStream(new ByteArrayInputStream(decoded)).readObject();
				// globals.put(new PyString(called.intern()),
				// Py.java2py(d));

				insideUnderscore.put(called, d);

				;// ;//System.out.println(" set <" + called +
					// "> to <" + d + ">");

			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}

			return;
		}

		try {
			state.stdout = new PyFile(thisRoot.printout);
			state.stderr = new PyFile(thisRoot.printerr);
			Py.exec(Py.compile_flags(message, "<string>", CompileMode.exec, cflags), globals, globals);
			thisRoot.printout.flush();
			thisRoot.printerr.flush();
		} catch (Exception e) {
			MaxObject.error("Python threw an exception <" + e + ">");

			try {
				thisRoot.printerr.write(("Max / Python threw an exception <" + e + ">\n").getBytes());
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw, true));
				sw.flush();
				thisRoot.printerr.write((sw.toString() + "\n").getBytes());
				thisRoot.printout.flush();
				thisRoot.printerr.flush();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	public MaxFieldRoot getRoot() {
		return thisRoot;
	}

	public MaxClock callInDelay(int inms, final PyObject call, final PyObject[] args) {
		lateInit();

		final MaxClock[] cr = { null };

		MaxClock c = new MaxClock(new Executable() {

			public void execute() {
				ongoingClocks.remove(cr[0]);

				state.stdout = new PyFile(thisRoot.printout);
				state.stderr = new PyFile(thisRoot.printerr);
				call.__call__(state, args, new String[] {});
				try {
					thisRoot.printout.flush();
					thisRoot.printerr.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		c.delay(inms);
		cr[0] = c;
		ongoingClocks.add(c);
		return c;
	}

	public void cancelAllTasks() {
		for (MaxClock cc : ongoingClocks) {
			cc.release();
		}
		ongoingClocks.clear();
	}

	@Override
	protected void notifyDeleted() {
		cancelAllTasks();
		super.notifyDeleted();
		thisRoot.notifyDeleted(this);
	}

	public PythonCallableMap _float = new PythonCallableMap();
	public PythonCallableMap _int = new PythonCallableMap();
	public PythonCallableMap _listAtoms = new PythonCallableMap();
	public PythonCallableMap _listFloat = new PythonCallableMap();
	public PythonCallableMap _listInt = new PythonCallableMap();
	public PythonCallableMap _bang = new PythonCallableMap();
	public PythonCallableMap _doubleClick = new PythonCallableMap();

	public PythonCallableMap _matrix = new PythonCallableMap();

	public void jit_matrix(String name) {
		lateInit();
		_matrix.invoke(new JitterMatrix(name));
	}

	@Override
	protected void inlet(float value) {
		lateInit();
		_float.invoke(getInlet(), value);
	}

	@Override
	protected void dblclick() {
		lateInit();
		_doubleClick.invoke();
	}

	@Override
	protected void bang() {
		lateInit();
		_bang.invoke(getInlet());
	}

	@Override
	protected void inlet(int value) {
		lateInit();
		_int.invoke(getInlet(), value);
	}

	@Override
	protected void list(Atom[] value) {
		lateInit();
		_listAtoms.invoke(getInlet(), value);
	}

	@Override
	protected void list(float[] value) {
		lateInit();
		_listFloat.invoke(getInlet(), value);
	}

	@Override
	protected void list(int[] value) {
		lateInit();
		_listInt.invoke(getInlet(), value);
	}

	Map<String, Object> insideUnderscore = new HashMap<String, Object>();

	public Object getAttribute(String name) {
		lateInit();
		return insideUnderscore.get(name);
	}

	public void setAttribute(String name, Object value) {
		lateInit();
		insideUnderscore.put(name, value);

		if (name != null) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				ObjectOutputStream oos = new ObjectOutputStream(baos);
				oos.writeObject(value);
				oos.close();
				String encodedValue = new Base64().encode(baos.toByteArray());

				// ;//;//System.out.println(" sending back data <"+this.getMaxBox().getName()+"> <"+name+"> <"+encodedValue+">");

				thisRoot.out.simpleSend("/data", thisRoot.getPath(this.getMaxBox()), name, encodedValue);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

}
