package field.core;

import static java.lang.System.out;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.NextUpdate;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.persistance.FluidCopyPastePersistence;
import field.core.windowing.GLComponentWindow;
import field.core.windowing.components.DraggableComponent;
import field.core.windowing.components.MainSelectionGroup;
import field.core.windowing.components.iComponent;
import field.launch.Launcher;
import field.math.linalg.Vector2;

@Woven
public class DragDuplicator {

	private final MainSelectionGroup group;
	private final iVisualElement root;

	public DragDuplicator(MainSelectionGroup group, iVisualElement root) {
		this.group = group;
		this.root = root;
	}

	boolean isDragging = false;
	private HashSet<iVisualElement> ongoing;

	Vector2 at = new Vector2();

	@NextUpdate
	public void begin(Event event) {


		Set<iComponent> c = group.getSelection();
		Set<iVisualElement> v = new LinkedHashSet<iVisualElement>();
		for (iComponent cc : c) {
			iVisualElement vv = cc.getVisualElement();
			if (vv != null)
				v.add(vv);
		}

		isDragging = v.size() > 0;

		System.out.println(" begin drag <"+isDragging+">");
		
		if (isDragging) {
			//TODO: 64 \u2014 confront mouse cursor setting in pure java
			//NSCursor.closedHandCursor().set();
//			GLComponentWindow.getCurrentWindow(null).getCanvas().setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));

			GLComponentWindow.getCurrentWindow(null).getCanvas().setCursor(Launcher.display.getSystemCursor(SWT.CURSOR_HAND));
			
			
			FluidCopyPastePersistence copier = iVisualElement.copyPaste.get(root);

			StringWriter temp = new StringWriter();
			HashSet<iVisualElement> savedOut = new HashSet<iVisualElement>();
			ObjectOutputStream oos = copier.getObjectOutputStream(temp, savedOut, v);
			try {
				oos.writeObject(v);
				oos.close();

				HashSet<iVisualElement> all = new HashSet<iVisualElement>(StandardFluidSheet.allVisualElements(root));

				ongoing = new HashSet<iVisualElement>();
				ObjectInputStream ois = copier.getObjectInputStream(new StringReader(temp.getBuffer().toString()), ongoing, all);
				Object in = ois.readObject();

				ois.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		at.x = event.x;
		at.y = event.y;

		out.println(" output <"+ongoing+">");
	}

	public void drag(Event event) {
		
		if (!isDragging)
			return;

		float deltaX = event.x - at.x;
		float deltaY = event.y - at.y;

		out.println(" delta <"+deltaX+", "+deltaY+"> ongoing <"+ongoing+">");

		at.x = event.x;
		at.y = event.y;

		for (iVisualElement v : ongoing) {
			Rect f = v.getFrame(null);
			f.x += deltaX;
			f.y += deltaY;
			v.setFrame(f);
		}
	}

	public void end(Event event) {

		//TODO: 64 \u2014 confront mouse cursor setting in pure java
		//NSCursor.arrowCursor().set();
		//GLComponentWindow.getCurrentWindow(null).getCanvas().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		GLComponentWindow.getCurrentWindow(null).getCanvas().setCursor(Launcher.display.getSystemCursor(SWT.CURSOR_ARROW));
		
		if (!isDragging)
			return;
		
		isDragging = false;
		ongoing.clear();

	}

}
