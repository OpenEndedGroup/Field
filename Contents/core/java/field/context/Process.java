package field.context;

import java.util.Collection;

import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyGenerator;
import org.python.core.PyObject;
import org.python.indexer.ast.NNodeVisitor.StopIterationException;

import field.context.Generator.Channel;
import field.launch.iUpdateable;
import field.math.abstraction.iDoubleProvider;
import field.math.abstraction.iProvider;

public abstract class Process<X> implements iUpdateable {

	protected final iDoubleProvider time;
	protected final double lookahead;
	protected Channel<X> output;

	public Process(iDoubleProvider time, double lookahead, Channel<X> output) {
		this.time = time;
		this.lookahead = lookahead;
		this.output = output;
	}

	@Override
	public void update() {

		double now = time.evaluate();

		Channel<X> window = output.range(now, Double.POSITIVE_INFINITY);

		;//System.out.println(" process :" + window + " " + now);

		try {
			while (window.size() == 0) {
				if (!pull(now))
					return;
			}

			if (window.size() > 0) {
				double next = window.timeFor(window.tail());

				;//System.out.println(" first event is <" + next + ">");

				while (next < now + lookahead) {
					if (!pull(next))
						return;
					next = window.timeFor(window.tail());
					;//System.out.println(" first event is now <" + next + ">");
				}
			}
		} finally {
			window.dispose();
		}

	}

	abstract protected boolean pull(double now);

	static public class ProviderProcess<X> extends Process<X> {

		private iProvider p;
		public double now;

		public ProviderProcess(iDoubleProvider time, double lookahead, Channel<X> output) {
			super(time, lookahead, output);
		}

		public ProviderProcess<X> set(iProvider p) {
			this.p = p;
			return this;
		}

		public ProviderProcess<X> set(final PyGenerator p) {
			this.p = new iProvider() {

				boolean first = true;

				@Override
				public Object get() {
					try {
						if (first) {
							first = false;
							return Py.tojava(p.next(), Object.class);
						}
						return Py.tojava(p.send(Py.java2py(new Object[] { now, output })), Object.class);
					} catch (PyException e) {
						if (e.type == Py.StopIteration) {
						} else
							e.printStackTrace();
						ProviderProcess.this.p = null;
						return null;
					}
				}
			};
			return this;
		}

		@Override
		protected boolean pull(double now) {

			this.now = now;

			if (p != null) {
				Object x = p.get();

				if (x == null)
					return false;

				if (x instanceof Collection) {
					for (Object xx : ((Collection) x)) {
						output.add((X) xx);
					}
					return ((Collection) x).size() > 0;
				} else {
					output.add((X) x);
					return true;
				}

			}

			return false;
		}

	}

}
