package field.graphics.core;

import static org.lwjgl.opengl.GL11.GL_MODELVIEW_MATRIX;
import static org.lwjgl.opengl.GL11.GL_PROJECTION_MATRIX;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_TRIANGLE_STRIP;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL11.glGetFloat;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_VERTEX_PROGRAM_POINT_SIZE;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glBindAttribLocation;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glGetAttribLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform2f;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glUniform4f;
import static org.lwjgl.opengl.GL20.glUniformMatrix4;
import static org.lwjgl.opengl.GL20.glValidateProgram;
import static org.lwjgl.opengl.GL32.GL_GEOMETRY_SHADER;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GL43;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.InheritWeave;
import field.core.plugins.drawing.opengl.OnCanvasLines;
import field.graphics.core.Base.StandardPass;
import field.graphics.core.Base.iSceneListElement;
import field.graphics.core.ShaderPreprocessor.PreprocessorException;
import field.graphics.windowing.FullScreenCanvasSWT;
import field.math.abstraction.iProvider;
import field.math.linalg.Matrix4;
import field.math.linalg.iToFloatArray;
import field.util.TaskQueue;
import field.util.TaskQueue.Task;

@Woven
public class BasicGLSLangProgram extends BasicUtilities.OnePassListElement implements iSceneListElement {

	public interface iErrorHandler {
		public void beginError();

		public void errorOnLine(int line, String error);

		public void endError();

		public void noError();
	}

	public class BasicGLSLangElement {

		public final ElementType isFragment;

		private String[] code;

		private int shader;

		private File[] originalFiles = {};

		public BasicGLSLangElement(File code, ElementType isFragment) {
			this(new String[] { readFile(code) }, isFragment);
			originalFiles = new File[] { code };
		}

		public BasicGLSLangElement(File[] code, ElementType isFragment) {
			this(readFiles(code), isFragment);
			originalFiles = code;
		}

		public BasicGLSLangElement(String code, ElementType isFragment) {
			this(new String[] { code }, isFragment);
		}

		public BasicGLSLangElement(String[] code, ElementType isFragment) {
			this.code = code;
			this.isFragment = isFragment;
			programs.add(this);
		}

		public File[] getFiles() {
			return originalFiles;
		}

		protected void delete() {
			GL20.glDeleteShader(shader);
		}

		protected void reload() {
			if (originalFiles != null) {
				code = readFiles(originalFiles);
				;// System.out.println(" code is <" + code +
					// ">");
			}
		}

		public void reload(String code) {
			this.code[0] = code;
		}

		iErrorHandler onError;

		public void reload(String code, iErrorHandler handler) {
			this.code[0] = code;
			onError = handler;
		}

		protected void setup() {
			if (code == null || code.length == 0)
				return;
			boolean found = false;
			for (String s : code) {
				if (s == null)
					return;
				if (s.trim().length() > 0)
					found = true;
			}
			if (!found)
				return;

			;// System.out.println(" compiling <" + isFragment +
				// "> <" + code[0] + "> from <" +
				// (originalFiles.length > 0 ? originalFiles[0]
				// : "") + ">");

			shader = GL20.glCreateShader(isFragment.gl);
			glShaderSource(shader, code);
			glCompileShader(shader);

			int didCompile = GL20.glGetShader(shader, GL20.GL_COMPILE_STATUS);

			;// System.out.println(" didCompile is ;" + didCompile);

			if (didCompile == 0) {
				String ret = GL20.glGetShaderInfoLog(shader, 10000);
				System.err.println(isFragment + " program failed to compile");
				System.err.println(" log is <" + ret + ">");
				System.err.println(" shader source is <" + code[0] + ">");
				if (onError != null) {
					onError.beginError();
					String log = ret;
					String[] lines = log.split("\n");
					for (String ll : lines) {
						try {
							String[] ss = ll.split(":");
							if (ss.length > 2) {
								int ii = Integer.parseInt(ss[2]);
								onError.errorOnLine(ii, ll);
							}
						} catch (NumberFormatException e) {
							try {
								Matcher q = Pattern.compile(".*?\\((.*?)\\)").matcher(ll);
								q.find();
								String g = q.group(1);
								int ii = Integer.parseInt(g);
								onError.errorOnLine(ii, ll);
							} catch (Exception e2) {
								e2.printStackTrace();
							}
						}
					}
					onError.endError();
				}
				// throw new IllegalStateException();
			} else {
				if (onError != null) {
					onError.noError();
				}
				GL20.glAttachShader(getProgram(), shader);
			}

		}

