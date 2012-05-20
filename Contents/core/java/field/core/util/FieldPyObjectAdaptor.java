package field.core.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyFloat;
import org.python.core.PyJavaType;
import org.python.core.PyObject;
import org.python.core.PyObjectDerived;
import org.python.core.PyString;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.core.adapter.ClassAdapter;
import org.python.core.adapter.ExtensiblePyObjectAdapter;
import org.python.core.adapter.PyObjectAdapter;

import field.core.StandardFluidSheet.RootSheetElement;
import field.core.dispatch.VisualElement;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.plugins.drawing.SplineComputingOverride;
import field.core.plugins.pseudo.PseudoPropertiesPlugin;
import field.core.plugins.python.PythonPlugin;
import field.core.ui.text.PythonTextEditor;
import field.core.ui.text.protect.ClassDocumentationProtect.Comp;
import field.math.abstraction.iHasScalar;
import field.namespace.generic.Generics.Pair;
import field.util.Dict;
import field.util.Dict.Prop;

public class FieldPyObjectAdaptor {

	static public class CallableToCallableJavaInstance implements PyObjectAdapter {

		public CallableToCallableJavaInstance() {
		}

		public PyObject adapt(Object o) {
			return new PyCallableJavaInstance((iCallable) o);
		}

		public boolean canAdapt(Object o) {
			return o instanceof iCallable;
		}

	}

	static public class CallableAndAttributesToCallableAndAttributesJavaInstance implements PyObjectAdapter {

		public CallableAndAttributesToCallableAndAttributesJavaInstance() {
		}

		public PyObject adapt(Object o) {
			return new PyCallableAndAttributesJavaInstance((iHandlesAttributesAndCallable) o);
		}

		public boolean canAdapt(Object o) {
			return o instanceof iHandlesAttributesAndCallable;
		}

	}

	static public class DictToPyFieldDict extends ClassAdapter {

		public DictToPyFieldDict() {
			super(Dict.class);
		}

		public PyObject adapt(Object o) {
			return new PyFieldDict((Dict) o);
		}

	}

	static public class ExtensibleToExtensibleJavaInstance implements PyObjectAdapter {

		public ExtensibleToExtensibleJavaInstance() {
		}

		public PyObject adapt(Object o) {
			return new PyExtensibleJavaInstance((iExtensible) o);
		}

		public boolean canAdapt(Object o) {
			return o instanceof iExtensible;
		}

	}

	static public class HandlesAttributesToPyHandlesAttributes implements PyObjectAdapter {

		public HandlesAttributesToPyHandlesAttributes() {
		}

		public PyObject adapt(Object o) {
			return new PyHandlesAttributes((iHandlesAttributes) o);
		}

		public boolean canAdapt(Object o) {
			return o instanceof iHandlesAttributes;
		}

	}

	static public class HasScalarToPyHasScalar implements PyObjectAdapter {

		public HasScalarToPyHasScalar() {
		}

		public PyObject adapt(Object o) {
			return new PyHasScalar((iHasScalar) o);
		}

		public boolean canAdapt(Object o) {
			return o instanceof iHasScalar;
		}

	}

	public interface iCallable {
		public Object call(Object[] args);
	}

	public interface iCallable_keywords extends iCallable{
		public Object callWithKeywords(Object[] args, Map<String, Object> kw);
	}

	
	public interface iExtensible {
		public Dict getDict();
	}

	public interface iHandlesAttributes {
		public Object getAttribute(String name);

		public void setAttribute(String name, Object value);
	}

	public interface iHandlesDeletionOfAttributes {
		public void deleteAttribute(String name);
	}
	
	public interface iHandlesAttributesAndCallable {
		public Object getAttribute(String name);

		public void setAttribute(String name, Object value);

		public Object call(Object[] args);
	}

	public interface iHandlesFindItem extends iHandlesAttributes {
		public Object getItem(Object object);

		public void setItem(Object name, Object value);
	}

