package field.namespace.context;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import field.bytecode.protect.BaseRef;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.namespace.context.CT.ContextTopology;
import field.namespace.context.CT.iContextStorage;
import field.namespace.context.CT.iStorage;
import field.util.BetterWeakHashMap.BaseWeakHashMapKey;

/**
 * class keeps around a mapping from an exernal object to internal contexts,
 * reassembles these as needed in the hierarchy loosely driven by the begin and
 * end sequence
 *
 * @author marc
 *
 */
public class ExternalContextTopology<t_Key> extends ContextTopology<t_Key, ExternalContextTopology<t_Key>.Context> {

	public class Context extends BaseWeakHashMapKey implements iStorage {
		WeakReference<t_Key> name;

		HashMap<t_Key, Context> children = new HashMap<t_Key, Context>();

		WeakReference<Context> parent;

		HashMap<String, Object> values = new HashMap<String, Object>();

		public Context(t_Key name) {
			this.name = new WeakReference<t_Key>(name);
		}

		protected Context() {
			this.name = null;
		}

		public Object get(String name) {
			BaseRef rr = new BaseRef(null);
			ExternalContextTopology.this.get(this, name, rr);
			return rr.get();
		}

		public VisitCode get(String name, BaseRef result) {
			return ExternalContextTopology.this.get(this, name, result);
		}

		public VisitCode set(String name, BaseRef value) {
			return ExternalContextTopology.this.set(this, name, value);
		}

		public void set(String name, Object value) {
			ExternalContextTopology.this.set(this, name, new BaseRef(value));
		}

		public VisitCode unset(String name) {
			return ExternalContextTopology.this.unset(this, name);
		}

	}

	protected Context root = new Context();

	WeakHashMap<t_Key, Context> knownContexts = new WeakHashMap<t_Key, Context>();

	public ExternalContextTopology(Class<t_Key> keyClass) {
		super(keyClass);
		setInterfaceClass((Class<Context>) root.getClass());
//		super(keyClass, Context.class);
		this.storage = new iContextStorage<t_Key, Context>() {
			public Context get(t_Key at, Method m) {
				return contextFor(null, at);
			}
		};

		knownContexts.put(null, new Context());
	}

	public void begin(t_Key k) {
		setAt(contextFor(contextFor(null, getAt()), k).name.get());
	}

	@Override
	public Set<t_Key> childrenOf(t_Key p) {
		return new HashSet<t_Key>(contextFor(null, p).children.keySet());
	}

	public Context contextFor(Context parent, t_Key k) {
		if (k == null)
			return root;

		Context cc = knownContexts.get(k);
		if (cc == null) {
			cc = new Context(k);
			knownContexts.put(k, cc);
		}

		if (parent != null) {
			cc.parent = new WeakReference<Context>(parent);
			parent.children.put(k, cc);
		}
		return cc;
	}

	@Override
	public void delete(t_Key child) {
		WeakReference<Context> p = contextFor(null, getAt()).parent;
		if (p!=null && p.get()!=null)
		{
			p.get().children.remove(child);
		}
		knownContexts.remove(child);
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

	public void end(t_Key k) {
		assert getAt() == k : k + " " + getAt();
		WeakReference<t_Key> nn = contextFor(null, getAt()).parent.get().name;
		if (nn == null)
			setAt(null);
		else
			setAt(nn.get());
	}

	@Override
	public Set<t_Key> parentsOf(t_Key k) {
		WeakReference<Context> pp = contextFor(null, k).parent;
		if (pp == null)
			return null;
		WeakReference<t_Key> n = pp.get().name;
		if (n == null)
			return null;

		return Collections.singleton(n.get());
	}

	public String pwd() {
		HashSet<t_Key> seen = new HashSet<t_Key>();
		t_Key aa = getAt();
		String p = "";
		while (!seen.contains(aa) && aa != null) {
			seen.add(aa);
			p = aa + "/" + p;
			aa = contextFor(null, aa).parent.get().name == null ? null : contextFor(null, aa).parent.get().name.get();
		}
		if (aa != null)
			p = "(loop)" + p;
		return p;

	}

	@Override
	public t_Key root() {
		return null;
	}

	protected void deleteChild(t_Key k) {
		contextFor(null, getAt()).children.remove(k);
	}

	protected VisitCode get(Context c, String name, BaseRef result) {
		if (c.values.containsKey(name)) {
			result.set(c.values.get(name));
			return VisitCode.stop;
		}
		return VisitCode.cont;
	}

	protected VisitCode set(Context c, String name, BaseRef value) {
		c.values.put(name, value.get());
		return VisitCode.stop;
	}

	protected VisitCode unset(Context c, String name) {
		c.values.remove(name);
		return VisitCode.stop;
	}

}
