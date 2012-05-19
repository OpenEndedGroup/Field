package field.util.filterstack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import field.math.abstraction.iFloatProvider;
import field.math.abstraction.iProvider;
import field.namespace.generic.Generics.Pair;


public class MultipleInputUnit<T> extends Unit<T> {

	public MultipleInputUnit(String name) {
		super(name);
	}

	protected iFloatProvider main;

	Map<String, Pair<iProvider<T>, iFloatProvider>> others = new LinkedHashMap<String, Pair<iProvider<T>, iFloatProvider>>();

	public MultipleInputUnit<T> setMainWeight(iFloatProvider main) {
		this.main = main;
		return this;
	}

	public MultipleInputUnit<T> setAdditional(String name, iProvider<T> in, iFloatProvider amount) {
		if (in == null)
			others.remove(name);
		else {
			others.put(name, new Pair<iProvider<T>, iFloatProvider>(in, amount));
		}
		return this;
	}

	@Override
	protected T filter(T input) {
		List<Float> am = new ArrayList<Float>();
		List<T> val = new java.util.ArrayList<T>();

		float tot = 0;
		val.add(input);
		if (main != null)
			am.add(tot = main.evaluate());
		else
			am.add(tot = 1f);

		Set<Entry<String, Pair<iProvider<T>, iFloatProvider>>> es = others.entrySet();
		for (Entry<String, Pair<iProvider<T>, iFloatProvider>> e : es) {
			T v = e.getValue().left.get();
			if (v != null) {
				val.add(v);
				float q = e.getValue().right.evaluate();
				tot += q;
				am.add(q);
			}
		}
		
		if(tot==0)
		{
			if (lastOutput!=null) return lastOutput;
			else return input;
		}
		
		T o = null;
		for(int i=0;i<val.size();i++)
		{
			o= filterStack.addImpl(val.get(i), am.get(i), o);
		}
		return o;
	}
}
