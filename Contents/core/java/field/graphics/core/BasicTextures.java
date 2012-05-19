package field.graphics.core;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL21.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL32.*;
import static org.lwjgl.opengl.GL33.*;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL41.*;

import java.net.URL;

import org.lwjgl.opengl.GL12;

import field.graphics.core.Base.StandardPass;
import field.graphics.core.Base.iSceneListElement;
import field.graphics.core.BasicUtilities.TwoPassElement;
import field.graphics.qt.ByteImage;
import field.graphics.windowing.FullScreenCanvasSWT;

/**
 * tools for texturing things
 */
public class BasicTextures {
	abstract static public class BaseTexture extends BasicUtilities.TwoPassElement implements iSceneListElement {
		public static boolean enableTextures = true;

		public int gl_texture_min_filter = GL_NEAREST;

		public int gl_texture_mag_filter = GL_NEAREST;

		public int gl_texture_wrap_s = GL_CLAMP_TO_EDGE;

		public int gl_texture_wrap_t = GL_CLAMP_TO_EDGE;

		public int gl_texture_env_mode = GL_MODULATE;

		public int textureTarget = GL_TEXTURE_2D;

		protected boolean doGenMip;

		public BaseTexture() {
			this("unnamed");
		}

		public BaseTexture(String name) {
			super(name, StandardPass.preRender, StandardPass.postRender);
		}

		public BaseTexture getMipMaps() {
			doGenMip = true;
			gl_texture_min_filter = GL_LINEAR_MIPMAP_LINEAR;
			gl_texture_mag_filter = GL_LINEAR;
			return this;
		}

		/**
		 * Sets the GL texture environment parameter
		 */
		public BaseTexture gl_texture_env_mode(int arg) {
			gl_texture_env_mode = arg;
			return this;
		}

		/**
		 * Sets the GL texture parameter of the same name, must be
		 * called before the first render.
		 */
		public BaseTexture gl_texture_mag_filter(int arg) {
			gl_texture_mag_filter = arg;
			return this;
		}

		/**
		 * Sets the GL texture parameter of the same name, must be
		 * called before the first render.
		 */
		public BaseTexture gl_texture_min_filter(int arg) {
			gl_texture_min_filter = arg;
			return this;
		}

		/**
		 * Sets the GL texture parameter of the same name, must be
		 * called before the first render.
		 */
		public BaseTexture gl_texture_wrap_s(int arg) {
			gl_texture_wrap_s = arg;
			return this;
		}

		/**
		 * Sets the GL texture parameter of the same name, must be
		 * called before the first render.
		 */
		public BaseTexture gl_texture_wrap_t(int arg) {
			gl_texture_wrap_t = arg;
			return this;
		}

		/**
		 * Default is not to use rectangle extension but this can be
		 * changed on a per texture basis.
		 */
		public BaseTexture use_gl_texture_rectangle_ext(boolean useRectangularTexture) {
			if (useRectangularTexture)
				textureTarget = GL_TEXTURE_RECTANGLE;
			else
				textureTarget = GL_TEXTURE_2D;
			return this;
		}

		@Override
		abstract protected void post();

		@Override
		abstract protected void pre();

		@Override
		abstract protected void setup();
	}

	static public class DisableTextures extends BasicUtilities.TwoPassElement implements iSceneListElement {
		boolean texturesEnabled;

		public DisableTextures() {
			super("disable texture", StandardPass.preRender, StandardPass.postRender);
		}

		@Override
		public void post() {
			BasicTextures.BaseTexture.enableTextures = true;
		}

		/**
		 * main entry point, do your work for Pass 'p' here
		 */
		@Override
		public void pre() {
			BasicTextures.BaseTexture.enableTextures = false;
		}

		@Override
		public void setup() {

		}
	}

	static public class ModifyUnit extends BasicUtilities.TwoPassElement {
		int unit;

		public ModifyUnit(int unit) {
			super("", StandardPass.preTransform, StandardPass.postRender);
			this.unit = unit;
		}

		@Override
		protected void post() {
			glActiveTexture(GL_TEXTURE0);
		}

		@Override
		protected void pre() {
			glActiveTexture(GL_TEXTURE0 + unit);
		}

		@Override
		protected void setup() {

		}
	}

