package field.core.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UID;
import java.rmi.server.UnicastRemoteObject;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.python.core.CompileMode;
import org.python.core.CompilerFlags;
import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyFile;
import org.python.core.PyString;
import org.python.core.PySystemState;

import field.bytecode.protect.DeferedInQueue.iProvidesQueue;
import field.bytecode.protect.DeferedInQueue.iRegistersUpdateable;
import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.InQueue;
import field.core.util.FieldPyObjectAdaptor.iHandlesAttributes;
import field.core.util.FieldPyObjectAdaptor2;
import field.launch.Launcher;
import field.launch.SystemProperties;
import field.launch.iLaunchable;
import field.launch.iUpdateable;
import field.util.TaskQueue;

/**
 * lightweight client that just runs Python in a separate process, but supports
 * sending and receiving data and code. Useful for long running tasks on single
 * machines and distributed computing.
 */
@Woven
public class Client implements iLaunchable, iHandlesAttributes, RemoteExecutionService, iUpdateable, iProvidesQueue {

	public PyDictionary globals = new PyDictionary();
	static public PySystemState state = new PySystemState();
	static public CompilerFlags cflags = new CompilerFlags();

	String returnAddress = null;

	RemoteExecutionServiceHost host;

	ArrayList<String> events = new ArrayList<String>();
	
	public ByteArrayOutputStream printout = new ByteArrayOutputStream() {
		@Override
		public void flush() throws IOException {
			try {
				String s = new String(toByteArray());
				if (host != null) {
					System.out.println(s);
					host.print(id, s);
				} else
					System.err.println(s);
			} finally {
				this.reset();
			}
		}
	};
	public ByteArrayOutputStream printerr = new ByteArrayOutputStream() {
		@Override
		public void flush() throws IOException {
			try {
				String s = new String(toByteArray());
				if (host != null) {
					System.out.println(s);
					host.printError(id, s);
				} else
					System.err.println(s);
			} finally {
				this.reset();
			}
		}
	};
	private String id;
	private RemoteExecutionService exported;

	@Override
	public void launch() {
		
		id = (SystemProperties.getProperty("id", new UID().toString()));
		System.out.println(" ------------ launched client <"+id+"> -----------------");

		Py.setSystemState(state);
		state.setClassLoader(this.getClass().getClassLoader());
		state.stdout = new PyFile(printout);
		state.stderr = new PyFile(printerr);

		Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
		FieldPyObjectAdaptor2.initialize();
		globals.put(new PyString("_self"), Py.java2py(this));
		System.out.println(" ------------ set self <"+id+"> -----------------");

		// todo \u2014 register, and keep registered

		try {
			System.out.println(" ------------ registering self <"+id+"> -----------------");
			exported = (RemoteExecutionService) UnicastRemoteObject.exportObject(this, 0);
		} catch (RemoteException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		insideUnderscore.put("events", events);
		events.add("<i>launched</i> "+DateFormat.getInstance().format(new Date()));
		
		Launcher.getLauncher().registerUpdateable(this);
	}

	long tick = -1;

	@Override
	public void update() {
		if (System.currentTimeMillis() - tick > 1000) {
			try {
				Registry registry = LocateRegistry.getRegistry();
				registry.rebind(id, exported);
			} catch (Throwable t) {
				t.printStackTrace();
			}

			tick = System.currentTimeMillis();
		}
		q.update();
	}
	
	TaskQueue q = new TaskQueue();

	Map<String, Object> insideUnderscore = new HashMap<String, Object>();

	public Object getAttribute(String name) {
		return insideUnderscore.get(name);
	}

	public void setAttribute(String name, Object value) {
		insideUnderscore.put(name, value);

		if (name != null && host != null && name.contains("_host_")) {
			try {
				host.setData(id, name.replace("_host_", ""), value);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

	}

	@Override
	public void addActive() {
	}

	@Override
	public Object getData(String name) {
		
		System.out.println(" -- get data from client <"+name+"> <"+insideUnderscore.get(name)+">");
		
		return insideUnderscore.get(name);
	}

	@Override
	public void setData(String name, Object value) {
		insideUnderscore.put(name, value);
	}

	@Override
	public void removeActive() {

	}

	
	
	@InQueue
	public void execute(String fragment, RemoteExecutionServiceHost host) {
		this.host = host;
		try {
			System.out.println(" about to execute <" + fragment + "> in client <" + id + "> with host <" + host + ">");
			Py.setSystemState(state);
			state.stdout = new PyFile(printout);
			state.stderr = new PyFile(printerr);

			Py.exec(Py.compile_flags(fragment, "<string>", CompileMode.exec, cflags), globals, globals);
			System.out.println(" finished client <" + id + ">");
			printout.flush();
			printerr.flush();
		} catch (Exception e) {
			System.err.println("Python threw an exception <" + e + ">");

			try {
				printerr.write(("Field remote threw an exception\n").getBytes());
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw, true));
				sw.flush();
				printerr.write((sw.toString() + "\n").getBytes());
				printout.flush();
				printerr.flush();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	@Override
	public void dieNow() throws RemoteException {
		Registry registry = LocateRegistry.getRegistry();
		try {
			registry.unbind(id);
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}

	
	@Override
	public boolean check() throws RemoteException {
		return true;
	}
	
	@Override
	public iRegistersUpdateable getQueueFor(Method m) {
		return q;
	}
}
