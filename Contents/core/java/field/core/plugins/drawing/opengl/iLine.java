package field.core.plugins.drawing.opengl;

import field.bytecode.protect.annotations.GenerateMethods;
import field.bytecode.protect.annotations.Mirror;
import field.util.Dict.Prop;

@GenerateMethods
public interface iLine {
	
	@Mirror
	public void moveTo(float x, float y);

	@Mirror
	public void lineTo(float x, float y);

	@Mirror
	public void cubicTo(float cx1, float cy1, float cx2, float cy2, float x, float y);

	@Mirror
	public <T> void setPointAttribute(Prop<T> p, T t);

	@Mirror
	public void close();
}