		public String getCode() {
			return code[0];
		}
	}

	public class CachedAttrib {
		public String name;

		int id = -1;

		public int getID() {
			return id != -1 ? id : (id = glGetAttribLocation(getProgram(), name));
		}
	}

	private HashMap<Object, UniformCache> uniformCache = new HashMap<Object, UniformCache>();

	public enum ElementType {
		vertex(GL_VERTEX_SHADER), geometry(GL_GEOMETRY_SHADER), fragment(GL_FRAGMENT_SHADER), tessControl(GL40.GL_TESS_CONTROL_SHADER), tessEval(GL40.GL_TESS_EVALUATION_SHADER), compute(GL43.GL_COMPUTE_SHADER);

		public int gl;

		private ElementType(int gl) {
			this.gl = gl;
		}
	}

	static public class ModelView implements iProvider<Matrix4> {
		float[] o = new float[16];

		Matrix4 m = new Matrix4();

		FloatBuffer f = ByteBuffer.allocateDirect(4 * 16).order(ByteOrder.nativeOrder()).asFloatBuffer();

		public Matrix4 get() {
			f.rewind();
			glGetFloat(GL_MODELVIEW_MATRIX, f);
			f.get(o);
			m.set(o);
			return new Matrix4(m);
		}
	}

	static public class ModelViewFromCurrentCamera implements iProvider<Matrix4> {
		public Matrix4 get() {
			return new Matrix4(BasicCamera.currentCamera.modelView);
		}
	}

	static public class PreviousModelViewFromCurrentCamera implements iProvider<Matrix4> {
		public Matrix4 get() {
			return new Matrix4(BasicCamera.currentCamera.previousModelView == null ? BasicCamera.currentCamera.modelView : BasicCamera.currentCamera.previousModelView);
		}
	}

	//
	// static public class ModelViewFromCamera implements iProvider<Matrix4>
	// {
	// private final BasicCamera c;
	//
	// float[] o = new float[16];
	//
	// Matrix4 m = new Matrix4();
	//
	// public ModelViewFromCamera(BasicCamera c) {
	// this.c = c;
	// }
	//
	// public Matrix4 get() {
	// CoordinateFrame cc = c.getModelViewMatrix();
	// BasicContextManager.getGl().glGetFloatv(BasicContextManager.getGl().GL_MODELVIEW_MATRIX,
	// o, 0);
	// m.set(o);
	// cc.getMatrix(m);
	// return new Matrix4(m);
	// }
	// }

	public static class None extends BasicUtilities.OnePassElement {
		public None() {
			super(StandardPass.postRender);
		}

		@Override
		public void performPass() {
			GL20.glUseProgram(0);
		}
	}

	// static public class Projection implements iProvider<Matrix4> {
	// float[] o = new float[16];
	//
	// Matrix4 m = new Matrix4();
	//
	// public Matrix4 get() {
	// BasicContextManager.getGl().glGetFloatv(BasicContextManager.getGl().GL_PROJECTION_MATRIX,
	// o, 0);
	// m.set(o);
	// // m.transpose();
	// return new Matrix4(m);
	// }
	// }
	//
	static public class ProjectionFromCamera implements iProvider<Matrix4> {
		private final BasicCamera c;
		float[] o = new float[16];

		Matrix4 m = new Matrix4();

		FloatBuffer f = ByteBuffer.allocateDirect(4 * 16).order(ByteOrder.nativeOrder()).asFloatBuffer();

		public ProjectionFromCamera(BasicCamera c) {
			this.c = c;
		}

		public Matrix4 get() {
			f.rewind();
			glGetFloat(GL_PROJECTION_MATRIX, f);
			f.get(o);
			m.set(o);
			return new Matrix4(m);
		}
	}

	public class SetIntegerUniform extends TaskQueue.Task {
		public iProvider<Integer> value;
		public String name;

		public SetIntegerUniform(String string, int i) {
			this(string, new iProvider.Constant<Integer>(i));
			this.name = string;
		}

		public SetIntegerUniform(String name, iProvider<Integer> value) {
			parameterQueue.super();
			this.value = value;
			this.name = name;
		}

