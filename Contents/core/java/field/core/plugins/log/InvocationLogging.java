package field.core.plugins.log;

import org.python.core.PyClass;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.core.PyString;

public class InvocationLogging {

	// public interface iMissingProperty {
	// public PyObject get(PyObject in, String name);
	//
	// public void set(PyObject in, String name, PyObject next, String
	// parentName, PyObject value);
	// }
	//
	// static public HashMap<String, iMissingProperty> missingProperty = new
	// HashMap<String, iMissingProperty>();
	//
	// static {
	// missingProperty.put("banana", new iMissingProperty() {
	//
	// public PyObject get(PyObject in, String name) {
	// return in;
	// }
	//
	// public void set(PyObject in, String name, PyObject next, String
	// parentName, PyObject value) {
	// next.__setattr__(new PyString(parentName), value);
	// }
	// });
	// }

	public interface iTypeErrorRecovery {
		public void recover(Object javaObject, String field, Object setToBe);
	}

	boolean defaultTypeRecoveryForces = false;

	public void linkCall(PyObject in, PyObject args, String path, boolean isRoot, Object result, PyObject from) {
	}

	public void linkCallFinish(PyObject parent, PyObject newChild) {
	}

	public PyObject linkGetAttr(PyObject in, String name, boolean isRoot, String path, PyObject parent) {

		PyObject r = in.__getattr__(name);
		return r;
	}

	public void linkGetAttrFinish(PyObject parent, PyObject newChild) {
	}

	public void linkSetAttr(PyObject in, String name, PyObject value, boolean isRoot, String path, PyObject from) {
		linkSetAttr(in, name, value, isRoot, path, from, getDefaultTypeRecovery(), defaultTypeRecoveryForces);
	}

	public void linkSetAttr(PyObject in, String name, PyObject value, boolean isRoot, String path, PyObject from, iTypeErrorRecovery er, boolean forceRecovery) {
		if (forceRecovery) {
			
			//beta1
//			if (in instanceof PyJavaInstance) 
			if (in instanceof PyObject) 
			{
				Object j = in.__tojava__(Object.class);
				if (er != null)
					er.recover(j, name, value.__tojava__(Object.class));
				return;
			}
		}

		try {
			in.__setattr__(new PyString(name), value);
		} catch (PyException e) {
			//beta1
//			if (in instanceof PyJavaInstance) 
			if (in instanceof PyObject) 
			{
				Object j = in.__tojava__(Object.class);
				if (er != null)
					er.recover(j, name, value.__tojava__(Object.class));
			} else {
				throw e;
			}
		}
	}

	public PyObject lookup(PyClass in, String name) {
		return in.__findattr__(name);
	}

	protected iTypeErrorRecovery getDefaultTypeRecovery() {
		return null;
	}
}
