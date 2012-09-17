package field.core.windowing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.text.PlainDocument;

import org.eclipse.swt.SWTError;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.jruby.compiler.ir.instructions.THROW_EXCEPTION_Instr;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.persistance.VisualElementReference;
import field.core.plugins.python.PythonPlugin;
import field.core.plugins.python.PythonPluginEditor;
import field.core.ui.text.rulers.StyledTextPositionSystem;
import field.core.ui.text.rulers.StyledTextPositionSystem.Position;
import field.core.windowing.GLComponentWindow.ComponentContainer;
import field.core.windowing.components.DraggableComponent;
import field.core.windowing.components.PlainDraggableComponent;
import field.core.windowing.components.iComponent;
import field.launch.Launcher;
import field.math.linalg.Vector2;
import field.math.linalg.Vector4;
import field.namespace.generic.Generics.Pair;

public class EditorSpaceBox {

	static public final VisualElementProperty<Boolean> isEditorSpace = new VisualElementProperty<Boolean>("isEditorSpace");
	static public final VisualElementProperty<VisualElementReference> isEditorSpace_belongsTo = new VisualElementProperty<VisualElementReference>("isEditorSpace_belongsTo");
	static public final VisualElementProperty<String> isEditorSpace_belongsToProperty = new VisualElementProperty<String>("isEditorSpace_belongsToProperty");
	private GLComponentWindow window;
	private final iVisualElement root;

	public EditorSpaceBox(GLComponentWindow window, iVisualElement root) {
		this.window = window;
		this.root = root;
	}

	HashMap<iVisualElement, Pair<Rect, StyledTextPositionSystem.Position>> frozen = new HashMap<iVisualElement, Pair<Rect, StyledTextPositionSystem.Position>>();
	private Vector4 frozenAt;
	private ArrayList<Vector2> frozenAtLineMap;

	public void freeze() {

		try {
			ComponentContainer root = window.getRoot();

			frozen = new HashMap<iVisualElement, Pair<Rect, StyledTextPositionSystem.Position>>();
			frozenAt = new Vector4(window.getXScale(), window.getYScale(), window.getXTranslation(), window.getYTranslation());
			frozenAtLineMap = buildLineMap();

			doFreeze(root);

			// System.out.println(" frozen has <"+frozenAtLineMap.size()+"> lines <"+frozenAt+">");

		} catch (Throwable e) {
			e.printStackTrace();
		}

	}

	private ArrayList<Vector2> buildLineMap() {
		try {
			PythonPluginEditor p = (PythonPluginEditor) PythonPluginEditor.python_plugin.get(root);
			StyledText t = p.getEditor().getInputEditor();
			ArrayList<Vector2> r = new ArrayList<Vector2>();
			for (int i = 0; i < t.getLineCount() + 1; i++) {

				Point ll = new Point(0, t.getLinePixel(i));
				ll = Launcher.getLauncher().display.map(t, window.getFrame(), ll);

				r.add(new Vector2(ll.x, ll.y));
			}

			// System.out.println(" line map <"+r+">");

			return r;
		} catch (SWTError e) {
			return new ArrayList<Vector2>();
		}
	}

	int tc = 0;

