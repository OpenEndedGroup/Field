package field.graphics.core;

import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL12.glCopyTexSubImage3D;
import static org.lwjgl.opengl.GL12.glTexImage3D;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_HALF_FLOAT;
import static org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_RGBA16F;
import static org.lwjgl.opengl.GL30.GL_TEXTURE_2D_ARRAY;
import static org.lwjgl.opengl.GL30.glFramebufferTextureLayer;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL32;

import field.graphics.core.BasicTextures.BaseTexture;

public class LayeredNullTexture extends BaseTexture {

	private int width;
	private int height;
	private int layers;
	private int textureId;
	private int unit;

	public LayeredNullTexture(int width, int height, int layers, int unit) {
		this.width = width;
		this.height = height;
		this.layers = layers;
		this.unit = unit;
	}

	@Override
	protected void post() {
		glActiveTexture(GL_TEXTURE0 + unit);
		glBindTexture(GL_TEXTURE_2D_ARRAY, 0);
		glActiveTexture(GL_TEXTURE0);
	}

	@Override
	protected void pre() {
		glActiveTexture(GL_TEXTURE0 + unit);
		glBindTexture(GL_TEXTURE_2D_ARRAY, textureId);
	}

	@Override
	protected void setup() {

		int[] textures = new int[1];
		textures[0] = glGenTextures();
		textureId = textures[0];
		BasicContextManager.putId(this, textureId);

		glBindTexture(GL_TEXTURE_2D_ARRAY, textureId);
		glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		glTexImage3D(GL_TEXTURE_2D_ARRAY, 0, GL_RGBA16F, width, height, layers, 0, GL_RGBA, GL_HALF_FLOAT, (ByteBuffer) null);

	}

	// doesn't work
	public void copyToNow(LayeredFrameBuffer b) {
		pre();
//		glFramebufferTextureLayer(GL_READ_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, b.tex[0], 0, 1);
		glCopyTexSubImage3D(GL_TEXTURE_2D_ARRAY, 0, 0, 0, 1, 0, 0, width, height);
//		glFramebufferTextureLayer(GL_READ_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, b.tex[0], 0, 0);
		glCopyTexSubImage3D(GL_TEXTURE_2D_ARRAY, 0, 0, 0, 0, 0, 0, width, height);
		post();
	}

}
