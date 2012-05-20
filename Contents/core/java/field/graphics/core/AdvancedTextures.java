package field.graphics.core;

import static org.lwjgl.opengl.APPLEClientStorage.GL_UNPACK_CLIENT_STORAGE_APPLE;
import static org.lwjgl.opengl.APPLETextureRange.GL_STORAGE_CACHED_APPLE;
import static org.lwjgl.opengl.APPLETextureRange.GL_STORAGE_PRIVATE_APPLE;
import static org.lwjgl.opengl.APPLETextureRange.GL_STORAGE_SHARED_APPLE;
import static org.lwjgl.opengl.APPLETextureRange.GL_TEXTURE_STORAGE_HINT_APPLE;
import static org.lwjgl.opengl.APPLETextureRange.glTextureRangeAPPLE;
import static org.lwjgl.opengl.GL11.GL_CLAMP;
import static org.lwjgl.opengl.GL11.GL_INTENSITY;
import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_LINEAR_MIPMAP_LINEAR;
import static org.lwjgl.opengl.GL11.GL_LUMINANCE;
import static org.lwjgl.opengl.GL11.GL_LUMINANCE8;
import static org.lwjgl.opengl.GL11.GL_LUMINANCE8_ALPHA8;
import static org.lwjgl.opengl.GL11.GL_LUMINANCE_ALPHA;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_REPEAT;
import static org.lwjgl.opengl.GL11.GL_RGB;
import static org.lwjgl.opengl.GL11.GL_RGB8;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_1D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_ENV;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_ENV_MODE;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNPACK_ROW_LENGTH;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glCopyTexSubImage2D;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL11.glPixelStorei;
import static org.lwjgl.opengl.GL11.glTexEnvi;
import static org.lwjgl.opengl.GL11.glTexImage1D;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL11.glTexSubImage1D;
import static org.lwjgl.opengl.GL11.glTexSubImage2D;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_3D;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_WRAP_R;
import static org.lwjgl.opengl.GL12.GL_UNSIGNED_INT_8_8_8_8;
import static org.lwjgl.opengl.GL12.GL_UNSIGNED_INT_8_8_8_8_REV;
import static org.lwjgl.opengl.GL12.glTexImage3D;
import static org.lwjgl.opengl.GL14.GL_GENERATE_MIPMAP;
import static org.lwjgl.opengl.GL30.GL_R32F;
import static org.lwjgl.opengl.GL30.GL_RGBA32F;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.opengl.GL31.GL_TEXTURE_RECTANGLE;


import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.*;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.LinkedHashMap;

import org.lwjgl.opengl.APPLEYcbcr422;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.OpenGLException;

import field.core.Platform;
import field.core.Platform.OS;
import field.graphics.core.Base.StandardPass;
import field.graphics.core.Base.iPass;
import field.graphics.qt.QTImage;
import field.util.TaskQueue;

/**
 * @author marc
 */
public class AdvancedTextures extends BasicTextures {

	static public class Base1ByteTexture extends BaseTexture {
		ByteBuffer pixelBuffer;

		TextureRange in;

		int allocated = 0;

		int width;

		int height;

		int textureId = 0;

		boolean dirty = false;

		public Base1ByteTexture(ByteBuffer b, int width, int height) {
			super("");
			this.pixelBuffer = b;
			this.width = width;
			this.height = height;
		}

		public void delete() {
			if (gl != null)
				glDeleteTextures(textureId);
		}

		public void dirty() {
			dirty = true;
		}

		public Base1ByteTexture allwaysDirty() {
			allwaysDirty = true;
			return this;
		}

		boolean allwaysDirty = false;

		public Base1ByteTexture setTextureRange(TextureRange in) {
			this.in = in;

			return this;
		}

		@Override
		protected void post() {
			glDisable(GL_TEXTURE_RECTANGLE);
		}

		@Override
		protected void pre() {
			int textureId = BasicContextManager.getId(this);
			if (textureId == BasicContextManager.ID_NOT_FOUND) {
				setup();
				textureId = BasicContextManager.getId(this);
				assert textureId != BasicContextManager.ID_NOT_FOUND : "called setup() in texture, didn't get an ID has subclass forgotten to call BasicContextIDManager.pudId(...) ?";
			}
			assert (glGetError() == 0) : this.getClass().getName();
			glBindTexture(GL_TEXTURE_RECTANGLE, textureId);
			assert (glGetError() == 0) : this.getClass().getName() + " " + BasicContextManager.getCurrentContext();
			glEnable(GL_TEXTURE_RECTANGLE);
			assert (glGetError() == 0) : this.getClass().getName();
			if (in != null)
				in.declareNow(gl);

			assert glGetError() == 0;

			if (allwaysDirty)
				dirty = true;

			if (dirty) {
				;//System.out.println(" pixel buffer is <" + pixelBuffer + ">");
				pixelBuffer.rewind();
				glTexSubImage2D(GL_TEXTURE_RECTANGLE, 0, 0, 0, width, height, GL_LUMINANCE, GL_UNSIGNED_BYTE, pixelBuffer);
				// glTexSubImage2D(GL_TEXTURE_RECTANGLE,
				// 0, 0, 0, width, height, GL_BGRA,
				// GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
			}
			dirty = false;
		}

		@Override
		protected void setup() {
			int[] textures = new int[1];
			textures[0] = glGenTextures();
			textureId = textures[0];
			BasicContextManager.putId(this, textureId);
			glBindTexture(GL_TEXTURE_RECTANGLE, textureId);

			if (in != null)
				in.declareNow(gl);

			// glTexParameteri(GL_TEXTURE_RECTANGLE,
			// GL_TEXTURE_STORAGE_HINT_APPLE,
			// GL_STORAGE_SHARED_APPLE);
			// glPixelStorei(GL_UNPACK_CLIENT_STORAGE_APPLE,
			// 1);
			// glTexParameteri(GL_TEXTURE_RECTANGLE,
			// GL_TEXTURE_MIN_FILTER, GL_NEAREST);
			// glTexParameteri(GL_TEXTURE_RECTANGLE,
			// GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
			glTexImage2D(GL_TEXTURE_RECTANGLE, 0, GL_LUMINANCE8, width, height, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, pixelBuffer);
			// glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);

			// glTexImage2D(GL_TEXTURE_RECTANGLE, 0,
			// GL_RGBA, width, height, 0, GL_BGRA,
			// GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
			assert (glGetError() == 0);
		}

		void declareNow() {
			// glTextureRangeAPPLE(GL_TEXTURE_RECTANGLE,
			// pixelBuffer.capacity(), pixelBuffer);
		}

	}

