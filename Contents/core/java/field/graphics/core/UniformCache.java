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
import static org.lwjgl.opengl.ARBShaderObjects.*;

import java.util.HashMap;

import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL40;

public class UniformCache {

	public HashMap<String, Integer> id = new HashMap<String, Integer>();
	public HashMap<String, Object> value = new HashMap<String, Object>();

	boolean lastWasNew = false;

	public int find(Object gl, int program, String name) {
		name = BasicGLSLangProgram.demungeArrayName(name);
		lastWasNew = false;

		Integer n = id.get(name);
		if (n != null /* && n>-1 */)
			return n;

		n = glGetUniformLocation(program, name);
		id.put(name, n);

		lastWasNew = true;

		return n;
	}

	public void set(String name, Object v) {
		name = BasicGLSLangProgram.demungeArrayName(name);
		value.put(name, v);
	}

	public Object get(String name) {
		name = BasicGLSLangProgram.demungeArrayName(name);
		return value.get(name);
	}

	public void clear() {
		id.clear();
	}

}
