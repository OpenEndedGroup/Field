package field.core.execution;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Stack;

import org.python.core.PyObject;

import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.plugins.python.PythonPlugin.CapturedEnvironment;

/**
 * @author marc created on Jan 31, 2004
 */
public class PythonScriptingSystem {

	public interface DerivativePromise
	{
		public Promise getDerivativeWithText(VisualElementProperty<String> s);
	}

	public interface Promise {

		public void beginExecute();

		public void endExecute();

		public PyObject getAttributes();

		public float getEnd();

		public Stack<CapturedEnvironment> getOngoingEnvironments();

		public float getPriority();

		public float getStart();

		public String getText();

		public void willExecute();

		public void willExecuteSubstring(String actualSubstring, int start, int end);

		public void wontExecute();
		
		public boolean isPaused();
	}

	public abstract class Runner {

		protected ArrayList active = new ArrayList();

		float currentTime;

		float lastTime;

		boolean firstRun = false;

		Comparator activeComparator = new Comparator(){

			public int compare(Object o1, Object o2) {
				Promise p1 = (Promise) o1;
				Promise p2 = (Promise) o2;
				float y1 = p1.getPriority();
				float y2 = p2.getPriority();
				return y1 < y2 ? -1 : 1;
			}
		};

		public Runner(float timeStart) {
			lastTime = timeStart;
			
			// changed this \u2014 we no longer 'auto execute' things that are <=0 on startup
			firstRun = false;
		}

		public void moveTo(float t) {
			lastTime = t;
			update(t);
		}

		public void restart(float at) {
			firstRun = true;
			lastTime = at;
		}

		public void update(float t) {
			LinkedHashSet deactive = new LinkedHashSet();
			LinkedHashSet nowactive = new LinkedHashSet();
			Set allowedToBeActive;

			boolean forwards = true;

			//System.err.println(" runner from slice <"+t+"> <"+lastTime+">");

			if (t >= lastTime) {
				Set intersection = intersect(lastTime, t);
				allowedToBeActive = intersection;

				Iterator i = intersection.iterator();
				while (i.hasNext()) {
					Promise p = (Promise) i.next();
					if (p.getStart() > lastTime || firstRun) {
						nowactive.add(p);
					}
					if (p.getEnd() < t) {
						deactive.add(p);
					}
				}
				lastTime = t;
			} else {
				Set intersection = intersect(t, lastTime);
				allowedToBeActive = intersection;

				Iterator i = intersection.iterator();
				while (i.hasNext()) {
					Promise p = (Promise) i.next();
					if (p.getStart() > t) {
						deactive.add(p);
					}
					if (p.getEnd() < lastTime || firstRun) {
						nowactive.add(p);
					}
				}
				lastTime = t;
				forwards = false;
			}

			Iterator i = nowactive.iterator();
			while (i.hasNext()) {
				Promise p = (Promise) i.next();
				if (deactive.contains(p)) {
					startAndStop(t, p, forwards);
				} else {
					if (!active.contains(p)) {
						start(t, p, forwards);
						active.add(p);
					}
				}
			}
			i = deactive.iterator();
			while (i.hasNext()) {
				Promise p = (Promise) i.next();
				if (nowactive.contains(p)) {
				} else {
					if (active.contains(p)) {
						stop(t, p, forwards);
						active.remove(p);
					}
				}
			}
			sortActive();
			i = active.iterator();
			while (i.hasNext()) {
				Promise p = (Promise) i.next();
				if (!allowedToBeActive.contains(p)) {
					jumpStop(t, p, forwards);
					i.remove();
				} else {
					continueToBeActive(t, p, forwards);
				}
			}
			firstRun = false;

		}

		private void sort(LinkedHashSet ret) {
			if (ret.size()>1)
			{
				ArrayList al = new ArrayList(ret);
				Collections.sort(al, activeComparator);
				ret.clear();
				ret.addAll(al);
			}
		}

		protected Collection allPythonScriptingElements() {
			return PythonScriptingSystem.this.allPythonScriptingElements();
		}

		abstract protected boolean continueToBeActive(float t, Promise p, boolean forwards);

		protected Set intersect(float from, float to) {
			LinkedHashSet ret = new LinkedHashSet();
			Iterator all = allPythonScriptingElements().iterator();

			while (all.hasNext()) {
				Promise p = (Promise) all.next();
				if ((p.getStart() <= to) && (p.getEnd() >= from)) {
					ret.add(p);
				}
			}

			sort(ret);
			filterIntersections(ret);
			
			return ret;
		}

		abstract protected void jumpStop(float t, Promise p, boolean forwards);

		protected void sortActive() {

		}

		abstract protected void start(float t, Promise p, boolean forwards);

		abstract protected void startAndStop(float t, Promise p, boolean forwards);

		abstract protected void stop(float t, Promise p, boolean forwards);
	}

	static public final VisualElementProperty<PythonScriptingSystem> pythonScriptingSystem = new VisualElementProperty<PythonScriptingSystem>("pythonScriptingSystem_");


	HashMap promises = new HashMap();

	HashMap reversePromises = new HashMap();

	public Collection allPythonScriptingElements() {
		return promises.values();
	}

	protected void filterIntersections(LinkedHashSet ret) {
	}

	public Promise promiseForKey(Object key) {
		Promise ret = (Promise) promises.get(key);
		return ret;
	}
	
	public Object keyForPromise(Promise p)
	{
		return reversePromises.get(p);
	}

	public void promisePythonScriptingElement(Object key, Promise p) {
		promises.put(key, p);
		reversePromises.put(p, key);
	}

	public Promise revokePromise(Object key) {
		Promise pr = (Promise) promises.remove(key);
		reversePromises.remove(pr);
		return pr;
	}

	protected float rewriteTime(float t, Promise p)
	{
		return t;
	}

}