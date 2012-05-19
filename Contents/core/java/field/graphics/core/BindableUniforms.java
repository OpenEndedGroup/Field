package field.graphics.core;

import static org.lwjgl.opengl.EXTBindableUniform.glUniformBufferEXT;
import static org.lwjgl.opengl.GL15.GL_STATIC_READ;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL31.GL_UNIFORM_BUFFER;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class BindableUniforms {

	static public class UniformBufferVec4 {
		private final String name;
		private final int length;
		private ByteBuffer storageB;
		private FloatBuffer storage;
		private int buffer;

		public boolean dirty = true;

		public UniformBufferVec4(String name, int length) {
			this.name = name;
			this.length = length;

			storageB = ByteBuffer.allocateDirect(4 * 4 * length);
			storage = storageB.order(ByteOrder.nativeOrder()).asFloatBuffer();

			buffer = glGenBuffers();

			dirty = true;
		}

		public FloatBuffer data() {
			dirty = true;
			return storage;
		}

		public void bindNow(BasicGLSLangProgram inside, UniformCache c) {
			// int location =
			// glGetUniformLocation(inside.getProgram(), name);

			int location = c.find(null, inside.getProgram(), name);
			if (c.lastWasNew) {
				glBindBuffer(GL_UNIFORM_BUFFER, buffer);
				storage.clear();
				glBufferData(GL_UNIFORM_BUFFER, storage, GL_STATIC_READ);
				glUniformBufferEXT(inside.getProgram(), location, buffer);
			} else if (dirty) {
				storage.clear();
				glBufferData(GL_UNIFORM_BUFFER, storage, GL_STATIC_READ);
			}
		}
	}

}
