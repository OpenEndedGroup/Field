package field.math.util;

import field.math.abstraction.iBlendable;
import field.math.abstraction.iHasScalar;
import field.math.linalg.Vector4;

public class ComplexCubicFloat implements iBlendable<ComplexCubicFloat>, iHasScalar {

	public enum TangentType {
		normal(0, 0, 0, 1), sharp(0,0, 0, 0), fat(0, 1, 0, 1), forward(0, 0, 1, 1), backward(0, 0, -1, 1);

		public Vector4 tcbw;

		TangentType(float t, float c, float b, float w) {
			this.tcbw = new Vector4(t, c, b, w);
		}
	}

	public float value;

	public TangentType tangent = TangentType.normal;

	public ComplexCubicFloat(ComplexCubicFloat data) {
		this.value = data.value;
		this.tangent = data.tangent;
	}

	public ComplexCubicFloat(float f) {
		this.value = f;
	}

	public ComplexCubicFloat(float f, TangentType tt) {
		this.value = f;
		this.tangent = tt;
	}

	public ComplexCubicFloat blendRepresentation_newZero() {
		return new ComplexCubicFloat(0);
	}

	public ComplexCubicFloat cerp(ComplexCubicFloat before, float beforeTime, ComplexCubicFloat now, float nowTime, ComplexCubicFloat next, float nextTime, ComplexCubicFloat after, float afterTime, float a) {
		ComplexCubicFloat z;
		if (now.tangent == null)
			now.tangent = TangentType.normal;
		if (next.tangent == null)
			next.tangent = TangentType.normal;
		value = (CubicTools.cubicTCB(before.value, now.value, next.value, after.value, a, now.tangent.tcbw, next.tangent.tcbw));
		return this;
	}

	public double getDoubleValue() {
		return value;
	}

	public ComplexCubicFloat lerp(ComplexCubicFloat before, ComplexCubicFloat now, float a) {
		value = CubicTools.cubicTCB(before.value, before.value, now.value, now.value, a, before.tangent.tcbw, now.tangent.tcbw);
		return this;
	}

	public ComplexCubicFloat setValue(ComplexCubicFloat to) {
		this.value = to.value;
		this.tangent = to.tangent;
		return this;
	}

	@Override
	public String toString() {
		return "ccf:" + value + "(" + tangent + ")";
	}
}
