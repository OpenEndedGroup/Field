package field.core.plugins.history;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.NextUpdate;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.plugins.history.GitVersioningQueries.DiffSet;
import field.core.plugins.history.GitVersioningQueries.Snippet;
import field.core.plugins.history.GitVersioningQueries.VersionsOfFile;
import field.core.plugins.pseudo.PseudoPropertiesPlugin;
import field.core.ui.BetterComboBox;
import field.core.ui.FieldMenus2;
import field.core.ui.SmallMenu.iHoverUpdate;
import field.core.ui.text.PythonTextEditor;
import field.launch.iUpdateable;

@Woven
public class VersionMenu {

	GitVersioningQueries q = new GitVersioningQueries(FieldMenus2.getCanonicalVersioningDir());
	private BetterComboBox box;

	List<iUpdateable> actions = new ArrayList<iUpdateable>();
	private ArrayList<DiffSet> diffs;
	private final PythonTextEditor editor;
	private iVisualElement element;
	private VisualElementProperty<String> prop;
	private String originalText;

	public VersionMenu(Composite inside, PythonTextEditor editor) {
		this.editor = editor;

		box = new BetterComboBox(inside, new String[] { "(no versions)" }) {
			@Override
			public void updateSelection(int index, String text) {
				if (index < actions.size())
					actions.get(index).update();
			}

			@Override
			protected void makeMenu() {
				doMakeMenu(element, prop);
			}
		};
		box.combo.setBounds(5, 5, 20, 20);
		box.titleText = "Versions";
		box.combo.setText("\u261E");

		box.setRightJustified(true);
		box.setNoDrag(true);
		box.setFixedText(true);
	}

	public void swapIn(final iVisualElement element, VisualElementProperty<String> prop) {

		this.element = element;
		this.prop = prop;

	}