		int previous = -1;

		@Override
		protected void run() {
			int id = getUniformCache().find(gl, getProgram(), name);

			if (id != -1) {
				Integer i = value.get();
				if (i != null && i != previous || getUniformCache().lastWasNew || getUniformCache().get(name) != i) {
					glUniform1i(id, i);

					getUniformCache().set(name, i);
					;// System.out.println(" integer uniform <"
						// + id + "/" + name + " = " + i
						// + ">");

					previous = i;
				}
			} else {
			}
			recur();
		}
	}

	static public class SetIntegerUniformElement extends BasicUtilities.OnePassListElement {

		private final iProvider<Integer> to;

		private final String name;

		public SetIntegerUniformElement(StandardPass requestPass, String name, iProvider<Integer> to) {
			super(requestPass, requestPass);
			this.name = name;
			this.to = to;
		}

		public SetIntegerUniformElement(String name, iProvider<Integer> to) {
			super(StandardPass.preRender, StandardPass.preRender);
			this.name = name;
			this.to = to;
		}

		int previous = -1;

		@Override
		public void performPass() {
			assert currentProgram != null;
			pre();

			int id = currentProgram.getUniformCache().find(gl, currentProgram.getProgram(), name);

			if (id != -1) {
				int a = to.get();

				if (a != previous || currentProgram.getUniformCache().lastWasNew || !currentProgram.getUniformCache().get(name).equals(a))
					glUniform1i(id, a);

				currentProgram.getUniformCache().set(name, a);

				previous = a;

			} else {
			}
			post();
		}
	}

	public class SetMatrixUniform extends TaskQueue.Task {
		public iProvider<Matrix4> value;

		float[] mm = new float[16];

		public String name;

		public SetMatrixUniform(String name, iProvider<Matrix4> value) {
			parameterQueue.super();
			this.value = value;
			this.name = name;
		}

		FloatBuffer matrix = ByteBuffer.allocateDirect(4 * 4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();

		@Override
		protected void run() {
			int id = getUniformCache().find(gl, getProgram(), name);
			if (id != -1) {
				Matrix4 i = value.get();
				if (i != null) {
					matrix.rewind();
					i.get(mm);
					matrix.put(mm);
					matrix.rewind();

					glUniformMatrix4(id, false, matrix);
					getUniformCache().set(name, matrix);

					// glUniformMatrix4fv(id, 1, false,
					// i.get(mm), 0);
				}
			} else {
			}
			recur();
		}
	}

	public class PreviousLeftCamera extends SetMatrixUniform {
		public PreviousLeftCamera(String name) {
			super(name, new iProvider<Matrix4>() {

				@Override
				public Matrix4 get() {
					BasicCamera c = BasicCamera.currentCamera;
					if (c instanceof StereoCamera) {
						return new Matrix4(((StereoCamera) c).previousModelViewLeft);
					} else {
						return new Matrix4(c.previousModelView);
					}
				}
			});
		}
	}

	
	public class PreviousCenterCamera extends SetMatrixUniform {
		public PreviousCenterCamera(String name) {
			super(name, new iProvider<Matrix4>() {

				@Override
				public Matrix4 get() {
					BasicCamera c = BasicCamera.currentCamera;
					if (c instanceof LayeredStereoCamera) {
						return new Matrix4(((LayeredStereoCamera) c).previousModelView);
					} else {
						return new Matrix4(c.previousModelView);
					}
				}
			});
		}
	}

	public class PreviousRightCamera extends SetMatrixUniform {
		public PreviousRightCamera(String name) {
			super(name, new iProvider<Matrix4>() {

				@Override
				public Matrix4 get() {
					BasicCamera c = BasicCamera.currentCamera;
					if (c instanceof StereoCamera) {
						return new Matrix4(((StereoCamera) c).previousModelViewRight);
					} else {
						return new Matrix4(c.previousModelView);
					}
				}
			});
		}
	}

	static public class SetMatrixUniformElement extends BasicUtilities.OnePassListElement {

		private final iProvider<Matrix4> to;

		private final String name;

		float[] mm = new float[16];

		public SetMatrixUniformElement(StandardPass requestPass, String name, iProvider<Matrix4> to) {
			super(requestPass, requestPass);
			this.name = name;
			this.to = to;
		}

