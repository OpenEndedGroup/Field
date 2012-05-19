package field.graphics.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import field.core.plugins.PythonOverridden.Callable;
import field.core.util.PythonCallableMap;
import field.graphics.core.Base.StandardPass;
import field.graphics.core.BasicUtilities.OnePassElement;
import field.launch.iUpdateable;
import field.util.Dict.Prop;

/**
 * tools for injecting raw gl commands into various positions in the tree
 * 
 * @author marc
 * 
 */
public class When {

	static public Prop<StandardPass> when = new Prop<StandardPass>("when");

	static public HashMap<BasicSceneList, Map<StandardPass, When>> masterMap = new LinkedHashMap<BasicSceneList, Map<StandardPass, When>>();

	private final BasicSceneList list;

	public When(BasicSceneList list) {
		this.list = list;
	}

	Base.StandardPass pass;

	PythonCallableMap map = new PythonCallableMap() {
		protected field.core.plugins.PythonOverridden.Callable newCallable(org.python.core.PyFunction f) {
			Callable c = super.newCallable(f);
			c.getInfo().put(when, pass);
			ensureShim(pass);
			return c;
		}

		protected field.core.plugins.PythonOverridden.Callable newCallable(String name, iUpdateable u) {
			Callable c = super.newCallable(name, u);
			c.getInfo().put(when, pass);
			ensureShim(pass);
			return c;
		}
	};

	HashMap<Base.StandardPass, BasicUtilities.OnePassElement> shims = new LinkedHashMap<StandardPass, OnePassElement>();

	public PythonCallableMap getMap(Base.StandardPass pass) {
		this.pass = pass;
		return map;
	}

	public PythonCallableMap getMap(int pass) {
		if (pass < 0)
			pass = 0;
		if (pass >= Base.StandardPass.values().length)
			pass = Base.StandardPass.values().length - 1;
		this.pass = Base.StandardPass.values()[pass];
		return map;
	}

	protected void ensureShim(final StandardPass q) {
		OnePassElement o = shims.get(q);
		if (o == null) {
			shims.put(q, o = new OnePassElement(q) {

				@Override
				public void performPass() {
					for (Map.Entry<String, Callable> c : map.known.entrySet()) {
						StandardPass pp = c.getValue().getInfo().get(When.when);
						if (pp != null && pp.equals(q)) {
							map.current = c.getKey();
							c.getValue().call(null, new Object[] { BasicContextManager.getGl() });
						}
					}

					map.known.keySet().removeAll(map.clear);
					map.clear.clear();
				}
			});

			list.addChild(o);
		}
	}

}
