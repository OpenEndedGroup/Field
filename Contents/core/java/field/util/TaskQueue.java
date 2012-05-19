package field.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.python.core.Py;
import org.python.core.PyFunction;
import org.python.core.PyObject;

import field.bytecode.protect.iInside;
import field.bytecode.protect.dispatch.iContainer;
import field.core.plugins.PythonOverridden;
import field.core.plugins.PythonOverridden.Callable;
import field.core.util.FieldPyObjectAdaptor.iCallable;
import field.launch.iPhasic;
import field.launch.iUpdateable;

/**
 * thread safe task queue
 */

public class TaskQueue implements iUpdateable, field.bytecode.protect.DeferedInQueue.iRegistersUpdateable, iContainer, iCallable {

	static public class Gate extends Task {
		private final iUpdateable up;

		private final boolean touch;

		boolean con = true;

		public Gate(TaskQueue in, iUpdateable up, boolean touch) {
			in.super();
			this.up = up;
			this.touch = touch;
			if (touch)
				con = false;
		}

		public void dontGoOn() {
			con = false;
		}

		public void goOn() {
			con = true;
		}

		@Override
		protected void run() {
			up.update();
			if (con)
				recur();
			if (touch)
				con = false;
		}

	}

	abstract public class Task {

		StackTraceElement[] alloc;


		public Task() {
			assert (alloc = new Exception().getStackTrace()) != null;
			TaskQueue.this.addTask(this);
		}

		public Task(boolean add) {
			assert (alloc = new Exception().getStackTrace()) != null;
			if (add)
				TaskQueue.this.addTask(this);
		}


		@Override
		public String toString() {
			return "alloc at :" + (alloc == null ? null : Arrays.asList(alloc));
		}

		protected void postrun() {
		}

		protected void prerun() {
			delay = false;
		}

		/**
		 * run methods can call this if they want to have another crack
		 * at it
		 * 
		 * @param q
		 */
		protected void recur() {
			addTask(this);
		}

		boolean delay = false;
		protected void delay()
		{
			delay = true;
		}
		
		protected void remove() {
		}

		protected abstract void run();
	}

	public class Updateable extends Task implements iPhasic {

		iUpdateable u;

		boolean first = true;

		boolean added = true;

		boolean removed = false;

		public Updateable(final iInside u) {
			this.u = new iPhasic() {
				public void begin() {
				}

				public void close() {
					u.close();
				}

				public void end() {
				}

				public void open() {
					u.open();
				}

				public void rebegin() {
				}

				public void update() {
				}
			};
		}

		public Updateable(iPhasic u) {
			this.u = u;
		}

		public Updateable(iUpdateable u) {
			this.u = u;
		}

		public void begin() {
			throw new IllegalArgumentException(" should not be dispatched over container ");
		}

		public void close() {
			if (u instanceof iPhasic)
				((iPhasic) u).close();
		}

		public void end() {
			throw new IllegalArgumentException(" should not be dispatched over container ");
		}

		public void open() {
			if (u instanceof iPhasic)
				((iPhasic) u).open();
		}

		public void rebegin() {
			if (u instanceof iPhasic) {
				((iPhasic) u).rebegin();
			}
		}

		public void update() {
			throw new IllegalArgumentException(" should not be dispatched over container ");
		}

		protected void end(TaskQueue into) {
			removed = true;
			if (added && u instanceof iPhasic) {
				added = false;
				Task t = into.new Task() {
					@Override
					protected void run() {
						((iPhasic) u).end();

					}
				};
			}
		}

		@Override
		protected void remove() {
			synchronized (lock) {
				live.remove(this);
			}
			removed = true;
			if (added && u instanceof iPhasic) {
				added = false;
				Task t = new Task() {
					@Override
					protected void run() {
						((iPhasic) u).end();

					}
				};
			}
		}

		@Override
		protected void run() {
			if (u instanceof iPhasic) {
				if (first) {
					((iPhasic) u).begin();
					first = false;
				} else if (removed) {
					((iPhasic) u).rebegin();
					removed = false;
					added = true;
				}
				u.update();
			} else {

				u.update();
			}
			if (!removed)
				recur();
		}
	}

	protected List<Task> live = new LinkedList<Task>();

	protected Object lock = new Object();

	HashMap<iUpdateable, Updateable> upMap = new HashMap<iUpdateable, Updateable>();

	public void addUpdateable(iUpdateable updateable) {
		Updateable up = new Updateable(updateable);
		upMap.put(updateable, up);
	}

	public void clear() {
		synchronized (lock) {
			live.clear();
		}
	}

	public void deregisterUpdateable(iUpdateable up) {
		removeUpdateable(up);
	}