		public SetMatrixUniformElement(String name, iProvider<Matrix4> to) {
			super(StandardPass.preRender, StandardPass.preRender);
			this.name = name;
			this.to = to;
		}

		FloatBuffer matrix = ByteBuffer.allocateDirect(4 * 4 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();

		@Override
		public void performPass() {
			pre();
			assert currentProgram != null;

			int id = currentProgram.getUniformCache().find(gl, currentProgram.getProgram(), name);
			if (id != -1) {
				Matrix4 i = to.get();
				if (i != null) {

					matrix.rewind();
					i.get(mm);
					matrix.put(mm);
					matrix.rewind();

					glUniformMatrix4(id, false, matrix);
					currentProgram.getUniformCache().set(name, matrix);

					// glUniformMatrix4fv(id, 1, false,
					// i.get(mm), 0);
				}
			} else {
			}
			post();
		}
	}

	public class SetUniform extends TaskQueue.Task {
		public iToFloatArray to;
		public String name;

		public SetUniform(String name, iToFloatArray to) {
			parameterQueue.super();
			this.name = demungeArrayName(name);
			this.to = to;
		}

		float[] previous = null;

		@Override
		protected void run() {

			int id = getUniformCache().find(gl, getProgram(), name);
			// ;//System.out.println(" SU :"+name+" running @ "+id);
			if (id != -1) {
				float[] a = to.get();

				if (!compare(a, previous) || getUniformCache().lastWasNew || !compare((float[]) getUniformCache().get(name), a)) {

					if (a.length == 1)
						glUniform1f(id, a[0]);
					if (a.length == 2)
						glUniform2f(id, a[0], a[1]);
					if (a.length == 3)
						glUniform3f(id, a[0], a[1], a[2]);
					if (a.length == 4)
						glUniform4f(id, a[0], a[1], a[2], a[3]);
					previous = a;

					getUniformCache().set(name, a);

				} else {
				}
				// String xx = "";
				// for (int i = 0; i < a.length; i++)
				// xx += a[i] + " ";

			} else {
			}

			assert glGetError() == 0 : name + " WRONG DIMENSION ?";
			recur();
		}

		public boolean compare(float[] a, float[] b) {
			if (a == null || b == null)
				return false;
			if (a.length != b.length)
				return false;
			for (int i = 0; i < a.length; i++) {
				if (Math.abs(a[i] - b[i]) > 1e-9)
					return false;
			}
			return true;
		}
	}

	/**
	 * to be added to individual pieces of geometry
	 */
	static public class SetUniformElement extends BasicUtilities.OnePassListElement {

		private final iToFloatArray to;

		private final String name;

		public SetUniformElement(StandardPass requestPass, String name, iToFloatArray to) {
			super(requestPass, requestPass);
			this.name = demungeArrayName(name);
			this.to = to;
		}

		public SetUniformElement(String name, iToFloatArray to) {
			super(StandardPass.preRender, StandardPass.preRender);
			this.name = demungeArrayName(name);
			this.to = to;
		}

		float[] previous = null;

		@Override
		public void performPass() {
			pre();
			assert currentProgram != null;

			int id = currentProgram.getUniformCache().find(gl, currentProgram.getProgram(), name);
			if (id != -1) {
				float[] a = to.get();

				if (!compare(a, previous) || currentProgram.getUniformCache().lastWasNew || !compare(a, (float[]) currentProgram.getUniformCache().get(name))) {

					if (a.length == 1)
						glUniform1f(id, a[0]);
					if (a.length == 2)
						glUniform2f(id, a[0], a[1]);
					if (a.length == 3)
						glUniform3f(id, a[0], a[1], a[2]);
					if (a.length == 4)
						glUniform4f(id, a[0], a[1], a[2], a[3]);
					currentProgram.getUniformCache().set(name, a);
				}

				previous = a;
			} else {
			}
			post();
		}

		public boolean compare(float[] a, float[] b) {
			if (a == null || b == null)
				return false;
			if (a.length != b.length)
				return false;
			for (int i = 0; i < a.length; i++) {
				if (Math.abs(a[i] - b[i]) > 1e-4)
					return false;
			}
			return true;
		}
	}

	public static BasicGLSLangProgram currentProgram;

	public static String[] readFiles(File[] code) {
		String[] ret = new String[code.length];
		for (int i = 0; i < ret.length; i++)
			ret[i] = readFile(code[i]);
		return ret;
	}