	static public class PairToPyTuple extends ClassAdapter {

		public PairToPyTuple() {
			super(Pair.class);
		}

		public PairToPyTuple(Class<? extends iVisualElement> c) {
			super(c);
		}

		public PyObject adapt(Object o) {
			Pair p = (Pair) o;
			return new PyTuple(new PyObject[] { Py.java2py(p.left), Py.java2py(p.right) });
		}

	}

	static public class PyCallableJavaInstance extends PyObjectDerived {
		iCallable contents;

		public PyCallableJavaInstance(iCallable e) {
			super(PyType.fromClass(e.getClass()));
			this.contents = e;
			this.javaProxy = e;
		}

		@Override
		public PyObject __call__(PyObject[] args, String[] keywords) {
			Object[] x = new Object[args.length];
			for (int i = 0; i < x.length; i++) {
				x[i] = args[i].__tojava__(Object.class);
			}

			Object r = contents.call(x);
			PyObject p = Py.java2py(r);
			return p;
		}

	}

	static public class PyCallableAndAttributesJavaInstance extends PyObjectDerived {
		iHandlesAttributesAndCallable contents;

		public PyCallableAndAttributesJavaInstance(iHandlesAttributesAndCallable e) {
			super(PyType.fromClass(e.getClass()));
			this.contents = e;
			this.javaProxy = contents;

		}

		@Override
		public PyObject __call__(PyObject[] args, String[] keywords) {
			Object[] x = new Object[args.length];
			for (int i = 0; i < x.length; i++) {
				x[i] = args[i].__tojava__(Object.class);
			}

			Object r = contents.call(x);
			PyObject p = Py.java2py(r);
			return p;
		}

		@Override
		public PyObject __findattr_ex__(String name) {
			PyObject alt = null;
			try {
				alt = super.__findattr_ex__(name);
			} catch (PyException e) {
			}
			if (alt == null) {
				Object o = contents.getAttribute(name);
				if (o == null)
					return null;
				return Py.java2py(o);
			}
			return alt;
		}

		// beta1
		// @Override
		// protected PyObject ifindfunction(String name) {
		// PyObject found = super.ifindfunction(name);
		// if (found == null) {
		// Object o = contents.getAttribute(name);
		// if (o == null)
		// return null;
		// return Py.java2py(o);
		// }
		// return found;
		// }

		@Override
		public PyObject __finditem__(PyObject key) {
			if (contents instanceof iHandlesFindItem) {
				Object x = ((iHandlesFindItem) contents).getItem(key.__tojava__(Object.class));
				return Py.java2py(x);
			} else
				return super.__finditem__(key);
		}

		@Override
		public PyObject __getitem__(PyObject key) {
			return __finditem__(key);
		}

		@Override
		public void __setattr__(String name, PyObject value) {
			if (super.__findattr__(name) != null) {
				super.__setattr__(name, value);
			} else {
				contents.setAttribute(name, value.__tojava__(Object.class));
			}
		}

		@Override
		public void __setitem__(PyObject key, PyObject value) {
			if (contents instanceof iHandlesFindItem) {
				((iHandlesFindItem) contents).setItem(key.__tojava__(Object.class), value.__tojava__(Object.class));
			} else
				super.__setitem__(key, value);
		}

		@Override
		public Object __tojava__(Class arg0) {
			return contents;
		}
	}

	static public class PyExtensibleJavaInstance extends PyObject {
		iExtensible contents;
		PyObjectDerived instance;

		public PyExtensibleJavaInstance(iExtensible e) {
			super();
			this.contents = e;
			instance = (PyObjectDerived) PyJavaType.wrapJavaObject(e);
		}

		@Override
		public PyObject __findattr_ex__(String name) {
			PyObject alt = null;
			try {
				alt = instance.__findattr_ex__(name);
			} catch (PyException e) {
			}
			;
			if (alt == null) {
				Dict d = contents.getDict();
				Object o = d.get(new Prop(name));
				if (o == null)
					return Py.None;
				return Py.java2py(o);
			}
			return alt;
		}

