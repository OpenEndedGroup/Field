package field.core.execution;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.python.core.CompileMode;
import org.python.core.CompilerFlags;
import org.python.core.Options;
import org.python.core.Py;
import org.python.core.PyCode;
import org.python.core.PyException;
import org.python.core.PyFile;
import org.python.core.PyFrame;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyStringMap;
import org.python.core.PySystemState;
import org.python.core.PyTraceback;
import org.python.core.ThreadState;
import org.python.core.TraceFunction;
import org.python.util.PythonInterpreter;

import field.bytecode.protect.Trampoline2;
import field.core.dispatch.iVisualElement;
import field.core.plugins.log.ElementInvocationLogging;
import field.core.plugins.log.Logging;
import field.core.plugins.log.Logging.iLoggingEvent;
import field.core.plugins.selection.PopupInfoWindow;
import field.core.ui.text.util.IndentationUtils;
import field.core.util.BetterPythonConstructors;
import field.launch.Launcher;
import field.launch.SystemProperties;
import field.launch.iUpdateable;
import field.namespace.generic.Bind.iFunction;
import field.namespace.generic.ReflectionTools;

public class PythonInterface implements ScriptingInterface {
	
	public HashMap<String, iFunction<PyObject, String>> specialVariables_read = new HashMap<String, iFunction<PyObject, String>>();
	public HashMap<String, iFunction<PyObject, PyObject>> specialVariables_write = new HashMap<String, iFunction<PyObject, PyObject>>();
	
	public final class LocalDictionary extends PyStringMap implements iLocalDictionary {
		
		@Override
		public PyObject __finditem__(String arg0) {
			PyObject r = super.__finditem__(arg0);
			try {
				iFunction<PyObject, String> s = specialVariables_read.get(arg0);
				if (s!=null) return s.f(arg0);
				
				if (globalTrap.size() > 0 && !arg0.startsWith("_")) {
					topic = arg0;
					r = (PyObject) globalTrap.peek().findItem(arg0, r);
				}
				return r;
			} catch (Throwable t) {
				return r;
			}
		}
		
		@Override
		public void __setitem__(String arg0, PyObject arg1) {
			try {
				iFunction<PyObject, PyObject> g = specialVariables_write.get(arg0);
				if (g!=null)
				{
					arg1 = g.f(arg1);
				}
				if (globalTrap.size() > 0) {
					PyObject was = super.__finditem__(arg0);
					arg1 = (PyObject) globalTrap.peek().setItem(arg0, was, arg1);
					
					if (shared.size() > 0 && !arg0.startsWith("_")) {
						topic = arg0;
						insideShared = true;
						try {
							Object t = arg1.__tojava__(Object.class);
							if (t != Py.NoConversion)
								for (ScriptingInterface s : shared) {
									s.setVariable(arg0, t);
								}
						} finally {
							insideShared = false;
						}
						
					}
				}
			} catch (Throwable t) {
			}
			super.__setitem__(arg0, arg1);
		}
		
		public PyObject __superfinditem__(String arg0) {
			return super.__finditem__(arg0);
		}
	}
	
	private static PythonInterface pythonInterface;
	
	static public PythonInterface getPythonInterface() {
		if (pythonInterface == null) {
			init();
		}
		return pythonInterface;
	}
	
	private static void init() {
		pythonInterface = new PythonInterface();
	}
	
	private final LocalDictionary localDictionary;
	
	private final PythonInterpreter interpreter;
	
	public Writer errOut;
	
	private Throwable lastError;
	
	Stack<iGlobalTrap> globalTrap = new Stack<iGlobalTrap>();
	
	Stack<Writer> outputRedirects = new Stack<Writer>();
	
	Stack<Writer> errorRedirects = new Stack<Writer>();
	
	List<ScriptingInterface> shared = new ArrayList<ScriptingInterface>();
	
	private ExecutionMonitor monitor;
	
	private PySystemState state;
	
	private ThreadState ts;
	
