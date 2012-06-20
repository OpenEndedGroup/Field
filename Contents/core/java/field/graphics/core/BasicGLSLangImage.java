package field.graphics.core;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL42;

import field.graphics.core.Base.StandardPass;
import field.graphics.core.BasicFrameBuffers.iHasTexture;
import field.graphics.core.BasicUtilities.OnePassElement;

public class BasicGLSLangImage extends OnePassElement{

	private int unit;
	private iHasTexture from;
	private int format;

	public BasicGLSLangImage(int unit, iHasTexture from, int format)
	{
		super(StandardPass.preRender);
		this.unit = unit;
		this.from = from;
		this.format = format;
	}
	
	@Override
	public void performPass() {
		GL42.glBindImageTexture(unit, from.getOutput().get(), 0, false, 0, GL15.GL_READ_WRITE, format);
	}
	
	
	
}
