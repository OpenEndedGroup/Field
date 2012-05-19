package field.core.ui.text;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.NextUpdate;
import field.core.Constants;
import field.core.Platform;
import field.core.dispatch.iVisualElement;
import field.core.plugins.BaseSimplePlugin;
import field.core.plugins.python.PythonPluginEditor;
import field.core.plugins.selection.ToolBarFolder;
import field.core.ui.BetterComboBox;
import field.core.ui.MacScrollbarHack;
import field.launch.Launcher;

@Woven
public class FindAndReplacePlugin extends BaseSimplePlugin {

	private Composite contents;
	private ScrolledComposite scroller;

	@Override
	protected String getPluginNameImpl() {
		return "findAndRepalce";
	}

	@Override
	public void registeredWith(iVisualElement root) {
		super.registeredWith(root);

		this.root = root;
		ToolBarFolder folder = ToolBarFolder.currentFolder;
		ScrolledComposite scroller;
		Composite contents;

		scroller = new ScrolledComposite(folder.getContainer(), SWT.VERTICAL | SWT.HORIZONTAL);
		contents = new Composite(scroller, SWT.NONE);

		init("F", folder, scroller, contents);
		new MacScrollbarHack(scroller);

	}

	static int rowHeight = 28;
	static int labelWidth = 60;
	private DivBox results;
	private ScopeBox scopeBox;
	private SearchBox replaceBox;
	private SearchBox searchBox;

	public abstract class BaseRow extends Composite {

		protected Control editor;
		String name;

		public BaseRow(String name) {
			super(contents, 0);
			this.name = name;
			makeControl();

			GridData d = new GridData();
			d.grabExcessHorizontalSpace = true;
			d.horizontalAlignment = SWT.FILL;
			d.verticalAlignment = SWT.CENTER;
			d.heightHint = rowHeight;
			d.widthHint = 1000;
			this.setLayoutData(d);

			// editor.setFont(new Font(Launcher.display,
			// Constants.defaultFont, 11, 0));

			scroller.setMinHeight(contents.getChildren().length * rowHeight + 50);
			scroller.setMinWidth(labelWidth * 2);

			GridLayout gl2 = new GridLayout(2, false);
			gl2.marginLeft = labelWidth;
			setLayout(gl2);

			GridData gd2 = new GridData(SWT.DEFAULT, SWT.DEFAULT);
			gd2.verticalAlignment = SWT.CENTER;
			gd2.widthHint = 1000;
			gd2.heightHint = rowHeight - 6;
			gd2.grabExcessHorizontalSpace = true;
			editor.setLayoutData(gd2);

			this.addListener(SWT.Paint, new Listener() {

				@Override
				public void handleEvent(Event event) {
					paint(event);

				}
			});

			layout();
			scroller.layout();
			contents.layout();

			this.setBackground(ToolBarFolder.background);

		}

		abstract protected void makeControl();

		protected void paint(Event event) {
			if (name == null)
				name = "";

			if (Platform.isMac())
				event.gc.setFont(new Font(event.display, Constants.defaultFont, 11, SWT.NORMAL));
			else
				event.gc.setFont(new Font(event.display, Constants.defaultFont, 9, SWT.NORMAL));

			Point m = event.gc.textExtent(name);
			int a = event.gc.getFontMetrics().getAscent();
			event.gc.drawText(name, labelWidth - m.x - 10, a - (Platform.isMac() ? 1 : 3), true);

			if (editor != contents.getChildren()[0]) {
				event.gc.setForeground(new Color(Launcher.display, 0, 0, 0));
				event.gc.setAlpha(10);
				event.gc.drawLine(0, 0, 600, 0);
			}
		}
	}

	public class SearchBox extends BaseRow {

		private final boolean isSearch;

		public SearchBox(String name, boolean isSearch) {
			super(name);
			this.isSearch = isSearch;
		}

		@Override
		protected void makeControl() {

			editor = new Text(this, SWT.SEARCH);
			((Text) editor).setText("");

			((Text) editor).addVerifyListener(new VerifyListener() {

				@Override
				public void verifyText(VerifyEvent e) {
					e.doit = true;
					if (isSearch) {
						updateFind((Text) editor);
					}
				}
			});

			if (Platform.isMac())
				editor.setFont(new Font(Launcher.display, Constants.defaultFont, 11, SWT.NORMAL));
		}

	}

	@NextUpdate
	protected void updateFind(Text editor) {

		doSearch(editor.getText());

	}

	private void doSearch(String text) {

		PythonPluginEditor ed = (PythonPluginEditor) PythonPluginEditor.python_plugin.get(root);
		StyledText input = ed.getEditor().getInputEditor();

		int h = ed.getEditor().highlightSearch(text);
		ed.getEditor().getInputEditor().redraw();

		results.info = "found " + h;

		if (scopeBox.getSelection() == 1) {

		} else if (scopeBox.getSelection() == 1) {

		}

	}

