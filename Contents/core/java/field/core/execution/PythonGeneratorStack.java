package field.core.execution;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.python.core.Py;
import org.python.core.PyFunction;
import org.python.core.PyGenerator;
import org.python.core.PyMethod;
import org.python.core.PyObject;
import org.python.core.PyTuple;

import field.launch.Launcher;
import field.launch.iUpdateable;
import field.math.abstraction.iProvider;
import field.util.PythonUtils;

/**
 * what's a python generator stack?
 * 
 * well, what's a python (2.3) generator?
 * 
 * consider the following code
 * 
 * class someClass: def __init__(self): self.i = 0; def oneToTen(self): if (i
 * <10): self.i = self.i+1 return i return None
 * 
 * a = someClass() print a.oneToTen() print a.oneToTen() print a.oneToTen()
 * print a.oneToTen() etc...
 * 
 * a "generator" makes it much much easier to have a single method or function
 * return results mid-way through execution. a generator is closely related to
 * an iterator, but with extra syntactical magic:
 * 
 * 
 * def oneToTen(): for i in range(10): yield i
 * 
 * a = oneToTen() print a.next() print a.next() print a.next() etc...
 * 
 * (no class is needed, although you can have one if you want) you'll get a
 * StopIteration exception when you should stop
 * 
 * much easier, no ?
 * 
 * this also lets you write things like
 * 
 * def myScript(): ... do something yield yup .. do something else yield nope
 * yield #pause some more for i in range(3): yield
 * 
 * etc...
 * 
 * at each yield control is passed back to the person that called it (e.g. the
 * world update loop) but at each subsequent call (to .next()) execution resumes
 * from the place after the yield, and your stackframe is completely maintained
 * 
 * so, this class is a nice interface between iUpdateable or and evaluate()
 * method and python generators. It's nice in three ways:
 * 
 * firstly, it lets you have a updateable/generator that registers, and
 * magically deregisters when it's done (at the last yield or a return). You can
 * stop reading here for basic usage. secondly, it lets you return / yield
 * another generator. this is placed on a stack and it will be executed until it
 * returns, at which point, control will pass back to the original generator.
 * This lets you nest generators quite nicely. One can make a generator called
 * wait(n) which is a for i in range(n): yield; and just pass one of those back
 * to wait. You can do that in python, but you can't do it in one line and keep
 * the nested generator executing in the world update loop. thirdly, returning a
 * (python) tuple of generators will start them up and execute them in
 * parrallel. Same rules as above apply for the spawned generators.
 * 
 * a powerful idea, no?
 * 
 * @author marc Created on Dec 10, 2003
 */

public class PythonGeneratorStack implements iUpdateable {
	protected static iProvider<Object> wrapGenerator(final PyGenerator top) {
		return new iProvider() {
			public Object get() {
				return top.__iternext__();
			}
		};
	}

	public final Object didNotEvaluate = new Object();

	protected Stack stack;

	protected List addTo;

	protected List subUpdateables = new ArrayList();

	boolean over = false;

	boolean first = true;

	public PythonGeneratorStack(iProvider top) {
		stack = new Stack();
		stack.push(top);
		Launcher.getLauncher().registerUpdateable(this);
	}

	public PythonGeneratorStack(iProvider top, List addTo) {
		this.addTo = addTo;
		stack = new Stack();
		stack.push(top);
		if (addTo != null)
			addTo.add(this);
	}

	public PythonGeneratorStack(PyFunction top, List addTo) {
		this.addTo = addTo;
		stack = new Stack();
		stack.push(wrapFunction(top));
		if (addTo != null)
			addTo.add(this);
	}

	public PythonGeneratorStack(PyMethod top, List addTo) {
		this.addTo = addTo;
		stack = new Stack();
		stack.push(wrapMethod(top));
		if (addTo != null)
			addTo.add(this);
	}

	public PythonGeneratorStack(PyGenerator top) {
		stack = new Stack();
		stack.push(wrapGenerator(top));
		Launcher.getLauncher().registerUpdateable(this);
	}

	public PythonGeneratorStack(PyGenerator top, List addTo) {
		this.addTo = addTo;
		stack = new Stack();
		stack.push(wrapGenerator(top));
		if (addTo != null)
			addTo.add(this);
	}

	protected void preamble() {
	};

	protected void postamble() {
	};

