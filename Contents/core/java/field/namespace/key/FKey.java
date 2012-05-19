package field.namespace.key;

import field.math.abstraction.iFloatProvider;
import field.namespace.context.SimpleContextTopology;


/**
 * @author marc
 */
public class FKey extends CKey implements iFloatProvider
{
	public FKey(String s)
	{
		super(s);
		pushRoot().lookup(this.getName()).pop();
	}

	public FKey(String s, SimpleContextTopology ct)
	{
		super(s, ct);
		pushRoot().lookup(this.getName()).pop();
	}

	public FKey defaults(final float f)
	{
		executionStack.add(new Defaults(new Phi()
		{
			public float ret()
			{
				return f;
			}
		}));
		return this;
	}

	public FKey defaults(final iFloatProvider f)
	{
		executionStack.add(new Defaults(new Phi()
		{
			public float ret()
			{
				return f.evaluate();
			}
		}));
		return this;
	}

	public FKey defaults(final Number f)
	{
		executionStack.add(new Defaults(new Phi()
		{
			public float ret()
			{
				return f.floatValue();
			}
		}));
		return this;
	}
	public float evaluate()
	{
		return asFloat();
	}
	public float get()
	{
		return evaluate();
	}

	public FKey rootSet(float f)
	{
		pushRoot();
		localContextTree.set(this.getName(), f);
		pop();
		return this;
	}

	public FKey set(float f)
	{
		localContextTree.set(this.getName(), f);
		return this;
	}
}