	private void doReplace(String text) {

		PythonPluginEditor ed = (PythonPluginEditor) PythonPluginEditor.python_plugin.get(root);
		StyledText input = ed.getEditor().getInputEditor();

		int h = ed.getEditor().doSearchAndReplace(text, ((Text) replaceBox.editor).getText());
		ed.getEditor().getInputEditor().redraw();

	}

	public class ButtonBox extends BaseRow {

		public ButtonBox(String name) {
			super(name);
		}

		@Override
		protected void makeControl() {

			editor = new Button(this, SWT.CENTER);
			((Button) editor).setText(name);

			name = "";

			((Button) editor).addSelectionListener(new SelectionListener() {

				@Override
				public void widgetSelected(SelectionEvent e) {
					doIt();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					// TODO Auto-generated method stub

				}
			});

		}

		protected void doIt() {
		}

	}

	public class DivBox extends BaseRow {

		public DivBox() {
			super("");
		}

		String info = "";

		@Override
		protected void makeControl() {
			editor = new Button(this, SWT.CENTER);
			editor.setVisible(false);
			name = "";
		}

		@Override
		protected void paint(Event event) {
			super.paint(event);

			Font f = event.gc.getFont();
			FontData[] data = f.getFontData();

			event.gc.setFont(new Font(f.getDevice(), data[0].getName(), data[0].getHeight() + 2, SWT.NORMAL));

			Point m = event.gc.textExtent(info);
			int a = event.gc.getFontMetrics().getAscent();
			event.gc.setAlpha(128);
			event.gc.setForeground(new Color(Launcher.display, 0, 0, 0));
			event.gc.drawText(info, Math.min(labelWidth, this.getSize().x - m.x), a - (Platform.isMac() ? 8 : 13), true);

			if (editor != contents.getChildren()[0]) {
				event.gc.setForeground(new Color(Launcher.display, 220, 220, 220));
				event.gc.drawLine(0, 0, 600, 0);
			}

		}

	}
	
	public class TitleBox extends BaseRow {

		public TitleBox() {
			super("");
			this.setBackground(ToolBarFolder.firstLineBackground);
		}

		String info = "";

		@Override
		protected void makeControl() {
			editor = new Button(this, SWT.CENTER);
			editor.setVisible(false);
			name = "";
		}

		@Override
		protected void paint(Event event) {
			super.paint(event);

			Font f = event.gc.getFont();
			FontData[] data = f.getFontData();

			event.gc.setFont(new Font(f.getDevice(), data[0].getName(), data[0].getHeight() + 2, SWT.NORMAL));

			Point m = event.gc.textExtent(info);
			int a = event.gc.getFontMetrics().getAscent();
			event.gc.setAlpha(255);
			event.gc.setForeground(new Color(Launcher.display, 0, 0, 0));
			event.gc.drawText(info, this.getSize().x/2-m.x/2, a - (Platform.isMac() ? 4 : 13), true);

			if (editor != contents.getChildren()[0]) {
				event.gc.setForeground(new Color(Launcher.display, 220, 220, 220));
				event.gc.drawLine(0, 0, 600, 0);
			}

		}

	}

	public class ScopeBox extends BaseRow {

		private BetterComboBox bcb;

		public ScopeBox(String name) {
			super(name);
		}

		public int getSelection() {
			return bcb.getCurrentlySelected();
		}

		@Override
		protected void makeControl() {
			bcb = new BetterComboBox(this, new String[] { "Text editor", "Selected box", "Current sheet" });
			editor = bcb.combo;
			editor.setEnabled(false);
		}

	}

	protected void init(String icon, ToolBarFolder folder, ScrolledComposite scroller, Composite contents) {

		this.contents = contents;
		this.scroller = scroller;

		GridLayout gl = new GridLayout();
		gl.makeColumnsEqualWidth = true;
		gl.numColumns = 1;
		gl.verticalSpacing = 0;
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		gl.marginTop = 0;

		contents.setLayout(gl);

		folder.add(icon, scroller);

		scroller.setContent(contents);
		scroller.setExpandHorizontal(true);
		scroller.setExpandVertical(true);

		contents.setBackground(ToolBarFolder.background);

		new TitleBox().info="Find & Replace";
		searchBox = new SearchBox("Find", true);
		results = new DivBox();
		results.info = "";
		replaceBox = new SearchBox("Replace", false);
		scopeBox = new ScopeBox("Scope");
		new ButtonBox("Replace") {
			public void doIt() {
				doReplace(((Text) searchBox.editor).getText());
			}
		};

	}
}