	/**
	 * as of our move to 1.6 this class is significantly misnamed. Prior to
	 * Apple's decision to be 64-bit only under 1.6, we used JNI/Quicktime
	 * to load textures (using a helper class QTImage). Since Quicktime
	 * proper is not 64 bit clean, nor is likely to be 64 bit clean, that's
	 * no longer an option.
	 * 
	 * Instead we are now using CoreImage's image loader.
	 */

	static public class TextureFromQTImage extends BaseTexture implements iSceneListElement {

		private boolean deallocated = false;

		protected boolean valid = false;

		// QTImage image;
		public ByteImage image;

		String textureName;

		int pixelsWide;

		int pixelsHigh;

		int samplesPerPixel;

		// static public void
		// setTextureDictionary(HashMap td) {
		// textureDictionary = td;}

		boolean hasAlpha;

		int textureId = -1;

		boolean onCard = false;

		int xx = 0;

		boolean dirty = false;

		public TextureFromQTImage(String name) {
			super(name);
			textureName = name;
			use_gl_texture_rectangle_ext(false);
			valid = loadImage(name);
		}

		public TextureFromQTImage(URL resource) {
			this(resource.toExternalForm());
			assert resource.getProtocol().equals("file") : resource + " " + resource.getProtocol();
		}

		protected TextureFromQTImage(ByteImage image) {
			valid = true;
			this.image = image;
			pixelsWide = image.pixelsWide();
			pixelsHigh = image.pixelsHigh();
			if (pixelsWide != pixelsHigh)
				use_gl_texture_rectangle_ext(true);
			samplesPerPixel = image.samplesPerPixel();
			hasAlpha = (samplesPerPixel == 4) ? true : false;
		}

		public void delete() {
			deallocated = true;
			if (gl != null) {
				glDeleteTextures(textureId);
			}
		}

		public void dirty() {
			dirty = true;

		}

		public void disable() {
		}

		public ByteImage getImage() {
			return image;
		}

		/**
		 * reads in the image data associated with the texture. It
		 * assumes that the texture file is in the resources bundle. It
		 * uses NSImage and NSBitmapImageRep, so it can handle
		 * tif,jpg,bmp, and others.
		 * 
		 */
		public boolean loadImage(String name) {
			image = new ByteImage(name);
			pixelsWide = image.pixelsWide();
			pixelsHigh = image.pixelsHigh();
			if ((pixelsWide != pixelsHigh) || (notPowerOfTwo(pixelsWide) || (notPowerOfTwo(pixelsHigh))))
				use_gl_texture_rectangle_ext(true);
			samplesPerPixel = image.samplesPerPixel();
			hasAlpha = (samplesPerPixel == 4) ? true : false;

			// ByteBuffer b = image.getImage();
			//
			// for (int i = 0; i < b.capacity(); i++) {
			// if (i % 4 == 0)
			// System.out.println();
			// System.out.print(b.get() + " ");
			// }
			//

			return true;
		}

		public void refresh() {
			glTexSubImage2D(textureTarget, 0, 0, 0, pixelsWide, pixelsHigh, GL12.GL_BGRA, GL_UNSIGNED_INT_8_8_8_8, image.getImage());
			if (doGenMip)
				glGenerateMipmap(GL_TEXTURE_2D);
		}

		public void reload() {
			loadImage(textureName);
			dirty = true;
		}

		
		
		
		public void setImage(ByteImage im) {
			if (im != image)
				dirty = true;
			image = im;
		}

		public TextureFromQTImage setOnCard(boolean on) {
			this.onCard = on;
			return this;
		}

		protected boolean notPowerOfTwo(int pixelsWide2) {
			return (pixelsWide2 & (pixelsWide2 - 1)) != 0;
		}

		@Override
		protected void post() {
			assert !deallocated;
			if (!CoreHelpers.isCore)
				glDisable(textureTarget);
			assert (glGetError() == 0);
		}

		@Override
		protected void pre() {
			assert !deallocated;
			assert (glGetError() == 0) : this.getClass().getName();
			int textureId = BasicContextManager.getId(this);
			if (textureId == BasicContextManager.ID_NOT_FOUND) {
				setup();
				textureId = BasicContextManager.getId(this);
				assert textureId != BasicContextManager.ID_NOT_FOUND : "called setup() in texture, didn't get an ID has subclass forgotten to call BasicContextIDManager.pudId(...) ?";
			}
			if (BaseTexture.enableTextures) {
				glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, gl_texture_env_mode);
				glBindTexture(textureTarget, textureId);
				if (!CoreHelpers.isCore)
					glEnable(textureTarget);
			}
			if (dirty) {
				dirty = false;
				refresh();
			}
			assert (glGetError() == 0) : this.getClass().getName();
		}

