package field.extras.graphics;

import org.python.core.Py;
import org.python.core.PyBuiltinMethodNarrow;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.core.PyType;

import field.graphics.core.Base.iGeometry;
import field.graphics.core.Base.iLongGeometry;
import field.graphics.core.BasicGeometry.LineList;
import field.graphics.core.BasicGeometry.LineList_long;
import field.graphics.core.BasicGeometry.TriangleMesh;
import field.graphics.core.BasicGeometry.TriangleMesh_long;
import field.graphics.jfbxlib.HierarchyOfCoordinateFrames.Element;
import field.graphics.jfbxlib.Loader2;
import field.math.abstraction.iInplaceProvider;
import field.math.linalg.CoordinateFrame;
import field.math.linalg.iCoordinateFrame;
import field.math.linalg.iCoordinateFrame.iMutable;

/**
 * a nice, flexible, pythonic interface for loading, playing and transforming,
 * FBX files
 * 
 * @author marc
 * 
 */
public class FieldJFBXExtensions {

	static {
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

					String n = Py.tojava(name, String.class);
					if (n.equals("transform")) {

						iGeometry s = Py.tojava(self, iGeometry.class);

						iInplaceProvider<iMutable> p = s.getCoordinateProvider();

						return Py.java2py(p);
					} else if (n.equals("worldTransform")) {
						iGeometry s = Py.tojava(self, iGeometry.class);

						iInplaceProvider p = s.getCoordinateProvider();

						if (p instanceof Element) {
							return Py.java2py(((Element) p).get(null));
						} else {

							return Py.java2py(p);
						}
					} else if (n.equals("localTransform")) {
						iGeometry s = Py.tojava(self, iGeometry.class);

						iInplaceProvider p = s.getCoordinateProvider();

						if (p instanceof Element) {
							return Py.java2py(((Element) p).getLocal());
						} else {

							return Py.java2py(p);
						}
					}
					throw Py.AttributeError("geometry has no attribute <" + n + ">");
				}
			};
			PyType.fromClass(iGeometry.class).addMethod(meth);
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

					String n = Py.tojava(name, String.class);
					if (n.equals("transform")) {

						iGeometry s = Py.tojava(self, iLongGeometry.class);

						iInplaceProvider<iMutable> p = s.getCoordinateProvider();

						return Py.java2py(p);
					} else if (n.equals("worldTransform")) {
						iGeometry s = Py.tojava(self, iLongGeometry.class);

						iInplaceProvider p = s.getCoordinateProvider();

						if (p instanceof Element) {
							return Py.java2py(((Element) p).get(null));
						} else {

							return Py.java2py(p);
						}
					} else if (n.equals("localTransform")) {
						iGeometry s = Py.tojava(self, iLongGeometry.class);

						iInplaceProvider p = s.getCoordinateProvider();

						if (p instanceof Element) {
							return Py.java2py(((Element) p).getLocal());
						} else {

							return Py.java2py(p);
						}
					}
					throw Py.AttributeError("geometry has no attribute <" + n + ">");
				}
			};
			PyType.fromClass(TriangleMesh_long.class).addMethod(meth);
			PyType.fromClass(LineList_long.class).addMethod(meth);
			PyType.fromClass(TriangleMesh.class).addMethod(meth);
			PyType.fromClass(LineList.class).addMethod(meth);
		}

		{
			final PyObject objectGetattribute = PyObject.TYPE.__getattr__("__setattr__");
			PyBuiltinMethodNarrow meth = new PyBuiltinMethodNarrow("__setattr__", 2) {
				@Override
				public PyObject __call__(PyObject name, PyObject value) {
					PyException was;
					try {
						return objectGetattribute.__call__(self, name, value);
					} catch (PyException e) {
						was = e;
					}

					String n = Py.tojava(name, String.class);
					iCoordinateFrame f = Py.tojava(value, iCoordinateFrame.class);

					if (n.equals("localTransform")) {
						iGeometry s = Py.tojava(self, iLongGeometry.class);

						iInplaceProvider p = s.getCoordinateProvider();

						if (p instanceof Element) {
							((Element) p).setLocal((CoordinateFrame) f);
							return Py.None;
						} else {
						}
					}
					throw was;
				}
			};
			PyType.fromClass(TriangleMesh_long.class).addMethod(meth);
			PyType.fromClass(LineList_long.class).addMethod(meth);
			PyType.fromClass(TriangleMesh.class).addMethod(meth);
			PyType.fromClass(LineList.class).addMethod(meth);
		}
	}

	public FieldJFBXExtensions() {

	}

	static public Loader2 loadFBX(String filename) {
		return new Loader2(filename);
	}

	static public Loader2 loadFBX(String filename, int take) {
		return new Loader2(filename, take);
	}

}
