package field.graphics.core;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL42;

import field.graphics.core.Base.StandardPass;
import field.graphics.core.BasicFrameBuffers.NullTexture;
import field.graphics.core.BasicFrameBuffers.iHasTexture;
import field.graphics.core.BasicTextures.TextureUnit;
import field.graphics.core.BasicUtilities.OnePassElement;
import field.graphics.core.BasicUtilities.TwoPassElement;

public class BasicGLSLangImage extends OnePassElement {

	private int unit;
	private iHasTexture from;
	private int format;
	private TextureUnit unitWrapper;

	public BasicGLSLangImage(int unit, iHasTexture from, int format) {
		super(StandardPass.preRender);
		this.unit = unit;
		this.from = from;
		this.format = format;
	}

	public BasicGLSLangImage(int unit, int width, int height) {
		super(StandardPass.preRender);
		this.unit = unit;

		BasicFrameBuffers.useRG = true;
		BasicFrameBuffers.use32 = false;
		
		from = new NullTexture(width, height);
//		((NullTexture)from).useStorage();
		unitWrapper = new TextureUnit(unit, (TwoPassElement) from);

//		this.format = GL30.GL_RGBA32F;
		this.format = GL30.GL_RG16F;
	}

	@Override
	public void performPass() {
		if (unitWrapper != null)
			unitWrapper.performPass(null);
		GL42.glBindImageTexture(unit, from.getOutput().get(), 0, false, 0, GL15.GL_READ_WRITE, format);
	}
}
