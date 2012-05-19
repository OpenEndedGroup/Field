package field.core.plugins.log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import field.core.plugins.log.AssemblingLogging.SimpleChange;
import field.math.abstraction.iBlendable;
import field.namespace.generic.Generics.Pair;


public class ChangeBlending {

	public void undoChangeSet(ArrayList<SimpleChange> set) {
		for (int i = set.size() - 1; i >= 0; i--) {
			SimpleChange s = set.get(i);
			s.writeChange(s.previousValue);
		}
	}

	public void reapplyChangeSetPartially(ArrayList<SimpleChange> set, float amount) {
		for (int i = 0; i < set.size(); i++) {
			SimpleChange s = set.get(i);
			Object o = getBlended(s.previousValue, s.value, amount);
			s.writeChange(o);
		}
	}

	public void applyChangeSet(ArrayList<SimpleChange> set, float amount) {
		for (int i = 0; i < set.size(); i++) {
			SimpleChange s = set.get(i);
			s.writeChange(s.value);
		}
	}

	// abandoned
	
//	public ArrayList<SimpleChange> createBlendedSet(ArrayList<Pair<List<SimpleChange>, Float>> a) {
//		LinkedHashMap<String, SimpleChange> overlap = null;
//
//		for (Pair<List<SimpleChange>, Float> p : a) {
//			LinkedHashMap<String, SimpleChange> left = new LinkedHashMap<String, SimpleChange>();
//			for (SimpleChange s : p.left)
//				left.put(s.target, s);
//			if (overlap == null) {
//				overlap = new LinkedHashMap<String, SimpleChange>();
//				overlap.putAll(left);
//			} else {
//				overlap.keySet().retainAll(left.keySet());
//			}
//		}
//
//		ArrayList<SimpleChange> ret = new ArrayList<SimpleChange>();
//		for (Pair<List<SimpleChange>, Float> p : a) {
//			
//		}
//	}

	public ArrayList<SimpleChange> createBlendedSet(ArrayList<SimpleChange> a, ArrayList<SimpleChange> b, float amount) {
		ArrayList<SimpleChange> ret = new ArrayList<SimpleChange>();
		LinkedHashMap<String, SimpleChange> left = new LinkedHashMap<String, SimpleChange>();
		LinkedHashMap<String, SimpleChange> right = new LinkedHashMap<String, SimpleChange>();
		for (SimpleChange s : a)
			left.put(s.target, s);
		for (SimpleChange s : b)
			right.put(s.target, s);

		LinkedHashMap<String, SimpleChange> overlap = new LinkedHashMap<String, SimpleChange>();

		// this is correct if left comes before right (we get left's previous value)
		overlap.putAll(right);
		overlap.keySet().retainAll(left.keySet());

		HashMap<String, SimpleChange> leftOnly = new HashMap<String, SimpleChange>();
		leftOnly.putAll(left);
		leftOnly.keySet().removeAll(right.keySet());
		HashMap<String, SimpleChange> rightOnly = new HashMap<String, SimpleChange>();
		rightOnly.putAll(right);
		rightOnly.keySet().removeAll(left.keySet());

		for (SimpleChange s : overlap.values()) {
			SimpleChange cloned = s.getSource().getSimpleChange();
			assert cloned != null;
			assert left.get(cloned.target) != null : cloned.target + " " + s.target + " " + left.get(s.target);
			assert right.get(cloned.target) != null : cloned.target + " " + s.target + " " + right.get(s.target);

			cloned.value = getBlended(left.get(cloned.target).value, right.get(cloned.target).value, amount);
			ret.add(cloned);
		}

		for (SimpleChange s : leftOnly.values()) {
			SimpleChange cloned = s.getSource().getSimpleChange();
			cloned.value = getBlended(left.get(cloned.target).value, cloned.previousValue, amount);
			ret.add(cloned);
		}

		for (SimpleChange s : rightOnly.values()) {
			SimpleChange cloned = s.getSource().getSimpleChange();
			cloned.value = getBlended(cloned.previousValue, right.get(cloned.target).value, amount);
			ret.add(cloned);
		}

		return ret;
	}

	public interface iBlendSupport<T> {
		public boolean isBlendSupported(Object o);

		public T blend(T a, T b, float alpha);
	}

	ArrayList<iBlendSupport> blenders = new ArrayList<iBlendSupport>();

	HashMap<Class, iBlendSupport> blendCache = new HashMap<Class, iBlendSupport>();

	public void addBlender(iBlendSupport s) {
		blenders.add(s);
	}

	public ChangeBlending() {

		blenders.add(new iBlendSupport<iBlendable>(){

			public boolean isBlendSupported(Object o) {
				return o instanceof iBlendable;
			}

			public iBlendable blend(iBlendable a, iBlendable b, float alpha) {
				iBlendable q = (iBlendable) a.blendRepresentation_newZero();
				return (iBlendable) q.lerp(a, b, alpha);
			}
		});

		blenders.add(new iBlendSupport<Number>(){

			public boolean isBlendSupported(Object o) {
				return o instanceof Number;
			}

			public Number blend(Number a, Number b, float alpha) {
				return new Double(a.doubleValue() * (1 - alpha) + b.doubleValue() * alpha);
			}
		});

	}

	protected Object getBlended(Object previousValue, Object value, float amount) {

		Class< ? extends Object> c = previousValue.getClass();
		iBlendSupport support = blendCache.get(c);
		if (support == null) {
			for (int i = 0; i < blenders.size(); i++) {
				if (blenders.get(i).isBlendSupported(previousValue)) {
					blendCache.put(c, support = blenders.get(i));
					break;
				}
			}
			if (support == null) {
				System.err.println(" warning: no blend support for " + previousValue);
				return value;
			}
		}
		return support.blend(previousValue, value, amount);
	}

}