	protected PythonInterface() {
		pythonInterface = this;
		
		monitor = new ExecutionMonitor();
		// System.setProperty("python.verbose", "debug");
		// System.setProperty("python.security.respectJavaAccessibility",
		// "false");
		
		System.out.println(" python path is <" + System.getProperty("python.path") + "> classloader is <" + this.getClass().getClassLoader() + ">");
		
		localDictionary = new LocalDictionary();
		state = new PySystemState();
		state.setClassLoader(this.getClass().getClassLoader());
		
		interpreter = new PythonInterpreter(localDictionary, state);
		
		Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
		
		// interpreter = new PythonInterpreter(state);
		
		// ReflectionTools.illegalSetObject(interpreter, "cflags", new
		// CompilerFlags(org.python.core.PyTableCode.CO_GENE
		// RATOR_ALLOWED));
		BetterPythonConstructors.load();

		execString("from __future__ import with_statement");
		execString("from field.util import PythonUtils");
		execString("from field.core.plugins.python import PythonPlugin");
		execString("from java.util import ArrayList");
		execString("from FluidTools import *");
		execString("from documentationHelper import *");
		execString("from FluidTools import __tokenizeHelper");
		execString("from field.math.linalg import *");
		execString("from Selection import *");
		execString("from TweakTools import *");
		execString("from field.core.execution import TemporalSliderOverrides");
		execString("import inspect");
		execString("import cPickle");
		execString("from field.core.ui import MarkingMenuBuilder");
		execString("from java.lang import Math");
		execString("from java.lang import System");
		execString("from field.util.PythonUtils import OKeyByName");
		execString("from field.util.PythonUtils import FKeyByName");
		execString("\n" + "from field.core.ui.text.embedded import FreezeProperties\n" + "from field.core.ui.text.embedded.FreezeProperties import Freeze\n" + "");
		execString("import TextTransforms\n" + "from TextTransforms import *");
		execString("from LineInteractionTools import *");
		execString("from field.core.plugins.log.KeyframeGroupOverride import Position");
		execString("from field.core.dispatch import iVisualElementOverrides\n" + "from field.core.dispatch.iVisualElementOverrides import DefaultOverride\n" + "from field.core.windowing.components import DraggableComponent\n");
		execString("from field.core.plugins.history import Templating");
		execString("from field.core.plugins.python.PythonPluginEditor import makeBoxLocalEverywhere");
		execString("_ex = ExtendedAssignment()");
		execString("sys.executable=\"\"");
		execString("from FluidTools import _now");
		List<String> ex = Trampoline2.extendedClassPaths;
		for (String s : ex) {
			if (s.endsWith(".jar")) {
				System.out.println(" add package <" + s + ">");
				PySystemState.add_package(s);
			} else {
				System.out.println(" add classdir <" + s + ">");
				PySystemState.add_classdir(s);
			}
		}
		execString("from array import array");
		execString("from java.lang import Runtime\n" + "from field.core.execution import PythonInterface\n" + "\n" + "import signal\n" + "\n" + "def __signal_handler( signal_number ):\n" + "	def __decorator( function ):\n" + "		was = signal.signal( signal_number, function )\n" + "		print was\n" + "		return function\n" + "	\n" + "	return __decorator\n" + "\n" + "@__signal_handler(signal.SIGINT)\n" + "def __forceExit(a,b):\n" + "	PythonInterface.getPythonInterface().forceExit()\n" + "\n" + "");
		execString("from NewCachedLines import CFrame, FLine");
        
		String extensionsDir = SystemProperties.getProperty("extensions.dir", "../../extensions/");
		addExtensionsDirectory(new File(extensionsDir));
		
		Options.includeJavaStackInExceptions = true;
		
		execString("from __builtin__ import zip");
        
		ts = Py.getThreadState(state);
		
	}
	