	public void thaw() {
		if (frozen == null)
			return;

		try {

			PythonPluginEditor p = (PythonPluginEditor) PythonPluginEditor.python_plugin.get(root);
			StyledText t = p.getEditor().getInputEditor();

			iVisualElement current = p.getEditor().getThisBox();
			VisualElementProperty currentProp = p.getEditor().getThisProperty();

			Vector4 thawAt = new Vector4(window.getXScale(), window.getYScale(), window.getXTranslation(), window.getYTranslation());
			ArrayList<Vector2> thrawAtLineMap = buildLineMap();

			System.out.println(" thaw has <" + thrawAtLineMap.size() + "> lines <" + thawAt + ">");

			// if (thawAt.equals(frozenAt) &&
			// thrawAtLineMap.equals(frozenAtLineMap)) return;

			boolean changed = false;
			for (Map.Entry<iVisualElement, Pair<Rect, StyledTextPositionSystem.Position>> e : frozen.entrySet()) {
				Rect r = transform(e.getValue(), frozenAt, thawAt, frozenAtLineMap.get(e.getValue().right.was), thrawAtLineMap.get(t.getLineAtOffset(e.getValue().right.at)));

				if (r == null)
					continue;
				if (e.getKey().getFrame(null) == null)
					continue;

				// System.out.println(r+" "+e.getKey().getFrame(null)+"       "+e.getValue().right.was+" "+t.getLineAtOffset(e.getValue().right.at));

				changed = changed || !r.equals(e.getKey().getFrame(null));

				VisualElementReference bt = isEditorSpace_belongsTo.get(e.getKey());
				String btp = isEditorSpace_belongsToProperty.get(e.getKey());

				if ((bt != null && bt.get(root) != null && bt.get(root) != current) || (btp != null && !btp.equals(currentProp))) {
					iComponent local = iVisualElement.localView.get(e.getKey());
					if (local instanceof DraggableComponent)
						((DraggableComponent) local).setHidden(true);
					else if (local instanceof PlainDraggableComponent)
						((PlainDraggableComponent) local).setHidden(true);
				} else {
					iComponent local = iVisualElement.localView.get(e.getKey());
					if (local instanceof DraggableComponent)
						((DraggableComponent) local).setHidden(false);
					else if (local instanceof PlainDraggableComponent)
						((PlainDraggableComponent) local).setHidden(false);
				}

				e.getKey().getProperty(iVisualElement.overrides).shouldChangeFrame(e.getKey(), r, e.getKey().getFrame(null), true);
			}

			System.out.println(" changed ? " + changed);

			if (changed)
				tc = 2;

			if (tc-- > 0)
				window.requestRepaint();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	private Rect transform(Pair<Rect, Position> pair, Vector4 from, Vector4 to, Vector2 f2, Vector2 to2) {

		Vector2 p1 = new Vector2(pair.left.x, pair.left.y);
		Vector2 p2 = new Vector2(pair.left.x + pair.left.w, pair.left.y + pair.left.h);

		transform(p1, from, to);
		transform(p2, from, to);

		p1.x = p1.x - f2.x + to2.x;
		p1.y = p1.y - f2.y + to2.y;
		p2.x = p2.x - f2.x + to2.x;
		p2.y = p2.y - f2.y + to2.y;

		return new Rect(p1.x, p1.y, p2.x - p1.x, p2.y - p1.y);

	}

	private void transform(Vector2 p, Vector4 from, Vector4 to) {
		p.x = (p.x - from.z) / from.x;
		p.y = (p.y - from.w) / from.y;

		p.x = p.x * to.x + to.z;
		p.y = p.y * to.y + to.w;
	}

	protected void doFreeze(iComponent root) {
		iVisualElement x = root.getVisualElement();

		PythonPluginEditor p = (PythonPluginEditor) PythonPluginEditor.python_plugin.get(this.root);
		StyledText t = p.getEditor().getInputEditor();

		if (x != null && isEditorSpace.getBoolean(x, false)) {
			Position ss = StyledTextPositionSystem.get(t).createPosition(getOffsetForFrame(x.getFrame(null)));
			frozen.put(x, new Pair<Rect, Position>(x.getFrame(null), ss));

			// System.out.println(" freezing <" + x +
			// "> position is <" + ss.at + ">");
			ss.was = t.getLineAtOffset(ss.at);

		}

		if (root instanceof ComponentContainer) {
			List<iComponent> children = ((ComponentContainer) root).components;
			for (iComponent cc : children) {
				doFreeze(cc);
			}
		}
	}

	private int getOffsetForFrame(Rect frame) {
		PythonPluginEditor p = (PythonPluginEditor) PythonPluginEditor.python_plugin.get(root);
		StyledText t = p.getEditor().getInputEditor();

		float x = (float) ((frame.x - window.getXTranslation()) / window.getXScale());
		float y = (float) ((frame.y - window.getYTranslation()) / window.getYScale());

		Point ll = new Point((int) x, (int) y);
		ll = Launcher.getLauncher().display.map(t, window.getFrame(), ll);
		for (int i = 0; i < frozenAtLineMap.size(); i++) {
			if (frozenAtLineMap.get(i).y > ll.y)
				return t.getOffsetAtLine(Math.max(0, i - 1));
		}

		try {
			return t.getOffsetAtLine(Math.max(0, frozenAtLineMap.size() - 1));
		} catch (IllegalArgumentException e) {
			return 0;
		}
	}

}
