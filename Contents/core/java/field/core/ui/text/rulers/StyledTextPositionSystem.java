package field.core.ui.text.rulers;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.TextChangeListener;
import org.eclipse.swt.custom.TextChangedEvent;
import org.eclipse.swt.custom.TextChangingEvent;

public class StyledTextPositionSystem {

	static public final WeakHashMap<StyledText, StyledTextPositionSystem> systems = new WeakHashMap<StyledText, StyledTextPositionSystem>();

	private final StyledText on;
	boolean installed = false;
	private TextChangeListener listener;

	public StyledTextPositionSystem(StyledText on) {
		this.on = on;
	}

	static public class Position {
		public int at;
		public int was;
		boolean valid = true;
	}

	List<Position> positions = new ArrayList<Position>();

	public void install() {
		if (installed)
			return;

		on.getContent().addTextChangeListener(listener = new TextChangeListener() {

			@Override
			public void textSet(TextChangedEvent event) {
			}

			@Override
			public void textChanging(TextChangingEvent event) {
				int start = event.start;
				int end = event.replaceCharCount + start;

				int newend = event.newCharCount + start;

				for (Position p : positions) {
					if (p.at >= end) {
						p.at += newend - end;
					} else if (p.at < start) {

					} else if (p.at > start && p.at < end) {
						p.at = start;
						p.valid = false;
					} else {
					}

				}
			}

			@Override
			public void textChanged(TextChangedEvent event) {
			}
		});
		installed = true;
	}

	public Position createPosition(int i) {
		Position p = new Position();
		p.at = i;
		p.valid = true;
		positions.add(p);
		return p;
	}

	public void deinstall() {
		if (!installed)
			return;

		installed = false;
		on.getContent().removeTextChangeListener(listener);
		positions.clear();
	}

	static public StyledTextPositionSystem get(StyledText t) {
		StyledTextPositionSystem m = systems.get(t);
		if (m == null)
			systems.put(t, m = new StyledTextPositionSystem(t));
		m.install();
		return m;
	}
}
