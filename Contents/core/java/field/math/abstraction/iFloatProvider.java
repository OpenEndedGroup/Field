package field.math.abstraction;

/**
 * Interface for a class that provides a float
 * 
 */
public interface iFloatProvider {
	public float evaluate();

	static public class Constant implements iFloatProvider {

		private float constant;

		public Constant(float f) {
			constant = f;
		}

		public Constant setConstant(float c) {
			constant = c;
			return this;
		}

		public float evaluate() {
			return constant;
		}
	}

	static public class HasChanged  implements iFloatProvider {
		private final iFloatProvider to;

		public HasChanged(iFloatProvider to) {
			this.to = to;
		}

		boolean needsLoad = true;

		float last = Float.POSITIVE_INFINITY;

		float now = Float.POSITIVE_INFINITY;

		public boolean hasChanged() {
			if (needsLoad) {
				last = now;
				now = to.evaluate();
				needsLoad = false;
			}

			return now != last;
		}

		public float evaluate() {
			if (needsLoad) {
				last = now;
				now = to.evaluate();
			} else {
				needsLoad = true;
			}
			return now;
		}

		public boolean hasChanged(boolean forceFetch)
		{
			if (needsLoad || forceFetch) {
				last = now;
				now = to.evaluate();
				needsLoad = false;
			}

			return now!=last;
		}

	}
}
