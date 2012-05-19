package field.math.linalg;

import field.math.BaseMath.MutableFloat;
import field.math.abstraction.iFloatProvider;
import field.math.abstraction.iProvider;

public interface iToFloatArray extends iProvider<float[]> {

	static public class Provided implements iToFloatArray {
		private final iProvider< ? extends iToFloatArray> p;

		public Provided(iProvider< ? extends iToFloatArray> p) {
			this.p = p;
		}

		

		public float[] get() {
			return p.get().get();
		}
	}

	static public class ProvidedMul implements iToFloatArray {
		private final iProvider< ? extends iToFloatArray> p;

		private final iFloatProvider by;

		public ProvidedMul(iProvider< ? extends iToFloatArray> p, iFloatProvider by) {
			this.p = p;
			this.by = by;
		}

		public float[] get() {
			float[] f = p.get().get();
			float[] f2 = new float[f.length];
			float m = by.evaluate();

			// a change

			for (int i = 0; i < f.length; i++)
				f2[i] = f[i] * (i == f.length - 1 ? m : 1);

			return f2;
		}
	}

	static public class SingleFloat implements iToFloatArray {
		private float[] f;

		private MutableFloat mf;

		public SingleFloat(float f) {
			this.f = new float[] { f};
		}

		public SingleFloat(float[] f) {
			this.f = f;
		}

		public SingleFloat(MutableFloat f) {
			this.mf = f;
			this.f = new float[1];
		}

		public float[] get() {
			if (mf != null) f[0] = (float) mf.d;
			return f;
		}

	}

	static public class MulVec4 implements iToFloatArray
	{
		private Vector4 a;
		private Vector4 b;
		public MulVec4(Vector4 a, Vector4 b)
		{
			this.a = a;
			this.b = b;
		}
		public float[] get() {
			return new float[]{a.x*b.x, a.y*b.y, a.z*b.z, a.w*b.w};
		}
		
	}
	
}
