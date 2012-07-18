package field.core.windowing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.plugins.python.PythonPlugin;
import field.core.plugins.python.PythonPluginEditor;
import field.core.windowing.GLComponentWindow.ComponentContainer;
import field.core.windowing.components.iComponent;
import field.launch.Launcher;
import field.math.linalg.Vector2;
import field.math.linalg.Vector4;
import field.namespace.generic.Generics.Pair;

public class EditorSpaceBox {

	static public final VisualElementProperty<Boolean> isEditorSpace = new VisualElementProperty<Boolean>("isEditorSpace");
	private GLComponentWindow window;
	private final iVisualElement root;

	public EditorSpaceBox(GLComponentWindow window, iVisualElement root) {
		this.window = window;
		this.root = root;
	}

	HashMap<iVisualElement, Pair<Rect, Integer>> frozen = new HashMap<iVisualElement, Pair<Rect, Integer>>();
	private Vector4 frozenAt;
	private ArrayList<Vector2> frozenAtLineMap;

	public void freeze() {
		ComponentContainer root = window.getRoot();

		frozen  = new HashMap<iVisualElement, Pair<Rect, Integer>>();
		frozenAt = new Vector4(window.getXScale(), window.getYScale(), window.getXTranslation(), window.getYTranslation());
		frozenAtLineMap = buildLineMap();
		
		doFreeze(root);
	}
	
	private ArrayList<Vector2> buildLineMap() {
		PythonPluginEditor p = (PythonPluginEditor) PythonPluginEditor.python_plugin.get(root);
		StyledText t = p.getEditor().getInputEditor();
		ArrayList<Vector2> r = new ArrayList<Vector2>();
		for(int i=0;i<t.getLineCount()+1;i++)
		{
			
			Point ll = new Point(0, t.getLinePixel(i));
			ll = Launcher.getLauncher().display.map(t, window.getFrame(), ll);
			
			r.add(new Vector2(ll.x, ll.y));
		}
		return r;
	}

	public void thaw()
	{
		Vector4 thawAt = new Vector4(window.getXScale(), window.getYScale(), window.getXTranslation(), window.getYTranslation());
		ArrayList<Vector2> thrawAtLineMap = buildLineMap();
		
		if (thawAt.equals(frozenAt) && thrawAtLineMap.equals(frozenAtLineMap)) return;
		
		for(Map.Entry<iVisualElement, Pair<Rect, Integer>> e : frozen.entrySet())
		{
			Rect r = transform(e.getValue(), frozenAt, thawAt, frozenAtLineMap.get(e.getValue().right.intValue()), thrawAtLineMap.get(e.getValue().right.intValue()));

			e.getKey().getProperty(iVisualElement.overrides).shouldChangeFrame(e.getKey(), r, e.getKey().getFrame(null), true);
		}
		window.requestRepaint();
	}

	private Rect transform(Pair<Rect, Integer> pair, Vector4 from, Vector4 to, Vector2 f2, Vector2 to2) {
		
		Vector2 p1 = new Vector2(pair.left.x, pair.left.y);
		Vector2 p2 = new Vector2(pair.left.x + pair.left.w, pair.left.y + pair.left.h);
		
		transform(p1, from, to);
		transform(p2, from, to);
		
		p1.x = p1.x-f2.x + to2.x;
		p1.y = p1.y-f2.y + to2.y;
		p2.x = p2.x-f2.x + to2.x;
		p2.y = p2.y-f2.y + to2.y;
		
		
		return new Rect(p1.x, p1.y, p2.x-p1.x, p2.y-p1.y);
		
	}

	private void transform(Vector2 p, Vector4 from, Vector4 to) {
		p.x = (p.x-from.z)/from.x;
		p.y = (p.y-from.w)/from.y;
		
		p.x = p.x*to.x+to.z;
		p.y = p.y*to.y+to.w;
	}

	protected void doFreeze(iComponent root) {
		iVisualElement x = root.getVisualElement();
		if (x != null && isEditorSpace.getBoolean(x, false)) {
			frozen.put(x, new Pair<Rect, Integer>(x.getFrame(null), getLineForFrame(x.getFrame(null))));
		}

		if (root instanceof ComponentContainer) {
			List<iComponent> children = ((ComponentContainer) root).components;
			for (iComponent cc : children) {
				doFreeze(cc);
			}
		}
	}

	private int getLineForFrame(Rect frame) {
		PythonPluginEditor p = (PythonPluginEditor) PythonPluginEditor.python_plugin.get(root);
		StyledText t = p.getEditor().getInputEditor();
		
		float x = (float) ((frame.x-window.getXTranslation())/window.getXScale());
		float y = (float) ((frame.y-window.getYTranslation())/window.getYScale());
		
		Point ll = new Point( (int)x, (int)y);
		ll = Launcher.getLauncher().display.map(t, window.getFrame(), ll);
		for(int i=0;i<frozenAtLineMap.size();i++)
		{
			if (frozenAtLineMap.get(i).y>ll.y) return Math.max(0, i-1);
		}
		return Math.max(0, frozenAtLineMap.size()-1);
	}

}
