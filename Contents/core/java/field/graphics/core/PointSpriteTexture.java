/**
 * 
 */
package field.graphics.core;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL21.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.ARBPointParameters.*;
import static org.lwjgl.opengl.ARBPointSprite.*;

public class PointSpriteTexture extends BasicTextures.TextureFromQTImage {
	public PointSpriteTexture(String name) {
		super(name);
	}

	float[] maxSize = new float[1];

	@Override
	protected void setup() {
		super.setup();
		maxSize[0] = glGetFloat(GL_POINT_SIZE_MAX_ARB);
	}

	@Override
	protected void pre() {
		super.pre();
		glPointSize(maxSize[0]);
		glPointParameterfARB(GL_POINT_SIZE_MAX_ARB, maxSize[0]);
		glPointParameterfARB(GL_POINT_SIZE_MIN_ARB, 1.0f);
		glTexEnvf(GL_POINT_SPRITE_ARB, GL_COORD_REPLACE_ARB, GL_TRUE);
		glEnable(GL_POINT_SPRITE_ARB);
	}
	
	@Override
	protected void post() {
		super.post();
		glDisable(GL_POINT_SPRITE_ARB);
		glTexEnvf(GL_POINT_SPRITE_ARB, GL_COORD_REPLACE_ARB, GL_FALSE);
	}
}