	public Object evaluate() {

		preamble();
		try {
			// ;//System.out.println(" stack is <"+stack+">");
			Object ret = didNotEvaluate;
			if (shouldEvaluateMain() && (stack.size() > 0)) {
				if (first) {
					first();
					first = false;
				}

				// ret = ((PyGenerator)
				// stack.peek()).__iternext__();
				Object oret = ret = ((iProvider) stack.peek()).get();
				ret = PythonUtils.maybeToJava(ret);

				// beta1
				// if (ret instanceof PyJavaInstance)
				// ret = ((PyJavaInstance)
				// ret).__tojava__(Object.class);

				// ;//System.out.println(" generator is
				// <"+stack.peek()+">
				// and returns <"+ret+" "+(ret == null ? null :
				// ret.getClass())+">");

				if (oret == stack.peek()) {
					// do nothing, just call it again next
					// time
				} else if (ret == null) {
					stack.pop();
					if (stack.size() == 0) {
						ret = didNotEvaluate;
						finished();
					} else {
						return evaluate();
					}
				} else if (ret instanceof PyGenerator) {
					stack.push(wrapGenerator((PyGenerator) ret));
					return evaluate();
				} else if (ret instanceof iProvider) {
					stack.push(ret);
				} else if (ret instanceof PyTuple) {
					PyTuple t = (PyTuple) ret;
					for (int i = 0; i < t.__len__(); i++) {
						if (t.__getitem__(i) instanceof PyGenerator) {
							PyGenerator g = (PyGenerator) t.__getitem__(i);
							newPythonGeneratorStack(wrapGenerator(g), subUpdateables);
						} else if (t.__getitem__(i) instanceof iProvider) {
							iProvider g = (iProvider) t.__getitem__(i);
							newPythonGeneratorStack(g, subUpdateables);
						}
					}
				}
			}

			for (int i = 0; i < subUpdateables.size(); i++) {
				int inSize = subUpdateables.size();
				Object sret = didNotEvaluate;
				PythonGeneratorStack stack2 = ((PythonGeneratorStack) subUpdateables.get(i));
				if (shouldEvaluate(stack2))
					sret = stack2.evaluate();

				sret = sret == null ? null : (sret instanceof PyObject ? ((PyObject) sret).__tojava__(Object.class) : sret);

				handleEvaluation(i, sret);
				int outSize = subUpdateables.size();
				i -= inSize - outSize;
			}

			if ((stack.size() == 0) && (subUpdateables.size() == 0))
				outOfThingsTodo();
			ret = ret == null ? null : (ret instanceof PyObject ? ((PyObject) ret).__tojava__(Object.class) : ret);

			handleEvaluation(-1, ret);

			return ret;
		} finally {
			postamble();
		}

	}

	public boolean isOver() {
		return over;
	}

	/**
	 * subclasses can override this to change the stop logic, for when stop
	 * is called from outside class.
	 */
	public void stop() {
		internalStop();
	}

	/** @see innards.iUpdateable#update() */
	public void update() {
		try {
			Object to = evaluate();
			evaluatedTo(to == didNotEvaluate ? null : to);
		} catch (Exception e) {
			e.printStackTrace();
			IllegalArgumentException iae = new IllegalArgumentException();
			iae.initCause(e);
			throw iae;
		}
	}

	protected void evaluatedTo(Object to) {
	}

	private iProvider<Object> wrapFunction(final PyFunction top) {
		return new iProvider() {
			public Object get() {
				PyObject c = top.__call__();
				if (c == null)
					return this;
				if (c == Py.None)
					return this;
				Object z = c.__tojava__(Object.class);
				return z;
			}
		};
	}

	private iProvider<Object> wrapMethod(final PyMethod top) {
		return new iProvider() {
			public Object get() {
				PyObject c = top.__call__();
				if (c == null)
					return this;
				if (c == Py.None)
					return this;
				Object z = c.__tojava__(Object.class);
				return z;
			}
		};
	}

	protected void finished() {

	}

	protected void first() {

	}

	protected void handleEvaluation(int childIndex, Object a) {
		return;
	}

	protected void internalStop() {
		if (addTo == null) {
			Launcher.getLauncher().isRegisteredUpdateable(this);
			Launcher.getLauncher().deregisterUpdateable(this);
		} else {
			addTo.remove(this);
		}
		over = true;
	}

	protected void newPythonGeneratorStack(iProvider g, List subUpdateables2) {
		new PythonGeneratorStack(g, subUpdateables2);
	}

	/**
	 * subclasses can override this to change the stop logic, for when stop
	 * is called becuase we're out of things to do
	 */
	protected void outOfThingsTodo() {
		internalStop();
	}

	protected boolean shouldEvaluate(PythonGeneratorStack stack2) {
		return true;
	}

	protected boolean shouldEvaluateMain() {
		return true;
	}

}