		@Override
		public void __setattr__(String name, PyObject value) {
			if (instance.__findattr__(name) != null) {
				instance.__setattr__(name, value);
			} else {
				Dict d = contents.getDict();
				d.put(new Prop(name), value.__tojava__(Object.class));
			}
		}

		@Override
		public Object __tojava__(Class c) {
			return contents;
		}

		@Override
		public String toString() {
			return contents.toString();
		}

	}

	static public class PyFieldDict extends PyObject {
		Dict contents;
		PyObjectDerived instance;

		public PyFieldDict(Dict e) {
			this.contents = e;
			instance = (PyObjectDerived) PyJavaType.wrapJavaObject(e);
		}

		@Override
		public PyObject __findattr_ex__(String name) {
			PyObject alt = null;
			try {
				alt = instance.__findattr_ex__(name);
			} catch (PyException e) {
			}
			if (alt == null) {
				Object o = contents.get(new Prop(name));
				if (o == null)
					return Py.None;
				return Py.java2py(o);
			}
			return Py.None;
		}

		@Override
		public void __setattr__(String name, PyObject value) {
			contents.put(new Prop(name), value.__tojava__(Object.class));
		}

		@Override
		public Object __tojava__(Class c) {
			return contents;
		}

	}

	static public class PyHandlesAttributes extends PyObject {
		iHandlesAttributes contents;
		PyObjectDerived instance;

		public PyHandlesAttributes(iHandlesAttributes e) {
			this.contents = e;
			instance = (PyObjectDerived) PyJavaType.wrapJavaObject(e);
		}

		@Override
		public PyObject __findattr_ex__(String name) {

			PyObject alt = null;
			
			try {
				alt = instance.__findattr_ex__(name);

			} catch (PyException e) {
			}
			
			if (alt == null) {
				Object o = contents.getAttribute(name);
				if (o == null)
					return null;
				return Py.java2py(o);
			}
			return alt;
		}

		@Override
		public PyObject __finditem__(PyObject key) {
			if (contents instanceof iHandlesFindItem) {
				Object x = ((iHandlesFindItem) contents).getItem(key.__tojava__(Object.class));
				return Py.java2py(x);
			} else
				return super.__finditem__(key);
		}

		@Override
		public PyObject __getitem__(PyObject key) {
			return __finditem__(key);
		}

		@Override
		public void __setattr__(String name, PyObject value) {
			if (instance.__findattr__(name) != null) {
				instance.__setattr__(name, value);
			} else {
				contents.setAttribute(name, value.__tojava__(Object.class));
			}
		}

		@Override
		public void __setitem__(PyObject key, PyObject value) {
			if (instance instanceof iHandlesFindItem) {
				((iHandlesFindItem) instance).setItem(key.__tojava__(Object.class), value.__tojava__(Object.class));
			} else
				super.__setitem__(key, value);
		}

		@Override
		public Object __tojava__(Class c) {
			return contents;
		}

	}

	static public class PyHasScalar extends PyFloat {

		private final iHasScalar contents;
		private final PyObjectDerived instance;

		public PyHasScalar(iHasScalar e) {
			super(e.getDoubleValue());
			this.contents = e;
			instance = (PyObjectDerived) PyJavaType.wrapJavaObject(e);
		}

		@Override
		public PyObject __findattr_ex__(String name) {
			return instance.__findattr_ex__(name);
		}

		@Override
		public Object __tojava__(Class c) {
			return contents;
		}

		@Override
		public double getValue() {
			return contents.getDoubleValue();
		}

	}

	static public Map<String, PyObject> injectedSelfMethods = new HashMap<String, PyObject>();

	static public class PyVisualElement extends PyObject {
		iVisualElement contents;

		PyObjectDerived instance;

		public PyVisualElement(iVisualElement e) {
			this.contents = e;
			instance = (PyObjectDerived) PyJavaType.wrapJavaObject(e);
		}