	// broken (GPU bug?)
	static public class Base2ByteTexture extends BaseTexture {
		ByteBuffer pixelBuffer;

		TextureRange in;

		int allocated = 0;

		int width;

		int height;

		int textureId = 0;

		boolean dirty = false;

		public Base2ByteTexture(ByteBuffer b, int width, int height) {
			super("");
			this.pixelBuffer = b;
			this.width = width;
			this.height = height;
		}

		public void dirty() {
			dirty = true;
		}

		public Base2ByteTexture setTextureRange(TextureRange in) {
			this.in = in;

			return this;
		}

		@Override
		protected void post() {
			glDisable(this.textureTarget);
		}

		@Override
		protected void pre() {
			int textureId = BasicContextManager.getId(this);
			if (textureId == BasicContextManager.ID_NOT_FOUND) {
				setup();
				textureId = BasicContextManager.getId(this);
				assert textureId != BasicContextManager.ID_NOT_FOUND : "called setup() in texture, didn't get an ID has subclass forgotten to call BasicContextIDManager.pudId(...) ?";
			}
			// ;//System.out.println(" binding texture <" + textureId +
			// "> <" + pixelBuffer + "> <" + in + ">");
			assert (glGetError() == 0) : this.getClass().getName();
			glBindTexture(this.textureTarget, textureId);
			assert (glGetError() == 0) : this.getClass().getName() + " " + BasicContextManager.getCurrentContext();
			glEnable(this.textureTarget);
			assert (glGetError() == 0) : this.getClass().getName();
			if (in != null)
				in.declareNow(gl);

			assert glGetError() == 0;

			if (dirty) {
				glTexSubImage2D(this.textureTarget, 0, 0, 0, width, height, GL_LUMINANCE_ALPHA, GL_UNSIGNED_BYTE, pixelBuffer);
				// glTexSubImage2D(this.textureTarget, 0, 0,
				// 0, width, height, GL_BGRA,
				// GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
			}
			dirty = false;
		}