	static public String demungeArrayName(String name) {
		int ii = name.indexOf("__");
		if (ii == -1)
			return name;

		return name.substring(0, ii) + "[" + name.substring(ii + 2) + "]";

	}

	public static File[] resources(String[] names) {
		File[] f = new File[names.length];
		for (int i = 0; i < names.length; i++) {
			// names[i] = names[i].replaceAll("content/shaders/",
			// "");

			if (CoreHelpers.isCore) {
				URL r_32 = ClassLoader.getSystemResource(names[i]);
				if (r_32 != null) {
					f[i] = new File(r_32.getPath());
					continue;
				}
			}
			URL r = ClassLoader.getSystemResource(names[i]);
			assert r != null : names[i];
			if (r == null) {
				System.err.println(" can't find resource <" + names[i] + ">");
			}
			f[i] = new File(r.getPath());
		}
		return f;
	}

	public static File[] resources(String[] names, ClassLoader loader) {
		File[] f = new File[names.length];
		for (int i = 0; i < names.length; i++) {

			// names[i] = names[i].replaceAll("content/shaders/",
			// "");

			if (CoreHelpers.isCore) {
				URL r_32 = loader.getResource(names[i] + "_32");
				if (r_32 != null) {
					f[i] = new File(r_32.getPath());
					continue;
				}
			}
			URL r = loader.getResource(names[i]);

			;// System.out.println(" url is <" + r + ">");

			assert r != null : names[i];
			if (r == null) {
				System.err.println(" can't find resource <" + names[i] + ">");
			}
			f[i] = new File(r.getPath());
		}
		return f;
	}

