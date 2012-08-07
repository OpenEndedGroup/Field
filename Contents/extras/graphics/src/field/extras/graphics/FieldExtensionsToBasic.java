package field.extras.graphics;

import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform2f;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glUniform4f;

import java.io.IOException;
import java.io.Writer;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.WeakHashMap;

import org.lwjgl.opengl.GL20;
import org.python.core.Py;
import org.python.core.PyBuiltinMethodNarrow;
import org.python.core.PyDictionary;
import org.python.core.PyException;
import org.python.core.PyInteger;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PySlice;
import org.python.core.PyString;
import org.python.core.PyType;

import field.bytecode.protect.iInside;
import field.bytecode.protect.dispatch.Cont;
import field.bytecode.protect.dispatch.Cont.ReturnCode;
import field.bytecode.protect.dispatch.Cont.aRun;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.execution.PythonInterface;
import field.core.plugins.drawing.opengl.CachedLine;
import field.core.plugins.drawing.opengl.CachedLineLayer;
import field.core.plugins.drawing.opengl.DirectLine;
import field.core.plugins.drawing.opengl.DirectMesh;
import field.core.plugins.drawing.opengl.DirectPoint;
import field.core.plugins.python.PythonPluginEditor;
import field.graphics.ci.Destination2;
import field.graphics.core.AdvancedTextures.Base4FloatTexture;
import field.graphics.core.AdvancedTextures.BaseFloatTexture;
import field.graphics.core.Base.StandardPass;
import field.graphics.core.Base.iAcceptsSceneListElement;
import field.graphics.core.Base.iBuildable;
import field.graphics.core.Base.iSceneListElement;
import field.graphics.core.BasicFrameBuffers.iDisplayable;
import field.graphics.core.BasicGLSLangProgram;
import field.graphics.core.BasicGLSLangProgram.BasicGLSLangElement;
import field.graphics.core.BasicGLSLangProgram.SetIntegerUniform;
import field.graphics.core.BasicGLSLangProgram.SetMatrixUniform;
import field.graphics.core.BasicGLSLangProgram.SetUniform;
import field.graphics.core.BasicGeometry.BasicMesh;
import field.graphics.core.BasicGeometry.Instance;
import field.graphics.core.BasicGeometry.QuadMesh_long;
import field.graphics.core.BasicGeometry.TriangleMesh;
import field.graphics.core.BasicGeometry.TriangleMesh_long;
import field.graphics.core.BasicSceneList;
import field.graphics.core.BasicTextures.TextureUnit;
import field.graphics.core.CoreHelpers;
import field.graphics.core.RawMesh2;
import field.graphics.dynamic.DynamicLine_long;
import field.graphics.dynamic.DynamicMesh;
import field.graphics.dynamic.DynamicMesh_long;
import field.graphics.dynamic.DynamicPointlist;
import field.graphics.imageprocessing.ImageProcessing;
import field.graphics.imageprocessing.ImageProcessingTwoOutput;
import field.graphics.imageprocessing.ImageProcessingTwoOutputMultisampled;
import field.graphics.imageprocessing.TwoPassImageProcessing;
import field.graphics.imageprocessing.TwoPassImageProcessingTwoOutput;
import field.graphics.imageprocessing.TwoPassImageProcessingTwoOutputMultisampled;
import field.graphics.windowing.FullScreenCanvasSWT;
import field.launch.iUpdateable;
import field.math.abstraction.iProvider;
import field.math.linalg.CoordinateFrame;
import field.math.linalg.Matrix4;
import field.math.linalg.Quaternion;
import field.math.linalg.Vector2;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;
import field.math.linalg.iToFloatArray;
import field.namespace.generic.Generics.Pair;
import field.util.PythonUtils;
import field.util.TaskQueue;
import field.util.TaskQueue.Task;

public class FieldExtensionsToBasic {
	static final public int _vertex = 0;
	static final public int _normal = 2;
	static final public int _color = 3;
	static final public int _texture0 = 8;

	static public WeakHashMap<Object, Map<String, Object>> perGeometryShaderValues = new WeakHashMap<Object, Map<String, Object>>();
	static public WeakHashMap<Object, iUpdateable> perGeometryShaderUpdators = new WeakHashMap<Object, iUpdateable>();

	// iAcceptsSceneListElement