		@Override
		public PyObject __call__() {
			SplineComputingOverride.executeMain(contents);
			return Py.None;
		}

		@Override
		public PyObject __call__(PyObject arg0) {
			String s = (String) arg0.__tojava__(String.class);
			SplineComputingOverride.executeMainWithLabel(contents, s);
			return Py.None;
		}

		@Override
		public PyObject __call__(PyObject[] args, String[] keywords) {
			if (args.length == 0)
				return __call__();
			if (args.length == 1)
				return __call__(args[0]);
			throw new PyException(Py.TypeError, " PyVisualElement not callable with <" + args.length + "> <" + keywords.length + ">");
		}

		@Override
		public boolean isCallable() {
			return true;
		}

		@Override
		public int __cmp__(PyObject other) {
			if (other == this)
				return 0;
			if (other instanceof PyVisualElement)
				return equals(other) ? 0 : 1;
			return -2;
		}
		
		@Override
		public PyObject __findattr_ex__(String name) {

			PyObject alt = null;

			try {
				alt = instance.__findattr_ex__(name);
			} catch (Exception e) {
				// e.printStackTrace();
			}
			;//System.out.println(" alt <" + alt + ">");
			// frame is both a field and
			// something that we want to be
			// a (psuedo) property

			final PyObject found = injectedSelfMethods.get(name);
			if (found != null) {
				return Py.java2py(new iCallable() {

					public Object call(Object[] args) {
						PyObject[] args2 = new PyObject[args.length + 1];
						for (int i = 0; i < args.length; i++) {
							args2[i + 1] = Py.java2py(args[i]);
						}
						args2[0] = PyVisualElement.this;
						return found.__call__(args2);
					}
				});
			}

			if (alt == null || alt == Py.None || alt.__tojava__(Object.class) instanceof VisualElementProperty || name.equals("frame")) {
				Object o = PythonPlugin.getAttr(contents, name);

				;//System.out.println(" parent lookup <" + o + ">");

				if (o == null)
					return Py.None;
				return Py.java2py(o);
			} else
				return alt;
		}

		@Override
		public PyString __repr__() {
			return new PyString(contents.toString());
		}

		@Override
		public void __setattr__(String name, PyObject value) {
			if (name.equals("frame")) {
				contents.getProperty(iVisualElement.overrides).shouldChangeFrame(contents, ((Rect) value.__tojava__(Rect.class)), contents.getFrame(null), true);
			} else
				PythonPlugin.setAttr(contents, contents, name, value.__tojava__(Object.class));
		}

		@Override
		public PyString __str__() {
			return new PyString("!ve: " + contents.getProperty(iVisualElement.name));
		}

		@Override
		public Object __tojava__(Class c) {
			return contents;
		}

		@Override
		public boolean equals(Object ob_other) {
			if (ob_other == null)
				return false;
			if (!(ob_other instanceof PyVisualElement || ob_other instanceof VisualElement))
				return false;
			if (ob_other instanceof PyVisualElement)
				return ((PyVisualElement) ob_other).contents.equals(contents);
			if (ob_other instanceof iVisualElement)
				return ((iVisualElement) ob_other).equals(contents);
			return false;
		}

		@Override
		public int hashCode() {
			return contents.hashCode();
		}

