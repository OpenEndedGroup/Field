package field.extras.wrapintransform;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.python.core.PyModule;
import org.python.core.PyObject;

import field.core.Constants;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides.DefaultOverride;
import field.core.execution.BasicRunner;
import field.core.execution.PythonInterface;
import field.core.execution.iExecutesPromise;
import field.core.plugins.BaseSimplePlugin;
import field.core.plugins.python.PythonPlugin;
import field.core.plugins.python.PythonPluginEditor;
import field.core.ui.SmallMenu;
import field.core.ui.text.PythonTextEditor;
import field.core.ui.text.BaseTextEditor2.Completion;
import field.core.ui.text.PythonTextEditor.EditorExecutionInterface;
import field.core.ui.text.embedded.MinimalTextField_blockMenu;
import field.launch.Launcher;
import field.launch.iUpdateable;
import field.math.graph.GraphNodeSearching.VisitCode;
import field.namespace.generic.Bind.iFunction;
import field.namespace.generic.Generics.Pair;

public class WrapInTransformPlugin extends BaseSimplePlugin {

	static public final VisualElementProperty<String> wrapInTransform = new VisualElementProperty<String>("wrapInTransform");

	public class LocalOver extends DefaultOverride {

		@Override
		public <T> VisitCode getProperty(iVisualElement source, VisualElementProperty<T> prop, Ref<T> ref) {
			if (prop.equals(PythonPluginEditor.python_customToolbar)) {
				ArrayList<Pair<String, iUpdateable>> r = (ArrayList<Pair<String, iUpdateable>>) ref.get();
				if (r == null) {
					r = new ArrayList<Pair<String, iUpdateable>>();
				}
				addElementsTo(source, r);
				ref.set((T) r);
			} else if (prop.equals(PythonPluginEditor.editorExecutionInterface)) {
				if (needsWrap(source)) {
					ref.set((T) getEditorExecutionInterface(source, (EditorExecutionInterface) ref.get()));
				}
			} else if (prop.equals(PythonPlugin.python_sourceFilter)) {
				ref.set((T) sourceFilter(source));
			}

			return super.getProperty(source, prop, ref);
		}

	}

	private BasicRunner runner;

	@Override
	protected String getPluginNameImpl() {
		return "wrapintransformplugin";
	}

	public iFunction<String, String> sourceFilter(final iVisualElement source) {
		return new iFunction<String, String>() {

			public String f(String in) {

				String wrap = source.getProperty(wrapInTransform);
				if (wrap==null) return in;
				if (in.startsWith(wrap) && in.endsWith("globals())")) return in;
				

				if (wrap == null)
					return in;
				else {
					in = wrap + "(_self,r\"\"\"\n" + in + "\n\"\"\", globals())";
				}
				return in;
			}
		};
	}

	public boolean needsWrap(iVisualElement source) {
		return source.getProperty(wrapInTransform) != null;
	}

	private iExecutesPromise ep;

	public void addElementsTo(final iVisualElement source, ArrayList<Pair<String, iUpdateable>> r) {
		String wrapIn = wrapInTransform.get(source);
		if (wrapIn == null) {
			wrapIn = "No transformation";
		}
		if (wrapIn.equals("defaultTransform"))
			wrapIn = "No transformation";

		wrapIn = "Transform '" + wrapIn + "'";
		r.add(new Pair<String, iUpdateable>(wrapIn, new iUpdateable() {
			public void update() {

				LinkedHashMap<String, iUpdateable> items = new LinkedHashMap<String, iUpdateable>();
				items.put("Available transforms, from TextTransforms.*", null);

				PyModule q = (PyModule) PythonInterface.getPythonInterface().getVariable("TextTransforms");
				List l = (List) q.__dir__();
				for (int i = 0; i < l.size(); i++) {
					final String name = (String) l.get(i);
					if (!name.startsWith("__")) {

						try {
							PyObject doc = (PyObject) PythonInterface.getPythonInterface().eval("TextTransforms." + name);
							String d = (String) doc.__getattr__("__doc__").__tojava__(String.class);

							if (d.length() > 0)

								d = "" + PythonTextEditor.limitDocumentation(d);

							String trimmed = d.replace("\n", "").replace("\t", " ").trim();
							if (trimmed.length()>0 && !trimmed.equals("The most base type"))
							items.put("\u223d <b>" + name + "</b> \u2014 <font size=-2>" + trimmed + "</font>", new iUpdateable() {

								public void update() {
									setTransformation(source, name.equals("defaultTransform") ? null : name);
								}
							});
						} catch (Exception ex) {
						}
					}
				}

				if (MinimalTextField_blockMenu.knownTextTransforms.size() > 0) {
					items.put("Available transforms, from knownTextTransforms", null);
					for (final Pair<String, String> s : MinimalTextField_blockMenu.knownTextTransforms) {
						items.put("\u223d <b>" + s.left + "</b> \u2014 <i>" + s.right + "</i>", new iUpdateable() {

							public void update() {
								setTransformation(source, s.left);
							}
						});
					}
				}

				PythonPlugin plugin = PythonPlugin.python_plugin.get(source);
				Shell frame = ((PythonPluginEditor) plugin).getEditor().getFrame();
				Point screen = Launcher.display.map(null, frame, Launcher.display.getCursorLocation());

				new SmallMenu().createMenu(items, frame, null).show(screen);
			}
		}));
	}

	@Override
	public void registeredWith(iVisualElement root) {
		super.registeredWith(root);
	}

	public EditorExecutionInterface getEditorExecutionInterface(final iVisualElement source, final EditorExecutionInterface delegateTo) {
		return new EditorExecutionInterface() {
			public void executeFragment(String fragment) {
				
				EditorExecutionInterface delegateTo2= delegateTo;
				if (delegateTo==null)
				{
					fragment = sourceFilter(source).f(fragment);
					PythonInterface.getPythonInterface().execString(fragment);
				}
				else
				{
					fragment = sourceFilter(source).f(fragment);
					delegateTo2.executeFragment(fragment);
				}
			}

			public Object executeReturningValue(String fragment) {
				fragment = sourceFilter(source).f(fragment);

				EditorExecutionInterface delegateTo2= delegateTo;
				if (delegateTo==null)
				{
					delegateTo2 = ((PythonPluginEditor)PythonPluginEditor.python_plugin.get(source)).getEditor().getInterface();
				}

				return delegateTo2.executeReturningValue(fragment);
			}
			
			@Override
			public boolean globalCompletionHook(String leftText, boolean publicOnly, ArrayList<Completion> comp) {
				
				
				
				return false;
			}

		};
	}

	@Override
	public void update() {

	}

	@Override
	protected DefaultOverride newVisualElementOverrides() {
		return new LocalOver();
	}

	public void setTransformation(iVisualElement source, String left) {
		if (left == null)
			wrapInTransform.delete(source, source);
		else
			wrapInTransform.set(source, source, left);
		PythonPlugin plugin = PythonPlugin.python_plugin.get(source);
		((PythonPluginEditor) plugin).swapInCustomToolbar();
	}

}