	private void doMakeMenu(final iVisualElement element, VisualElementProperty<String> prop) {

		Point range = editor.getInputEditor().getSelectionRange();

		if (range.y == 0) {

			originalText = editor.getText();

			String sf = PseudoPropertiesPlugin.sheetFolder.get(element);
			String now = prop.get(element);
			final List<VersionsOfFile> v = q.versionsForFile(now, sf + "/" + element.getUniqueID() + "/" + prop.getName() + ".property");
			String[] s = new String[v.size()];
			actions.clear();
			actions.add(new iUpdateable() {
				@Override
				public void update() {
					swapInText_next(element, originalText);
				}
			});
			s[0] = "Current";
			for (int ii = 1; ii < v.size(); ii++) {
				final VersionsOfFile f = v.get(ii);
				System.out.println(" date is :" + f.date);
				s[ii] = f.date;
				actions.add(new iUpdateable() {
					@Override
					public void update() {
						swapInText_next(element, f.getContents().replaceFirst("string>", ""));
					}
				});
			}
			System.out.println(" 0 ");

			diffs = new ArrayList<DiffSet>();
			for (int ii = 0; ii < v.size() - 1; ii++) {
				System.out.println(" making diff <" + ii + "> of <" + v.size() + ">");
				diffs.add(q.new DiffSet(v.get(ii).getContents().split("\n"), v.get(ii + 1).getContents().split("\n")));
			}
			System.out.println(" 1 ");

			streamline(s);

			System.out.println(" A ");

			box.setLabelsWithHTML(s);
			System.out.println(" B ");
			box.currentlySelected = -1;
			// box.combo.setText("\u261E");
			System.out.println(" C ");
			box.setHoverUpdate(new iHoverUpdate() {

				@Override
				public void update(int index) {
					System.out.println(" hu -- " + index);

					swapInText_next(element, v.get(index - 1).getContents().replaceFirst("string>", ""));

				}

				@Override
				public void cancel() {
					System.out.println(" -- cancelling ! --");
					swapInText_next(element, originalText);
					new Exception().printStackTrace();
				}

			});
			System.out.println(" E ");
		} else {
			System.out.println(" doing line analysis");
			originalText = editor.getText();

			int start = editor.getInputEditor().getLineAtOffset(range.x);
			int end = editor.getInputEditor().getLineAtOffset(range.x + range.y);

			if (end == start)
				end = start + 1;

			final int[] cs = { editor.getInputEditor().getOffsetAtLine(start) };
			final int[] ce = { editor.getInputEditor().getOffsetAtLine(end) };

			editor.getInputEditor().setSelection(cs[0], ce[0]);

			if (end == start)
				end = start + 1;

			String sf = PseudoPropertiesPlugin.sheetFolder.get(element);
			String now = prop.get(element);
			List<String> titles = new ArrayList<String>();
			final List<iUpdateable> actions = new ArrayList<iUpdateable>();
			final List<VersionsOfFile> v = q.versionsForFile(now, sf + "/" + element.getUniqueID() + "/" + prop.getName() + ".property");
			String ls = "";
			for (int ii = 0; ii < v.size() - 1; ii++) {
				DiffSet ds = q.new DiffSet(v.get(ii).getContents().split("\n"), v.get(ii + 1).getContents().split("\n"));
				Integer ns = ds.mapLineOut(start);
				Integer ne = ds.mapLineOut(end - 1);

				System.out.println(" mapped " + start + " -> " + end + "  to " + ns + " " + ne);

				if (ns == null || ne == null)
					break;

				ne += 1;

				final Snippet s = ds.snippetsForVersions(ns, ne);
				start = ns;
				end = ne;

				System.out.println(" snippet text is <" + s.contents + ">");

				if (s.contents.equals(ls)) {
				} else {
					titles.add(v.get(ii + 1).date);
					actions.add(new iUpdateable() {

						@Override
						public void update() {
							swapInRange_next(cs, ce, s);
						}
					});
				}
				ls = s.contents;
			}

			String[] ss = titles.toArray(new String[titles.size()]);
			streamline(ss);
			box.setLabelsWithHTML(ss);
			box.currentlySelected = -1;
			System.out.println(" C ");
			box.setHoverUpdate(new iHoverUpdate() {

				@Override
				public void update(int index) {
					if (index > 0)
						actions.get(index - 1).update();
				}

				@Override
				public void cancel() {
					System.out.println(" --- cancelling !! --- ");
					swapInText_next(element, originalText);
				}

			});

		}

	}

	@NextUpdate
	protected void swapInText_next(iVisualElement element, String contents) {
		swapInText(element, contents);
	}

	protected void swapInText(iVisualElement element, String contents) {
	}

	private void streamline(String[] s) {
		List<String[]> m = new ArrayList<String[]>();
		int d = -1;
		for (String ss : s) {
			m.add(ss.split("[ \\:]"));
			if (d == -1)
				d = m.get(0).length;
			else if (d != m.get(m.size() - 1).length) {
				System.out.println(" length mismatch <" + d + " " + m.get(m.size() - 1).length + ">");
				return;
			}
		}

		for (int i = 1; i < m.size(); i++) {
			String[] a = m.get(i - 1);
			String[] b = m.get(i);

			s[i] = "";
			for (int dd = 0; dd < a.length; dd++) {
				System.out.println(" comparing <" + a[dd] + "> <" + b[dd] + "> <" + dd + ">");

				String end = dd == 3 || dd == 4 ? ":" : " ";
				if (a[dd].equals(b[dd])) {
					if (dd < a.length - 1)
						s[i] += "<g>" + b[dd] + end + "</g>";
				} else
					s[i] += b[dd] + end;
			}
		}

	}

	@NextUpdate
	protected void swapInRange_next(final int[] cs, final int[] ce, final Snippet s) {
		swapInRange(cs, ce, s.contents);
	}

	protected void swapInRange(int[] cs, int[] ce, String contents) {
		editor.getInputEditor().replaceTextRange(cs[0], ce[0] - cs[0], contents);
		ce[0] = cs[0] + contents.length();
		editor.getInputEditor().setSelection(cs[0], ce[0]);
	}

}
