package field.math.util;

import field.math.abstraction.iBlendAlgebra;
import field.math.abstraction.iFilter;

public class Blender implements iBlendAlgebra<Blender>, iFilter<Float, Float> {

	float a;

	float b;

	public Blender(float a, float b) {
		this.a = a;
		this.b = b;
	}
	
	public Float filter(Float value) {
		return value*a+b;
	}

	public Blender setFromMaxMin(float max, float min) {
		this.a = max - min;
		this.b = min;
		return this;
	}

	public Blender blendRepresentation_multiply(float by, Blender input) {
		input.a = input.a * by + 1 - by;
		input.b = input.b * by;
		return input;
	}

	public Blender blendRepresentation_add(Blender one, Blender two, Blender out) {
		out.a = one.a * two.a;
		out.b = one.b * two.a + two.b;
		return out;
	}

	public Blender blendRepresentation_newZero() {
		return new Blender(1, 0);
	}

	public Blender blendRepresentation_duplicate(Blender t) {
		return new Blender(t.a, t.b);
	}

	@Override
	public String toString() {
		return "blend:"+a+", "+b;
	}
}
