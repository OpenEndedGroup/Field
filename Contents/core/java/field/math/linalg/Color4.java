package field.math.linalg;

import java.nio.FloatBuffer;

import org.eclipse.swt.graphics.Color;

import field.launch.Launcher;

public class Color4 extends Vector4{
	
	private static final long serialVersionUID = -8118940753288852521L;

	public Color4() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Color4(double d, double e, double f, double g) {
		super(d, e, f, g);
		// TODO Auto-generated constructor stub
	}

	public Color4(float x, float y, float z, float w) {
		super(x, y, z, w);
		// TODO Auto-generated constructor stub
	}

	public Color4(float[] v) {
		super(v);
		// TODO Auto-generated constructor stub
	}

	public Color4(FloatBuffer color, int i) {
		super(color, i);
		// TODO Auto-generated constructor stub
	}

	public Color4(Tuple4 t1) {
		super(t1);
		// TODO Auto-generated constructor stub
	}

	public Color4(Vector3 t1) {
		super(t1);
		// TODO Auto-generated constructor stub
	}

	public Color4(Vector4 v1) {
		super(v1);
		// TODO Auto-generated constructor stub
	}

	public Color toSWTColor() {
		return new Color(Launcher.display,  (int)(x*255), (int)(y*255), (int)(z*255));
	}

}
