package field.graphics.core;

import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL15.glUnmapBuffer;
import static org.lwjgl.opengl.GL30.GL_MAP_INVALIDATE_BUFFER_BIT;
import static org.lwjgl.opengl.GL30.GL_MAP_UNSYNCHRONIZED_BIT;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL30.glMapBufferRange;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.ARBShaderAtomicCounters;

import field.graphics.core.Base.StandardPass;
import field.graphics.core.BasicUtilities.TwoPassElement;

public class BasicAtomicCounters extends TwoPassElement {

	private int num;
	private int buffer;
	private ByteBuffer mapped;
	int[] current;
	private final int id;
	
	public BasicAtomicCounters(int id, StandardPass prePass, StandardPass postPass,
			int num) {
		super(""+id, prePass, postPass);
		this.id = id;
		this.num = num;
		current = new int[num];
	}

	@Override
	protected void post() {
		glBindBuffer(ARBShaderAtomicCounters.GL_ATOMIC_COUNTER_BUFFER, buffer);
		mapped = glMapBufferRange(
				ARBShaderAtomicCounters.GL_ATOMIC_COUNTER_BUFFER, 0, num * 4,
				GL_MAP_WRITE_BIT | GL_MAP_READ_BIT, mapped);
		mapped.asIntBuffer().get(current);
		for(int i=0;i<mapped.capacity();i++)
			mapped.put((byte)0);
		mapped.rewind();
		glUnmapBuffer(ARBShaderAtomicCounters.GL_ATOMIC_COUNTER_BUFFER);
		glBindBuffer(ARBShaderAtomicCounters.GL_ATOMIC_COUNTER_BUFFER, 0);
	}

	@Override
	protected void pre() {
		glBindBufferBase(ARBShaderAtomicCounters.GL_ATOMIC_COUNTER_BUFFER, id,
			buffer);
	}

	@Override
	protected void setup() {
		buffer = glGenBuffers();
		glBindBuffer(ARBShaderAtomicCounters.GL_ATOMIC_COUNTER_BUFFER, buffer);
		glBufferData(ARBShaderAtomicCounters.GL_ATOMIC_COUNTER_BUFFER, num * 4,
				GL_DYNAMIC_DRAW);
		glBindBuffer(ARBShaderAtomicCounters.GL_ATOMIC_COUNTER_BUFFER, 0);
		BasicContextManager.putId(this, buffer);
	}

}