		/**
		 * Called once per instance of TextureFromQTImage to create the
		 * appropriate texture object. It uses the texture parameters
		 * associated with the instance at the time it is called.
		 */
		@Override
		protected void setup() {
			assert valid;
			if (!valid)
				return;
			xx++;
			// create the textureId and bind
			// the texture
			int[] textures = new int[1];
			textures[0] = glGenTextures();

			textureId = textures[0];
			BasicContextManager.putId(this, textureId);
			glBindTexture(textureTarget, textureId);

			// now create the texture
			// if (!onCard) {
			// glPixelStorei(GL_UNPACK_CLIENT_STORAGE_APPLE, 1);
			// } else {
			// glPixelStorei(GL_UNPACK_CLIENT_STORAGE_APPLE, 1);
			// }

			glTexImage2D(textureTarget, 0, GL_RGBA, pixelsWide, pixelsHigh, 0, GL12.GL_BGRA, GL_UNSIGNED_INT_8_8_8_8, image.getImage());
			if (doGenMip) {
				glTexParameteri(GL_TEXTURE_2D, GL_GENERATE_MIPMAP, 1);
			}
			if (onCard) {
				// glTexParameteri(textureTarget,
				// GL_TEXTURE_STORAGE_HINT_APPLE,
				// GL_STORAGE_CACHED_APPLE);

			}
			// now set parameters based on
			// textureParams
			glTexParameteri(textureTarget, GL_TEXTURE_WRAP_S, gl_texture_wrap_s);
			glTexParameteri(textureTarget, GL_TEXTURE_WRAP_T, gl_texture_wrap_t);
			System.out.println(" tex parameter i <" + textureTarget + "> <" + gl_texture_mag_filter + "> <" + gl_texture_min_filter + ">");
			glTexParameteri(textureTarget, GL_TEXTURE_MAG_FILTER, gl_texture_mag_filter);
			glTexParameteri(textureTarget, GL_TEXTURE_MIN_FILTER, gl_texture_min_filter);
			glTexParameteri(textureTarget, GL_TEXTURE_WRAP_S, gl_texture_wrap_s);
			glTexParameteri(textureTarget, GL_TEXTURE_WRAP_T, gl_texture_wrap_t);
			System.out.println(" tex parameter i <" + textureTarget + "> <" + gl_texture_mag_filter + "> <" + gl_texture_min_filter + ">");
			glTexParameteri(textureTarget, GL_TEXTURE_MAG_FILTER, gl_texture_mag_filter);
			glTexParameteri(textureTarget, GL_TEXTURE_MIN_FILTER, gl_texture_min_filter);

			if (doGenMip)
				glGenerateMipmap(GL_TEXTURE_2D);
			
		}
	}

	static public class ExplicitMipTexture extends BaseTexture implements iSceneListElement {

		private boolean deallocated = false;

		protected boolean valid = false;

		// QTImage image;
		ByteImage[] image;

		String[] textureName;

		int pixelsWide;

		int pixelsHigh;

		int samplesPerPixel;

		// static public void
		// setTextureDictionary(HashMap td) {
		// textureDictionary = td;}

		boolean hasAlpha;

		int textureId = -1;

		boolean onCard = false;

		int xx = 0;

		boolean dirty = false;

		public ExplicitMipTexture(String... name) {
			super(name[0]);

			System.out.println(" ExplicitMipTexture constructor");

			textureName = name;
			use_gl_texture_rectangle_ext(false);
			loadImage(name);
			valid = true;
		}

		public void delete() {
			deallocated = true;
			if (gl != null) {
				glDeleteTextures(textureId);
			}
		}

		public void dirty() {
			dirty = true;

		}

		public void disable() {
		}

		public boolean loadImage(String... name) {
			image = new ByteImage[name.length];

			System.out.println(" ExplicitMipTexture <" + name.length + ">");
			for (int i = 0; i < name.length; i++) {
				image[i] = new ByteImage(name[i]);

				System.out.println(" loaded <" + image[i] + "> <" + name[i] + ">");

				if (i == 0) {
					pixelsWide = image[0].pixelsWide();
					pixelsHigh = image[0].pixelsHigh();
					samplesPerPixel = image[0].samplesPerPixel();
					hasAlpha = (samplesPerPixel == 4) ? true : false;
				}
			}
			return true;
		}

		public void refresh() {
			for (int i = 0; i < image.length; i++)
				glTexSubImage2D(textureTarget, i, 0, 0, pixelsWide >> i, pixelsHigh >> i, GL12.GL_BGRA, GL_UNSIGNED_INT_8_8_8_8, image[i].getImage());
		}

		public void reload() {
			loadImage(textureName);
			dirty = true;
		}

		@Override
		protected void post() {
			assert !deallocated;
			if (!CoreHelpers.isCore)
				glDisable(textureTarget);
			assert (glGetError() == 0);
		}

		@Override
		protected void pre() {

			System.out.println(" pre for ExplicitMipTexture");

			assert !deallocated;
			assert (glGetError() == 0) : this.getClass().getName();
			int textureId = BasicContextManager.getId(this);
			if (textureId == BasicContextManager.ID_NOT_FOUND) {
				setup();
				textureId = BasicContextManager.getId(this);
				assert textureId != BasicContextManager.ID_NOT_FOUND : "called setup() in texture, didn't get an ID has subclass forgotten to call BasicContextIDManager.pudId(...) ?";
			}
			if (BaseTexture.enableTextures) {
				glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, gl_texture_env_mode);
				glBindTexture(textureTarget, textureId);
				if (!CoreHelpers.isCore)
					glEnable(textureTarget);
			}
			if (dirty) {
				dirty = false;
				refresh();
			}
			assert (glGetError() == 0) : this.getClass().getName();
		}

		/**
		 * Called once per instance of TextureFromQTImage to create the
		 * appropriate texture object. It uses the texture parameters
		 * associated with the instance at the time it is called.
		 */
		@Override
		protected void setup() {

			System.out.println(" setup for ExplicitMipTexture <" + valid + ">");

			assert valid;
			if (!valid)
				return;
			xx++;
			// create the textureId and bind
			// the texture
			int[] textures = new int[1];
			textures[0] = glGenTextures();

			textureId = textures[0];
			BasicContextManager.putId(this, textureId);
			glBindTexture(textureTarget, textureId);

			// now create the texture
			// if (!onCard) {
			// glPixelStorei(GL_UNPACK_CLIENT_STORAGE_APPLE, 1);
			// } else {
			// glPixelStorei(GL_UNPACK_CLIENT_STORAGE_APPLE, 1);
			// }

			// glTexParameteri(GL_TEXTURE_2D,
			// GL_GENERATE_MIPMAP_SGIS, 1);

			System.out.println(" binding explicit mip texture");
			for (int i = 0; i < image.length; i++) {
				System.out.println(" level <" + i + "> is <" + image[i].getImage() + "> <" + (pixelsWide >> i) + " x " + (pixelsHigh >> i) + "> target <" + textureTarget + "> <" + GL_TEXTURE_2D + ">");
				glTexImage2D(textureTarget, i, GL_RGBA, pixelsWide >> i, pixelsHigh >> i, 0, GL12.GL_BGRA, GL_UNSIGNED_INT_8_8_8_8, image[i].getImage());
			}
			// glTexParameteri(textureTarget,
			// GL_TEXTURE_STORAGE_HINT_APPLE,
			// GL_STORAGE_CACHED_APPLE);
			// now set parameters based on
			// textureParams
			glTexParameteri(textureTarget, GL_TEXTURE_WRAP_S, gl_texture_wrap_s);
			glTexParameteri(textureTarget, GL_TEXTURE_WRAP_T, gl_texture_wrap_t);
			System.out.println(" tex parameter i <" + textureTarget + "> <" + gl_texture_mag_filter + "> <" + gl_texture_min_filter + ">");
			glTexParameteri(textureTarget, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			glTexParameteri(textureTarget, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);

		}
	}

	/**
	 * synonym for the now misnamed TextureFromQTImage
	 */
	static public class TextureFromImage extends TextureFromQTImage {

		public TextureFromImage(ByteImage image) {
			super(image);
		}

		public TextureFromImage(String name) {
			super(name);
		}

		public TextureFromImage(URL resource) {
			super(resource);
		}

		public TextureFromImage rectangle() {
			use_gl_texture_rectangle_ext(true);
			return this;
		}

		public TextureFromImage square() {
			use_gl_texture_rectangle_ext(false);
			return this;
		}
	}

	/** wraps some other texture thing inside a different unit */
	static public class TextureUnit extends BaseTexture {
		private int unit;

		TwoPassElement wrap;

		public TextureUnit(int unit, TwoPassElement wrap) {
			this.unit = unit;
			this.wrap = wrap;
			assert wrap != null;
		}

		public void in(Object gl) {
			glActiveTexture(GL_TEXTURE0 + getUnit());
		}

		public void out(Object gl) {
			glActiveTexture(GL_TEXTURE0);
		}

		@Override
		protected void post() {
			// System.out.println(unit+" ))");
			assert glGetError() == 0;
			glActiveTexture(GL_TEXTURE0 + getUnit());
			assert glGetError() == 0;
			wrap.gl = gl;
			wrap.glu = glu;
			assert glGetError() == 0;
			wrap.post();
			assert glGetError() == 0;
			glActiveTexture(GL_TEXTURE0);
			assert glGetError() == 0;
		}

		@Override
		protected void pre() {
			// System.out.println("(("+unit);
			assert glGetError() == 0;
			glActiveTexture(GL_TEXTURE0 + getUnit());
			assert glGetError() == 0;
			wrap.gl = gl;
			wrap.glu = glu;
			assert glGetError() == 0;
			wrap.pre();
			assert glGetError() == 0 : wrap.getClass();
			assert glGetError() == 0;
			glActiveTexture(GL_TEXTURE0);
			assert glGetError() == 0;
		}

		@Override
		protected void setup() {
			wrap.gl = gl;
			wrap.glu = glu;

			assert glGetError() == 0;
			// why was this commented out?

			glActiveTexture(GL_TEXTURE0 + getUnit());
			assert glGetError() == 0;

			wrap.setup();
			assert glGetError() == 0;
			glActiveTexture(GL_TEXTURE0);
			assert glGetError() == 0;
		}

		public int getUnit() {
			return unit;
		}

		public TwoPassElement getWrapped() {
			return wrap;
		}
	}

	static public class StereoUnit extends BaseTexture {
		private int unit;

		TwoPassElement left;

		TwoPassElement right;

		public StereoUnit(int unit, TwoPassElement left, TwoPassElement right) {
			this.unit = unit;
			this.left = left;
			this.right = right;

		}

		public void in(Object gl) {
			glActiveTexture(GL_TEXTURE0 + getUnit());
		}

		public void out(Object gl) {
			glActiveTexture(GL_TEXTURE0);
		}

		@Override
		protected void post() {

			TwoPassElement left;
			if (FullScreenCanvasSWT.currentCanvas.getSide().x < 0)
				left = this.left;
			else
				left = this.right;

			// System.out.println(unit+" ))");
			assert glGetError() == 0;
			glActiveTexture(GL_TEXTURE0 + getUnit());
			assert glGetError() == 0;
			left.gl = gl;
			left.glu = glu;
			assert glGetError() == 0;
			left.post();
			assert glGetError() == 0;
			glActiveTexture(GL_TEXTURE0);
			assert glGetError() == 0;
		}

		@Override
		protected void pre() {
			TwoPassElement left;
			if (FullScreenCanvasSWT.currentCanvas.getSide().x < 0)
				left = this.left;
			else
				left = this.right;
			// System.out.println("(("+unit);
			assert glGetError() == 0;
			glActiveTexture(GL_TEXTURE0 + getUnit());
			assert glGetError() == 0;
			left.gl = gl;
			left.glu = glu;
			assert glGetError() == 0;
			left.pre();
			assert glGetError() == 0 : left.getClass();
			assert glGetError() == 0;
			glActiveTexture(GL_TEXTURE0);
			assert glGetError() == 0;
		}

		@Override
		protected void setup() {
			left.gl = gl;
			left.glu = glu;

			assert glGetError() == 0;
			// why was this commented out?

			glActiveTexture(GL_TEXTURE0 + getUnit());
			assert glGetError() == 0;

			left.setup();
			right.setup();
			assert glGetError() == 0;
			glActiveTexture(GL_TEXTURE0);
			assert glGetError() == 0;
		}

		public int getUnit() {
			return unit;
		}

		public TwoPassElement getWrapped() {
			return left;
		}
	}
}
