package field.core.ui.text;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.custom.ExtendedModifyEvent;
import org.eclipse.swt.custom.ExtendedModifyListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;

public class StyledTextUndo {

	static public class Change {
		int start, length;
		String replacedBy;
		int selectionStart, selectionLength;

		public Change(int start, int length, String replacedBy, int selectionStart, int selectionLength) {
			super();
			this.start = start;
			this.length = length;
			this.replacedBy = replacedBy;
			this.selectionLength = selectionLength;
			this.selectionStart = selectionStart;
		}

		@Override
		public String toString() {
			return "Change [start=" + start + ", length=" + length + ", replacedBy=" + replacedBy + ", selectionStart=" + selectionStart + ", selectionLength=" + selectionLength + "]";
		}

	}

	List<Change> changes = new ArrayList<Change>();
	List<Change> redo = new ArrayList<Change>();

	int limit = 1000;

	boolean ignore = false;
	boolean isRedoing = false;

	private final StyledText target;

	int selectionStart;
	int selectionLength;

	public StyledTextUndo(final StyledText target) {

		this.target = target;
		target.addVerifyListener(new VerifyListener() {

			@Override
			public void verifyText(VerifyEvent e) {
				selectionStart = target.getSelectionRange().x;
				selectionLength = target.getSelectionRange().y;

			}
		});

		target.addExtendedModifyListener(new ExtendedModifyListener() {

			@Override
			public void modifyText(ExtendedModifyEvent event) {

				if (ignore) {
					Change c = new Change(event.start, event.length, event.replacedText, selectionStart, selectionLength);

					redo.add(c);
					if (redo.size() > limit)
						redo.remove(0);
				} else {
					Change c = new Change(event.start, event.length, event.replacedText, selectionStart, selectionLength);

					changes.add(c);
					if (changes.size() > limit)
						changes.remove(0);

					if (!isRedoing)
						redo.clear();

				}
			}
		});

	}

	public void undo() {
		if (changes.size() > 0) {
			Change c = changes.remove(changes.size() - 1);
			ignore = true;
			try {
				target.replaceTextRange(c.start, c.length, c.replacedBy);
				target.setSelectionRange(c.selectionStart, c.selectionLength);
			} finally {
				ignore = false;
			}
		}
	}

	public void redo() {
		if (redo.size() > 0) {
			Change c = redo.remove(redo.size() - 1);
			ignore = false;
			isRedoing = true;
			try {
				target.replaceTextRange(c.start, c.length, c.replacedBy);
				target.setSelectionRange(c.selectionStart, c.selectionLength);
			} finally {
				ignore = false;
				isRedoing = false;
			}
		}
	}

	static public class Memo 
	{
		List<Change> changes = new ArrayList<Change>();
		List<Change> redo = new ArrayList<Change>();
	}

	public Memo toMemo() {
		Memo m = new Memo();
		m.changes.addAll(changes);
		m.redo.addAll(redo);
		return m;
	}

	public void fromMemo(Memo m) {
		changes.clear();
		if (m != null)
			changes.addAll(m.changes);
		redo.clear();
		if (m != null)
			redo.addAll(m.redo);
	}
}