	public <T> T dispatchInside(Class<T> interfase, final T on) {
		return (T) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { interfase }, new InvocationHandler() {
			public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
				TaskQueue.this.new Task() {
					@Override
					protected void run() {
						try {
							method.invoke(on, args);
						} catch (IllegalArgumentException e) {
							e.printStackTrace();
						} catch (IllegalAccessException e) {
							e.printStackTrace();
						} catch (InvocationTargetException e) {
							e.printStackTrace();
						}
					}
				};
				return null;
			}
		});
	}

	public void end(TaskQueue into) {
		synchronized (lock) {
			List<Task> todo = live;
			for (Object o : todo) {
				if (o instanceof Updateable) {
					((Updateable) o).end(into);
				}
			}
		}
	}

	public int getNumTasks() {
		synchronized (lock) {
			return live.size();
		}
	}

	public List<Task> propagateTo(String tag, Class clazz, Method method, Object... args) {
		return live;
	}

	public Task queueSingleUpdate(final iUpdateable u) {
		return new Task() {
			@Override
			protected void run() {
				u.update();
			}
		};
	}

	public void registerUpdateable(final iUpdateable up) {
		Updateable task = new Updateable(up);
		upMap.put(up, task);
	}

	public void removeTask(Task task) {
		synchronized (lock) {
			if (live.remove(task))
				task.remove();
		}
	}

	public void removeUpdateable(iUpdateable view) {
		Updateable up = upMap.remove(view);

		if (up != null)
			up.remove();
		else {
			assert false : "no <" + view + "> in <" + upMap + ">";
		}
	}

	@Override
	public String toString() {
		return ("tq:" + live);
	}

	public void update() {

		if (live.size() == 0)
			return;
		try {
//			AliasingSystem.declareAliased(this, "container", this);
			List<Task> todo;
			synchronized (lock) {
				todo = live;
				live = new LinkedList<Task>();
			}
			for (int i = 0; i < todo.size(); i++) {
				Task task = todo.get(i);
				if (task instanceof iInside) {
					task.prerun();
					try {
						((iInside) task).open();
					} finally {
						task.postrun();
					}
				}
			}
			for (int i = 0; i < todo.size(); i++) {
				Task task = todo.get(i);
				task.prerun();
				try {
					task.run();
					if (task.delay)
					{
						for(int n=i;n<todo.size();n++)
						{
							addTask(todo.remove(i));
						}
					}
				} finally {
					task.postrun();
				}
			}
			for (int i = 0; i < todo.size(); i++) {
				Task task = todo.get(i);
				if (task instanceof iInside) {
					task.prerun();
					try {
						((iInside) task).close();
					} finally {
						task.postrun();
					}
				}
			}
			todo.clear();
		} finally {
			//AliasingSystem.undeclareAliased(this, "container");
		}
	}

	public void update_close(List todo) {
		for (int i = 0; i < todo.size(); i++) {
			Task task = (Task) todo.get(i);
			if (task instanceof iInside) {
				task.prerun();
				try {
					((iInside) task).close();
				} finally {
					task.postrun();
				}
			}
		}
		todo.clear();
	}

	public List<Task> update_open() {
		if (live.size() == 0)
			return Collections.EMPTY_LIST;
		List<Task> todo;
		synchronized (lock) {
			todo = live;
			live = new LinkedList<Task>();
		}
		for (int i = 0; i < todo.size(); i++) {
			Task task = todo.get(i);
			if (task instanceof iInside) {
				task.prerun();
				try {
					((iInside) task).open();
				} finally {
					task.postrun();
				}
			}
		}

		return todo;
	}

	public List update_update(List todo) {
		for (int i = 0; i < todo.size(); i++) {
			Task task = (Task) todo.get(i);
			// System.out.println(" in, task
			// <"+task+">");
			task.prerun();
			try {
				task.run();
			} finally {
				task.postrun();
			}
			// System.out.println(" out,
			// task <"+task+">");
		}
		return todo;
	}

	public void addTask(Task task) {
		synchronized (lock) {
			live.add(task);
		}
	}

	public List<Task> getTasks() {
		synchronized (lock) {
			return new ArrayList<Task>(live);
		}
	}

	Object[] pythonInvocationArgs = new Object[]{};
	HashMap<String, Task> knownPythonTasks = new HashMap<String, TaskQueue.Task>();
	
	public TaskQueue setPythonInvocationArgs(Object[] pythonInvocationArgs) {
		this.pythonInvocationArgs = pythonInvocationArgs;
		return this;
	}
	
	@Override
	public Object call(Object[] args) {
		
		String name = args.length==1 ? ((PyFunction)args[0]).__name__ : (String)args[0];
		final Callable m = PythonOverridden.callableForFunction((PyFunction) args[args.length-1]);
		
		Task n = knownPythonTasks.get(name);
		if (n!=null)
			n.remove();
		
		Task t = new Task()
		{
			@Override
			protected void run() {
				Object r = m.call(null, pythonInvocationArgs);
				if (r instanceof PyObject) 
					r = Py.tojava((PyObject)r, Object.class);
				if (r != null)
					recur();
			}
		};
		knownPythonTasks.put(name, t);
		return args[0];
	}
}