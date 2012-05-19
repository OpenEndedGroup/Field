package field.extras.typing;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.NextUpdate;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.execution.PythonInterface;
import field.core.execution.PythonScriptingSystem;
import field.core.execution.PythonScriptingSystem.Promise;
import field.core.plugins.log.ElementInvocationLogging;
import field.core.plugins.log.Logging;
import field.core.plugins.python.PythonPluginEditor;
import field.core.ui.SmallMenu;
import field.core.ui.SmallMenu.BetterPopup;
import field.core.ui.SmallMenu.Documentation;
import field.core.ui.SmallMenu.iKeystrokeUpdate;
import field.core.ui.text.BaseTextEditor2.Completion;
import field.core.ui.text.PythonTextEditor;
import field.core.ui.text.PythonTextEditor.EditorExecutionInterface;
import field.core.util.PythonCallableMap;
import field.core.windowing.GLComponentWindow;
import field.core.windowing.components.iComponent;
import field.launch.iUpdateable;
import field.math.abstraction.iAcceptor;

public class TypingExecution {

	static public final VisualElementProperty<PythonCallableMap> executesTyping = new VisualElementProperty<PythonCallableMap>("executesTyping_");
	private static BetterPopup menu;

	static public void execute(iVisualElement root, String text) {
		Set<iVisualElement> marked = getSelected(root);

		// three cases, no selection, single selection, multiple
		// selection. Right now the latter two are identical.

		if (marked.size() == 0) {
			if (text.startsWith(".")) {
				text = "_self.root" + text;
			}

			PythonInterface.getPythonInterface().execString(text);
		} else {
			if (text.startsWith(".")) {
				text = "_self" + text;
			}

			for (iVisualElement e : marked) {
				PythonScriptingSystem pss = PythonScriptingSystem.pythonScriptingSystem.get(e);
				Promise promise = pss.promiseForKey(e);
				EditorExecutionInterface eei = PythonPluginEditor.editorExecutionInterface.get(e);

				promise.beginExecute();

				PythonCallableMap type = executesTyping.get(e);

				if (type != null)
					type.invoke(text);
				else if (eei != null)
					eei.executeFragment(text);
				else
					PythonInterface.getPythonInterface().execString(text);

				if (Logging.enabled())
					Logging.logging.addEvent(new ElementInvocationLogging.ElementTextFragmentWasExecuted(text, e));
				
				promise.endExecute();
			}
		}
	}

	private static Set<iVisualElement> getSelected(iVisualElement root) {
		Set<iVisualElement> marked = new LinkedHashSet<iVisualElement>();
		Set<iComponent> sel;
		sel = iVisualElement.selectionGroup.get(root).getSelection();
		for (iComponent c : sel)
			if (c.getVisualElement() != null)
				marked.add(c.getVisualElement());
		return marked;
	}

	public static void beginCompletion(final iVisualElement root, final String text, final iComponent iComponent, final int x, final int y, final iAcceptor<String> insertor) {

		if (text.length() < 1)
			return;

		PythonPluginEditor plugin = (PythonPluginEditor) PythonPluginEditor.python_plugin.get(root);
		PythonTextEditor e = plugin.getEditor();

		String text2 = text;

		Set<iVisualElement> sel = getSelected(root);
		if (sel.size() > 0) {
			if (text.startsWith(".")) {
				text2 = "_self" + text;
			}
			iVisualElement first = sel.iterator().next();

			PythonScriptingSystem pss = PythonScriptingSystem.pythonScriptingSystem.get(first);
			Promise promise = pss.promiseForKey(first);

			promise.beginExecute();
		}
		else
		{
			if (text.startsWith(".")) {
				text2 = "_self.root" + text;
			}
		}
		iKeystrokeUpdate ks = new iKeystrokeUpdate() {

			public boolean update(Event arg0) {
				if (arg0.character>0)
				if (arg0.keyCode == '\b') {
					iComponent.keyTyped(GLComponentWindow.getCurrentWindow(iComponent).getRoot(), arg0);
					beginCompletion(root, text + arg0.character, iComponent, x, y, insertor);
					return true;
				}
				return false;

			}
		};

		List<Completion> completions = e.getCompletions(text2, true, null);
		if (sel.size() > 0) {
			iVisualElement first = sel.iterator().next();

			PythonScriptingSystem pss = PythonScriptingSystem.pythonScriptingSystem.get(first);
			Promise promise = pss.promiseForKey(first);

			promise.endExecute();
		}

		if (completions.size() == 0)
			return;

		LinkedHashMap<String, iUpdateable> insert = new LinkedHashMap<String, iUpdateable>();

		boolean optionalYes = completions.size() < 4;

		for (final Completion c : completions) {
			if (c == null)
				continue;

			if (c.optionalDocumentation != null && optionalYes) {
				insert.put(c + "_optional", new Documentation("\n\n" + c.optionalDocumentation));
			}
			if (c.isDocumentation) {
				insert.put("" + c, new Documentation(c.text));
			} else if (c.enabled) {
				insert.put(c.text, new iUpdateable() {

					public void update() {
						insertor.set(c.optionalPlainText);
					}
				});
			} else {
				insert.put(c.text, null);
			}
		}

		menu = new SmallMenu().createMenu(insert, GLComponentWindow.getCurrentWindow(iComponent).getFrame(), ks);
		menu.show(new Point(x, y));
		menu.selectFirst();
		
		menu.doneHook = new iUpdateable() {
			
			@Woven
			@NextUpdate(delay=2)
			public void update() {
				GLComponentWindow.getCurrentWindow(iComponent).getFrame().forceFocus();
			}
		};
	}

}