	// scan the directory for .py files. open them, see if they are
	// marked as "field-library", and execute them if needed,
	// otherwise, import them.
	protected void addExtensionsDirectory(File file) {
		
		String[] pythonFiles = file.list(new FilenameFilter() {
			
			public boolean accept(File dir, String name) {
				return name.endsWith(".py");
			}
		});
		if (pythonFiles == null)
			return;
		
		for (String p : pythonFiles) {
			try {
				BufferedReader reader;
				reader = new BufferedReader(new FileReader(new File(file.getAbsolutePath() + "/" + p)));
				String firstLine = reader.readLine();
				if (firstLine.contains("field-library")) {
					System.err.println(" --- executing file from extensions directory <" + file.getAbsolutePath() + ">");
					executeFile(file.getAbsolutePath() + "/" + p);
				} else {
					System.err.println(" --- importing module from extensions directory <" + p.replace(".py", "") + ">");
					execString("from " + p.replace(".py", "") + " import *");
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void addSharedScriptingInterface(ScriptingInterface s) {
		shared.add(s);
	}
	
	public Object eval(String fragment) {
		
		monitor.enter();
		try {
			
			fragment = clean(fragment);
			
			try {
				fragment = fragment.trim();
				PyObject o = eval(interpreter, fragment);
				return o;
			} catch (Throwable t) {
				if (errOut == null)
					t.printStackTrace();
				else {
					if (t instanceof PyException) {
						PrintWriter w = new PrintWriter(errOut);
						writeException(t, w);
						
					} else {
						t.printStackTrace(new PrintWriter(errOut));
						handlePythonException(null, null, t);
					}
				}
			}
			return null;
		} finally {
			monitor.exit();
		}
	}
	
	public void execString(String fragment) {
		
		long in = System.currentTimeMillis();
		monitor.enter();
		try {
			
			fragment = clean(fragment);
			
			try {
				exec(interpreter, fragment);
			} catch (Throwable t) {
				if (errOut == null)
					t.printStackTrace();
				else {
					if (t instanceof PyException) {
						PrintWriter w = new PrintWriter(errOut);
						String s = "";
						try {
							writeException(t, w);
							
						} catch (Throwable t2) {
							System.err.println(" throw another exception ");
							w.println("python exception <" + ((PyException) t).value.__tojava__(Object.class) + ">");
							w.println("on line " + ((((PyException) t).traceback != null ? ((PyException) t).traceback.tb_lineno : "null")));
							t.printStackTrace(w);
							t.printStackTrace(System.err);
							
							handlePythonException(null, null, t);
							
							t2.printStackTrace();
						}
					} else {
						lastError = t;
						t.printStackTrace(System.err);
						t.printStackTrace(new PrintWriter(errOut));
						handlePythonException(null, null, t);
					}
				}
			}
		} finally {
			monitor.exit();
			long out = System.currentTimeMillis();
		}
		
	}
	
	public void execStringWithContinuation(String fragment, iUpdateable waiting, iUpdateable ending) {
		
		long in = System.currentTimeMillis();
		monitor.enter();
		try {
			
			fragment = clean(fragment);
			
			try {
				exececuteWithWaitContinuation(interpreter, fragment, waiting, ending);
			} catch (Throwable t) {
				t.printStackTrace(System.err);
				if (errOut == null)
					t.printStackTrace();
				else {
					if (t instanceof PyException) {
						PrintWriter w = new PrintWriter(errOut);
						String s = "";
						try {
							writeException(t, w);
							
						} catch (Throwable t2) {
							System.err.println(" throw another exception ");
							w.println("python exception <" + ((PyException) t).value.__tojava__(Object.class) + ">");
							w.println("on line " + ((((PyException) t).traceback != null ? ((PyException) t).traceback.tb_lineno : "null")));
							t.printStackTrace(w);
							t.printStackTrace(System.err);
							
							handlePythonException(null, null, t);
							
							t2.printStackTrace();
						}
					} else {
						lastError = t;
						t.printStackTrace(System.err);
						t.printStackTrace(new PrintWriter(errOut));
						handlePythonException(null, null, t);
					}
				}
			}
		} finally {
			monitor.exit();
			long out = System.currentTimeMillis();
		}
		
	}
	
	static public void writeException(Throwable t) {
		t.printStackTrace(System.err);
		if (t instanceof PyException) {
			PrintWriter w = new PrintWriter(PythonInterface.getPythonInterface().errOut);
			PythonInterface.getPythonInterface().writeException(t, w);
			
			Throwable cause = t.getCause();
			if (cause!=null)
			{
				writeException(cause);
			}
			
		} else {
			t.printStackTrace();
			Throwable cause = t.getCause();
			if (cause!=null)
			{
				writeException(cause);
			}
		}
	}
	
	private void writeException(Throwable t, PrintWriter w) {
		w.println("python exception <" + ((PyException) t).value + ">");
		try {
			w.println("on line " + ((PyException) t).traceback.tb_lineno + " " + moduleNameFor(((PyException) t).traceback.tb_frame.f_code.co_filename) + " / " + ((PyException) t).traceback.tb_frame.f_code.co_name + " " + HyperlinkedErrorMessage.error(((PyException) t).traceback.tb_frame.f_code.co_filename, ((PyException) t).traceback.tb_lineno));
			PyTraceback n = (PyTraceback) (((PyException) t).traceback.tb_next);
			while (n != null) {
				w.println("from line " + n.tb_lineno + " " + moduleNameFor(n.tb_frame.f_code.co_filename) + " / " + n.tb_frame.f_code.co_name + " " + HyperlinkedErrorMessage.error(n.tb_frame.f_code.co_filename, n.tb_lineno));
				n = (PyTraceback) n.tb_next;
			}
			lastError = t;
            //			t.printStackTrace(System.err);
			
			Throwable cause = t.getCause();
			if (cause!=null) 
			{
				Throwable last = cause;
				while(cause!=null)
                {
					last = cause;
                    cause = cause.getCause();
                }
                
				if (last instanceof PyException)
					writeException(last, w);
				else
				{
                    //					last.printStackTrace(w);
					StackTraceElement[] s = last.getStackTrace();
					for(int i=0;i<s.length;i++)
					{
						w.println(" from java - "+s[i]);
						if (!s[i].getClassName().startsWith("java.")) break;
					}
				}
			}
			
		} catch (Exception e) {
		}
		
		handlePythonException(null, null, t);
	}
	
	private String moduleNameFor(String m) {
		if (m.indexOf("[") == -1)
			return m;
		return m.split("\\[")[0];
	}
	
	private PyObject eval(final PythonInterpreter i, final String fragment) {
        //		if (ThreadedLauncher.isTimer2Thread())
        return i.eval(fragment);
        //		else {
        //			final PyObject[] ret = { null };
        //			final boolean[] run = { false };
        //			
        //			final Condition c = ThreadedLauncher.lock2.newCondition();
        //			ThreadedLauncher.timer2Tasks.new Task() {
        //				
        //				@Override
        //				protected void run() {
        //					try {
        //						ret[0] = eval(i, fragment);
        //					} finally {
        //						run[0] = true;
        //						c.signal();
        //					}
        //				}
        //			};
        //			
        //			while (!run[0]) {
        //				ThreadedLauncher.lock2.lock();
        //				try {
        //					c.await();
        //				} catch (InterruptedException e) {
        //					e.printStackTrace();
        //				} finally {
        //					ThreadedLauncher.lock2.unlock();
        //				}
        //			}
        //			return ret[0];
        //		}
	}
	
	String moduleName = "<unknown>";
	
	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}
	
	public String getModuleName() {
		return moduleName;
	}
	
	private void exec(final PythonInterpreter i, final String f) {
		
		//System.out.println(" inside exec <" + i + " " + f + "> <" + Thread.currentThread() + ">");
		//new Exception().printStackTrace();
		
        //		if (ThreadedLauncher.isTimer2Thread()) {
        // i.exec(f);
        // yuck
        
        PyCode code = Py.compile_flags(f, moduleName, CompileMode.exec, (CompilerFlags) ReflectionTools.illegalGetObject(i, "cflags"));
        i.exec(code);
        
        return;
        //		} else {
        //			final boolean[] run = { false };
        //			final Condition c = ThreadedLauncher.lock2.newCondition();
        //			
        //			ThreadedLauncher.timer2Tasks.new Task() {
        //				@Override
        //				protected void run() {
        //					// synchronized (run)
        //					// {
        //					
        //					try {
        //						System.out.println(" thread2 about to exec");
        //						i.exec(f);
        //					} finally {
        //						run[0] = true;
        //						c.signal();
        //					}
        //					// }
        //				}
        //			};
        //			while (!run[0]) {
        //				System.out.println(" thread1 about to go sync");
        //				while (!run[0]) {
        //					
        //					System.out.println(" waiting ...");
        //					try {
        //						Thread.sleep(100);
        //					} catch (InterruptedException e1) {
        //						e1.printStackTrace();
        //					}
        //					
        //					try {
        //						if (ThreadedLauncher.lock2.tryLock(10, TimeUnit.MILLISECONDS)) {
        //							try {
        //								
        //								System.out.println(" got lock? ");
        //								
        //								c.await(10, TimeUnit.MILLISECONDS);
        //								System.out.println(" await finished ");
        //							} catch (InterruptedException e) {
        //								e.printStackTrace();
        //							} finally {
        //								ThreadedLauncher.lock2.unlock();
        //							}
        //						}
        //					} catch (InterruptedException e) {
        //						e.printStackTrace();
        //					}
        //					
        //				}
        //				
        //			}
        //		}
	}
	
	public void exececuteWithWaitContinuation(final PythonInterpreter i, final String f, final iUpdateable next, final iUpdateable end) {
		
        //		if (ThreadedLauncher.isTimer2Thread()) {
        // i.exec(f);
        // yuck
        
        PyCode code = Py.compile_flags(f, moduleName, CompileMode.exec, (CompilerFlags) ReflectionTools.illegalGetObject(i, "cflags"));
        i.exec(code);
        end.update();
        
        return;
        //		} else {
        //			final boolean[] run = { false };
        //			final Condition c = ThreadedLauncher.lock2.newCondition();
        //			
        //			ThreadedLauncher.timer2Tasks.new Task() {
        //				@Override
        //				protected void run() {
        //					// synchronized (run)
        //					// {
        //					
        //					try {
        //						System.out.println(" thread2 about to exec");
        //						i.exec(f);
        //					} finally {
        //						run[0] = true;
        //						c.signal();
        //					}
        //					// }
        //				}
        //			};
        //			park(next, end, run, c);
        //		}
	}
	
    //	private void park(final iUpdateable next, final iUpdateable end, final boolean[] run, final Condition c) {
    //		System.out.println(" thread1 about to go sync");
    //		while (!run[0]) {
    //			
    //			System.out.println(" waiting ...");
    //			try {
    //				Thread.sleep(100);
    //			} catch (InterruptedException e1) {
    //				e1.printStackTrace();
    //			}
    //			
    //			try {
    //				if (ThreadedLauncher.lock2.tryLock(10, TimeUnit.MILLISECONDS)) {
    //					try {
    //						
    //						System.out.println(" got lock? ");
    //						
    //						c.await(10, TimeUnit.MILLISECONDS);
    //						System.out.println(" await finished ");
    //					} catch (InterruptedException e) {
    //						e.printStackTrace();
    //					} finally {
    //						ThreadedLauncher.lock2.unlock();
    //					}
    //				}
    //			} catch (InterruptedException e) {
    //				e.printStackTrace();
    //			}
    //			
    //			if (!run[0]) {
    //				Launcher.getLauncher().setContinuation(new iContinuation() {
    //					
    //					@Override
    //					public void next() {
    //						System.out.println(" about to wait");
    //						next.update();
    //						System.out.println(" about to park again");
    //						park(next, end, run, c);
    //					}
    //				});
    //				return;
    //			}
    //		}
    //		
    //		Launcher.getLauncher().setContinuation(new iContinuation() {
    //			
    //			@Override
    //			public void next() {
    //				end.update();
    //			}
    //		});
    //	}
	
	public void executeFile(String filename) {
		
		try {
			BufferedReader r = new BufferedReader(new FileReader(new File(filename)));
			
			String s = "";
			while (r.ready()) {
				s += r.readLine() + "\n";
			}
			execString(s);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public PyObject executeStringReturnPyObject(String script, String tag) {
		script = clean(script);
		try {
			// System.out.println(" executing\n"+script);
			exec(interpreter, script);
			PyObject py = (interpreter).get(tag);
			return py;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}
	
	public Object executeStringReturnRawValue(String script, String tag) {
		script = clean(script);
		try {
			exec(interpreter, script);
			PyObject py = (interpreter).get(tag);
			return py;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}
	
	protected String topic;
	
	public void clearTopic() {
		topic = null;
	}
	
	public String getTopic() {
		return topic;
	}
	
	public Object executeStringReturnValue(String script, String tag) {
		script = clean(script);
		
		try {
			
			exec(interpreter, script);
			
			PyObject py = (interpreter).get(tag);
			
			if (py == null)
				return null;
			
			Object result = py.__tojava__(Object.class);
			
			return result;
		} catch (Throwable ex) {
			writeException(ex);
			// ex.printStackTrace();
			IllegalArgumentException a = new IllegalArgumentException(ex);
			throw a;
		}
	}
	
	public Language getLanguage() {
		return Language.python;
	}
	
	public Throwable getLastError() {
		return lastError;
	}
	
	public LocalDictionary getLocalDictionary() {
		return localDictionary;
	}
	
	public Stack<Writer> getOutputRedirects() {
		return outputRedirects;
	}
	
	public Stack<Writer> getErrorRedirects() {
		return errorRedirects;
	}
	
	public Object getVariable(String name) {
		Object o = null;
		try {
			PyObject pob = interpreter.get(name);
			if (pob == null)
				return null;
			o = pob.__tojava__(Object.class);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return o;
	}
	
	public Map<String, Object> getVariables() {
		
		PyList keys = ((PyStringMap) interpreter.getLocals()).keys();
		
		Object o = ((PyStringMap) (interpreter.getLocals())).values();
		if (o instanceof PyList) {
			PyList values = (PyList) o;
			
			HashMap<String, Object> ret = new HashMap<String, Object>();
			
			for (int i = 0; i < keys.__len__(); i++) {
				PyObject key = keys.__getitem__(i);
				PyObject value = values.__getitem__(i);
				ret.put((String) key.__tojava__(String.class), value.__tojava__(Object.class));
			}
			return ret;
		} else {
			Collection values = (Collection) o;
			HashMap<String, Object> ret = new HashMap<String, Object>();
			
			Iterator vi = values.iterator();
			for (int i = 0; i < keys.__len__(); i++) {
				try {
					PyObject key = keys.__getitem__(i);
					Object value = vi.next();
					ret.put((String) key.__tojava__(String.class), value);
				} catch (NoClassDefFoundError e) {
				}
				;
			}
			return ret;
			
		}
	}
	
	public void importJava(String pack, String clas) {
		
		System.out.println("exec<" + "from " + pack + " import " + clas + ">");
		execString("from " + pack + " import " + clas);
	}
	
	public void popGlobalTrap() {
		globalTrap.pop();
	}
	
	boolean insideShared = false;
	
	public void popOutput() {
		if (insideShared)
			return;
		insideShared = true;
		try {
			if (outputRedirects.size() > 0)
				outputRedirects.pop();
			if (errorRedirects.size() > 0)
				errorRedirects.pop();
			
			if (outputRedirects.size() > 0)
				interpreter.setOut(wrapWriter(outputRedirects.peek()));
			// else
			// interpreter.setOut(System.out);
			
			if (errorRedirects.size() > 0) {
				interpreter.setErr(wrapWriter(errorRedirects.peek()));
				errOut = errorRedirects.peek();
			} else {
				// interpreter.setErr(System.err);
				// errOut = null;
			}
			
			for (ScriptingInterface s : shared)
				s.popOutput();
		} finally {
			insideShared = false;
		}
	}
	
	public void print(String p) {
		((PyFile) state.stdout).write(p);
		((PyFile) state.stdout).flush();
	}
	
	public void printError(String p) {
		((PyFile) state.stderr).write(p);
		((PyFile) state.stderr).flush();
	}
	
	public void pushGlobalTrap(iGlobalTrap gt) {
		globalTrap.add(gt);
	}
	
	public void pushOutput(Writer output, Writer error) {
		if (insideShared)
			return;
		insideShared = true;
		try {
			outputRedirects.add(output);
			errorRedirects.add(error);
			interpreter.setErr(wrapWriter(error));
			interpreter.setOut(wrapWriter(output));
			errOut = error;
			for (ScriptingInterface s : shared)
				s.pushOutput(output, error);
		} finally {
			insideShared = false;
		}
	}
	
	private OutputStream wrapWriter(final Writer error) {
		return new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				error.append((char) b);
			}
		};
	}
	
	public void setVariable(String name, Object value) {
		if (insideShared)
			return;
		insideShared = true;
		try {
			interpreter.set(name, Py.java2py(value));
			for (ScriptingInterface s : shared)
				s.setVariable(name, value);
		} finally {
			insideShared = false;
		}
	}
	
	/**
	 * the automagic character \u000b means 'indent the same as the previous
	 * line' and is useful for making text transformations in a white-space
	 * sensitive language.
	 */
	
	private String clean(String script) {
		if (!script.contains("\u000b"))
			return script;
		
		System.out.println(" cleaning <" + script + ">");
		
		String[] lines = script.split("\n");
		StringBuilder b = new StringBuilder(script.length() + 5);
		int lastTab = 0;
		for (int i = 0; i < lines.length; i++) {
			String ll = lines[i];
			if (ll.startsWith("\u000b")) {
				String tabs = "";
				for (int j = 0; j < lastTab; j++) {
					tabs += "\t";
				}
				ll = ll.replace("\u000b", tabs);
			} else {
				int t = IndentationUtils.numTabIndent(ll);
				if (ll.trim().length() > 0)
					lastTab = t;
			}
			
			b.append(ll + "\n");
		}
		return b.toString();
		
		// StringBuilder b = new StringBuilder(script.length() + 10);
		// int tab = 0;
		// int clearIn = 0;
		// for (int i = 0; i < script.length(); i++) {
		// char c = script.charAt(i);
		// if (c == '\t') {
		// clearIn = 0;
		// tab++;
		// b.append(c);
		// } else if (c == '\n') {
		// clearIn = 2;
		// b.append(c);
		// } else if (c == '\u000b') {
		// for (int n = 0; n < tab; n++)
		// b.append('\t');
		// } else {
		// b.append(c);
		// }
		// clearIn--;
		// if (clearIn == 0)
		// tab = 0;
		// }
		//
		// return b.toString();
	}
	
	static public void handlePythonException(final iVisualElement element, final iVisualElement parentElement, final Throwable t) {
		Logging.logging.addEvent(new iLoggingEvent() {
			
			public String getLongDescription() {
				
				String m = "<html>" + PopupInfoWindow.title("Exception") + PopupInfoWindow.content(t.getMessage() + " / (" + t.getClass().getSimpleName() + ")") + "<br>";
				StackTraceElement[] trace = t.getStackTrace();
				
				System.out.println(" stack trace is <" + Arrays.asList(trace) + ">");
				if (t instanceof PyException) {
					PyException ee = ((PyException) t);
					
					m += PopupInfoWindow.title("Python Information " + ee.type + "");
					if (ee.value.toString().trim().length() > 0)
						m += PopupInfoWindow.content("" + ee.value) + "<br>";
					
					m += PopupInfoWindow.content(ee.toString().replace("\n", "<br>"));
					m += PopupInfoWindow.title("Java Stack");
					m += PopupInfoWindow.stackTrace(trace) + "<BR>";
				} else {
					m += PopupInfoWindow.title("At:") + PopupInfoWindow.stackTrace(trace) + "<BR>";
				}
				
				return m;
			}
			
			public String getReplayExpression() {
				return null;
			}
			
			public String getTextDescription() {
				return "<html><font bgcolor=#ffeeee><b> Exception  " + (t.getMessage() == null ? "" : t.getMessage()) + " (" + t.getClass().getSimpleName() + ") thrown in element " + ElementInvocationLogging.describeElementLink(element) + (parentElement == null ? "" : ("called from " + ElementInvocationLogging.describeElementLink(parentElement)));
			}
			
			public boolean isError() {
				return true;
			}
		});
	}
	
	/**
	 * useful for stopping the python execution from sigint handler
	 */
	public void forceExit() {
		System.out.println(" -- inside forceExit");
		Launcher.shuttingDown = true;
		if (ts.frame != null)
			ts.frame.tracefunc = ts.tracefunc = new TraceFunction() {
				
				@Override
				public TraceFunction traceReturn(PyFrame frame, PyObject ret) {
					System.out.println(frame + " " + ret);
					ts.tracefunc = null;
					throw new IllegalStateException("aborted");
				}
				
				@Override
				public TraceFunction traceLine(PyFrame frame, int line) {
					System.out.println(frame + " " + line);
					ts.tracefunc = null;
					throw new IllegalStateException("aborted");
				}
				
				@Override
				public TraceFunction traceException(PyFrame frame, PyException exc) {
					System.out.println(frame + " " + exc);
					ts.tracefunc = null;
					throw new IllegalStateException("aborted");
				}
				
				@Override
				public TraceFunction traceCall(PyFrame frame) {
					System.out.println(frame);
					ts.tracefunc = null;
					throw new IllegalStateException("aborted");
				}
			};
		
		Launcher.getLauncher().runRegisteredShutdownHooks();
		
	}
	
	public PrintStream getOutputStream() {
		return new PrintStream(new OutputStream() {
			
			@Override
			public void write(int b) throws IOException {
				print(""+(char)b);
			}
		});
		
	}
	
	public PrintStream getErrorStream() {
		return new PrintStream(new OutputStream() {
			
			@Override
			public void write(int b) throws IOException {
				printError(""+(char)b);
			}
		});
		
		
	}
    
	@Override
	public void finishInstall() {	}
	
}
