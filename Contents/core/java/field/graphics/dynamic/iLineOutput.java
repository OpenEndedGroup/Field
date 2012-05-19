package field.graphics.dynamic;

import java.util.HashMap;
import java.util.Map;

import field.bytecode.protect.iInside;
import field.math.linalg.Vector3;
import field.math.linalg.iToFloatArray;
import field.namespace.context.SimpleContextTopology;


/**
 * we need a class that wraps splinedrawers with this
 * 
 * Created on May 23, 2004 \u2014 alison
 * 
 * @author marc
 */
public interface iLineOutput extends iInside {

	public interface iLineIdentifier {
		public String getName();

		public String[] getParts();

		public iLineIdentifier append(String s);

		public iLineIdentifier prepend(String s);

		public iLineIdentifier stripFirst();

		public void intoContext(SimpleContextTopology here);

		public void outOfContext(SimpleContextTopology here);

	}

	public void open();

	public void beginSpline(iLineIdentifier identitfier);

	public void moveTo(Vector3 v3);

	public void lineTo(Vector3 v3);

	public void curveTo(Vector3 to, Vector3 c1, Vector3 c2, int numSamples);

	public void endSpline();

	public void close();

	public void remove();

	public void setAuxOnSpline(int id, float a1);

	public void setAuxOnSpline(int id, float a1, float a2);

	public void setAuxOnSpline(int id, float a1, float a2, float a3);

	public void setAuxOnSpline(int id, float a1, float a2, float a3, float a4);

	static public class Join implements iLineOutput {
		private final iLineOutput[] o;

		public Join(iLineOutput[] o) {
			this.o = o;
		}

		public void open() {
			for (int i = 0; i < o.length; i++)
				o[i].open();
		}

		public void beginSpline(iLineIdentifier identitfier) {
			for (int i = 0; i < o.length; i++)
				o[i].beginSpline(identitfier);
		}

		public void moveTo(Vector3 v3) {
			for (int i = 0; i < o.length; i++)
				o[i].moveTo(v3);
		}

		public void lineTo(Vector3 v3) {
			for (int i = 0; i < o.length; i++)
				o[i].lineTo(v3);
		}

		public void curveTo(Vector3 to, Vector3 c1, Vector3 c2, int numSamples) {
			for (int i = 0; i < o.length; i++)
				o[i].curveTo(to, c1, c2, numSamples);
		}

		public void endSpline() {
			for (int i = 0; i < o.length; i++)
				o[i].endSpline();
		}

		public void close() {
			for (int i = 0; i < o.length; i++)
				o[i].close();
		}

		public void remove() {
			for (int i = 0; i < o.length; i++)
				o[i].remove();
		}

		public void setAuxOnSpline(int id, float a1) {
			for (int i = 0; i < o.length; i++)
				o[i].setAuxOnSpline(id, a1);
		}

		public void setAuxOnSpline(int id, float a1, float a2) {
			for (int i = 0; i < o.length; i++)
				o[i].setAuxOnSpline(id, a1, a2);
		}

		public void setAuxOnSpline(int id, float a1, float a2, float a3) {
			for (int i = 0; i < o.length; i++)
				o[i].setAuxOnSpline(id, a1, a2, a3);
		}

		public void setAuxOnSpline(int id, float a1, float a2, float a3, float a4) {
			for (int i = 0; i < o.length; i++)
				o[i].setAuxOnSpline(id, a1, a2, a3, a4);
		}
	}

	static public class Defaults implements iLineOutput {
		iLineOutput delegate;

		Map<Integer, iToFloatArray> defaults = new HashMap<Integer, iToFloatArray>();

		int[] touched = new int[16];

		public Defaults(iLineOutput delegate) {
			this.delegate = delegate;
		}

		public Defaults setAux(int id, iToFloatArray value) {
			defaults.put(id, value);
			return this;
		}

		public void beginSpline(iLineIdentifier identitfier) {
			delegate.beginSpline(identitfier);
			for (int i = 0; i < touched.length; i++)
				touched[i] = 0;
			for (Map.Entry<Integer, iToFloatArray> e : defaults.entrySet()) {
				int id = e.getKey();
				float[] value = e.getValue().get();
				if (value.length == 1)
					delegate.setAuxOnSpline(id, value[0]);
				if (value.length == 2)
					delegate.setAuxOnSpline(id, value[0], value[1]);
				if (value.length == 3)
					delegate.setAuxOnSpline(id, value[0], value[1], value[2]);
				if (value.length == 4)
					delegate.setAuxOnSpline(id, value[0], value[1], value[2], value[3]);
			}
		}

		public void close() {
			delegate.close();
		}

		public void curveTo(Vector3 to, Vector3 c1, Vector3 c2, int numSamples) {
			delegate.curveTo(to, c1, c2, numSamples);
		}

