package field.namespace.key;

import field.math.abstraction.iProvider;
import field.namespace.context.SimpleContextTopology;

/**
 * @author marc
 */
public class OKey<T> extends CKey implements iProvider<T> {

	final Object nothing = new Object();

	public OKey(String s) {
		super(s);
		pushRoot().lookup(getName()).pop();
	}
	public OKey(String s, SimpleContextTopology ct) {
		super(s, ct);
		pushRoot().lookup(getName()).pop();
	}
	public T evaluate()
	{
		return get();
	}


	public T get() {
		Object o = this.run(nothing);
		if (o == nothing)
			throw new IllegalArgumentException(" ran OKey <" + this
					+ ">, no default, found nothing in context <"
					+ localContextTree.pwd() + ">");
		return (T) o;
	}

	public T get(T def) {
		Object odef = def;
		if (odef == null) odef  = nothing;

		Object o = this.run(odef);
		if (o == failure)
			throw new IllegalArgumentException(" ran OKey <" + this
					+ ">, with default <"+def+">got failure <"
					+ localContextTree.pwd() + ">");
		return (T) (o==nothing ? null : o);
	}

	public OKey<T> rootSet(T f)
	{
		pushRoot();
		localContextTree.set(this.getName(), f);
		pop();
		return this;
	}

	public OKey<T> set(T f)
	{
		localContextTree.set(this.getName(), f);
		return this;
	}
}