	public static String readFile(File code) {
		if (code.getPath().indexOf(".jar!") != -1) {
			String p = code.getPath();

			// if (p.startsWith("file"))
			p = "jar:" + p;

			if (p.startsWith("jar:http:/"))
				p = p.replace("jar:http:/", "jar:http://");

			;// System.out.println(" trying url :" + p);

			InputStream st;
			try {
				st = new URL(p).openConnection().getInputStream();

			} catch (MalformedURLException e1) {
				e1.printStackTrace();
				return null;
			} catch (IOException e1) {
				e1.printStackTrace();
				return null;
			}

			BufferedReader reader = new BufferedReader(new InputStreamReader(st));
			String s = "";
			try {
				while (reader.ready()) {
					s += reader.readLine() + "\n";
				}
				reader.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			return s;

		} else {

			BufferedReader reader = null;
			String s = "";
			try {
				reader = new BufferedReader(new FileReader(code));
				while (reader.ready()) {
					s += reader.readLine() + "\n";
				}
				reader.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			return s;
		}
	}

	public List<BasicGLSLangElement> programs = new ArrayList<BasicGLSLangElement>();

	private int shader;

	public boolean bound = false;

	String vertexFile, fragmentFile;

	TaskQueue parameterQueue = new TaskQueue();

	boolean doPointSize = false;

	byte[] buffer = new byte[2048];

	int geometryVertexNumOut = 3;

	int geometryVertexInType = GL_TRIANGLES;

	int geometryVertexOutType = GL_TRIANGLE_STRIP;

	public BasicGLSLangProgram() {
		super(StandardPass.preRender, StandardPass.preRender);
	}

	public BasicGLSLangProgram(String vertex, ShaderPreprocessor fragmentProc, String reading) throws PreprocessorException {
		super(StandardPass.preRender, StandardPass.preRender);
		new BasicGLSLangElement(BasicGLSLangProgram.resources(new String[] { vertex }, this.getClass().getClassLoader()), ElementType.vertex);
		new BasicGLSLangElement(fragmentProc.read(fragmentProc.filenameToString(reading)), ElementType.fragment);
		this.vertexFile = vertex;
		this.fragmentFile = null;
	}

	public BasicGLSLangProgram(String vertex, String fragment) {
		super(StandardPass.preRender, StandardPass.preRender);
		new BasicGLSLangElement(BasicGLSLangProgram.resources(new String[] { vertex }, this.getClass().getClassLoader()), ElementType.vertex);
		new BasicGLSLangElement(BasicGLSLangProgram.resources(new String[] { fragment }, this.getClass().getClassLoader()), ElementType.fragment);
		this.vertexFile = vertex;
		this.fragmentFile = fragment;
	}

	public BasicGLSLangProgram(String vertex, String fragment, StandardPass postRender) {
		super(postRender, StandardPass.preRender);
		new BasicGLSLangElement(BasicGLSLangProgram.resources(new String[] { vertex }, this.getClass().getClassLoader()), ElementType.vertex);
		new BasicGLSLangElement(BasicGLSLangProgram.resources(new String[] { fragment }, this.getClass().getClassLoader()), ElementType.fragment);
		this.vertexFile = vertex;
		this.fragmentFile = fragment;
	}

	public BasicGLSLangProgram(String vertex, String geometry, String fragment) {
		super(StandardPass.preRender, StandardPass.preRender);
		new BasicGLSLangElement(BasicGLSLangProgram.resources(new String[] { geometry }, this.getClass().getClassLoader()), ElementType.geometry);
		new BasicGLSLangElement(BasicGLSLangProgram.resources(new String[] { vertex }, this.getClass().getClassLoader()), ElementType.vertex);
		new BasicGLSLangElement(BasicGLSLangProgram.resources(new String[] { fragment }, this.getClass().getClassLoader()), ElementType.fragment);
		this.vertexFile = vertex;
		this.fragmentFile = fragment;
	}

	public void delete() {
		BasicContextManager.markAsInvalidInAllContexts(this);
		BasicContextManager.markAsNoIDInAllContexts(this);
		for (BasicGLSLangElement element : programs)
			element.delete();
		GL20.glDeleteProgram(getProgram());
	}

	public String getLog(int i) {

		return GL20.glGetShaderInfoLog(shader, 5000);
	}

	public TaskQueue getParameterTaskQueue() {
		return parameterQueue;
	}

	public int getProgram() {
		return shader;
	}

	public List<BasicGLSLangElement> getPrograms() {
		return programs;
	}

	int numx, numy, numz;

	public BasicGLSLangProgram setupWorkgroups(int numx, int numy, int numz) {
		this.numx = numx;
		this.numy = numy;
		this.numz = numz;
		isComputeShader = true;
		return this;
	}

	boolean isComputeShader = false;

	@Override
	@InheritWeave
	public void performPass() {

		if (needsReloading) {
			needsReloading = false;
			clearUniformCache();
			delete();
			fix();
		}

		assert glGetError() == 0 : getLog(getProgram());
		int boundID = BasicContextManager.getId(this);
		if (boundID == BasicContextManager.ID_NOT_FOUND) {
			;// System.out.println(" no id, fixing");
			fix();
		}

		bound = true;
		if (currentProgram != this && good) {

			if (isComputeShader) {
				if (numx == 0 || numy == 0 || numz == 0) {
					System.out.println(" warning: running a compute shader without having setup it's workgroups");
				}
				GL43.glDispatchCompute(numx, numy, numz);
			} else {
				GL20.glUseProgram(shader);
			}
		} else {
		}

		currentProgram = this;

		pre();

		assert glGetError() == 0 : getLog(getProgram());
		parameterQueue.update();
		assert glGetError() == 0 : getLog(getProgram());

		if (doPointSize)
			glEnable(GL_VERTEX_PROGRAM_POINT_SIZE);
		else
			glDisable(GL_VERTEX_PROGRAM_POINT_SIZE);

		post();
		assert glGetError() == 0 : getLog(getProgram());

		bound = false;

		// debugPrintUniforms();
	}

	public void bindNow() {
		if (needsReloading) {
			needsReloading = false;
			clearUniformCache();
			delete();
			fix();
		}

		// ;//System.out.println(" -- perform pass for program <" + this
		// +
		// ">");

		// System.err.println(" {" + vertexFile + ", " + fragmentFile
		// +"} in ");

		assert glGetError() == 0 : getLog(getProgram());
		int boundID = BasicContextManager.getId(this);
		if (boundID == BasicContextManager.ID_NOT_FOUND) {
			;// System.out.println(" no id, fixing");
			fix();
		}

		bound = true;
		if (currentProgram != this && good) {
			GL20.glUseProgram(shader);
		} else {
		}

		currentProgram = this;

		assert glGetError() == 0 : getLog(getProgram());
		parameterQueue.update();
		assert glGetError() == 0 : getLog(getProgram());

		if (doPointSize)
			glEnable(GL_VERTEX_PROGRAM_POINT_SIZE);
		else
			glDisable(GL_VERTEX_PROGRAM_POINT_SIZE);
	}

	public void reload() {
		;// System.out.println(" reloading programs");
		delete();
		for (BasicGLSLangElement element : programs)
			element.reload();
	}

	boolean needsReloading = false;

	public void deferReload() {
		needsReloading = true;
	}

	public BasicGLSLangProgram setDoPointSize() {
		doPointSize = true;
		return this;
	}

	public void setGeometryShaderParameters(int numOut, int typeIn, int typeOut) {
		this.geometryVertexInType = typeIn;
		this.geometryVertexOutType = typeOut;
		this.geometryVertexNumOut = numOut;
	}

	static public boolean noMoreParameterTaskQueue = false;

	public void updateParameterTaskQueue() {
		// assert bound;

		if (noMoreParameterTaskQueue)
			return;

		if (currentProgram == this) {
			parameterQueue.update();
		}
	}

	boolean good = false;

	protected void fix() {

		assert BasicContextManager.getGl() != null;
		good = false;

		setup();
		assert glGetError() == 0 : getLog(getProgram());
		for (BasicGLSLangElement element : programs)
			element.setup();
		assert glGetError() == 0 : getLog(getProgram());

		glBindAttribLocation(getProgram(), 0, "position");
		glBindAttribLocation(getProgram(), Base.color0_id, "s_Color");
		glBindAttribLocation(getProgram(), Base.color0_id, "color");
		glBindAttribLocation(getProgram(), Base.normal_id, "s_Normal");
		glBindAttribLocation(getProgram(), Base.normal_id, "normal");
		glBindAttribLocation(getProgram(), Base.texture0_id, "s_Texture");
		glBindAttribLocation(getProgram(), Base.texture0_id, "texture0");
		glBindAttribLocation(getProgram(), Base.texture0_id + 1, "s_Texture1");
		glBindAttribLocation(getProgram(), 11, "s_Noise");
		glBindAttribLocation(getProgram(), 10, "s_Last");
		glBindAttribLocation(getProgram(), 4, "s_Four");
		glBindAttribLocation(getProgram(), 5, "s_Five");
		glBindAttribLocation(getProgram(), 10, "s_Ten");
		glBindAttribLocation(getProgram(), 9, "s_Nine");
		glBindAttribLocation(getProgram(), 8, "s_Eight");
		glBindAttribLocation(getProgram(), 7, "s_Seven");
		glBindAttribLocation(getProgram(), 6, "s_Six");
		glBindAttribLocation(getProgram(), 3, "s_Three");
		glBindAttribLocation(getProgram(), 2, "s_Two");
		glBindAttribLocation(getProgram(), 1, "s_One");

		/*
		 * static public final int id_tangentIncomming = 13; static
		 * public final int id_tangentOutgoing = 14;
		 */

		glBindAttribLocation(getProgram(), 13, "s_TangentIncomming");
		glBindAttribLocation(getProgram(), 13, "s_PointParameters");
		glBindAttribLocation(getProgram(), 14, "s_TangentOutgoing");

		glBindAttribLocation(getProgram(), 10, "offset0");
		glBindAttribLocation(getProgram(), 11, "offset1");
		glBindAttribLocation(getProgram(), 12, "offset2");
		glBindAttribLocation(getProgram(), 13, "offset3");
		glBindAttribLocation(getProgram(), 14, "offset4");
		glBindAttribLocation(getProgram(), 15, "offset5");

		glBindAttribLocation(getProgram(), 12, "ambient");
		glBindAttribLocation(getProgram(), 13, "diffuse");
		glBindAttribLocation(getProgram(), 14, "specular");
		glBindAttribLocation(getProgram(), 15, "materialParameters");

		if (CoreHelpers.isCore) {
			GL30.glBindFragDataLocation(getProgram(), 0, "_output");
			GL30.glBindFragDataLocation(getProgram(), 0, "_output0");
			GL30.glBindFragDataLocation(getProgram(), 1, "_output1");
			GL30.glBindFragDataLocation(getProgram(), 2, "_output2");
			GL30.glBindFragDataLocation(getProgram(), 3, "_output3");

		}
		for (int i = 1; i < 16; i++)
			glBindAttribLocation(getProgram(), i, "attribute" + i);

		assert glGetError() == 0 : getLog(getProgram());

		// if (Platform.getOS() == OS.mac) {
		// CGLExtImpl e = (CGLExtImpl) getPlatformGLExtensions();
		// boolean m = e.isFunctionAvailable("glProgramParameteriEXT");
		// if (m) {

		// TODO lwjgl APPLE

		;// System.out.println(" -- setting geometry parameters :" +
			// geometryVertexOutType + " " + geometryVertexInType +
			// " " + geometryVertexNumOut);
			// GL41.glProgramParameteri(program, pname, value)

		// GL41.glProgramParameteri(getProgram(),
		// GL_GEOMETRY_OUTPUT_TYPE, geometryVertexOutType);
		// GL41.glProgramParameteri(getProgram(),
		// GL_GEOMETRY_INPUT_TYPE, geometryVertexInType);
		// GL41.glProgramParameteri(getProgram(),
		// GL_GEOMETRY_VERTICES_OUT, geometryVertexNumOut);

		// }
		// }
		glLinkProgram(shader);

		int didLink = GL20.glGetProgram(shader, GL20.GL_LINK_STATUS);
		if (didLink == 0) {
			String ret = GL20.glGetProgramInfoLog(shader, 10000);
			System.err.println(" program failed to link");
			System.err.println(" log is <" + ret + ">");
			throw new IllegalStateException();
		} else {
		}

		glValidateProgram(shader);
		didLink = GL20.glGetProgram(shader, GL20.GL_VALIDATE_STATUS);
		assert glGetError() == 0 : getLog(getProgram());
		if (didLink == 0) {
			String ret = GL20.glGetProgramInfoLog(shader, 10000);
			System.err.println(" program failed to validate");
			System.err.println(" log is <" + this.getLog(getProgram()) + ">");
			throw new IllegalStateException();
		} else {
			System.err.println(" validated ");
		}

		good = true;

		clearUniformCache();

		assert glGetError() == 0 : getLog(getProgram());
	}

	private void clearUniformCache() {
		for (UniformCache c : uniformCache.values())
			c.clear();
	}

	protected void setup() {
		shader = GL20.glCreateProgram();
		// shader = glCreateProgramObjectARB();
		BasicContextManager.putId(this, shader);
	}

	public int getShader() {
		return shader;
	}

	HashMap<FullScreenCanvasSWT, OnCanvasLines> cachedOCPL = new LinkedHashMap<FullScreenCanvasSWT, OnCanvasLines>();

	public OnCanvasLines getOnCanvasPLine(FullScreenCanvasSWT canvas) {
		OnCanvasLines c = cachedOCPL.get(canvas);
		if (c == null) {
			cachedOCPL.put(canvas, c = new OnCanvasLines(this, canvas));
		}
		return c;
	}

	public OnCanvasLines getOnCanvasLines(FullScreenCanvasSWT canvas) {
		OnCanvasLines c = cachedOCPL.get(canvas);
		if (c == null) {
			cachedOCPL.put(canvas, c = new OnCanvasLines(this, canvas));
		}
		return c;
	}

	public List<Object> lines(FullScreenCanvasSWT canvas) {
		return getOnCanvasPLine(canvas).submit;
	}

	public void debugPrintUniforms() {

		new Exception().printStackTrace();

		System.out.println(" debug print uniforms ------- ");
		for (BasicGLSLangElement e : this.programs) {
			System.out.println("    loaded from :" + Arrays.asList(e.originalFiles));
		}
		for (Task t : getParameterTaskQueue().getTasks()) {
			if (t instanceof SetUniform)
				System.out.println(((SetUniform) t).name + "  = " + ((SetUniform) t).to);
			if (t instanceof SetIntegerUniform)
				System.out.println(((SetIntegerUniform) t).name + "  = " + ((SetIntegerUniform) t).value);
		}
		System.out.println(" cached uniform locations -");
		System.out.println("      " + getUniformCache().id);
	}

	String name;

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String getAllCode() {
		StringBuffer b = new StringBuffer();
		for (BasicGLSLangElement e : getPrograms()) {
			b.append(e.getCode() + "\n");
		}
		return b.toString();
	}

	public UniformCache getUniformCache() {
		UniformCache c = uniformCache.get(BasicContextManager.getCurrentContext());
		if (c == null) {
			uniformCache.put(BasicContextManager.getCurrentContext(), c = new UniformCache());
		}
		return c;
	}

}
