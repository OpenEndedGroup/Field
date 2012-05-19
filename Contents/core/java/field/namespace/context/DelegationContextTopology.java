package field.namespace.context;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.Map.Entry;

import field.namespace.context.CT.ContextTopology;
import field.namespace.context.CT.iContextStorage;
import field.namespace.generic.ReflectionTools;
import field.util.BetterWeakHashMap.BaseWeakHashMapKey;

/**
 * a set of "contexts" that can be hand assembled for making delegation chains
 * 
 * @author marc
 * 
 */
public class DelegationContextTopology<t_Key> extends ContextTopology<t_Key, t_Key> {

	protected Context root = new Context();
	
	public class Context extends BaseWeakHashMapKey {
		WeakReference<t_Key> name;

		LinkedHashMap<t_Key, Context> children = new LinkedHashMap<t_Key, Context>();

		LinkedHashMap<t_Key, Context> parent = new LinkedHashMap<t_Key, Context>();

		public Context(t_Key name) {
			this.name = new WeakReference<t_Key>(name);
		}

		protected Context() {
			this.name = null;
		}
	}

	public DelegationContextTopology(Class<t_Key> c) {
		super(c,c);
		this.storage = new iContextStorage<t_Key, t_Key>() {
			public t_Key get(t_Key at, Method m) {
				return at;
			}
		};

		knownContexts.put(null, new Context());
	}

	public void begin(t_Key k) {
		setAt(contextFor(contextFor(null, getAt()), k).name.get());
	}

	public void end(t_Key k) {
		assert getAt().equals(k) : k + " " + getAt();

		Context c = contextFor(null, getAt());
		if (c.parent.size() > 0) {
			t_Key top = getTop(c.parent);
			setAt(top);
		} else
			throw new IllegalArgumentException(" end with no next ? ");
	}

	private t_Key getTop(LinkedHashMap<t_Key, Context> p) {
		Map.Entry<t_Key, Context> newest = (Entry<t_Key, Context>) ReflectionTools.illegalGetObject(((Map.Entry<t_Key, Context>) ReflectionTools.illegalGetObject(p, "header")), "before");
		t_Key top = newest.getKey();
		return top;
	}

	// useful for constructing delegation chains
	public void begin(t_Key... k) {
		for (t_Key n : k)
			begin(n);
	}

	// useful for constructing delegation chains
	public void end(t_Key... k) {
		for (int i = 0; i < k.length; i++)
			end(k[k.length - 1 - i]);
	}

	public void connect(t_Key parent, t_Key child) {
		contextFor(contextFor(null, parent), child);
	}

	WeakHashMap<t_Key, Context> knownContexts = new WeakHashMap<t_Key, Context>();

	public Context contextFor(Context parent, t_Key k) {
		if (k == null)
			return root;

		Context cc = knownContexts.get(k);
		if (cc == null) {
			cc = new Context(k);
			knownContexts.put(k, cc);
		}

		if (parent != null) {
			t_Key nn = parent.name == null ? null : parent.name.get();
			cc.parent.remove(nn);
			cc.parent.put(nn, parent);
			parent.children.put(k, cc);
		}
		return cc;
	}

	@Override
	public Set<t_Key> childrenOf(t_Key p) {
		return new HashSet<t_Key>(contextFor(null, p).children.keySet());
	}

	@Override
	public void deleteChild(t_Key parent, t_Key name) {
		Context cp = contextFor(null, parent);
		Set<Map.Entry<t_Key, Context>> e = cp.children.entrySet();
		for (Map.Entry<t_Key, Context> ee : e) {
			if (ee.getValue() == name) {
				synchronized (getAt()) {
					cp.children.remove(ee.getKey());
				}
				contextFor(null, name).parent.clear();
				return;
			}
		}
		return;
	}

	@Override
	public Set<t_Key> parentsOf(t_Key k) {
		HashMap<t_Key, Context> pp = contextFor(null, k).parent;
		if (pp == null)
			return null;
		return new HashSet<t_Key>(pp.keySet());
	}

	@Override
	public t_Key root() {
		return null;
	}

	protected void deleteChild(t_Key k) {
		contextFor(null, getAt()).children.remove(k);
	}
	
	@Override
	public void delete(t_Key child) {
		assert getAt()==null || !getAt().equals(child) : "can't delete current context";
		LinkedHashMap<t_Key, Context> p = contextFor(null, getAt()).parent;
		for(t_Key pp : p.keySet())
		{
			contextFor(null, pp).children.remove(child);
		}
		knownContexts.remove(child);
	}


	public String pwd() {
		HashSet<t_Key> seen = new HashSet<t_Key>();
		t_Key aa = getAt();
		String p = "";
		while (!seen.contains(aa) && aa != null) {
			seen.add(aa);
			p = aa + "/" + p;
			LinkedHashMap<t_Key, Context> pnext = contextFor(null, aa).parent;
			if (pnext.size() == 0) {
				aa = null;
				break;
			}
			aa = getTop(pnext);
		}
		if (aa != null)
			p = "(loop)" + p;
		return p;

	}

}