		@Override
		protected void setup() {
			int[] textures = new int[1];
			textures[0] = glGenTextures();
			textureId = textures[0];
			BasicContextManager.putId(this, textureId);
			glBindTexture(this.textureTarget, textureId);

			if (in != null)
				in.declareNow(gl);

			glTexParameteri(this.textureTarget, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			glTexParameteri(this.textureTarget, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
			glTexParameteri(this.textureTarget, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			glTexParameteri(this.textureTarget, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(this.textureTarget, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
			// not sure about this...
			glTexImage2D(this.textureTarget, 0, GL_LUMINANCE8_ALPHA8, width, height, 0, GL_LUMINANCE_ALPHA, GL_UNSIGNED_BYTE, pixelBuffer);
			assert (glGetError() == 0);
			// glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);

			// glTexImage2D(this.textureTarget, 0, GL_RGBA,
			// width, height, 0, GL_BGRA,
			// GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
			assert (glGetError() == 0);
		}

		void declareNow() {
			glTextureRangeAPPLE(this.textureTarget, pixelBuffer);
		}

	}

	static public class Base3ByteTexture extends BaseTexture {
		protected ByteBuffer pixelBuffer;

		TextureRange in;

		int allocated = 0;

		int width;

		int height;

		int textureId = 0;

		int dirty = 0;
		LinkedHashMap<Object, Integer> modCounts = new LinkedHashMap<Object, Integer>();

		public Base3ByteTexture(ByteBuffer b, int width, int height) {
			super("");
			this.pixelBuffer = b;
			this.width = width;
			this.height = height;
		}

		public void dirty() {
			dirty ++;
		}

		public Base3ByteTexture allwaysDirty() {
			allwaysDirty = true;
			return this;
		}

		boolean allwaysDirty = false;

		@Override
		public void post() {
			glDisable(this.textureTarget);
		}

		@Override
		public void pre() {
			int textureId = BasicContextManager.getId(this);
			if (textureId == BasicContextManager.ID_NOT_FOUND) {
				setup();
				textureId = BasicContextManager.getId(this);
				assert textureId != BasicContextManager.ID_NOT_FOUND : "called setup() in texture, didn't get an ID has subclass forgotten to call BasicContextIDManager.pudId(...) ?";
			}
			glEnable(this.textureTarget);
			glBindTexture(this.textureTarget, textureId);
			
			if (Platform.isMac())
			{
				glTexParameteri(textureTarget, GL_TEXTURE_STORAGE_HINT_APPLE, GL_STORAGE_SHARED_APPLE);

				glPixelStorei(GL_UNPACK_CLIENT_STORAGE_APPLE, GL11.GL_TRUE);
			}

			if (in != null)
				in.declareNow(gl);

			assert glGetError() == 0;

//			if (allwaysDirty)
//				dirty = true;

			if (isDirty(BasicContextManager.getCurrentContext())) {
				pixelBuffer.rewind();

				if (this.doGenMip)
				{

					glTexParameteri(this.textureTarget, GL_GENERATE_MIPMAP, 1);
				}
				glTexSubImage2D(this.textureTarget, 0, 0, 0, width, height, GL_RGB, GL_UNSIGNED_BYTE, pixelBuffer);
				// glTexSubImage2D(this.textureTarget, 0, 0,
				// 0, width, height, GL_BGRA,
				// GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
			}
			
			modCounts.put(BasicContextManager.getCurrentContext(), dirty);
			
		}

		private boolean isDirty(Object currentContext) {
			Integer o = modCounts.get(currentContext);
			if (o==null) return true;
			if (o.intValue()!=dirty) return true;
			return false;
		}

		public void setGL(Object gl) {
			this.gl = gl;
		}

		public Base3ByteTexture setTextureRange(TextureRange in) {
			this.in = in;

			return this;
		}

		@Override
		public void setup() {
			int[] textures = new int[1];
			textures[0] = glGenTextures();
			textureId = textures[0];
			BasicContextManager.putId(this, textureId);
			glBindTexture(this.textureTarget, textureId);

			if (in != null)
				in.declareNow(gl);

			glTexParameteri(this.textureTarget, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			glTexParameteri(this.textureTarget, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
			glTexParameteri(this.textureTarget, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			glTexParameteri(this.textureTarget, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(this.textureTarget, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
			
			if (doGenMip)
			{

				glTexParameteri(this.textureTarget, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
				glTexParameteri(this.textureTarget, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
				glTexParameteri(this.textureTarget, GL_GENERATE_MIPMAP, 1);
			}

			// not sure about this...
			glTexImage2D(this.textureTarget, 0, GL_RGB8, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, pixelBuffer);
			assert (glGetError() == 0);
			// glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);

			// glTexImage2D(this.textureTarget, 0, GL_RGBA,
			// width, height, 0, GL_BGRA,
			// GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
			assert (glGetError() == 0);
		}

		void declareNow() {
			glTextureRangeAPPLE(this.textureTarget, pixelBuffer);
		}

	}

	static public class Base4FloatTexture extends BaseTexture {
		protected FloatBuffer pixelBuffer;

		TextureRange in;

		int allocated = 0;

		int width;

		int height;

		int textureId = 0;

		boolean dirty = false;

		public Base4FloatTexture(FloatBuffer b, int width, int height) {
			super("");
			this.pixelBuffer = b;
			this.width = width;
			this.height = height;
		}

		public void dirty() {
			dirty = true;
		}

		public FloatBuffer getStorage() {
			return pixelBuffer;
		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}

		public Base4FloatTexture setTextureRange(TextureRange in) {
			this.in = in;

			return this;
		}

		@Override
		protected void post() {
			glDisable(this.textureTarget);
		}

		@Override
		protected void pre() {
			int textureId = BasicContextManager.getId(this);
			if (textureId == BasicContextManager.ID_NOT_FOUND) {
				setup();
				textureId = BasicContextManager.getId(this);
				assert textureId != BasicContextManager.ID_NOT_FOUND : "called setup() in texture, didn't get an ID has subclass forgotten to call BasicContextIDManager.pudId(...) ?";
			}
			assert (glGetError() == 0) : this.getClass().getName();
			glBindTexture(this.textureTarget, textureId);
			assert (glGetError() == 0) : this.getClass().getName() + " " + BasicContextManager.getCurrentContext();
			glEnable(this.textureTarget);
			assert (glGetError() == 0) : this.getClass().getName();
			if (in != null)
				in.declareNow(gl);

			assert glGetError() == 0;

			if (dirty) {
				glTexSubImage2D(this.textureTarget, 0, 0, 0, width, height, GL_RGBA, GL11.GL_FLOAT, pixelBuffer);
				// glTexSubImage2D(this.textureTarget, 0, 0,
				// 0, width, height, GL_BGRA,
				// GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
			}
			dirty = false;
		}

		@Override
		protected void setup() {
			int[] textures = new int[1];
			textureId = textures[0] = glGenTextures();
			BasicContextManager.putId(this, textureId);
			glBindTexture(this.textureTarget, textureId);

			if (in != null)
				in.declareNow(gl);

			// glTexParameteri(GL_TEXTURE_RECTANGLE,
			// GL_TEXTURE_STORAGE_HINT_APPLE,
			// GL_STORAGE_SHARED_APPLE);
			// glPixelStorei(GL_UNPACK_CLIENT_STORAGE_APPLE,
			// 1);
			// glTexParameteri(this.textureTarget,
			// GL_TEXTURE_MIN_FILTER, GL_NEAREST);
			glTexParameteri(this.textureTarget, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			glTexParameteri(this.textureTarget, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			glTexParameteri(this.textureTarget, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			glTexParameteri(this.textureTarget, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(this.textureTarget, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
			glTexImage2D(this.textureTarget, 0, GL_RGBA32F, width, height, 0, GL_RGBA, GL11.GL_FLOAT, pixelBuffer);
			glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);

			// glTexImage2D(this.textureTarget, 0, GL_RGBA,
			// width, height, 0, GL_BGRA,
			// GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
			assert (glGetError() == 0);
		}

		void declareNow() {
			// glTextureRangeAPPLE(this.textureTarget,
			// pixelBuffer.capacity() * 4, pixelBuffer);
		}

	}

	// this texture is dangerously direct,
	// for maximum shearing protection, use BufferedDynamic texture (which
	// will use this class) and render no more than one frame ahead
	// (typical)
	static public class BaseFastRawTexture extends BaseTexture {
		IntBuffer from;

		TextureRange in;

		int width;

		int height;

		int textureId = 0;

		boolean dirty = false;

		boolean deallocated = false;

		private final String name;

		public BaseFastRawTexture(String name, IntBuffer from, int width, int height, TextureRange in) {
			super(name);
			this.name = name;
			this.from = from;
			this.in = in;
			this.width = width;
			this.height = height;
			dirty = true;
			use_gl_texture_rectangle_ext(true);
		}

		protected BaseFastRawTexture(String name) {
			super(name);
			this.name = name;
		}

		public BaseFastRawTexture allwaysDirty() {
			allwaysDirty = true;
			return this;
		}

		boolean allwaysDirty = false;

		public void deallocate(TaskQueue atRenderTime) {
			atRenderTime.new Task() {
				@Override
				public void run() {
					if (!deallocated)
					// strange that this doesnt work?
					// if (BasicContextManager.getId(this)
					// != BasicContextManager.ID_NOT_FOUND
					// && BasicContextManager.isValid(this))
					{
						gl = BasicContextManager.getGl();
						glDeleteTextures(textureId);
						BasicContextManager.putId(this, BasicContextManager.ID_NOT_FOUND);
					}
					deallocated = true;
				}

			};
		}

		public void dirty() {
			dirty = true;
		}

		@Override
		public void post() {
			glBindTexture(textureTarget, 0);
			try {
				if (!CoreHelpers.isCore) glDisable(textureTarget);
			} catch (OpenGLException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void pre() {
			glBindTexture(textureTarget, textureId);

//			;//System.out.println(" -- binding to <" + textureId + "> <" + textureTarget + ">"+(GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE)-GL13.GL_TEXTURE0));

			try {

//				BasicGLSLangProgram.currentProgram.debugPrintUniforms();
				if (!CoreHelpers.isCore) glEnable(textureTarget);
				if (in != null)
					in.declareNow(gl);

				if (allwaysDirty)
					dirty = true;

				if (dirty)
					glTexSubImage2D(textureTarget, 0, 0, 0, width, height, GL12.GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, from);

				
				if (textureTarget == GL_TEXTURE_2D) {
					// glTexParameterf(GL_TEXTURE_2D,
					// GL_TEXTURE_MAX_ANISOTROPY_EXT, 16);
					if (!CoreHelpers.isCore)
					{
						glTexParameteri(GL_TEXTURE_2D, GL_GENERATE_MIPMAP, 1);
						glGenerateMipmap(GL_TEXTURE_2D);
					}
					else
						GL30.glGenerateMipmap(textureTarget);
				}
				
				// if (dirty)
				// glTexSubImage2D(textureTarget, 0, 0,
				// 0, width, height, GL_RGBA,
				// GL_UNSIGNED_INT_8_8_8_8, from);
				dirty = false;
			} catch (OpenGLException e) {

				e.printStackTrace();
			}
		}

		public void setGL(Object gl) {
			this.gl = gl;
		}

		@Override
		public void setup() {
			int[] textures = new int[1];

			try {
				textures[0] = glGenTextures();
				textureId = textures[0];
				BasicContextManager.putId(this, textureId);
				glBindTexture(textureTarget, textureId);
				if (in != null)
					in.declareNow(gl);

				if (Platform.getOS() == OS.mac)
					glTexParameteri(textureTarget, GL_TEXTURE_STORAGE_HINT_APPLE, GL_STORAGE_SHARED_APPLE);


				glTexParameteri(textureTarget, GL_TEXTURE_MIN_FILTER, textureTarget == GL_TEXTURE_2D ? GL_LINEAR_MIPMAP_LINEAR : GL_NEAREST);
				int best = Platform.getOS() == OS.mac ? GL_LINEAR : GL_LINEAR;

				glTexParameteri(textureTarget, GL_TEXTURE_MAG_FILTER, textureTarget == GL_TEXTURE_2D ? best : GL_NEAREST);


				glTexParameteri(textureTarget, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
				glTexParameteri(textureTarget, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

				glTexImage2D(textureTarget, 0, GL_RGBA, width, height, 0, GL12.GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, from);

				if (textureTarget == GL_TEXTURE_2D) {
//					GL11.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, 16);
					if (!CoreHelpers.isCore)
					{
						glTexParameteri(GL_TEXTURE_2D, GL_GENERATE_MIPMAP, 1);
//						glGenerateMipmap(GL_TEXTURE_2D);
					}
					else
						GL30.glGenerateMipmap(textureTarget);
				}


				glBindTexture(textureTarget, 0);
			} catch (OpenGLException e) {
				e.printStackTrace();

			}
		}
	}

	static public class BaseFastNoStorageTexture extends BaseTexture {
		int width;

		int height;

		int textureId = -1;

		boolean dirty = false;

		boolean deallocated = false;

		public BaseFastNoStorageTexture(int width, int height) {
			super("");
			this.width = width;
			this.height = height;
			textureTarget = GL_TEXTURE_2D;
		}

		public void deallocate(TaskQueue atRenderTime) {
			atRenderTime.new Task() {
				@Override
				public void run() {
					if (!deallocated) {
						gl = BasicContextManager.getGl();
						glDeleteTextures(textureId);
						BasicContextManager.putId(this, BasicContextManager.ID_NOT_FOUND);
					}
					deallocated = true;
				}

			};
		}

		public void dirty() {
			dirty = true;
		}

		@Override
		public void post() {
			glBindTexture(GL_TEXTURE_2D, 0);
			glDisable(GL_TEXTURE_2D);
		}

		@Override
		public void pre() {
			glBindTexture(GL_TEXTURE_2D, textureId);
			glEnable(GL_TEXTURE_2D);
			if (genMipsNext)
			{
				genMipsNext = false;
				glTexParameteri(this.textureTarget, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
				glTexParameteri(this.textureTarget, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
				glGenerateMipmap(GL_TEXTURE_2D);
			}

		}

		public boolean genMipsNext = false;
		
		@Override
		public void setup() {
			int[] textures = new int[1];

			textures[0] = glGenTextures();
			textureId = textures[0];
			BasicContextManager.putId(this, textureId);
			glBindTexture(GL_TEXTURE_2D, textureId);

			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_STORAGE_HINT_APPLE, GL_STORAGE_PRIVATE_APPLE);
			// glPixelStorei(GL_UNPACK_CLIENT_STORAGE_APPLE, 0);

			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
			assert (glGetError() == 0);

			glBindTexture(GL_TEXTURE_2D, 0);

		}

		public int getTextureID() {

			;//System.out.println(" returning texture id <" + textureId + ">");
			return textureId;
		}
	}

	static public class BaseFastSwitchedRawTexture extends BaseTexture {
		Buffer from;

		TextureRange in;

		int width;

		int height;

		int textureId = 0;

		boolean dirty = false;

		boolean deallocated = false;

		private final String name;

		public BaseFastSwitchedRawTexture(String name, int width, int height) {
			super(name);
			this.name = name;
			this.width = width;
			this.height = height;
			dirty = true;
		}

		public void deallocate(TaskQueue atRenderTime) {
			atRenderTime.new Task() {
				@Override
				public void run() {
					if (!deallocated)
					// strange that this doesnt work?
					// if (BasicContextManager.getId(this)
					// != BasicContextManager.ID_NOT_FOUND
					// && BasicContextManager.isValid(this))
					{
						gl = BasicContextManager.getGl();
						glDeleteTextures(textureId);
						BasicContextManager.putId(this, BasicContextManager.ID_NOT_FOUND);
					}
					deallocated = true;
				}

			};
		}

		public void dirty() {
			dirty = true;
		}

		@Override
		public void post() {
			if (unset)
				return;

			glBindTexture(GL_TEXTURE_RECTANGLE, 0);
			glDisable(GL_TEXTURE_RECTANGLE);
		}

		boolean unset = true;

		public void setFrame(Buffer b) {
			from = b;
			dirty = true;
		}

		@Override
		public void pre() {
			if (BasicContextManager.getId(this) == BasicContextManager.ID_NOT_FOUND)
				setup();
			if (unset)
				return;

			glBindTexture(GL_TEXTURE_RECTANGLE, textureId);
			glEnable(GL_TEXTURE_RECTANGLE);
			if (in != null)
				in.declareNow(gl);

			if (dirty && from != null) {
				if (from instanceof ByteBuffer)
					glTexSubImage2D(GL_TEXTURE_RECTANGLE, 0, 0, 0, width, height, GL12.GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, (ByteBuffer) from);
				if (from instanceof IntBuffer)
					glTexSubImage2D(GL_TEXTURE_RECTANGLE, 0, 0, 0, width, height, GL12.GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, (IntBuffer) from);
			}

			// if (dirty)
			// glTexSubImage2D(GL_TEXTURE_RECTANGLE, 0, 0,
			// 0, width, height, GL_RGBA,
			// GL_UNSIGNED_INT_8_8_8_8, from);
			dirty = false;
		}

		public void setGL(Object gl) {
			this.gl = gl;
		}

		@Override
		public void setup() {

			if (from == null) {
				unset = true;
				return;
			}
			unset = false;

			int[] textures = new int[1];

			;//System.out.println(" setting up ");

			textures[0] = glGenTextures();
			textureId = textures[0];
			BasicContextManager.putId(this, textureId);
			glBindTexture(GL_TEXTURE_RECTANGLE, textureId);
			if (in != null)
				in.declareNow(gl);

			if (Platform.isMac())
				glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_STORAGE_HINT_APPLE, GL_STORAGE_SHARED_APPLE);
			// glPixelStorei(GL_UNPACK_CLIENT_STORAGE_APPLE, 1);

			glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
			if (from instanceof ByteBuffer)
				glTexImage2D(GL_TEXTURE_RECTANGLE, 0, GL_RGBA, width, height, 0, GL12.GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, (ByteBuffer) from);
			else
				glTexImage2D(GL_TEXTURE_RECTANGLE, 0, GL_RGBA, width, height, 0, GL12.GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, (IntBuffer) from);
			// glTexImage2D(GL_TEXTURE_RECTANGLE, 0,
			// GL_RGBA, width, height, 0, GL_RGBA,
			// GL_UNSIGNED_INT_8_8_8_8, from);
			assert (glGetError() == 0);

			glBindTexture(GL_TEXTURE_RECTANGLE, 0);

		}
	}

	static public class BaseFastRaw422Texture extends BaseTexture {
		IntBuffer from;

		TextureRange in;

		int width;

		int height;

		int textureId = 0;

		boolean dirty = false;

		boolean deallocated = false;

		public BaseFastRaw422Texture(String name, IntBuffer from, int width, int height, TextureRange in) {
			super(name);
			this.from = from;
			this.in = in;
			this.width = width;
			this.height = height;
			dirty = true;
		}

		protected BaseFastRaw422Texture(String name) {
			super(name);
		}

		public BaseFastRaw422Texture allwaysDirty() {
			allwaysDirty = true;
			return this;
		}

		boolean allwaysDirty = false;

		public void deallocate(TaskQueue atRenderTime) {
			atRenderTime.new Task() {
				@Override
				public void run() {
					if (!deallocated)
					// strange that this doesnt work?
					// if (BasicContextManager.getId(this)
					// != BasicContextManager.ID_NOT_FOUND
					// && BasicContextManager.isValid(this))
					{
						gl = BasicContextManager.getGl();
						glDeleteTextures(textureId);
						BasicContextManager.putId(this, BasicContextManager.ID_NOT_FOUND);
					}
					deallocated = true;
				}

			};
		}

		public void dirty() {
			dirty = true;
		}

		@Override
		public void post() {
			glBindTexture(GL_TEXTURE_RECTANGLE, 0);
			glDisable(GL_TEXTURE_RECTANGLE);
		}

		@Override
		public void pre() {
			glBindTexture(GL_TEXTURE_RECTANGLE, textureId);
			glEnable(GL_TEXTURE_RECTANGLE);
			if (in != null)
				in.declareNow(gl);

			if (allwaysDirty)
				dirty = true;

			glTexParameteri(GL_TEXTURE_RECTANGLE, GL_GENERATE_MIPMAP, 1);
			if (dirty)
				glTexSubImage2D(GL_TEXTURE_RECTANGLE, 0, 0, 0, width, height, APPLEYcbcr422.GL_YCBCR_422_APPLE, APPLEYcbcr422.GL_UNSIGNED_SHORT_8_8_APPLE, from);
			// if (dirty)
			// glTexSubImage2D(GL_TEXTURE_RECTANGLE, 0, 0,
			// 0, width, height, GL_RGBA,
			// GL_UNSIGNED_INT_8_8_8_8, from);
			dirty = false;
		}

		public void setGL(Object gl) {
			this.gl = gl;
		}

		@Override
		public void setup() {
			int[] textures = new int[1];
			textures[0] = glGenTextures();
			textureId = textures[0];
			BasicContextManager.putId(this, textureId);
			glBindTexture(GL_TEXTURE_RECTANGLE, textureId);
			if (in != null)
				in.declareNow(gl);

			if (Platform.isMac())
				glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_STORAGE_HINT_APPLE, GL_STORAGE_SHARED_APPLE);
			// glPixelStorei(GL_UNPACK_CLIENT_STORAGE_APPLE, 1);

			glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
			glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_MAG_FILTER, GL_LINEAR_MIPMAP_LINEAR);
			glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
			// glTexImage2D(GL_TEXTURE_RECTANGLE, 0,
			// GL_RGBA, width, height, 0, GL_BGRA,
			// GL_UNSIGNED_INT_8_8_8_8_REV, from);
			glTexImage2D(GL_TEXTURE_RECTANGLE, 0, GL_RGBA, width, height, 0, APPLEYcbcr422.GL_YCBCR_422_APPLE, APPLEYcbcr422.GL_UNSIGNED_SHORT_8_8_APPLE, from);

			// glTexImage2D(GL_TEXTURE_RECTANGLE, 0,
			// GL_RGBA, width, height, 0, GL_RGBA,
			// GL_UNSIGNED_INT_8_8_8_8, from);
			assert (glGetError() == 0);

			glBindTexture(GL_TEXTURE_RECTANGLE, 0);

		}
	}

	/**
	 * @author marc
	 */

	static public class BaseFloatTexture extends BaseTexture {
		protected FloatBuffer pixelBuffer;

		TextureRange in;

		int allocated = 0;

		int width;

		int height;

		int textureId = 0;

		boolean dirty = false;

		public BaseFloatTexture(FloatBuffer b, int width, int height) {
			super("");
			this.pixelBuffer = b;
			this.width = width;
			this.height = height;
		}

		public FloatBuffer getStorage() {
			return pixelBuffer;
		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}

		public void dirty() {
			dirty = true;
		}

		public BaseFloatTexture setTextureRange(TextureRange in) {
			this.in = in;

			return this;
		}

		@Override
		protected void post() {
			if (!CoreHelpers.isCore) glDisable(textureTarget);
		}

		@Override
		protected void pre() {
			int textureId = BasicContextManager.getId(this);
			if (textureId == BasicContextManager.ID_NOT_FOUND) {
				setup();
				textureId = BasicContextManager.getId(this);
				assert textureId != BasicContextManager.ID_NOT_FOUND : "called setup() in texture, didn't get an ID has subclass forgotten to call BasicContextIDManager.pudId(...) ?";
			}
			assert (glGetError() == 0) : this.getClass().getName();
			
			
			glBindTexture(textureTarget, textureId);
			assert (glGetError() == 0) : this.getClass().getName() + " " + BasicContextManager.getCurrentContext();
			if (!CoreHelpers.isCore) glEnable(textureTarget);
			assert (glGetError() == 0) : this.getClass().getName();
			if (in != null)
				in.declareNow(gl);

			assert glGetError() == 0;

			if (dirty) {
				glTexSubImage2D(textureTarget, 0, 0, 0, width, height, GL11.GL_RED, GL11.GL_FLOAT, pixelBuffer);
				// glTexSubImage2D(GL_TEXTURE_RECTANGLE,
				// 0, 0, 0, width, height, GL_BGRA,
				// GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
			}
			dirty = false;
		}

		@Override
		protected void setup() {
			int[] textures = new int[1];
			textures[0] = glGenTextures();
			textureId = textures[0];
			BasicContextManager.putId(this, textureTarget);
			glBindTexture(textureTarget, textureId);

			if (in != null)
				in.declareNow(gl);

			// glTexParameteri(GL_TEXTURE_RECTANGLE,
			// GL_TEXTURE_STORAGE_HINT_APPLE,
			// GL_STORAGE_SHARED_APPLE);
			// glPixelStorei(GL_UNPACK_CLIENT_STORAGE_APPLE,
			// 1);
			glTexParameteri(textureTarget, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
			glTexParameteri(textureTarget, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			glTexParameteri(textureTarget, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			glTexParameteri(textureTarget, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			glTexParameteri(textureTarget, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(textureTarget, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
			glTexImage2D(textureTarget, 0, GL_R32F, width, height, 0, GL11.GL_RED, GL11.GL_FLOAT, pixelBuffer);
			glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);

			// glTexImage2D(GL_TEXTURE_RECTANGLE, 0,
			// GL_RGBA, width, height, 0, GL_BGRA,
			// GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
			assert (glGetError() == 0);
		}

		void declareNow() {
			// glTextureRangeAPPLE(GL_TEXTURE_RECTANGLE,
			// pixelBuffer.capacity() * 4, pixelBuffer);
		}

	}

	/**
	 * 2d, with mip map generation
	 * 
	 * @author marc
	 * 
	 */
	static public class BaseSlowRawTexture extends BaseTexture {
		IntBuffer from;

		TextureRange in;

		int width;

		int height;

		public boolean genMip = true;

		int textureId = 0;

		boolean dirty = false;

		boolean deallocated = false;

		public BaseSlowRawTexture(String name, IntBuffer from, int width, int height, TextureRange in) {
			super(name);
			this.from = from;
			this.in = in;
			this.width = width;
			this.height = height;
			dirty = true;
			use_gl_texture_rectangle_ext(false);
		}

		protected BaseSlowRawTexture(String name) {
			super(name);
		}

		public void deallocate(TaskQueue atRenderTime) {
			atRenderTime.new Task() {

				@Override
				public void run() {
					if (BasicContextManager.getId(this) != BasicContextManager.ID_NOT_FOUND && BasicContextManager.isValid(this)) {
						gl = BasicContextManager.getGl();
						glDeleteTextures(textureId);
						BasicContextManager.putId(this, BasicContextManager.ID_NOT_FOUND);
					}
					deallocated = true;

				}

			};
		}

		public void dirty() {
			dirty = true;
		}

		@Override
		public void performPass(iPass p) {
			super.performPass(p);
		}

		@Override
		public void post() {
			glBindTexture(this.textureTarget, 0);
			glDisable(this.textureTarget);
		}

		@Override
		public void pre() {
			assert !deallocated;
			assert glGetError() == 0;
			glBindTexture(this.textureTarget, textureId);
			assert glGetError() == 0;
			glEnable(this.textureTarget);
			assert glGetError() == 0;
			if (in != null)
				in.declareNow(gl);

			if (dirty) {
				if (genMip)
					glTexParameteri(this.textureTarget, GL_GENERATE_MIPMAP, 1);

				glTexSubImage2D(this.textureTarget, 0, 0, 0, width, height, GL12.GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, from);
			}
			dirty = false;
			assert glGetError() == 0;
		}

		public BaseSlowRawTexture setGenMip(boolean genMip) {
			this.genMip = genMip;
			return this;
		}

		public void setGL(Object gl) {
			this.gl = gl;
		}

		@Override
		public void setup() {
			assert glGetError() == 0;
			int[] textures = new int[1];
			textures[0] = glGenTextures();
			textureId = textures[0];
			BasicContextManager.putId(this, textureId);
			glBindTexture(this.textureTarget, textureId);

			assert glGetError() == 0;

			if (in != null)
				in.declareNow(gl);

			assert glGetError() == 0;
			if (Platform.isMac())
				glTexParameteri(this.textureTarget, GL_TEXTURE_STORAGE_HINT_APPLE, GL_STORAGE_SHARED_APPLE);
			// glPixelStorei(GL_UNPACK_CLIENT_STORAGE_APPLE, 1);
			assert glGetError() == 0;
			if (genMip) {
				glTexParameteri(this.textureTarget, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
			} else {
				glTexParameteri(this.textureTarget, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			}
			glTexParameteri(this.textureTarget, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			glTexParameteri(this.textureTarget, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(this.textureTarget, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
			glTexImage2D(this.textureTarget, 0, GL_RGBA, width, height, 0, GL12.GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, from);
			assert glGetError() == 0;
		}
	}

	static public class BufferedAnimationTexture extends BaseTexture {
		IntBuffer buffer;

		IntBuffer[] animationData;

		BaseFastRawTexture text;

		int count = 0;

		public BufferedAnimationTexture(String name, int width, int height, TextureRange in, IntBuffer[] animationData) {
			buffer = in.allocate(width * height);
			text = new BaseFastRawTexture(name, buffer, width, height, in);
			this.animationData = animationData;
		}

		public void advance() {
			int c = count;
			if (c >= animationData.length)
				c = 2 * animationData.length - 1 - c;
			IntBuffer data = animationData[c];

			data.rewind();

			// perform copySource
			buffer.rewind();
			buffer.put(data);
			text.dirty();
			count = (count + 1) % (animationData.length * 2);
		}

		@Override
		protected void post() {
			text.post();
		}

		@Override
		protected void pre() {
			text.pre();
		}

		@Override
		protected void setup() {
			text.setup();
		}
	}

	static public class BufferedDynamicTexture extends BaseTexture {
		IntBuffer withGL;

		IntBuffer withApp;

		BaseFastRawTexture texture1;

		BaseFastRawTexture texture2;

		TextureRange in;

		int width;

		int height;

		int textureId = 0;

		public BufferedDynamicTexture(String name, int width, int height, TextureRange in) {
			super(name);
			this.withApp = in.allocate(width * height);
			this.withGL = in.allocate(width * height);
			this.in = in;
			this.width = width;
			this.height = height;
			texture1 = new BaseFastRawTexture(name + ":1", withGL, width, height, in);
			texture2 = new BaseFastRawTexture(name + ":2", withApp, width, height, in);
		}

		public IntBuffer getGLBuffer() {
			return withGL;
		}

		public IntBuffer getWriteBuffer() {
			return withApp;
		}

		public void swap() {
			// swap pointers

			BaseFastRawTexture tmp = texture1;
			texture1 = texture2;
			texture2 = tmp;

			IntBuffer tmp2 = withGL;
			withGL = withApp;
			withApp = tmp2;
		}

		@Override
		protected void post() {
			texture1.post();
		}

		@Override
		protected void pre() {
			texture1.pre();
		}

		@Override
		protected void setup() {
			texture1.setup();
			texture2.setup();
		}
	}

	//
	// static public class DVTexture extends TextureFromQTImage {
	// public DVTexture() {
	// super(new QTImage().becomeDV(720, 480));
	// }
	//
	// public DVTexture(int width, int height) {
	// super(new QTImage().becomeDV(width, height));
	// }
	//
	// @Override
	// public QTImage getImage() {
	// return this.image;
	// }
	//
	// /** @return */
	// public DVTexture set2d() {
	// textureTarget = GL_TEXTURE_2D;
	// ;
	// return this;
	// }
	//
	// @Override
	// protected void pre() {
	// assert glGetError() == 0 : this.getClass();
	// super.pre();
	// assert glGetError() == 0 : this.getClass();
	// this.image.updateDV();
	// assert glGetError() == 0 : this.getClass();
	// refresh();
	// assert glGetError() == 0 : this.getClass();
	// }
	//
	// @Override
	// protected void setup() {
	// super.setup();
	// assert glGetError() == 0 : this.getClass();
	// glTexParameteri(this.textureTarget,
	// GL_TEXTURE_STORAGE_HINT_APPLE, GL_STORAGE_SHARED_APPLE);
	// assert glGetError() == 0 : this.getClass();
	// }
	// }
	//
	//

	static public class LuminanceTexture extends BaseTexture {
		ByteBuffer pixelBuffer;

		int allocated = 0;

		int width;

		int height;

		int textureId = 0;

		boolean dirty = false;

		public LuminanceTexture(ByteBuffer b, int width, int height) {
			super("");
			this.pixelBuffer = b;
			this.width = width;
			this.height = height;
		}

		public void dirty() {
			dirty = true;
		}

		@Override
		protected void post() {
			glDisable(GL_TEXTURE_RECTANGLE);
		}

		@Override
		protected void pre() {
			glBindTexture(GL_TEXTURE_RECTANGLE, textureId);
			glEnable(GL_TEXTURE_RECTANGLE);

			if (dirty) {
				glTexSubImage2D(GL_TEXTURE_RECTANGLE, 0, 0, 0, width, height, GL_LUMINANCE, GL_UNSIGNED_BYTE, pixelBuffer);
				// glTexSubImage2D(GL_TEXTURE_RECTANGLE,
				// 0, 0, 0, width, height, GL_BGRA,
				// GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
				assert (glGetError() == 0);
			}
			dirty = false;
		}

		@Override
		protected void setup() {
			int[] textures = new int[1];
			textures[0] = glGenTextures();
			textureId = textures[0];
			BasicContextManager.putId(this, textureId);
			glBindTexture(GL_TEXTURE_RECTANGLE, textureId);

			// glTexParameteri(GL_TEXTURE_RECTANGLE,
			// GL_TEXTURE_STORAGE_HINT_APPLE,
			// GL_STORAGE_SHARED_APPLE);
			// glPixelStorei(GL_UNPACK_CLIENT_STORAGE_APPLE,
			// 1);
			glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
			glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
			glTexImage2D(GL_TEXTURE_RECTANGLE, 0, GL_INTENSITY, width, height, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, pixelBuffer);
			// glTexImage2D(GL_TEXTURE_RECTANGLE, 0,
			// GL_RGBA, width, height, 0, GL_BGRA,
			// GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
			assert (glGetError() == 0);
		}

	}

	static public class OneDTexture extends BaseTexture {
		private final int width;

		private int textureId;

		ByteBuffer buffer;

		boolean dirty = false;

		public OneDTexture(ByteBuffer buffer, int width) {
			super("");
			this.width = width;
			this.buffer = buffer;
		}

		public void delete() {
			if (gl != null) {
				glDeleteTextures(textureId);
			}
		}

		public void dirty() {
			dirty = true;
		}

		@Override
		protected void post() {
			glDisable(GL_TEXTURE_1D);
		}

		/** @see field.graphics.core.BasicTextures.BaseTexture#pre() */
		@Override
		protected void pre() {
			int textureId = BasicContextManager.getId(this);
			if (textureId == BasicContextManager.ID_NOT_FOUND) {
				assert glGetError() == 0 : buffer;
				setup();
				assert glGetError() == 0 : buffer;
				textureId = BasicContextManager.getId(this);
				assert textureId != BasicContextManager.ID_NOT_FOUND : "called setup() in texture, didn't get an ID has subclass forgotten to call BasicContextIDManager.pudId(...) ?";
			}
			if (dirty) {
				assert glGetError() == 0 : buffer;
				glBindTexture(GL_TEXTURE_1D, textureId);
				glTexSubImage1D(GL_TEXTURE_1D, 0, 0, width, GL12.GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, buffer);
				assert glGetError() == 0 : buffer;
			}
			dirty = false;
			assert glGetError() == 0 : buffer;
			if (BaseTexture.enableTextures) {
				assert glGetError() == 0 : buffer;
				glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, gl_texture_env_mode);
				glBindTexture(GL_TEXTURE_1D, textureId);
				glEnable(GL_TEXTURE_1D);
				assert glGetError() == 0 : buffer;
			}
			assert glGetError() == 0 : buffer;
		}

		/** @see field.graphics.core.BasicTextures.BaseTexture#setup() */
		@Override
		protected void setup() {
			int[] textures = new int[1];
			textureId = textures[0] = glGenTextures();
			BasicContextManager.putId(this, textureId);
			glBindTexture(GL_TEXTURE_1D, textureId);
			assert (glGetError() == 0);

			if (Platform.isMac())
				glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_STORAGE_HINT_APPLE, GL_STORAGE_SHARED_APPLE);
			// glPixelStorei(GL_UNPACK_CLIENT_STORAGE_APPLE, 1);
			glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

			glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_WRAP_S, this.gl_texture_wrap_s);
			glTexParameteri(GL_TEXTURE_1D, GL_TEXTURE_WRAP_T, this.gl_texture_wrap_t);
			assert (glGetError() == 0);
			glTexImage1D(GL_TEXTURE_1D, 0, GL_RGBA, width, 0, GL12.GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, buffer);
			assert (glGetError() == 0);

			dirty = false;
		}
	}

	// hacking around
	// static public class PBufferSelfBuffer extends BaseTexture {
	//
	// private final FullScreenCanvas full;
	//
	// public PBufferSelfBuffer(FullScreenCanvas full) {
	// this.full = full;
	// }
	//
	// @Override
	// protected void post() {
	// }
	//
	// @Override
	// protected void pre() {
	// }
	//
	// @Override
	// protected void setup() {
	// long b = CcreatePBuffer(GL_TEXTURE_RECTANGLE_ARB, GL_RGBA, 1280,
	// 1024);
	//
	// GLContext context = full.getCanvas().getContext();
	// long contextPointer = (Long)
	// ReflectionTools.illegalGetObject(context, "nsContext");
	// CsetContextPBuffer(contextPointer, b);
	// }
	// }

	static public class SelfTexture extends BaseTexture {
		private final int width;

		private final int height;

		private int textureId;

		boolean dirty = false;

		public SelfTexture(int width, int height) {
			this.width = width;
			this.height = height;
		}

		public void dirty() {
			dirty = true;
		}

		public void updateNow() {
			glBindTexture(GL_TEXTURE_RECTANGLE, textureId);
			glCopyTexSubImage2D(GL_TEXTURE_RECTANGLE, 0, 0, 0, 0, 0, width, height);
			dirty = false;
		}

		@Override
		protected void post() {
			glDisable(GL_TEXTURE_RECTANGLE);
			if (dirty) {
				glBindTexture(GL_TEXTURE_RECTANGLE, textureId);
				glCopyTexSubImage2D(GL_TEXTURE_RECTANGLE, 0, 0, 0, 0, 0, width, height);
				// glCopyTexImage2D(GL_TEXTURE_RECTANGLE,
				// 0, 0,0, 0,0, width, height);
				dirty = false;
			}
		}

		@Override
		protected void pre() {
			glEnable(GL_TEXTURE_RECTANGLE);
			glBindTexture(GL_TEXTURE_RECTANGLE, textureId);
		}

		@Override
		protected void setup() {
			int[] textures = new int[1];
			textures[0] = glGenTextures();
			textureId = textures[0];
			BasicContextManager.putId(this, textureId);
			glBindTexture(GL_TEXTURE_RECTANGLE, textureId);
			glTexImage2D(GL_TEXTURE_RECTANGLE, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_INT_8_8_8_8, (ByteBuffer) null);
			// glPixelStorei(GL_UNPACK_CLIENT_STORAGE_APPLE,
			// 1);
			glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_STORAGE_HINT_APPLE, GL_STORAGE_CACHED_APPLE);
			glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_WRAP_S, GL_CLAMP);
			glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_WRAP_T, GL_CLAMP);
			glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_RECTANGLE, GL_TEXTURE_MIN_FILTER, GL_NEAREST);

		}
	}

	static public class Stack3dTexture extends BasicUtilities.TwoPassElement {
		private final int width;

		private final QTImage[] images;

		private final ByteBuffer buffer;

		int[] tex = { -1 };

		public Stack3dTexture(int width, String[] filename) {
			super("", StandardPass.preRender, StandardPass.postRender);
			this.width = width;
			images = new QTImage[filename.length];
			buffer = ByteBuffer.allocateDirect(width * width * filename.length * 4);
			for (int i = 0; i < images.length; i++) {
				images[i] = new QTImage();
				images[i].loadTexture(filename[i]);

				assert width == images[i].pixelsWide() && width == images[i].pixelsHigh() : width + " " + images[i].pixelsWide() + " " + images[i].pixelsHigh();

				buffer.put(images[i].getImage());
			}
			buffer.rewind();
		}

		@Override
		protected void post() {
			glBindTexture(GL_TEXTURE_3D, 0);
			glDisable(GL_TEXTURE_3D);
		}

		@Override
		protected void pre() {
			glBindTexture(GL_TEXTURE_3D, tex[0]);
			glEnable(GL_TEXTURE_3D);
		}

		@Override
		protected void setup() {
			assert (glGetError() == 0) : this.getClass().getName();

			tex[0] = glGenTextures();
			BasicContextManager.putId(this, tex[0]);
			assert (glGetError() == 0) : this.getClass().getName();

			glBindTexture(GL_TEXTURE_3D, tex[0]);
			glTexImage3D(GL_TEXTURE_3D, 0, GL_RGBA, width, width, images.length, 0, GL_RGBA, GL_UNSIGNED_INT_8_8_8_8, buffer);
			assert (glGetError() == 0) : this.getClass().getName();
			glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_STORAGE_HINT_APPLE, GL_STORAGE_CACHED_APPLE);
			glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_S, GL_REPEAT);
			glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_T, GL_REPEAT);
			glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_WRAP_R, GL_REPEAT);
			glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
			glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			assert (glGetError() == 0) : this.getClass().getName();
			glBindTexture(GL_TEXTURE_3D, 0);
			assert (glGetError() == 0) : this.getClass().getName();
		}

	}

	// VERTEX_PROGRAM_POINT_SIZE_ARB

	/**
	 * this is storage for all of your FastRawTextures
	 * 
	 * @author marc
	 */
	static public class TextureRange {
		IntBuffer pixelBuffer;

		ByteBuffer bpixelBuffer;

		int allocated = 0;

		public TextureRange(int totalPixels) {
			pixelBuffer = (bpixelBuffer = ByteBuffer.allocateDirect(totalPixels * 4)).order(ByteOrder.nativeOrder()).asIntBuffer();
		}

		/**
		 * retuns null if no size left;
		 * 
		 * @param size
		 * @return IntBuffer
		 */
		public IntBuffer allocate(int size) {
			if (pixelBuffer.capacity() < allocated + size)
				return null;
			IntBuffer i = (((IntBuffer) pixelBuffer.position(allocated)).slice());
			i.limit(size);
			allocated += size;
			return i;
		}

		public ByteBuffer allocateBytes(int sizeInPixels) {
			if (pixelBuffer.capacity() < allocated + sizeInPixels)
				return null;
			ByteBuffer i = (((ByteBuffer) bpixelBuffer.position(allocated * 4)).slice());
			i.limit(sizeInPixels * 4);
			allocated += sizeInPixels;
			return i;
		}

		public void declareNow(Object gl) {

			glTextureRangeAPPLE(GL_TEXTURE_RECTANGLE, bpixelBuffer);
		}
	}
}