		static public List<Comp> getClassCustomCompletion(String prefix, Object of) {
			PyVisualElement adaptor = ((PyVisualElement) of);
			List<Comp> c = new ArrayList<Comp>();
			if (prefix.length() == 0) {
				c
					.add(new Comp(
						"<h3>The <i>_self</i> variable and other visual element references<hr noshade='none'></h3><div class='wellspacedblack'><i>_self</i> is a special variable that refers to this <i>visual element</i> (i.e. box). It gives you read and write access to <i>properties</i> which are stored in this element or it's parents. A great many things in Field are properties, and understanding them is often the key to managing the power of many visual elements or customizing the behavior of Field.</div>"));
			} else {
				String name = iVisualElement.name.get(adaptor.contents);
				if (name == null)
					name = "unnamed element";
				c.add(new Comp("<h3><i><font color='#555555' >\u2014\u2014</font> " + name + " <font color='#555555' >\u2014\u2014</font></i> . " + prefix + " <font color='#555555' size=+3>\u2041\u2014</font></h3>"));
			}

			List<Comp> ps = new ArrayList<Comp>();
			ps.add(new Comp("", "<i>Psuedo</i>properties (generally read only)").setTitle(true));
			for (VisualElementProperty p : PseudoPropertiesPlugin.properties) {
				if (p.getName().startsWith(prefix)) {
					ps.add(new Comp(p.getName(), PythonTextEditor.limit("" + p.get(adaptor.contents))));
				}
			}
			if (ps.size() > 1)
				c.addAll(ps);
			Map<Object, Object> set = adaptor.contents.payload();
			String groupname = "Already set, local to this element";
			addPropertiesByInspection(prefix, adaptor.contents, c, set, groupname);
			return c;
		}

		private static void addPropertiesByInspection(String prefix, iVisualElement visualElement, List<Comp> c, Map<Object, Object> set, String groupname) {
			if (set.size() > 0) {
				List<Comp> sub = new ArrayList<Comp>();
				Set<Entry<Object, Object>> e = set.entrySet();
				for (Entry<Object, Object> ee : e) {
					VisualElementProperty k = (VisualElementProperty) ee.getKey();
					String name = k.getName();
					if (name.startsWith(prefix)) {
						Object value = ee.getValue();

						Comp cc = new Comp(name, PythonTextEditor.limit("\u2190" + value));
						sub.add(cc);
					}
				}
				if (sub.size() > 0) {
					Comp t = new Comp("", groupname).setTitle(true);
					c.add(t);
					c.addAll(sub);
				}
			}
			List<iVisualElement> childern = visualElement.getChildren();
			if (c != null) {
				for (iVisualElement vv : childern) {
					Map<Object, Object> setp = vv.payload();
					String name = vv.getProperty(iVisualElement.name);
					if (name == null)
						name = vv.getClass().getName();
					addPropertiesByInspection(prefix, vv, c, setp, "Set in parent <b>" + name + "</b>");
				}
			}
		}
	}

	static public class VisualElementToPyVisualElement extends ClassAdapter {

		public VisualElementToPyVisualElement() {
			super(VisualElement.class);
		}

		public VisualElementToPyVisualElement(Class<? extends iVisualElement> c) {
			super(c);
		}

		public PyObject adapt(Object o) {
			return new PyVisualElement((iVisualElement) o);
		}

	}

	static public FieldPyObjectAdaptor fieldPyObjectAdaptor;

	public static ExtensiblePyObjectAdapter adaptor;

	static protected boolean initialized = false;

	public static void initialize() {
		
		if (true)
			return;
		
		if (!initialized) {

			;//System.out.println(" initializing adaptor ");

			ExtensiblePyObjectAdapter a = Py.getAdapter();
			fieldPyObjectAdaptor = new FieldPyObjectAdaptor(a);
			adaptor = a;
			initialized = true;
		}
	}

	protected FieldPyObjectAdaptor(ExtensiblePyObjectAdapter ex) {
		ex.addPreClass(new CallableAndAttributesToCallableAndAttributesJavaInstance());
		ex.addPreClass(new ExtensibleToExtensibleJavaInstance());
		ex.addPreClass(new CallableToCallableJavaInstance());
		ex.addPreClass(new HasScalarToPyHasScalar());
		ex.addPreClass(new HandlesAttributesToPyHandlesAttributes());
		ex.add(new VisualElementToPyVisualElement() {
		});
		ex.add(new VisualElementToPyVisualElement(RootSheetElement.class) {
		});
//		ex.add(new DictToPyFieldDict() {
//		});
		ex.add(new PairToPyTuple());

	}

}