		public void endSpline() {
			for (Map.Entry<Integer, iToFloatArray> e : defaults.entrySet()) {
				int id = e.getKey();
				if (touched[id] == 0) {
					float[] value = e.getValue().get();
					if (value.length == 1)
						delegate.setAuxOnSpline(id, value[0]);
					if (value.length == 2)
						delegate.setAuxOnSpline(id, value[0], value[1]);
					if (value.length == 3)
						delegate.setAuxOnSpline(id, value[0], value[1], value[2]);
					if (value.length == 4)
						delegate.setAuxOnSpline(id, value[0], value[1], value[2], value[3]);
				}
			}
			delegate.endSpline();
		}

		public void lineTo(Vector3 v3) {

			delegate.lineTo(v3);
		}

		public void moveTo(Vector3 v3) {
			delegate.moveTo(v3);
		}

		public void open() {
			delegate.open();
		}

		public void remove() {
			delegate.remove();
		}

		public void setAuxOnSpline(int id, float a1, float a2, float a3, float a4) {
			delegate.setAuxOnSpline(id, a1, a2, a3, a4);
			touched[id]++;
		}

		public void setAuxOnSpline(int id, float a1, float a2, float a3) {
			delegate.setAuxOnSpline(id, a1, a2, a3);
			touched[id]++;
		}

		public void setAuxOnSpline(int id, float a1, float a2) {
			delegate.setAuxOnSpline(id, a1, a2);
			touched[id]++;
		}

		public void setAuxOnSpline(int id, float a1) {
			delegate.setAuxOnSpline(id, a1);
			touched[id]++;
		}

		public iLineOutput getDelegate() {
			return delegate;
		}

	}

	static public class Force implements iLineOutput {
		iLineOutput delegate;

		Map<Integer, iToFloatArray> defaults = new HashMap<Integer, iToFloatArray>();

		int[] touched = new int[16];

		public Force(iLineOutput delegate) {
			this.delegate = delegate;
		}

		public Force setAux(int id, iToFloatArray value) {
			defaults.put(id, value);
			return this;
		}

		public void beginSpline(iLineIdentifier identitfier) {
			delegate.beginSpline(identitfier);
			for (int i = 0; i < touched.length; i++)
				touched[i] = 0;
			for (Map.Entry<Integer, iToFloatArray> e : defaults.entrySet()) {
				int id = e.getKey();
				float[] value = e.getValue().get();
				if (value.length == 1)
					delegate.setAuxOnSpline(id, value[0]);
				if (value.length == 2)
					delegate.setAuxOnSpline(id, value[0], value[1]);
				if (value.length == 3)
					delegate.setAuxOnSpline(id, value[0], value[1], value[2]);
				if (value.length == 4)
					delegate.setAuxOnSpline(id, value[0], value[1], value[2], value[3]);
			}
		}

		public void close() {
			delegate.close();
		}

		public void curveTo(Vector3 to, Vector3 c1, Vector3 c2, int numSamples) {
			delegate.curveTo(to, c1, c2, numSamples);
		}

		public void endSpline() {
			for (Map.Entry<Integer, iToFloatArray> e : defaults.entrySet()) {
				int id = e.getKey();
				{
					float[] value = e.getValue().get();
					if (value.length == 1)
						delegate.setAuxOnSpline(id, value[0]);
					if (value.length == 2)
						delegate.setAuxOnSpline(id, value[0], value[1]);
					if (value.length == 3)
						delegate.setAuxOnSpline(id, value[0], value[1], value[2]);
					if (value.length == 4)
						delegate.setAuxOnSpline(id, value[0], value[1], value[2], value[3]);
				}
			}
			delegate.endSpline();
		}

		public void lineTo(Vector3 v3) {

			delegate.lineTo(v3);
		}

		public void moveTo(Vector3 v3) {
			delegate.moveTo(v3);
		}

		public void open() {
			delegate.open();
		}

		public void remove() {
			delegate.remove();
		}

		public void setAuxOnSpline(int id, float a1, float a2, float a3, float a4) {
			if (!defaults.containsKey(id))
				delegate.setAuxOnSpline(id, a1, a2, a3, a4);
			touched[id]++;
		}

		public void setAuxOnSpline(int id, float a1, float a2, float a3) {
			if (!defaults.containsKey(id))
				delegate.setAuxOnSpline(id, a1, a2, a3);
			touched[id]++;
		}

		public void setAuxOnSpline(int id, float a1, float a2) {
			if (!defaults.containsKey(id))
				delegate.setAuxOnSpline(id, a1, a2);
			touched[id]++;
		}

		public void setAuxOnSpline(int id, float a1) {
			if (!defaults.containsKey(id))
				delegate.setAuxOnSpline(id, a1);
			touched[id]++;
		}

		public iLineOutput getDelegate() {
			return delegate;
		}

	}
}