	public FieldExtensionsToBasic() {

		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__ilshift__", 1) {
				@Override
				public PyObject __call__(PyObject composeWith) {

					iAcceptsSceneListElement s = Py.tojava(self, iAcceptsSceneListElement.class);
					Object w = Py.tojava(composeWith, Object.class);
					wire(s, w);

					return self;
				}
			};
			PyType.fromClass(iAcceptsSceneListElement.class).addMethod(meth);
		}
		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__lshift__", 1) {
				@Override
				public PyObject __call__(PyObject composeWith) {

					iAcceptsSceneListElement s = Py.tojava(self, iAcceptsSceneListElement.class);
					Object w = Py.tojava(composeWith, Object.class);
					wire(s, w);

					return composeWith;
				}

			};
			PyType.fromClass(iAcceptsSceneListElement.class).addMethod(meth);
		}

		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__or__", 1) {
				@Override
				public PyObject __call__(PyObject composeWith) {

					iAcceptsSceneListElement s = Py.tojava(self, iAcceptsSceneListElement.class);

					Object w = Py.tojava(composeWith, Object.class);

					unwire(s, w);

					return composeWith;
				}

			};
			PyType.fromClass(iAcceptsSceneListElement.class).addMethod(meth);
		}

		// {
		// PyBuiltinMethodNarrow meth = new
		// PyBuiltinMethodNarrow("__pow__", 1) {
		// @Override
		// public PyObject __call__(PyObject composeWith) {
		//
		// final Object o = PythonUtils.maybeToJava(composeWith);
		// final FullScreenCanvas c = Py.tojava(self,
		// FullScreenCanvas.class);
		//
		// if (o instanceof ImageProcessing) {
		// ((ImageProcessing) o).join(c);
		// } else if (o instanceof ImageProcessingTwoOutput) {
		// ((ImageProcessingTwoOutput) o).join(c);
		// } else if (o instanceof ImageProcessingTwoOutputMultisampled)
		// {
		// ((ImageProcessingTwoOutputMultisampled) o).join(c);
		// } else if (o instanceof TwoPassImageProcessing) {
		// ((TwoPassImageProcessing) o).join(c);
		// } else if (o instanceof TwoPassImageProcessingTwoOutput) {
		// ((TwoPassImageProcessingTwoOutput) o).join(c);
		// } else if (o instanceof
		// TwoPassImageProcessingTwoOutputMultisampled) {
		// ((TwoPassImageProcessingTwoOutputMultisampled) o).join(c);
		// } else if (o instanceof iDisplayable) {
		// aRun arun = new Cont.aRun() {
		// @Override
		// public ReturnCode head(Object calledOn, Object[] args) {
		//
		// ((iDisplayable) o).display();
		// return super.head(calledOn, args);
		// }
		// };
		// Cont.linkWith(c, c.method_beforeFlush, arun);
		// } else
		// throw Py.AttributeError("cannot attach <" + o + "> to <" + c
		// + ">");
		//
		// return self;
		// }
		// };
		// PyType.fromClass(FullScreenCanvas.class).addMethod(meth);
		// }
		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__pow__", 1) {
				@Override
				public PyObject __call__(PyObject composeWith) {

					final Object o = PythonUtils.maybeToJava(composeWith);
					final FullScreenCanvasSWT c = Py.tojava(self, FullScreenCanvasSWT.class);

					if (o instanceof ImageProcessing) {
						((ImageProcessing) o).join(c);
					} else if (o instanceof ImageProcessingTwoOutput) {
						((ImageProcessingTwoOutput) o).join(c);
					} else if (o instanceof ImageProcessingTwoOutputMultisampled) {
						((ImageProcessingTwoOutputMultisampled) o).join(c);
					} else if (o instanceof TwoPassImageProcessing) {
						((TwoPassImageProcessing) o).join(c);
					} else if (o instanceof TwoPassImageProcessingTwoOutput) {
						((TwoPassImageProcessingTwoOutput) o).join(c);
					} else if (o instanceof TwoPassImageProcessingTwoOutputMultisampled) {
						((TwoPassImageProcessingTwoOutputMultisampled) o).join(c);
					} else if (o instanceof iDisplayable) {
						aRun arun = new Cont.aRun() {
							@Override
							public ReturnCode head(Object calledOn, Object[] args) {

								((iDisplayable) o).display();
								return super.head(calledOn, args);
							}
						};
						Cont.linkWith(c, c.getCurrentFlushMethod(), arun);
					} else
						throw Py.AttributeError("cannot attach <" + o + "> to <" + c + ">");

					return self;
				}
			};
			PyType.fromClass(FullScreenCanvasSWT.class).addMethod(meth);
		}
	
		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__ilshift__", 1) {
				@Override
				public PyObject __call__(PyObject composeWith) {

					ImageProcessing s = Py.tojava(self, ImageProcessing.class);
					iSceneListElement e = toSceneListElement(Py.tojava(composeWith, Object.class));
					s.addChild(e);

					return self;
				}
			};
			PyType.fromClass(ImageProcessing.class).addMethod(meth);
		}
		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__lshift__", 1) {
				@Override
				public PyObject __call__(PyObject composeWith) {

					ImageProcessing s = Py.tojava(self, ImageProcessing.class);
					iSceneListElement e = toSceneListElement(Py.tojava(composeWith, Object.class));
					s.addChild(e);

					return composeWith;
				}
			};
			PyType.fromClass(ImageProcessing.class).addMethod(meth);
		}

		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__ishift__", 1) {
				@Override
				public PyObject __call__(PyObject composeWith) {

					iBuildable s = Py.tojava(self, iBuildable.class);
					Object e = Py.tojava(composeWith, Object.class);

					if (!s.attach(e)) {
						throw Py.AttributeError("cannot attach <" + e + "> to <" + s + ">");
					}

					return composeWith;
				}
			};
			PyType.fromClass(iBuildable.class).addMethod(meth);
		}

		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__pow__", 1) {
				@Override
				public PyObject __call__(PyObject composeWith) {

					DynamicMesh mesh = (DynamicMesh) Py.tojava(self, DynamicMesh.class);

					Object add = Py.tojava(composeWith, Object.class);

					PyObject r = appendToMesh(mesh, add);

					return r;
				}
			};
			PyType.fromClass(DynamicMesh.class).addMethod(meth);
		}
		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__pow__", 1) {
				@Override
				public PyObject __call__(PyObject composeWith) {

					DynamicMesh_long Mesh_long = (DynamicMesh_long) Py.tojava(self, DynamicMesh_long.class);

					Object add = Py.tojava(composeWith, Object.class);

					PyObject r = appendToMesh_long(Mesh_long, add);

					return r;
				}
			};
			PyType.fromClass(DynamicMesh_long.class).addMethod(meth);
		}

		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__enter__", 0) {
				@Override
				public PyObject __call__() {

					iInside mesh = (iInside) Py.tojava(self, iInside.class);
					mesh.open();

					return Py.java2py(mesh);
				}
			};
			PyType.fromClass(iInside.class).addMethod(meth);
		}

		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__exit__", 3) {
				@Override
				public PyObject __call__(PyObject a, PyObject b, PyObject c) {

					iInside mesh = (iInside) Py.tojava(self, iInside.class);
					mesh.close();

					return Py.None;
				}
			};
			PyType.fromClass(iInside.class).addMethod(meth);
		}

		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__setattr__", 2) {
				@Override
				public PyObject __call__(PyObject key, PyObject value) {

					BasicGLSLangProgram program = (BasicGLSLangProgram) Py.tojava(self, BasicGLSLangProgram.class);
					Object o = PythonUtils.maybeToJava(value);
					String k = Py.tojava(key, String.class);

					setValueOnShader(program, o, k);

					// todo, matrix

					return Py.None;
				}

			};
			PyType.fromClass(BasicGLSLangProgram.class).addMethod(meth);
		}

		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__getattr__", 1) {
				@Override
				public PyObject __call__(PyObject key) {
					BasicGLSLangProgram program = (BasicGLSLangProgram) Py.tojava(self, BasicGLSLangProgram.class);
					TaskQueue queue = program.getParameterTaskQueue();
					List<TaskQueue.Task> tasks = queue.getTasks();
					String k = Py.tojava(key, String.class);

					for (Task t : tasks) {
						if (t instanceof SetUniform) {
							if (((SetUniform) t).name.equals(k)) {
								return Py.java2py(((SetUniform) t).to);
							}
						}
					}
					return Py.None;
				}
			};
			PyType.fromClass(BasicGLSLangProgram.class).addMethod(meth);
		}

		// transforming whole pieces of geometry, slowly

		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__iadd__", 1) {
				@Override
				public PyObject __call__(PyObject add) {
					DynamicMesh mesh = Py.tojava(self, DynamicMesh.class);
					FloatBuffer v = mesh.getUnderlyingGeometry().vertex();

					Object o = PythonUtils.maybeToJava(add);

					transformAdd(v, mesh.getVertexCursor(), o);
					return self;
				}
			};
			PyType.fromClass(DynamicMesh.class).addMethod(meth);
		}

		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__imul__", 1) {
				@Override
				public PyObject __call__(PyObject add) {
					DynamicMesh mesh = Py.tojava(self, DynamicMesh.class);
					FloatBuffer v = mesh.getUnderlyingGeometry().vertex();

					Object o = PythonUtils.maybeToJava(add);

					transformMul(v, mesh.getVertexCursor(), o);
					return self;
				}
			};
			PyType.fromClass(DynamicMesh.class).addMethod(meth);
		}
		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__isub__", 1) {
				@Override
				public PyObject __call__(PyObject add) {
					DynamicMesh mesh = Py.tojava(self, DynamicMesh.class);
					FloatBuffer v = mesh.getUnderlyingGeometry().vertex();

					Object o = PythonUtils.maybeToJava(add);

					transformMul(v, mesh.getVertexCursor(), o);
					return self;
				}
			};
			PyType.fromClass(DynamicMesh.class).addMethod(meth);
		}

		{
			final PyObject objectGetattribute = PyObject.TYPE.__getattr__("__getattribute__");
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__getattribute__", 1) {
				@Override
				public PyObject __call__(PyObject name) {
					try {
						return objectGetattribute.__call__(self, name);
					} catch (PyException e) {
						if (!Py.matchException(e, Py.AttributeError)) {
							throw e;
						}
					}

					DynamicMesh mesh = (DynamicMesh) Py.tojava(self, DynamicMesh.class);
					PyObject under = Py.java2py(mesh.getUnderlyingGeometry());

					PyException was;
					try {
						return under.__getattr__((PyString) name);
					} catch (PyException e) {
						was = e;
					}

					final String nn = ((PyString) name).asString();
					Map<String, Object> m = perGeometryShaderValues.get(mesh.getUnderlyingGeometry());
					// if (m == null)
					// throw was;
					Object o = m.get(nn);
					// if (o == null)
					// throw was;
					return Py.java2py(o);
				}
			};
			PyType.fromClass(DynamicMesh.class).addMethod(meth);
		}
		{
			final PyObject objectGetattribute = PyObject.TYPE.__getattr__("__getattribute__");
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__getattribute__", 1) {
				@Override
				public PyObject __call__(PyObject name) {
					try {
						return objectGetattribute.__call__(self, name);
					} catch (PyException e) {
						if (!Py.matchException(e, Py.AttributeError)) {
							throw e;
						}
					}

					BasicMesh mesh = (BasicMesh) Py.tojava(self, BasicMesh.class);

					final String nn = ((PyString) name).asString();
					Map<String, Object> m = perGeometryShaderValues.get(mesh);
					if (m == null)
						return Py.None;
					Object o = m.get(nn);
					if (o == null)
						return Py.None;
					return Py.java2py(o);
				}
			};
			PyType.fromClass(BasicMesh.class).addMethod(meth);
		}
		{
			final PyObject objectGetattribute = PyObject.TYPE.__getattr__("__getattribute__");
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__getattribute__", 1) {
				@Override
				public PyObject __call__(PyObject name) {
					try {
						return objectGetattribute.__call__(self, name);
					} catch (PyException e) {
						if (!Py.matchException(e, Py.AttributeError)) {
							throw e;
						}
					}

					BasicMesh mesh = (BasicMesh) Py.tojava(self, BasicMesh.class);

					final String nn = ((PyString) name).asString();
					Map<String, Object> m = perGeometryShaderValues.get(mesh);
					if (m == null)
						return Py.None;
					Object o = m.get(nn);
					if (o == null)
						return Py.None;
					return Py.java2py(o);
				}
			};
			PyType.fromClass(TriangleMesh.class).addMethod(meth);
		}

		{
			final PyObject objectGetattribute = PyObject.TYPE.__getattr__("__getattribute__");
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__getattribute__", 1) {
				@Override
				public PyObject __call__(PyObject name) {
					try {
						return objectGetattribute.__call__(self, name);
					} catch (PyException e) {
						if (!Py.matchException(e, Py.AttributeError)) {
							throw e;
						}
					}

					TriangleMesh_long mesh = (TriangleMesh_long) Py.tojava(self, TriangleMesh_long.class);

					final String nn = ((PyString) name).asString();
					Map<String, Object> m = perGeometryShaderValues.get(mesh);
					if (m == null)
						return Py.None;
					Object o = m.get(nn);
					if (o == null)
						return Py.None;
					return Py.java2py(o);
				}
			};
			PyType.fromClass(TriangleMesh_long.class).addMethod(meth);
		}
		{
			final PyObject objectGetattribute = PyObject.TYPE.__getattr__("__getattribute__");
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__getattribute__", 1) {
				@Override
				public PyObject __call__(PyObject name) {
					try {
						return objectGetattribute.__call__(self, name);
					} catch (PyException e) {
						if (!Py.matchException(e, Py.AttributeError)) {
							throw e;
						}
					}

					QuadMesh_long mesh = (QuadMesh_long) Py.tojava(self, QuadMesh_long.class);

					final String nn = ((PyString) name).asString();
					Map<String, Object> m = perGeometryShaderValues.get(mesh);
					if (m == null)
						return Py.None;
					Object o = m.get(nn);
					if (o == null)
						return Py.None;
					return Py.java2py(o);
				}
			};
			PyType.fromClass(QuadMesh_long.class).addMethod(meth);
		}
		{
			final PyObject objectGetattribute = PyObject.TYPE.__getattr__("__getattribute__");
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__getattribute__", 1) {
				@Override
				public PyObject __call__(PyObject name) {
					try {
						return objectGetattribute.__call__(self, name);
					} catch (PyException e) {
						if (!Py.matchException(e, Py.AttributeError)) {
							throw e;
						}
					}

					Instance mesh = (Instance) Py.tojava(self, Instance.class);

					final String nn = ((PyString) name).asString();
					Map<String, Object> m = perGeometryShaderValues.get(mesh);
					if (m == null)
						return Py.None;
					Object o = m.get(nn);
					if (o == null)
						return Py.None;
					return Py.java2py(o);
				}
			};
			PyType.fromClass(Instance.class).addMethod(meth);
		}

		{
			final PyObject objectGetattribute = PyObject.TYPE.__getattr__("__setattr__");
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__setattr__", 1) {
				@Override
				public PyObject __call__(PyObject name, final PyObject value) {
					PyException was;
					try {
						return objectGetattribute.__call__(self, name);
					} catch (PyException e) {
						// if (!Py.matchException(e,
						// Py.AttributeError)) {
						// throw e;
						// }
						// was = e;
					}

					final DynamicMesh mesh = (DynamicMesh) Py.tojava(self, DynamicMesh.class);
					// PyObject under =
					// Py.java2py(mesh.getUnderlyingGeometry());
					//
					// try {
					// under.__setattr__((PyString) name,
					// value);
					// } catch (PyException e) {
					// }

					final String nn = ((PyString) name).asString();

					Map<String, Object> m = perGeometryShaderValues.get(mesh.getUnderlyingGeometry());
					if (m == null) {
						perGeometryShaderValues.put(mesh, m = new HashMap<String, Object>());

						;//;//System.out.println(" --- new per geometry shader values --");
					}

					;//;//System.out.println(" values were <" + m + ">");

					installUniformUpdator((BasicSceneList) mesh.getUnderlyingGeometry(), m);

					m.put(nn, Py.tojava(value, Object.class));

					;//;//System.out.println(" values now <" + m + ">");

					return Py.None;
				}
			};
			PyType.fromClass(DynamicMesh.class).addMethod(meth);
		}
		{
			final PyObject objectGetattribute = PyObject.TYPE.__getattr__("__setattr__");
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__setattr__", 1) {
				@Override
				public PyObject __call__(PyObject name, final PyObject value) {
					PyException was;
					try {
						return objectGetattribute.__call__(self, name);
					} catch (PyException e) {
					}

					final BasicMesh mesh = (BasicMesh) Py.tojava(self, BasicMesh.class);

					final String nn = ((PyString) name).asString();

					Map<String, Object> m = perGeometryShaderValues.get(mesh);
					if (m == null)
						perGeometryShaderValues.put(mesh, m = new HashMap<String, Object>());

					installUniformUpdator((BasicSceneList) mesh, m);

					m.put(nn, Py.tojava(value, Object.class));

					return Py.None;
				}
			};
			PyType.fromClass(BasicMesh.class).addMethod(meth);
		}
		{
			final PyObject objectGetattribute = PyObject.TYPE.__getattr__("__setattr__");
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__setattr__", 1) {
				@Override
				public PyObject __call__(PyObject name, final PyObject value) {
					PyException was;
					try {
						return objectGetattribute.__call__(self, name);
					} catch (PyException e) {
					}

					final BasicMesh mesh = (BasicMesh) Py.tojava(self, BasicMesh.class);

					final String nn = ((PyString) name).asString();

					Map<String, Object> m = perGeometryShaderValues.get(mesh);
					if (m == null)
						perGeometryShaderValues.put(mesh, m = new HashMap<String, Object>());

					installUniformUpdator((BasicSceneList) mesh, m);

					m.put(nn, Py.tojava(value, Object.class));

					return Py.None;
				}
			};
			PyType.fromClass(TriangleMesh.class).addMethod(meth);
		}

		{
			final PyObject objectGetattribute = PyObject.TYPE.__getattr__("__setattr__");
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__setattr__", 1) {
				@Override
				public PyObject __call__(PyObject name, final PyObject value) {
					PyException was;
					try {
						return objectGetattribute.__call__(self, name);
					} catch (PyException e) {
					}

					final TriangleMesh_long mesh = (TriangleMesh_long) Py.tojava(self, TriangleMesh_long.class);

					final String nn = ((PyString) name).asString();

					Map<String, Object> m = perGeometryShaderValues.get(mesh);
					if (m == null)
						perGeometryShaderValues.put(mesh, m = new HashMap<String, Object>());

					installUniformUpdator((BasicSceneList) mesh, m);

					m.put(nn, Py.tojava(value, Object.class));

					return Py.None;
				}
			};
			PyType.fromClass(TriangleMesh_long.class).addMethod(meth);
		}
		{
			final PyObject objectGetattribute = PyObject.TYPE.__getattr__("__setattr__");
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__setattr__", 1) {
				@Override
				public PyObject __call__(PyObject name, final PyObject value) {
					PyException was;
					try {
						return objectGetattribute.__call__(self, name);
					} catch (PyException e) {
					}

					final QuadMesh_long mesh = (QuadMesh_long) Py.tojava(self, QuadMesh_long.class);

					final String nn = ((PyString) name).asString();

					Map<String, Object> m = perGeometryShaderValues.get(mesh);
					if (m == null)
						perGeometryShaderValues.put(mesh, m = new HashMap<String, Object>());

					installUniformUpdator((BasicSceneList) mesh, m);

					m.put(nn, Py.tojava(value, Object.class));

					return Py.None;
				}
			};
			PyType.fromClass(QuadMesh_long.class).addMethod(meth);
		}
		{
			final PyObject objectGetattribute = PyObject.TYPE.__getattr__("__setattr__");
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__setattr__", 1) {
				@Override
				public PyObject __call__(PyObject name, final PyObject value) {
					PyException was;
					try {
						return objectGetattribute.__call__(self, name);
					} catch (PyException e) {
					}

					final Instance mesh = (Instance) Py.tojava(self, Instance.class);

					final String nn = ((PyString) name).asString();

					Map<String, Object> m = perGeometryShaderValues.get(mesh);
					if (m == null)
						perGeometryShaderValues.put(mesh, m = new HashMap<String, Object>());

					installUniformUpdator((BasicSceneList) mesh, m);

					m.put(nn, Py.tojava(value, Object.class));

					return Py.None;
				}
			};
			PyType.fromClass(Instance.class).addMethod(meth);
		}

		{
			final PyObject objectGetattribute = PyObject.TYPE.__getattr__("__setattr__");
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__setattr__", 1) {
				@Override
				public PyObject __call__(PyObject name, final PyObject value) {
					PyException was;
					try {
						return objectGetattribute.__call__(self, name);
					} catch (PyException e) {
						// if (!Py.matchException(e,
						// Py.AttributeError)) {
						// throw e;
						// }
						// was = e;
					}

					RawMesh2 mesh = (RawMesh2) Py.tojava(self, RawMesh2.class);

					final String nn = ((PyString) name).asString();

					Map<String, Object> m = perGeometryShaderValues.get(mesh);
					if (m == null)
						perGeometryShaderValues.put(mesh, m = new HashMap<String, Object>());

					installUniformUpdator(mesh, m);

					m.put(nn, Py.tojava(value, Object.class));

					return Py.None;
				}
			};
			PyType.fromClass(RawMesh2.class).addMethod(meth);
		}

		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__iadd__", 1) {
				@Override
				public PyObject __call__(PyObject add) {
					DynamicMesh_long Mesh_long = Py.tojava(self, DynamicMesh_long.class);
					FloatBuffer v = Mesh_long.getUnderlyingGeometry().vertex();

					Object o = PythonUtils.maybeToJava(add);

					transformAdd(v, Mesh_long.getVertexCursor(), o);
					return self;
				}
			};
			PyType.fromClass(DynamicMesh_long.class).addMethod(meth);
		}

		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__imul__", 1) {
				@Override
				public PyObject __call__(PyObject add) {
					DynamicMesh_long Mesh_long = Py.tojava(self, DynamicMesh_long.class);
					FloatBuffer v = Mesh_long.getUnderlyingGeometry().vertex();

					Object o = PythonUtils.maybeToJava(add);

					transformMul(v, Mesh_long.getVertexCursor(), o);
					return self;
				}
			};
			PyType.fromClass(DynamicMesh_long.class).addMethod(meth);
		}
		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__isub__", 1) {
				@Override
				public PyObject __call__(PyObject add) {
					DynamicMesh_long Mesh_long = Py.tojava(self, DynamicMesh_long.class);
					FloatBuffer v = Mesh_long.getUnderlyingGeometry().vertex();

					Object o = PythonUtils.maybeToJava(add);

					transformMul(v, Mesh_long.getVertexCursor(), o);
					return self;
				}
			};
			PyType.fromClass(DynamicMesh_long.class).addMethod(meth);
		}

		{
			final PyObject objectGetattribute = PyObject.TYPE.__getattr__("__getattribute__");
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__getattribute__", 1) {
				@Override
				public PyObject __call__(PyObject name) {
					try {
						return objectGetattribute.__call__(self, name);
					} catch (PyException e) {
						if (!Py.matchException(e, Py.AttributeError)) {
							throw e;
						}
					}

					DynamicMesh_long Mesh_long = (DynamicMesh_long) Py.tojava(self, DynamicMesh_long.class);
					PyObject under = Py.java2py(Mesh_long.getUnderlyingGeometry());

					PyException was;
					try {
						return under.__getattr__((PyString) name);
					} catch (PyException e) {
						was = e;
					}

					final String nn = ((PyString) name).asString();
					Map<String, Object> m = perGeometryShaderValues.get(Mesh_long.getUnderlyingGeometry());
					if (m == null)
						throw was;
					Object o = m.get(nn);
					if (o == null)
						throw was;
					return Py.java2py(o);
				}
			};
			PyType.fromClass(DynamicMesh_long.class).addMethod(meth);
		}
		{
			final PyObject objectGetattribute = PyObject.TYPE.__getattr__("__setattr__");
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__setattr__", 1) {
				@Override
				public PyObject __call__(PyObject name, final PyObject value) {

					;//;//System.out.println(" inside setattr for dynamicmesh_long");

					PyException was;
					try {
						return objectGetattribute.__call__(self, name);
					} catch (PyException e) {
						// if (!Py.matchException(e,
						// Py.AttributeError)) {
						// throw e;
						// }
						// was = e;
					}

					final DynamicMesh_long Mesh_long = (DynamicMesh_long) Py.tojava(self, DynamicMesh_long.class);
					// PyObject under =
					// Py.java2py(Mesh_long.getUnderlyingGeometry());
					//
					// try {
					// under.__setattr__((PyString) name,
					// value);
					// } catch (PyException e) {
					// // if (!Py.matchException(e,
					// // Py.AttributeError)) {
					// // throw e;
					// // }
					// // was = e;
					// }

					final String nn = ((PyString) name).asString();

					Map<String, Object> m = perGeometryShaderValues.get(Mesh_long.getUnderlyingGeometry());
					if (m == null) {
						perGeometryShaderValues.put(Mesh_long.getUnderlyingGeometry(), m = new HashMap<String, Object>());
						;//;//System.out.println(" --- new per geometry shader values --");
					}

					;//;//System.out.println(" values were <" + m + ">");

					installUniformUpdator((BasicSceneList) Mesh_long.getUnderlyingGeometry(), m);

					m.put(nn, Py.tojava(value, Object.class));

					;//;//System.out.println(" values now <" + m + ">");

					return Py.None;
				}
			};
			PyType.fromClass(DynamicMesh_long.class).addMethod(meth);
		}

		// coordinate frame math

		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__add__", 1) {
				@Override
				public PyObject __call__(PyObject add) {

					CoordinateFrame frame = Py.tojava(self, CoordinateFrame.class);

					Object added = PythonUtils.maybeToJava(add);

					return Py.java2py(transformFrameAdd(frame.duplicate(), added));
				}
			};
			PyType.fromClass(CoordinateFrame.class).addMethod(meth);
		}
		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__mul__", 1) {
				@Override
				public PyObject __call__(PyObject add) {

					CoordinateFrame frame = Py.tojava(self, CoordinateFrame.class);

					Object added = PythonUtils.maybeToJava(add);

					return Py.java2py(transformFrameMul(frame.duplicate(), added));
				}
			};
			PyType.fromClass(CoordinateFrame.class).addMethod(meth);
		}
		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__iadd__", 1) {
				@Override
				public PyObject __call__(PyObject add) {

					CoordinateFrame frame = Py.tojava(self, CoordinateFrame.class);

					Object added = PythonUtils.maybeToJava(add);

					return Py.java2py(transformFrameAdd(frame, added));
				}
			};
			PyType.fromClass(CoordinateFrame.class).addMethod(meth);
		}
		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__imul__", 1) {
				@Override
				public PyObject __call__(PyObject add) {

					CoordinateFrame frame = Py.tojava(self, CoordinateFrame.class);

					Object added = PythonUtils.maybeToJava(add);

					return Py.java2py(transformFrameMul(frame, added));
				}
			};
			PyType.fromClass(CoordinateFrame.class).addMethod(meth);
		}

		// buffers

		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__getitem__", 1) {
				@Override
				public PyObject __call__(PyObject item) {

					FloatBuffer frame = Py.tojava(self, FloatBuffer.class);
					int ii = Py.tojava(item, Integer.class);
					return Py.java2py(frame.get(ii));
				}
			};
			PyType.fromClass(FloatBuffer.class).addMethod(meth);
		}
		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__getitem__", 1) {
				@Override
				public PyObject __call__(PyObject item) {

					ShortBuffer frame = Py.tojava(self, ShortBuffer.class);
					int ii = Py.tojava(item, Integer.class);
					return Py.java2py(frame.get(ii));
				}
			};
			PyType.fromClass(ShortBuffer.class).addMethod(meth);
		}
		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__getitem__", 1) {
				@Override
				public PyObject __call__(PyObject item) {

					IntBuffer frame = Py.tojava(self, IntBuffer.class);
					int ii = Py.tojava(item, Integer.class);
					return Py.java2py(frame.get(ii));
				}
			};
			PyType.fromClass(IntBuffer.class).addMethod(meth);
		}

		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__setitem__", 2) {
				@Override
				public PyObject __call__(PyObject item, PyObject value) {

					if (item instanceof PySlice) {
						FloatBuffer frame = Py.tojava(self, FloatBuffer.class);
						PySlice s = ((PySlice) item);
						int[] m = s.indicesEx(frame.limit());
						int q = 0;
						for (int i = m[0]; i < m[1]; i += m[2]) {
							frame.put(i, Py.tojava(value.__getitem__(q), Float.class));
							q += 1;
						}
						return self;
					} else {

						FloatBuffer frame = Py.tojava(self, FloatBuffer.class);
						int ii = Py.tojava(item, Integer.class);
						float to = Py.tojava(value, Float.class);
						frame.put(ii, to);
						return self;
					}
				}
			};
			PyType.fromClass(FloatBuffer.class).addMethod(meth);
		}
		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__setitem__", 2) {
				@Override
				public PyObject __call__(PyObject item, PyObject value) {

					ShortBuffer frame = Py.tojava(self, ShortBuffer.class);
					int ii = Py.tojava(item, Integer.class);
					short to = Py.tojava(value, Short.class);
					frame.put(ii, to);
					return self;
				}
			};
			PyType.fromClass(ShortBuffer.class).addMethod(meth);
		}
		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__setitem__", 2) {
				@Override
				public PyObject __call__(PyObject item, PyObject value) {

					IntBuffer frame = Py.tojava(self, IntBuffer.class);
					int ii = Py.tojava(item, Integer.class);
					int to = Py.tojava(value, Integer.class);
					frame.put(ii, to);
					return self;
				}
			};
			PyType.fromClass(ShortBuffer.class).addMethod(meth);
		}
		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__len__", 0) {
				@Override
				public PyObject __call__() {

					Buffer frame = Py.tojava(self, Buffer.class);

					return Py.java2py(frame.remaining());
				}
			};
			PyType.fromClass(Buffer.class).addMethod(meth);
		}
		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__nonzero__", 0) {
				@Override
				public PyObject __call__() {

					Buffer frame = Py.tojava(self, Buffer.class);

					return Py.java2py(frame.remaining() > 0);
				}
			};
			PyType.fromClass(Buffer.class).addMethod(meth);
		}

		{
			final PyObject objectGetattribute = PyObject.TYPE.__getattr__("__getattribute__");
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__getattribute__", 1) {
				@Override
				public PyObject __call__(PyObject name) {
					try {
						return objectGetattribute.__call__(self, name);
					} catch (PyException e) {
						if (!Py.matchException(e, Py.AttributeError)) {
							throw e;
						}
					}

					TextureUnit mesh = (TextureUnit) Py.tojava(self, TextureUnit.class);
					PyObject under = Py.java2py(mesh.getWrapped());

					return under.__getattr__((PyString) name);
				}
			};
			PyType.fromClass(TextureUnit.class).addMethod(meth);
		}

		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__ilshift__", 1) {
				@Override
				public PyObject __call__(PyObject composeWith) {

					Object o = Py.tojava(composeWith, Object.class);
					if (o instanceof float[])
						Conversions.update((float[]) o, (BaseFloatTexture) self.__tojava__(BaseFloatTexture.class));
					else if (o instanceof float[][])
						Conversions.update((float[][]) o, (BaseFloatTexture) self.__tojava__(BaseFloatTexture.class));
					else if (o instanceof int[][])
						Conversions.update((int[][]) o, (BaseFloatTexture) self.__tojava__(BaseFloatTexture.class));
					else
						throw new IllegalArgumentException();
					return self;
				}
			};
			PyType.fromClass(BaseFloatTexture.class).addMethod(meth);
		}

		{
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__ilshift__", 1) {
				@Override
				public PyObject __call__(PyObject composeWith) {

					Object o = Py.tojava(composeWith, Object.class);
					if (o instanceof float[])
						Conversions.update((float[]) o, (Base4FloatTexture) self.__tojava__(Base4FloatTexture.class));
					else if (o instanceof float[][])
						Conversions.update((float[][]) o, (Base4FloatTexture) self.__tojava__(Base4FloatTexture.class));
					else if (o instanceof int[][])
						Conversions.update((int[][]) o, (Base4FloatTexture) self.__tojava__(Base4FloatTexture.class));
					else
						throw new IllegalArgumentException();

					return self;
				}
			};
			PyType.fromClass(Base4FloatTexture.class).addMethod(meth);
		}
	}

	static public void installUniformUpdator(final BasicSceneList underlyingGeometry, final Map<String, Object> lookupIn) {

		iUpdateable u = perGeometryShaderUpdators.get(underlyingGeometry);

		if (u == null) {
			;//;//System.out.println(" installing per shader updator for <" + underlyingGeometry + ">");

			class PushPullPair {

				LinkedHashMap<String, Object> was = new LinkedHashMap<String, Object>();

				public iUpdateable getPush() {
					return new iUpdateable() {

						Task t = null;

						public void update() {

							Map<String, Object> m = lookupIn;
							// ;//;//System.out.println(" -- push <"
							// + underlyingGeometry
							// +
							// "> <"+BasicGLSLangProgram.currentProgram+"> <"+lookupIn+">");
							if (BasicGLSLangProgram.currentProgram != null) {

								for (Map.Entry<String, Object> e : m.entrySet()) {
									int id = BasicGLSLangProgram.currentProgram.getUniformCache().find(BasicGLSLangProgram.currentProgram.gl, BasicGLSLangProgram.currentProgram.getProgram(), e.getKey());
									if (id > -1) {

										// if
										// (underlyingGeometry
										// instanceof
										// Instance)
										// ;//;//System.out.println(" setting "
										// +
										// id
										// +
										// " "
										// +
										// e.getKey()
										// +
										// " "
										// +
										// e.getValue());

										was.put(e.getKey(), BasicGLSLangProgram.currentProgram.getUniformCache().get(e.getKey()));
										setUniformNow(BasicGLSLangProgram.currentProgram.gl, id, e.getValue());
									}
								}
							}

						}
					};
				}

				public iUpdateable getPop() {
					return new iUpdateable() {

						Task t = null;

						public void update() {

							Map<String, Object> m = lookupIn;

							if (BasicGLSLangProgram.currentProgram != null) {

								for (Map.Entry<String, Object> e : m.entrySet()) {
									int id = BasicGLSLangProgram.currentProgram.getUniformCache().find(BasicGLSLangProgram.currentProgram.gl, BasicGLSLangProgram.currentProgram.getProgram(), e.getKey());
									if (id > -1) {
										Object x = was.get(e.getKey());
										if (x != null) {

											// if
											// (underlyingGeometry
											// instanceof
											// Instance)
											// ;//;//System.out.println(" unsetting "
											// +
											// id
											// +
											// " "
											// +
											// e.getKey()
											// +
											// " "
											// +
											// x);

											setUniformNow(BasicGLSLangProgram.currentProgram.gl, id, x);
											BasicGLSLangProgram.currentProgram.getUniformCache().set(e.getKey(), x);
										}
									}
								}
							}

						}
					};
				}
			}
			;

			PushPullPair ppp = new PushPullPair();
			u = ppp.getPush();
			underlyingGeometry.add(StandardPass.preRender).register("__updator__", u);
			underlyingGeometry.add(StandardPass.preDisplay).register("__dupdator__", ppp.getPop());
			perGeometryShaderUpdators.put(underlyingGeometry, u);
		}
	}

	static FloatBuffer tq = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
	static float[] tqm = new float[16];
	
	static public void setUniformNow(Object gl, int id, Object value) {

		if (value instanceof Vector4) {
			glUniform4f(id, ((Vector4) value).x, ((Vector4) value).y, ((Vector4) value).z, ((Vector4) value).w);
			// ;//;//System.out.println(" set uniform right now <" + id +
			// " " + value + ">");
		} else if (value instanceof Vector3) {
			glUniform3f(id, ((Vector3) value).x, ((Vector3) value).y, ((Vector3) value).z);
		} else if (value instanceof Vector2) {
			glUniform2f(id, ((Vector2) value).x, ((Vector2) value).y);
		} else if (value instanceof Float) {
			glUniform1f(id, ((Float) value));
		} else if (value instanceof Double) {
			glUniform1f(id, ((Double) value).floatValue());
		} else if (value instanceof Integer) {
			glUniform1i(id, ((Integer) value).intValue());
		} else if (value instanceof iProvider) {
			Object nvalue = ((iProvider) value).get();
			if (nvalue != value) {
				setUniformNow(gl, id, nvalue);
			}
		} else if (value instanceof Matrix4) {

			tq.rewind();
			tq.put( ((Matrix4)value).get(tqm) );
			tq.rewind();

			GL20.glUniformMatrix4(id, true, tq);
		}
	}

	public static Task setValueOnShader(BasicGLSLangProgram program, Object o, String k) {

		k = BasicGLSLangProgram.demungeArrayName(k);

		if (o instanceof TextureUnit) {
			o = ((TextureUnit) o).getUnit();
		}

		final Object fo = o;

		List<Task> tasks = program.getParameterTaskQueue().getTasks();

		Task rt = null;
		boolean found = false;
		for (Task t : tasks) {
			if (t instanceof SetUniform) {
				if (((SetUniform) t).name.equals(k)) {
					if (o instanceof iToFloatArray)
						(((SetUniform) t).to) = (iToFloatArray) o;
					else if (o instanceof Number)
						(((SetUniform) t).to) = new iToFloatArray() {
							public float[] get() {
								return new float[] { ((Number) fo).floatValue() };
							}
						};
					rt = t;
					found = true;
					break;
				}
			}
			if (t instanceof SetIntegerUniform) {
				if (((SetIntegerUniform) t).name.equals(k)) {

					;//;//System.out.println(" found integer uniform ");

					if ((((SetIntegerUniform) t).value instanceof iProvider.Constant)) {
						((iProvider.Constant) ((SetIntegerUniform) t).value).set(o);
					} else {
						((SetIntegerUniform) t).value = new iProvider.Constant<Integer>((Integer) o);
					}
					found = true;
					rt = t;
					break;
				}
			}
			if (t instanceof SetMatrixUniform) {
				if (((SetMatrixUniform) t).name.equals(k)) {
					if ((((SetMatrixUniform) t).value instanceof iProvider.Constant)) {
						((iProvider.Constant) ((SetMatrixUniform) t).value).set(o);
					} else {
						((SetMatrixUniform) t).value = new iProvider.Constant<Matrix4>((Matrix4) o);
					}
					found = true;
					rt = t;
				}
			}
		}
		if (!found) {
			if (o instanceof iToFloatArray)
				rt = program.new SetUniform(k, ((iToFloatArray) o));
			else if (o instanceof Integer)
				rt = program.new SetIntegerUniform(k, ((Integer) o).intValue());
			else if (o instanceof Long)
				rt = program.new SetIntegerUniform(k, ((Long) o).intValue());
			else if (o instanceof Number) {
				rt = program.new SetUniform(k, new iToFloatArray() {

					public float[] get() {
						return new float[] { ((Number) fo).floatValue() };
					}
				});
			} else if (o instanceof Matrix4) {
				rt = program.new SetMatrixUniform(k, new iProvider.Constant<Matrix4>().set((Matrix4) o));
			}
		}

		// if (program.currentProgram == program) {
		// program.updateParameterTaskQueue();
		// }

		return rt;
	}

	static protected CoordinateFrame transformFrameAdd(CoordinateFrame frame, Object added) {
		if (added instanceof Vector3) {
			frame.setTranslation(frame.getTranslation(null).add((Vector3) added));
			return frame;
		} else if (added instanceof Vector2) {
			Vector3 x = frame.getTranslation(null);
			x.x += ((Vector2) added).x;
			x.y += ((Vector2) added).y;
			frame.setTranslation(x);
			return frame;
		} else if (added instanceof Quaternion) {
			frame.setRotation((new Quaternion().mul(((Quaternion) added), frame.getRotation(null))));
			return frame;
		}

		throw Py.AttributeError(" cant add <" + added + "> to a coordinate frame");
	}

	static protected CoordinateFrame transformFrameMul(CoordinateFrame frame, Object added) {
		if (added instanceof Vector3) {
			frame.setScale(frame.getScale(null).scale((Vector3) added));
			return frame;
		} else if (added instanceof Vector2) {
			Vector3 x = frame.getScale(null);
			x.x *= ((Vector2) added).x;
			x.y *= ((Vector2) added).y;
			frame.setScale(x);
			return frame;
		} else if (added instanceof Quaternion) {
			frame.setRotation((new Quaternion().mul(((Quaternion) added), frame.getRotation(null))));
			return frame;
		} else if (added instanceof CoordinateFrame) {
			frame.multiply(frame.duplicate(), (CoordinateFrame) added);
			return frame;
		}

		throw Py.AttributeError(" can't mul <" + added + "> to a coordinate frame");
	}

	static protected void transformAdd(FloatBuffer v, int vertexCursor, Object o) {
		if (o instanceof Vector3) {
			Vector3 a = ((Vector3) o);
			for (int i = 0; i < vertexCursor; i++) {
				v.put(3 * i + 0, v.get(3 * i + 0) + a.x);
				v.put(3 * i + 1, v.get(3 * i + 1) + a.y);
				v.put(3 * i + 2, v.get(3 * i + 2) + a.z);
			}
			return;
		} else if (o instanceof Vector2) {
			Vector2 a = ((Vector2) o);
			for (int i = 0; i < vertexCursor; i++) {
				v.put(3 * i + 0, v.get(3 * i + 0) + a.x);
				v.put(3 * i + 1, v.get(3 * i + 1) + a.y);
			}
			return;
		} else if (o instanceof Quaternion) {
			Quaternion a = ((Quaternion) o);
			for (int i = 0; i < vertexCursor; i++) {
				Vector3 b = a.rotateVector(new Vector3(v.get(3 * i + 0), v.get(3 * i + 1), v.get(3 * i + 2)));
				v.put(3 * i + 0, b.x);
				v.put(3 * i + 1, b.y);
				v.put(3 * i + 2, b.z);
			}
			return;
		}
		throw Py.AttributeError(" can't add <" + o + "> to mesh contents");
	}

	static protected void transformSub(FloatBuffer v, int vertexCursor, Object o) {
		if (o instanceof Vector3) {
			Vector3 a = ((Vector3) o);
			for (int i = 0; i < vertexCursor; i++) {
				v.put(3 * i + 0, v.get(3 * i + 0) - a.x);
				v.put(3 * i + 1, v.get(3 * i + 1) - a.y);
				v.put(3 * i + 2, v.get(3 * i + 2) - a.z);
			}
			return;
		} else if (o instanceof Vector2) {
			Vector2 a = ((Vector2) o);
			for (int i = 0; i < vertexCursor; i++) {
				v.put(3 * i + 0, v.get(3 * i + 0) - a.x);
				v.put(3 * i + 1, v.get(3 * i + 1) - a.y);
			}
			return;
		} else if (o instanceof Quaternion) {
			Quaternion a = ((Quaternion) o);
			a = a.inverse(new Quaternion());
			for (int i = 0; i < vertexCursor; i++) {
				Vector3 b = a.rotateVector(new Vector3(v.get(3 * i + 0), v.get(3 * i + 1), v.get(3 * i + 2)));
				v.put(3 * i + 0, b.x);
				v.put(3 * i + 1, b.y);
				v.put(3 * i + 2, b.z);
			}
			return;
		}
		throw Py.AttributeError(" can't add <" + o + "> to mesh contents");
	}

	static protected void transformMul(FloatBuffer v, int vertexCursor, Object o) {
		if (o instanceof Vector3) {
			Vector3 a = ((Vector3) o);
			for (int i = 0; i < vertexCursor; i++) {
				v.put(3 * i + 0, v.get(3 * i + 0) * a.x);
				v.put(3 * i + 1, v.get(3 * i + 1) * a.y);
				v.put(3 * i + 2, v.get(3 * i + 2) * a.z);
			}
			return;
		} else if (o instanceof Vector2) {
			Vector2 a = ((Vector2) o);
			for (int i = 0; i < vertexCursor; i++) {
				v.put(3 * i + 0, v.get(3 * i + 0) * a.x);
				v.put(3 * i + 1, v.get(3 * i + 1) * a.y);
			}
			return;
		} else if (o instanceof Quaternion) {
			Quaternion a = ((Quaternion) o);
			for (int i = 0; i < vertexCursor; i++) {
				Vector3 b = a.rotateVector(new Vector3(v.get(3 * i + 0), v.get(3 * i + 1), v.get(3 * i + 2)));
				v.put(3 * i + 0, b.x);
				v.put(3 * i + 1, b.y);
				v.put(3 * i + 2, b.z);
			}
			return;
		}
		throw Py.AttributeError(" can't add <" + o + "> to mesh contents");
	}

	static public PyObject appendToMesh(DynamicMesh mesh, Object add) {
		if (add instanceof Vector3) {
			int r = mesh.nextVertex((Vector3) add);
			return Py.java2py(r);
		}

		if (add instanceof Vector2) {
			int r = mesh.nextVertex(((Vector2) add).toVector3());
			return Py.java2py(r);
		}

		if (add instanceof PyDictionary) {
			Set<Entry> e = ((PyDictionary) add).entrySet();

			Object vertex = ((PyDictionary) add).get(0);
			PyObject appended = appendToMesh(mesh, vertex);
			if (appended instanceof List) {
				for (int i = 0; i < ((List) appended).size(); i++) {
					Object o = ((List) appended).get(i);
					int n = ((Number) PythonUtils.maybeToJava(o)).intValue();
					applyProperty(e, mesh, n);
				}
				return appended;
			} else if (appended instanceof PyInteger) {
				applyProperty(e, mesh, ((PyInteger) appended).getValue());
				return appended;
			}
		}

		if (add instanceof List) {
			List tuple = ((List) add);
			int s = tuple.size();
			if (s == 0)
				return Py.java2py(mesh);

			PyList list = new PyList();
			for (int n = 0; n < tuple.size(); n++)
				list.append(appendToMesh(mesh, PythonUtils.maybeToJava(tuple.get(n))));

			return list;
		}

		if (add instanceof Integer || add instanceof Long) {
			long x = ((Number) add).longValue();
			mesh.nextFace((int) x);
		}

		return Py.java2py(mesh);
	}

	static public PyObject appendToMesh_long(DynamicMesh_long mesh, Object add) {
		if (add instanceof Vector3) {
			int r = mesh.nextVertex((Vector3) add);
			return Py.java2py(r);
		}

		if (add instanceof Vector2) {
			int r = mesh.nextVertex(((Vector2) add).toVector3());
			return Py.java2py(r);
		}

		if (add instanceof PyDictionary) {
			Set<Entry> e = ((PyDictionary) add).entrySet();

			Object vertex = ((PyDictionary) add).get(0);
			PyObject appended = appendToMesh_long(mesh, vertex);
			if (appended instanceof List) {
				for (int i = 0; i < ((List) appended).size(); i++) {
					Object o = ((List) appended).get(i);
					int n = ((Number) PythonUtils.maybeToJava(o)).intValue();
					applyProperty_long(e, mesh, n);
				}
				return appended;
			} else if (appended instanceof PyInteger) {
				applyProperty_long(e, mesh, ((PyInteger) appended).getValue());
				return appended;
			}
		}

		if (add instanceof List) {
			List tuple = ((List) add);
			int s = tuple.size();
			if (s == 0)
				return Py.java2py(mesh);

			PyList list = new PyList();
			for (int n = 0; n < tuple.size(); n++)
				list.append(appendToMesh_long(mesh, PythonUtils.maybeToJava(tuple.get(n))));

			return list;
		}

		if (add instanceof Integer || add instanceof Long) {
			long x = ((Number) add).longValue();
			mesh.nextFace((int) x);
		}

		return Py.java2py(mesh);
	}

	private static void applyProperty(Set<Entry> e, DynamicMesh mesh, int vertex) {
		for (Entry ee : (Set<Entry>) e) {
			Object k = ee.getKey();
			Object v = ee.getValue();

			int i = ((Number) k).intValue();
			if (i == 0)
				continue;
			if (v instanceof Vector4) {
				float x = ((Vector4) v).x;
				float y = ((Vector4) v).y;
				float z = ((Vector4) v).z;
				float a = ((Vector4) v).w;
				mesh.setAux(vertex, i, x, y, z, a);
			}
			if (v instanceof Vector3) {
				float x = ((Vector3) v).x;
				float y = ((Vector3) v).y;
				float z = ((Vector3) v).z;
				mesh.setAux(vertex, i, x, y, z);
			}
			if (v instanceof Vector2) {
				float x = ((Vector2) v).x;
				float y = ((Vector2) v).y;
				mesh.setAux(vertex, i, x, y);
			}

			if (v instanceof Number) {
				float x = ((Number) v).floatValue();
				mesh.setAux(vertex, i, x);
			}
		}
	}

	private static void applyProperty_long(Set<Entry> e, DynamicMesh_long mesh, int vertex) {
		for (Entry ee : (Set<Entry>) e) {
			Object k = ee.getKey();
			Object v = ee.getValue();

			int i = ((Number) k).intValue();
			if (i == 0)
				continue;
			if (v instanceof Vector4) {
				float x = ((Vector4) v).x;
				float y = ((Vector4) v).y;
				float z = ((Vector4) v).z;
				float a = ((Vector4) v).w;
				mesh.setAux(vertex, i, x, y, z, a);
			}
			if (v instanceof Vector3) {
				float x = ((Vector3) v).x;
				float y = ((Vector3) v).y;
				float z = ((Vector3) v).z;
				mesh.setAux(vertex, i, x, y, z);
			}
			if (v instanceof Vector2) {
				float x = ((Vector2) v).x;
				float y = ((Vector2) v).y;
				mesh.setAux(vertex, i, x, y);
			}

			if (v instanceof Number) {
				float x = ((Number) v).floatValue();
				mesh.setAux(vertex, i, x);
			}
		}
	}

	static public iSceneListElement toSceneListElement(Object o) {
		if (o instanceof iSceneListElement)
			return (iSceneListElement) o;
		if (o instanceof DynamicMesh)
			return ((DynamicMesh) o).getUnderlyingGeometry();
		if (o instanceof DynamicMesh_long)
			return ((DynamicMesh_long) o).getUnderlyingGeometry();
		if (o instanceof Destination2)
			return ((Destination2) o).fbo;
		throw Py.NotImplementedError("cant convert <" + o + "> to scene list element");
	}

	static public final VisualElementProperty<String> vertexShader = new VisualElementProperty<String>("vertexShader_v");
	static public final VisualElementProperty<String> fragmentShader = new VisualElementProperty<String>("fragmentShader_v");
	static public final VisualElementProperty<String> geometryShader = new VisualElementProperty<String>("geometryShader_v");
	static public final VisualElementProperty<String> tessEvalShader = new VisualElementProperty<String>("tessEvalShader_v");
	static public final VisualElementProperty<String> tessControlShader = new VisualElementProperty<String>("tessControlShader_v");

	static {
		PythonPluginEditor.knownPythonProperties.put("<b>Vertex Shader</b> - <font size=-2>vertexShader_v</font>", vertexShader);
		if (CoreHelpers.isCore) {
			PythonPluginEditor.knownPythonProperties.put("<b>Tessellation Control Shader</b> - <font size=-2>tessControlShader_v</font>", tessControlShader);
			PythonPluginEditor.knownPythonProperties.put("<b>Tessellation Evaluation Shader</b> - <font size=-2>tessEvalShader_v</font>", tessEvalShader);
		}
		PythonPluginEditor.knownPythonProperties.put("<b>Geometry Shader</b> - <font size=-2>geometryShader_v</font>", geometryShader);
		PythonPluginEditor.knownPythonProperties.put("<b>Fragment Shader</b> - <font size=-2>fragmentShader_v</font>", fragmentShader);
	}

	@SuppressWarnings("unchecked")
	static public BasicGLSLangProgram makeShaderFromElement(final iVisualElement element) {

		final BasicGLSLangProgram program = new BasicGLSLangProgram();

		String vs = vertexShader.get(element);
		;//;//System.out.println(" initial vs is <" + vs + ">");
		if (vs == null || vs.trim().length() == 0) {

			vs = "varying vec4 vertexColor;\n" + "attribute vec4 s_Color;\n" + "\n" + "void main()\n" + "{\n" + "\tgl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n" + "\tvertexColor = s_Color;\n" + "}";
			if (CoreHelpers.isCore) {
				vs = "#version 150\n" + "\n" + "in vec3 position;\n" + "\n" + "uniform mat4 _projMatrix;\n" + "uniform mat4 _viewMatrix;\n" + "\n" + "out vec4 vertexColor;\n" + "in vec4 s_Color;\n" + "\n" + "void main()\n" + "{\n" + "	gl_Position = _projMatrix * (_viewMatrix * vec4(position, 1.0));\n" + "\n" + "	vertexColor = s_Color;\n" + "}";
			}
			vertexShader.set(element, element, vs);
		}
		
		;//;//System.out.println(" initial vs is <" + vs + ">");

		String fs = fragmentShader.get(element);
		;//;//System.out.println(" initial fs is <" + fs + ">");
		if (fs == null || fs.trim().length() == 0) {

			fs = "varying vec4 vertexColor;\n" + "\n" + "void main()\n" + "{\n" + "\tgl_FragColor = vertexColor+vec4(0.1, 0.1, 0.1, 0.1);\n" + "}\n";
			if (CoreHelpers.isCore) {
				fs = "#version 150\n" + "\n" + "in vec4 vertexColor;\n" + "out vec4 _output;\n" + "\n" + "void main()\n" + "{\n" + "	_output  = vertexColor+vec4(0.1, 0.1, 0.1, 0.1);\n" + "}\n" + "";
			}
			fragmentShader.set(element, element, fs);
		}

		;//;//System.out.println(" initial fs is <" + fs + ">");

		final Stack<Writer> redir = new Stack<Writer>();
		redir.addAll(PythonInterface.getPythonInterface().getErrorRedirects());
		final Stack<Writer> reodir = new Stack<Writer>();
		reodir.addAll(PythonInterface.getPythonInterface().getOutputRedirects());
		final BasicGLSLangElement e1 = program.new BasicGLSLangElement(vertexShader.get(element), BasicGLSLangProgram.ElementType.vertex);
		final BasicGLSLangElement e2 = program.new BasicGLSLangElement(geometryShader.get(element), BasicGLSLangProgram.ElementType.geometry);
		final BasicGLSLangElement e3 = program.new BasicGLSLangElement(fragmentShader.get(element), BasicGLSLangProgram.ElementType.fragment);
		final BasicGLSLangElement e4 = program.new BasicGLSLangElement(tessControlShader.get(element), BasicGLSLangProgram.ElementType.tessControl);
		final BasicGLSLangElement e5 = program.new BasicGLSLangElement(tessEvalShader.get(element), BasicGLSLangProgram.ElementType.tessEval);

		PythonPluginEditor.python_customToolbar.addToList(ArrayList.class, element, new Pair<String, iUpdateable>("Refresh shader", new iUpdateable() {
			public void update() {
				;//;//System.out.println(" refreshing shader ");

				program.deferReload();
				e1.reload(vertexShader.get(element), new BasicGLSLangProgram.iErrorHandler() {

					public void beginError() {
						if (redir.size() > 0) {
							Writer e = redir.peek();
							try {
								e.write(" Errors occured on vertex shader reload \n");
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						} else {
							System.err.println(" errors on vertex shader reload ");
						}
					}

					public void endError() {
					}

					public void errorOnLine(int line, String error) {
						Writer e = redir.peek();
						try {
							e.write("on line " + line + " '" + error + "\n");
						} catch (IOException e1) {
							e1.printStackTrace();
						}

					}

					public void noError() {
						if (reodir.size() > 0) {
							Writer e = reodir.peek();
							try {
								e.write(" Reloaded vertex shader successfully \n");
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						} else {
							System.err.println(" Reloaded vertex shader successfully \n");
						}
					}

				});
				
				if (CoreHelpers.isCore)
					e4.reload(tessControlShader.get(element), new BasicGLSLangProgram.iErrorHandler() {

						public void beginError() {
							if (redir.size() > 0) {
								Writer e = redir.peek();
								try {
									e.write(" Errors occured on tess control shader reload \n");
								} catch (IOException e1) {
									e1.printStackTrace();
								}
							} else {
								System.err.println(" errors on tess control shader reload ");
							}
						}

						public void endError() {
						}

						public void errorOnLine(int line, String error) {
							Writer e = redir.peek();
							try {
								e.write("on line " + line + " '" + error + "\n");
							} catch (IOException e1) {
								e1.printStackTrace();
							}

						}

						public void noError() {
							if (reodir.size() > 0) {
								Writer e = reodir.peek();
								try {
									e.write(" Reloaded tess control shader successfully \n");
								} catch (IOException e1) {
									e1.printStackTrace();
								}
							} else {
								System.err.println(" Reloaded tess control shader successfully \n");
							}
						}

					});
				if (CoreHelpers.isCore)
					e5.reload(tessEvalShader.get(element), new BasicGLSLangProgram.iErrorHandler() {

						public void beginError() {
							if (redir.size() > 0) {
								Writer e = redir.peek();
								try {
									e.write(" Errors occured on tess eval shader reload \n");
								} catch (IOException e1) {
									e1.printStackTrace();
								}
							} else {
								System.err.println(" errors on tess eval shader reload ");
							}
						}

						public void endError() {
						}

						public void errorOnLine(int line, String error) {
							Writer e = redir.peek();
							try {
								e.write("on line " + line + " '" + error + "\n");
							} catch (IOException e1) {
								e1.printStackTrace();
							}

						}

						public void noError() {
							if (reodir.size() > 0) {
								Writer e = reodir.peek();
								try {
									e.write(" Reloaded tess control eval successfully \n");
								} catch (IOException e1) {
									e1.printStackTrace();
								}
							} else {
								System.err.println(" Reloaded tess control eval successfully \n");
							}
						}

					});
					
				e2.reload(geometryShader.get(element), new BasicGLSLangProgram.iErrorHandler() {

					public void beginError() {
						if (redir.size() > 0) {
							Writer e = redir.peek();
							try {
								e.write(" Errors occured on geometry shader reload \n");
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						} else {
							System.err.println(" errors on geometry shader reload ");
						}
					}

					public void endError() {
					}

					public void errorOnLine(int line, String error) {
						Writer e = redir.peek();
						try {
							e.write("on line " + line + " '" + error + "\n");
						} catch (IOException e1) {
							e1.printStackTrace();
						}

					}

					public void noError() {
						if (reodir.size() > 0) {
							Writer e = reodir.peek();
							try {
								e.write(" Reloaded geometry shader successfully \n");
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						} else {
							System.err.println(" Reloaded geometry shader successfully \n");
						}
					}

				});
				e3.reload(fragmentShader.get(element), new BasicGLSLangProgram.iErrorHandler() {

					public void beginError() {
						if (redir.size() > 0) {
							Writer e = redir.peek();
							try {
								e.write(" Errors occured on fragment shader reload \n");
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						} else {
							System.err.println(" errors on fragment shader reload ");
						}
					}

					public void endError() {
					}

					public void errorOnLine(int line, String error) {
						Writer e = redir.peek();
						try {
							e.write("on line " + line + " '" + error + "\n");
						} catch (IOException e1) {
							e1.printStackTrace();
						}

					}

					public void noError() {
						if (reodir.size() > 0) {
							Writer e = reodir.peek();
							try {
								e.write(" Reloaded fragment shader successfully \n");
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						} else {
							System.err.println(" Reloaded fragment shader successfully \n");
						}
					}

				});

				iVisualElement.dirty.set(element, element, true);
			}
		}));

		return program;
	}

	static public Object makeTextureForArray(float[][] f) {
		return Conversions.makeFloatTexture(f);
	}

	static public Object makeTextureForArray(float[] f, int width) {
		return Conversions.makeFloatTexture(f, width, f.length / width);
	}

	static public Object makeTextureForArray(int[][] f) {
		return Conversions.makeFloatTexture(f);
	}

	DirectMesh directMesh = new DirectMesh();
	DirectLine directLine = new DirectLine();
	DirectPoint directPoint = new DirectPoint();

	protected void wire(iAcceptsSceneListElement s, Object w) {

		if (s instanceof DynamicLine_long && w instanceof CachedLine) {
			wireLine(((DynamicLine_long) s), ((CachedLine) w));
		} else if (s instanceof DynamicPointlist && w instanceof CachedLine) {
			wirePoint(((DynamicPointlist) s), ((CachedLine) w));
		} else if (s instanceof DynamicMesh_long && w instanceof CachedLine) {
			wireLine_mesh(((DynamicMesh_long) s), ((CachedLine) w));
		} else if (w instanceof CachedLineLayer) {

			if (s instanceof DynamicLine_long)
				wire(s, ((CachedLineLayer) w).new UpdateLine((DynamicLine_long) s, directLine));
			else if (s instanceof DynamicMesh_long)
				wire(s, ((CachedLineLayer) w).new UpdateMesh((DynamicMesh_long) s, directMesh));

		} else {
			iSceneListElement e = toSceneListElement(w);
			s.addChild(e);
		}
	}

	private void wireLine(DynamicLine_long dynamicLine_long, CachedLine cachedLine) {
		directLine.wire(dynamicLine_long, cachedLine);
	}

	private void wirePoint(DynamicPointlist dynamicLine_long, CachedLine cachedLine) {
		directPoint.wire(dynamicLine_long, cachedLine);
	}

	private void wireLine_mesh(DynamicMesh_long dynamicLine_long, CachedLine cachedLine) {
		directMesh.wire(dynamicLine_long, cachedLine);
	}

	private void unwireLine(DynamicLine_long dynamicLine_long, CachedLine cachedLine) {
		directLine.unwire(dynamicLine_long, cachedLine);
	}

	private void unwireLine_mesh(DynamicMesh_long dynamicLine_long, CachedLine cachedLine) {
		directMesh.unwire(dynamicLine_long, cachedLine);
	}

	private void unwirePoint(DynamicPointlist dynamicLine_long, CachedLine cachedLine) {
		directPoint.unwire(dynamicLine_long, cachedLine);
	}

	protected void unwire(iAcceptsSceneListElement s, Object w) {
		if (s instanceof DynamicLine_long && w instanceof CachedLine) {
			unwireLine(((DynamicLine_long) s), ((CachedLine) w));
		} else if (s instanceof DynamicPointlist && w instanceof CachedLine) {
			unwirePoint(((DynamicPointlist) s), ((CachedLine) w));
		} else if (s instanceof DynamicMesh_long && w instanceof CachedLine) {
			unwireLine_mesh(((DynamicMesh_long) s), ((CachedLine) w));
		} else if (s instanceof CachedLineLayer) {
		} else {
			iSceneListElement e = toSceneListElement(w);
			s.removeChild(e);
		}
	}

}
