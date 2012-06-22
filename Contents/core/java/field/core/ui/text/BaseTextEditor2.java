package field.core.ui.text;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.JComponent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineStyleListener;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.GCData;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

import field.bytecode.protect.Woven;
import field.core.Platform;
import field.core.Platform.OS;
import field.core.dispatch.iVisualElement.Rect;
import field.core.plugins.selection.ToolBarFolder;
import field.core.ui.MacScrollbarHack;
import field.core.ui.SmallMenu;
import field.core.ui.SmallMenu.BetterPopup;
import field.core.ui.SmallMenu.Documentation;
import field.core.ui.SmallMenu.iKeystrokeUpdate;
import field.core.ui.UbiquitousLinks;
import field.core.ui.text.embedded.CustomInsertDrawing.Nub;
import field.core.ui.text.embedded.iOutOfBandDrawing;
import field.core.ui.text.rulers.ExecutedAreas.Area;
import field.core.ui.text.rulers.ExecutionRuler;
import field.core.ui.text.rulers.StyledTextPositionSystem.Position;
import field.core.ui.text.rulers.iRuler;
import field.core.ui.text.syntax.PythonScanner;
import field.core.ui.text.syntax.PythonScanner.TokenTypes;
import field.core.util.LocalFuture;
import field.core.util.PythonCallableMap;
import field.core.windowing.BetterSash;
import field.core.windowing.GLComponentWindow;
import field.launch.Launcher;
import field.launch.SystemProperties;
import field.launch.iUpdateable;
import field.namespace.generic.Bind.iFunction;
import field.namespace.generic.ReflectionTools;
import field.util.AutoPersist;

@Woven
public class BaseTextEditor2 {

	public abstract class Completion implements iUpdateable {
		public String text;

		public boolean enabled;
		public boolean isDocumentation = false;

		abstract public void update();

		public String optionalDocumentation;
		public String optionalPlainText;
	}

	public interface iPositionAnnotation {
		public void drawRect(Rectangle r, GC g);

		public Position getEndPosition();

		public Position getStartPosition();

		public String toolTipText();
	}

	public String frameName = "";

	private final int transpInsetX = 50;

	private final int transpInsetY = 25;

	private final int transpWidth = 40;

	private final int transpHeight = 200;

	// private final JPanel toolbarPanel;

	private SashForm vsplit;
	private SashForm hsplit;

	protected StyledText ed;
	protected StyledText edOut;
	protected Shell frame;

	protected ExecutionRuler executionRuler;
	// protected HistoryRuler historyRuler;
	// protected LineNumberRuler lineNumberRuler;

	protected iRuler currentRuler;

	// protected String hint = "";
	// protected float hintAlpha = 0;

	// protected String localCopiedText;

	List<iPositionAnnotation> positionAnnotations = new ArrayList<iPositionAnnotation>();

	// protected boolean clipboardIsOurs = false;

	// TextEditorDecoration textEditorDecoration = new
	// TextEditorDecoration();

	// TODO swt pausedText not implemented right now
	public String pausedText = null;

	protected org.eclipse.swt.widgets.Canvas rulerCanvas;

	private PythonScanner scanner;

	private Composite composite;

	private ToolBar toolbar;

	static public Rectangle defaultRect = new AutoPersist().persist(
			"textEditorPosition", new Rectangle(500, 50, 500, 600));

	private StyledTextUndo undoHelper;

	private Color[] colors;

	PythonCallableMap textDecoration = new PythonCallableMap();

	String searchString = null;

	class LineRec {
		int offset;
		String string;

		public LineRec(int offset, String string) {
			super();
			this.offset = offset;
			this.string = string;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + offset;
			result = prime * result
					+ ((string == null) ? 0 : string.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			LineRec other = (LineRec) obj;
			if (offset != other.offset)
				return false;
			if (string == null) {
				if (other.string != null)
					return false;
			} else if (!string.equals(other.string))
				return false;
			return true;
		}

	}

	LinkedHashMap<LineRec, StyleRange[]> cache;

	private OpportunisticSlider2 sliders;

	public BaseTextEditor2() {

		// ;//System.out.println(" window is <"+GLComponentWindow.lastCreatedWindow);

		// todo ÔøΩ pass in parent

		Composite target = GLComponentWindow.lastCreatedWindow.rightComp;
		final Composite targetWindow = GLComponentWindow.lastCreatedWindow
				.getFrame();
		frame = GLComponentWindow.lastCreatedWindow.getFrame();

		// toolbar = GLComponentWindow.lastCreatedWindow.toolbar;
		// toolbar = target.getToolBar();
		if (toolbar == null) {
			toolbar = new ToolBar(target, SWT.FLAT);
			{
				GridData data = new GridData();
				data.heightHint = 30;
				data.widthHint = 1000;
				data.horizontalAlignment = SWT.FILL;
				data.grabExcessHorizontalSpace = true;
				data.verticalIndent = 0;
				data.horizontalIndent = 0;

				toolbar.setLayoutData(data);
			}
			toolbar.setBackground(ToolBarFolder.background);
			toolbar.getParent().setBackground(ToolBarFolder.background);

		}

		RowLayout fillLayout = new RowLayout();
		toolbar.setLayout(fillLayout);
		fillLayout.marginHeight = 3;
		fillLayout.marginWidth = 2;
		fillLayout.spacing = 4;

		if (Platform.isLinux()) {
			fillLayout.marginHeight = 0;
			fillLayout.marginWidth = 0;
		}

		composite = new Composite(target, 0);
		composite.setLayout(new FillLayout());
		{
			GridData data = new GridData();
			data.grabExcessVerticalSpace = true;
			data.grabExcessHorizontalSpace = true;
			data.verticalAlignment = SWT.FILL;
			data.horizontalAlignment = SWT.FILL;
			composite.setLayoutData(data);
		}

		hsplit = new SashForm(composite, SWT.HORIZONTAL);
		hsplit.setSashWidth(0);
		hsplit.setBackground(ToolBarFolder.sashBackground);

		rulerCanvas = new org.eclipse.swt.widgets.Canvas(hsplit, 0);

		rulerCanvas.setBackground(ToolBarFolder.sashBackground);

		rulerCanvas.addPaintListener(new PaintListener() {

			@Override
			public void paintControl(PaintEvent e) {
				try {
					if (currentRuler != null)
						currentRuler.paintNow(e.gc, ed, rulerCanvas);
				} catch (Exception ee) {
					ee.printStackTrace();
				}
			}
		});

		rulerCanvas.addMouseListener(new MouseListener() {

			@Override
			public void mouseDoubleClick(MouseEvent e) {

			}

			@Override
			public void mouseUp(MouseEvent arg0) {
				currentRuler.mouseUp(arg0);
			}

			@Override
			public void mouseDown(MouseEvent arg0) {
				currentRuler.mouseDown(arg0);

				if (currentRuler == executionRuler) {
					if (executionRuler.getCurrentArea() != null)
						handleMouseEventOnArea(arg0,
								executionRuler.getCurrentArea());
				}

			}
		});

		rulerCanvas.addMouseMoveListener(new MouseMoveListener() {

			@Override
			public void mouseMove(MouseEvent e) {
				currentRuler.mouseMove(e);
			}
		});

		final Canvas c = new Canvas(toolbar, 0) {
			@Override
			public void drawBackground(GC gc, int x, int y, int width,
					int height) {
				// super.drawBackground(gc, x, y, width,
				// height);

				;// System.out.println(" toolbar spacer is <" + this.getBounds()
					// + ">");
			}
		};
		c.setBackground(ToolBarFolder.background);

		c.setLayoutData(new RowData(rulerCanvas.getSize().x, 1));
		rulerCanvas.addListener(SWT.Resize, new Listener() {

			@Override
			public void handleEvent(Event event) {
				c.setLayoutData(new RowData(rulerCanvas.getSize().x - 10 - 25,
						1));
				toolbar.layout();
			}
		});

		vsplit = new SashForm(hsplit, SWT.VERTICAL);
		vsplit.setLayout(new FillLayout());
		vsplit.setSashWidth(10);

		vsplit.setBackground(ToolBarFolder.sashBackground);

		hsplit.setWeights(new int[] { 1, 10 });
		hsplit.setBackground(ToolBarFolder.sashBackground);

		ed = new StyledText(vsplit, SWT.WRAP | SWT.H_SCROLL | SWT.V_SCROLL) {

			String clipAtCopy = null;
			String localCopyRewritten = null;

			public void paste() {

				String clipContents = getClipContents();
				;// System.out.println(" inside paste <" + clipAtCopy + "> <" +
					// clipContents + ">");

				if (clipAtCopy != null && clipAtCopy.equals(clipContents)) {
					System.out.print("local paste" + clipAtCopy);
					String toPaste = localPaste(clipAtCopy, localCopyRewritten);
					if (toPaste != null) {
						return;
					}
				} else {
					clipAtCopy = null;
				}

				String t1 = ed.getText();

				super.paste();
				String t2 = ed.getText();

				;// System.out.println(" text before <" + t1 + "> text after <"
					// + t2 + ">");
			}

			protected String getClipContents() {
				TextTransfer plainTextTransfer = TextTransfer.getInstance();
				String clipContents = (String) new Clipboard(Launcher.display)
						.getContents(plainTextTransfer, DND.CLIPBOARD);
				return clipContents;
			}

			@Override
			public void cut() {
				;// System.out.println(" cut called ");
				super.cut();
				clipAtCopy = getClipContents();
				localCopyRewritten = localCopy(clipAtCopy);
			}

			public void copy() {
				super.copy();
				;// System.out.println(" copy called ");
				clipAtCopy = getClipContents();

				localCopyRewritten = localCopy(clipAtCopy);
			}

			public void copy(int i) {
				super.copy(i);
				;// System.out.println(" copy called <" + i + ">");
			}

		};

		ed.addListener(SWT.Paint, new Listener() {

			int tick = 0;

			@Override
			public void handleEvent(Event event) {
				Rectangle clip = event.gc.getClipping();
				Control[] c = ed.getChildren();

				Rect r = new Rect(clip.x, clip.y, clip.width, clip.height);
				Rect ro = new Rect(clip.x, clip.y, clip.width, clip.height);

				for (Control cc : c) {
					Object data = cc.getData();

					if (data instanceof Nub) {
						JComponent comp = ((Nub) data).getComponent();
						if (comp instanceof iOutOfBandDrawing)
							((iOutOfBandDrawing) comp).expandDamage(r);
					}
				}

				if (!r.equals(ro) && tick == 0) {

					;// System.out.println(" forcing redraw of text editor, we have a changed ro :"
						// + r + " " + ro);

					// ed.redraw((int)r.x, (int)r.y,
					// (int)r.w, (int)r.h, true);
					ed.redraw();
					tick++;
				} else {
					tick = 0;
				}

			}
		});

		edOut = new StyledText(vsplit, SWT.MULTI | SWT.WRAP | SWT.H_SCROLL
				| SWT.V_SCROLL);

		int size = SystemProperties.getIntProperty("editorFontSize",
				(int) (Platform.isMac() ? (Launcher.display.getSystemFont()
						.getFontData()[0].height * 1.25f) : (Launcher.display
						.getSystemFont().getFontData()[0].height)));
		Font font = new Font(Launcher.display,
				field.core.Constants.defaultTextEditorFont, size, SWT.NORMAL);
		ed.setTabs(SystemProperties.getIntProperty("editorTabSize", 8));
		ed.setFont(font);
		ed.setText("");
		edOut.setText("");
		edOut.setFont(font);

		ed.setEnabled(false);

		ed.addVerifyKeyListener(new VerifyKeyListener() {

			@Override
			public void verifyKey(VerifyEvent event) {

				System.out.println(" event.stateMask :" + event.stateMask + " "
						+ Platform.getCommandModifier2() + " " + event.keyCode);

				if (event.keyCode == '\r'
						&& (event.stateMask & Platform.getCommandModifier2()) != 0) {
					if ((event.stateMask & SWT.ALT) != 0) {
						edOut.setText("");
					} else if ((event.stateMask & SWT.SHIFT) == 0)
						executeHandle();
					else
						executeHandleSpecial();
					event.doit = false;
				} else if (event.keyCode == '\''
						&& (event.stateMask & Platform.getCommandModifier2()) != 0) {
					completionQuotedHandle();
					event.doit = false;
				} else if (event.keyCode == '.'
						&& (event.stateMask & Platform.getCommandModifier2()) != 0) {
					completionHandle((event.stateMask & SWT.ALT) == 0);
					event.doit = false;
				} else if (event.keyCode == '/'
						&& (event.stateMask & Platform.getCommandModifier2()) != 0) {
					executePrintHandle();
					event.doit = false;
				} else if (event.keyCode >= '0'
						&& event.keyCode <= '9'
						&& (event.stateMask & Platform.getCommandModifier2()) != 0) {
					executeSpecial(event.keyCode - '0');
					event.doit = false;
				} else if ((event.keyCode == SWT.PAGE_UP)
						&& (event.stateMask & Platform.getCommandModifier2()) != 0) {
					executionBegin();
					event.doit = false;
				} else if ((event.keyCode == SWT.PAGE_DOWN)
						&& (event.stateMask & Platform.getCommandModifier2()) != 0) {
					executionEnd();
					event.doit = false;
				} else if ((event.keyCode == SWT.ARROW_UP)
						&& (event.stateMask & SWT.ALT) != 0) {
					sliders.scroll(1, true);
					;// System.out.println(" scrolling ");
					event.doit = false;
				} else if ((event.keyCode == SWT.ARROW_DOWN)
						&& (event.stateMask & SWT.ALT) != 0) {
					sliders.scroll(-1, true);
					event.doit = false;
				} else if ((event.keyCode == 'i')
						&& (event.stateMask & Platform.getCommandModifier2()) != 0) {
					importHandle();
					event.doit = false;
				} else if ((event.keyCode == 'a')
						&& (event.stateMask & Platform.getCommandModifier2()) != 0) {
					ed.selectAll();
					event.doit = false;
				} else if ((event.keyCode == ']')
						&& (event.stateMask & Platform.getCommandModifier2()) != 0) {
					indentSelectionOrCurrentLine();
					event.doit = false;
				} else if ((event.keyCode == '[')
						&& (event.stateMask & Platform.getCommandModifier2()) != 0) {
					outdentSelectionOrCurrentLine();
					event.doit = false;
				} else if ((event.keyCode == 'z')
						&& (event.stateMask & Platform.getCommandModifier()) != 0) {
					undoHelper.undo();
					event.doit = false;
				} else if ((event.keyCode == 'y')
						&& (event.stateMask & Platform.getCommandModifier()) != 0) {
					undoHelper.redo();
					event.doit = false;
				} else if ((event.keyCode == 'z')
						&& ((event.stateMask & SWT.SHIFT) != 0 && (event.stateMask & Platform
								.getCommandModifier()) != 0)) {
					undoHelper.redo();
					event.doit = false;
				} else if (globalShortcutHook(event)) {
					event.doit = false;
				} else {
					event.doit = true;
				}

				if (textDecoration.known.size() > 0) {
					;// System.out.println(" redrawing everything ");
					ed.redrawRange(0, ed.getText().length(), true);
				}

				if (currentRuler != null)
					rulerCanvas.redraw();
			}
		});

		ed.addPaintListener(new PaintListener() {

			@Override
			public void paintControl(PaintEvent e) {

				if (Platform.isMac()) {
					GCData data = e.gc.getGCData();
					data.state &= ~(1 << 10);
				}

				paintPositionAnnotations(e.gc, ed.getSize().x);

				if (!ed.isEnabled()) {
					// e.gc.setAdvanced(true);
					e.gc.setBackground(new Color(Launcher.display, 200, 200,
							200));
					Rectangle area = ed.getClientArea();
					e.gc.fillRectangle(area);
					e.gc.setForeground(Launcher.display
							.getSystemColor(SWT.COLOR_BLACK));
					e.gc.setAlpha(128);
					String tt = "Disabled (nothing selected)";
					Point rr = e.gc.textExtent(tt);
					e.gc.drawText(tt, area.width / 2 - rr.x / 2, area.height
							/ 2 - rr.y / 2);
				}

				if (ed.isEnabled() && searchString != null)
					paintSearchString(e.gc, searchString);

				for (Control c : ed.getChildren()) {
					// ;//System.out.println(" child control of styled text is <"+c+">");
					Object d = c.getData();
					if (d instanceof Nub) {
						JComponent component = ((Nub) d).getComponent();
						if (component instanceof iOutOfBandDrawing) {
							((iOutOfBandDrawing) component).paintOutOfBand(
									e.gc, ed);
						}
					}
				}

				try {
					textDecoration.invoke(e.gc, ed);
				} catch (Throwable t) {
					t.printStackTrace();
				}

				rulerCanvas.redraw();

				Color b1 = edOut.getBackground();
				Color b2 = ed.getBackground();
				if (!b1.equals(b2)) {
					edOut.setBackground(b2);
				}
			}
		});

		ed.addListener(SWT.KeyDown, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				currentRuler.keyEvent(arg0, ed.getCaretOffset(),
						ed.getSelectionRanges()[0], ed.getSelectionRanges()[1]
								+ ed.getSelectionRanges()[0]);
			}
		});

		ed.addListener(SWT.MouseDown, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				if (arg0.button == 3)
					popUp(arg0);
			}
		});

		undoHelper = new StyledTextUndo(ed);

		ed.setBackground(new Color(ed.getBackground().getDevice(), 85, 85, 85));
		ed.setSelectionBackground(new Color(ed.getBackground().getDevice(), 50,
				50, 60));
		edOut.setBackground(new Color(ed.getBackground().getDevice(), 85, 85,
				85));
		edOut.setSelectionBackground(new Color(ed.getBackground().getDevice(),
				50, 50, 60));

		ed.setMargins(5, 5, 5, 5);
		edOut.setMargins(5, 5, 5, 15);

		colors = new Color[PythonScanner.TokenTypes.values().length];
		Device d = target.getBackground().getDevice();

		/**
		 * public static final Color4 tab = new
		 * AutoPersist().persist("Color4_tab", new Color4(0, 0, 0, 0.1f));
		 * public static final Color4 self = new
		 * AutoPersist().persist("Color4_self", new Color4(0.5f, 0.5f, 0.4f,
		 * 1f)); public static final Color4 localTemp = new
		 * AutoPersist().persist("Color4_localTemp", new Color4(0.5f, 0.5f,
		 * 0.6f, 1f)); public static final Color4 localPersistant = new
		 * AutoPersist().persist("Color4_localPersistant", new Color4(0.6f,
		 * 0.5f, 0.5f, 1f)); public static final Color4 number = new
		 * AutoPersist().persist("Color4_number", new Color4(0.6f, 0.8f, 1,
		 * 1f)); public static final Color4 operator = new
		 * AutoPersist().persist("Color4_operator", new Color4(1, 0.7f, 0.75f,
		 * 1f)); public static final Color4 identifier = new
		 * AutoPersist().persist("Color4_identifier", new Color4(1, 1, 1, 1f));
		 * public static final Color4 string = new
		 * AutoPersist().persist("Color4_string", new Color4(0.7f, 0.7f, 1,
		 * 1f)); public static final Color4 keyword = new
		 * AutoPersist().persist("Color4_keyword", new Color4(0.75f, 0.8f, 1,
		 * 1f)); public static final Color4 decorator = new
		 * AutoPersist().persist("Color4_decorator", new Color4(1.0f, 1.0f,
		 * 1.0f, 1f)); public static final Color4 comment = new
		 * AutoPersist().persist("Color4_comment", new Color4(0.0f, 0.0f, 0.0f,
		 * 0.5f)); public static final Color4 background = new
		 * AutoPersist().persist("Color4_background", new Color4(85/255f,
		 * 85/255f, 85/255f, 1f));
		 */
		colors[PythonScanner.TokenTypes.comment.ordinal()] = new Color(d, 128,
				128, 128);
		colors[PythonScanner.TokenTypes.self.ordinal()] = new Color(d, 128,
				128, 100);
		colors[PythonScanner.TokenTypes.localTemp.ordinal()] = new Color(d,
				128, 128, 160);
		colors[PythonScanner.TokenTypes.localPersistant.ordinal()] = new Color(
				d, 160, 128, 128);
		colors[PythonScanner.TokenTypes.number.ordinal()] = new Color(d, 160,
				200, 255);
		colors[PythonScanner.TokenTypes.operator.ordinal()] = new Color(d, 255,
				200, 210);
		colors[PythonScanner.TokenTypes.identifier.ordinal()] = new Color(d,
				255, 255, 255);
		colors[PythonScanner.TokenTypes.string.ordinal()] = new Color(d, 200,
				200, 255);
		colors[PythonScanner.TokenTypes.keyword.ordinal()] = new Color(d, 210,
				220, 255);
		colors[PythonScanner.TokenTypes.whitespace.ordinal()] = new Color(d,
				255, 255, 255);
		colors[PythonScanner.TokenTypes.decorator.ordinal()] = new Color(d,
				255, 255, 255);

		SyntaxHighlightingStyles2.initStyles(colors, ed);

		scanner = new PythonScanner();
		ed.setData("scanner", scanner);

		ed.addLineStyleListener(new LineStyleListener() {

			{
				BaseTextEditor2.this.cache = new LinkedHashMap<LineRec, StyleRange[]>() {
					protected boolean removeEldestEntry(
							java.util.Map.Entry<LineRec, StyleRange[]> eldest) {
						return size() > 100;
					};
				};
			}

			@Override
			public void lineGetStyle(LineStyleEvent event) {
				String text = event.lineText;

				LineRec lr = new LineRec(event.lineOffset, text);
				StyleRange[] rr = cache.get(lr);

				if (rr != null) {
					event.styles = rr;
					return;
				}

				List<StyleRange> ranges = new ArrayList<StyleRange>();

				int left = 0;

				boolean doNotCache = false;

				while (left < event.lineText.length()) {
					scanner.setString(text.substring(left), 0);
					scanner.scan();

					int token = scanner.token;

					StyleRange s = new StyleRange(left + event.lineOffset,
							scanner.getEndOffset(), colors[token], ed
									.getBackground());

					if (token == TokenTypes.embedded_control.ordinal()) {
						doNotCache = true;

						getStyleForEmbeddedString(
								s,
								text.substring(left,
										left + scanner.getEndOffset()));
						if (s.data instanceof JComponent) {
							int h = ((JComponent) s.data).getMinimumSize().height;
							if (h < 12)
								h = 12;
							s.metrics = new GlyphMetrics((int) (h * 2 / 3.0f),
									(int) (h * 1 / 3.0f), s.metrics.width);
						}
					}

					ranges.add(s);

					// ;//System.out.println(" style <" +
					// text.substring(left)
					// + " -> " + s + ">");

					left = Math.max(left + scanner.getEndOffset(), left + 1);
				}

				event.styles = new StyleRange[ranges.size()];
				for (int i = 0; i < ranges.size(); i++)
					event.styles[i] = ranges.get(i);

				// ;//System.out.println(" cache miss ");

				if (!doNotCache)
					cache.put(lr, event.styles);
			}

		});

		edOut.setEditable(false);

		// getOutputActions();

		edOut.addListener(SWT.MouseDown, new Listener() {

			@Override
			public void handleEvent(Event event) {
				if (Platform.isPopupTrigger(event)) {
					LinkedHashMap<String, iUpdateable> items = getActionOutputMenu();
					BetterPopup m = new SmallMenu().createMenu(items,
							edOut.getShell(), null);
					m.show(Launcher.display.map(edOut, edOut.getShell(),
							new Point(event.x, event.y)));
				}
			}
		});

		ed.addListener(SWT.MouseHover, new Listener() {

			@Override
			public void handleEvent(Event event) {
				String tt = computeToolTip();
				tt = tt == null ? "" : tt;
				ed.setToolTipText(tt);
			}
		});

		executionRuler = new ExecutionRuler(ed, rulerCanvas) {
			public void executeArea(Area area) {
				BaseTextEditor2.this.executeArea(area);
			};

			protected void executeAreaSpecial(Area area) {
				BaseTextEditor2.this.executeAreaSpecial(area);
			};

			protected void executeAreaAndRewrite(Area minIs,
					field.namespace.generic.Bind.iFunction<String, String> up) {
				BaseTextEditor2.this.executeAreaAndRewrite(minIs, up);
			};

			protected field.core.util.LocalFuture<Boolean> runAndCheckArea(
					Area area) {
				return BaseTextEditor2.this.runAndCheckArea(area);
			};

			protected void runAndReviseArea(Area area) {
				BaseTextEditor2.this.runAndReviseArea(area);
			};
		};
		currentRuler = executionRuler;

		sliders = new OpportunisticSlider2(this);

		new MacScrollbarHack(ed);
		new MacScrollbarHack(edOut);
		target.layout();

		new BetterSash(hsplit, false);
		new BetterSash(vsplit, false);

	}

	protected String computeToolTip() {
		return "";
	}

	protected void paintSearchString(GC gc, String ss) {
		String m = ed.getText();
		if (m.length() == 0)
			return;

		int s = 0;
		int i = m.indexOf(ss, s);

		while (i != -1 && s < m.length()) {
			s = i + ss.length();

			Point leftp = ed.getLocationAtOffset(i);
			Point rightp = ed.getLocationAtOffset(i + ss.length());

			Rectangle left = new Rectangle(leftp.x, leftp.y, 0,
					ed.getLineHeight(i));
			Rectangle right = new Rectangle(rightp.x, rightp.y, 0,
					ed.getLineHeight(i));

			gc.setForeground(new Color(Launcher.display, 128, 128, 255));
			gc.setAlpha(255);
			if (leftp.y == rightp.y) {
				gc.drawRectangle(left.union(right));
				gc.setAlpha(64);
				gc.fillRectangle(left.union(right));
			} else {
				left.width = ed.getSize().x - left.x;
				gc.drawRectangle(left);
				left.y += left.height;
				while (left.y < right.y) {
					left.x = 0;
					left.width = ed.getSize().x;
					gc.setAlpha(255);
					gc.drawRectangle(left);
					gc.setAlpha(64);
					gc.fillRectangle(left);
					left.y += left.height;
				}
				left.y = right.y;
				left.height = right.height;
				left.x = 0;
				left.width = 0;
				left.add(right);
				gc.setAlpha(255);
				gc.drawRectangle(left);
				gc.setAlpha(64);
				gc.fillRectangle(left);

			}
			i = m.indexOf(ss, s);
		}
	}

	protected String localCopy(String clipAtCopy) {
		return null;

	}

	protected String localPaste(String clipAtCopy, String localCopyRewritten) {
		return null;
	}

	protected boolean globalShortcutHook(VerifyEvent event) {
		return false;
	}

	protected void handleMouseEventOnArea(MouseEvent arg0, Area currentArea) {
	}

	protected void getStyleForEmbeddedString(StyleRange s, String substring) {
		;// System.out.println(" get style for embedded string <" + s + " " +
			// substring + ">");
	}

	protected void paintRulerNow(GC gc) {
		// TODO Auto-generated method stub

	}

	protected void nextSelection() {
		// TODO Auto-generated method stub

	}

	protected void previousSelection() {
		// TODO Auto-generated method stub

	}

	protected void executionEnd() {
		// TODO Auto-generated method stub

	}

	protected void executionBegin() {
		// TODO Auto-generated method stub

	}

	// JButton actions = null;
	//
	// private JButton getActions() {
	// if (actions == null) {
	// actions = new JButton();
	// actions.setPreferredSize(new Dimension(24, 24));
	// actions.setMaximumSize(new Dimension(24, 24));
	// actions.setMinimumSize(new Dimension(24, 24));
	// // actions.setPressedIcon(new
	// //
	// ImageIcon(getClass().getResource("/content/icons/Action_Pressed.jpg")));
	// // actions.setIcon(new
	// // ImageIcon(getClass().getResource("/content/icons/action.tiff")));
	// // actions.setIcon(BetterComboBox.getDiscloseIcon());
	// actions.setIcon(new ImageIcon("content/icons/Gear.png"));
	// actions.setIconTextGap(0);
	// actions.putClientProperty("Quaqua.Button.style", "square");
	// actions.addActionListener(new ActionListener() {
	// public void actionPerformed(ActionEvent e) {
	// BetterPopup menu = getActionMenu();
	//
	// // TODO swt ÔøΩ text editor
	// // if (menu != null)
	// // menu.show(actions, 20, 10);
	// }
	// });
	//
	// actions.setOpaque(false);
	//
	// }
	// return actions;
	// }
	//

	Button actionsOutput = null;

	private Button getOutputActions() {
		if (actionsOutput == null) {
			// actionsOutput = new Button(edOut, SWT.FLAT);
			// actionsOutput.setText("g");
			//
			// edOut.addListener(SWT.Paint, new Listener() {
			//
			// @Override
			// public void handleEvent(Event event) {
			// LinkedHashMap<String, iUpdateable> items =
			// getActionOutputMenu();
			// if (items == null)
			// return;
			//
			// if (items.size() > 0) {
			// Rectangle ca = edOut.getClientArea();
			// actionsOutput.setEnabled(true);
			// ;//System.out.println(" setting bounds <" + ca + ">");
			// actionsOutput.setBounds(ca.width - 30 + ca.x, 5 +
			// ca.y,
			// 25, 25);
			// actionsOutput.redraw();
			// } else {
			// Rectangle ca = edOut.getClientArea();
			// actionsOutput.setEnabled(false);
			// ;//System.out.println(" setting bounds <" + ca + ">");
			// actionsOutput.setBounds(ca.width - 30 + ca.y, 5 +
			// ca.y,
			// 25, 25);
			// actionsOutput.redraw();
			// }
			// }
			// });
			// actionsOutput.addListener(SWT.Selection, new
			// Listener() {
			//
			// @Override
			// public void handleEvent(Event event) {
			// LinkedHashMap<String, iUpdateable> items =
			// getActionOutputMenu();
			// BetterPopup m = new SmallMenu().createMenu(items,
			// actionsOutput.getShell(), null);
			// m.show(Launcher.display.map(actionsOutput,
			// actionsOutput
			// .getShell(), new Point(event.x, event.y)));
			// }
			// });
		}
		return actionsOutput;
	}

	LinkedHashMap<String, iUpdateable> actionMenu = null;

	protected LinkedHashMap<String, iUpdateable> getActionMenu() {
		return actionMenu;
	}

	LinkedHashMap<String, iUpdateable> actionOutputMenu = null;

	private BetterPopup menu;

	protected LinkedHashMap<String, iUpdateable> getActionOutputMenu() {
		return actionOutputMenu;
	}

	public void setActionMenu(LinkedHashMap<String, iUpdateable> actionMenu) {
		this.actionMenu = actionMenu;
	}

	public void setOutputActionMenu(
			LinkedHashMap<String, iUpdateable> actionMenu) {
		this.actionOutputMenu = actionMenu;
	}

	public void addPositionAnnotation(iPositionAnnotation annotation) {
		positionAnnotations.add(annotation);
		this.frame.redraw();
	}

	public void clearPositionAnnotations() {
		positionAnnotations.clear();
		this.frame.redraw();
	}

	public void completionHandle(final boolean publicOnly) {

		int pos = ed.getCaretOffset();
		Point line = null;
		line = ed.getLocationAtOffset(pos);

		String text = ed.getText();
		int a = text.lastIndexOf("\n", pos - 1);
		if (a == -1)
			a = 0;
		String leftText = text.substring(a, pos).trim();

		// String
		// leftText =
		// ed.getText().substring(0,
		// pos);

		iKeystrokeUpdate ks = new iKeystrokeUpdate() {

			public boolean update(Event arg0) {
				if (arg0 == null) {
					completionHandle(publicOnly);
					return false;
				}

				if (!Character.isISOControl(arg0.character)) {
					;// System.out.println(" inserting character ");
					ed.insert("" + arg0.character);
					ed.setCaretOffset(ed.getCaretOffset() + 1);
					completionHandle(publicOnly);
					return true;
				}

				if (arg0.character == SWT.BS) {
					ed.replaceTextRange(ed.getCaretOffset() - 1, 1, "");
					completionHandle(publicOnly);
					return true;
				}

				;// System.out.println(" what is an option key <" + arg0 + "> <"
					// + arg0.keyCode + ">");
				if (arg0.keyCode == SWT.ALT) {
					completionHandle(!publicOnly);
					return true;
				}
				return false;

				// TODO - swt completion
				// if (arg0.getID() == KeyEvent.KEY_TYPED ||
				// arg0.getKeyChar()
				// == '\b' || arg0.getKeyChar() == '\n') {
				// new MiscNative().disableScreenUpdates();
				// if (arg0 != null && arg0.getKeyChar() ==
				// '\n')
				// arg0.setSource(ed);
				// menu.setVisible(false);
				// if (arg0 != null)
				// try {
				// ;//System.out.println(" sending event <" + arg0
				// + ">");
				// ReflectionTools.findFirstMethodCalled(JEditorPane.class,
				// "processKeyEvent").invoke(ed, new Object[] {
				// arg0 });
				// } catch (IllegalArgumentException e) {
				// e.printStackTrace();
				// } catch (IllegalAccessException e) {
				// e.printStackTrace();
				// } catch (InvocationTargetException e) {
				// e.printStackTrace();
				// }
				// completionHandle(publicOnly);
				// } else if (arg0.getID() ==
				// KeyEvent.KEY_PRESSED) {
				// if (arg0.getKeyCode() == KeyEvent.VK_ALT) {
				// completionHandle(!publicOnly);
				// }
				// }

			}
		};

		List<Completion> completions = getCompletions(leftText, publicOnly, ks);

		if (completions.size() == 0)
			return;

		LinkedHashMap<String, iUpdateable> insert = new LinkedHashMap<String, iUpdateable>();

		boolean optionalYes = completions.size() < 4;

		for (Completion c : completions) {
			if (c == null)
				continue;

			;// System.out.println(" text is <" + c + " " + c.text + ">");

			if (c.optionalDocumentation != null && optionalYes) {
				insert.put(c + "_optional", new Documentation("\n\n"
						+ c.optionalDocumentation));
			}
			if (c.isDocumentation) {
				insert.put("" + c, new Documentation(c.text));
			} else if (c.enabled) {
				insert.put(c.text, c);
			} else {
				insert.put(c.text, null);
			}
		}

		menu = new SmallMenu().createMenu(insert, frame, ks);
		menu.doneHook = new iUpdateable() {

			@Override
			public void update() {
				;// System.out.println(" forcing focus <" + ed + ">");
				frame.forceActive();
				ed.forceFocus();
			}
		};
		// menu.setMinimumSize(new Dimension(500, 50));
		menu.show(Launcher.display
				.map(ed, frame, new Point(line.x + 2, line.y)));

		menu.selectFirst();
		// menu.requestFocusInWindow();
		// ((BetterPopup) menu).getKey().selectSecond();

	}

	public void executeBrowseHandle() {
		String s = ed.getSelectionText();
		if (s == null || s.equals("")) {
			int pos = ed.getCaretOffset();
			String text = ed.getText();
			int a = text.lastIndexOf("\n", pos - 1);
			if (a == -1)
				a = 0;
			int b = text.indexOf("\n", pos);
			if (b == -1)
				b = text.length();
			s = text.substring(a, b);
		}

		executeBrowseHandle(s);

	}

	public void executeHandle() {
	}

	public void executeHandleSpecial() {
	}

	public void frameMoved() {
	}

	public StyledText getInputEditor() {
		return ed;
	}

	// public JPanel getToolbarPanel() {
	// return toolbarPanel;
	// }

	public void removePositionAnnotation(iPositionAnnotation annotation) {
		if (positionAnnotations.remove(annotation))
			this.frame.redraw();
	}

	// @NextUpdate(delay=4)
	public BaseTextEditor2 setVisible(boolean b) {

		if (true)
			return this;

		if (b) {

			System.err.println(" about to open text editor ");
			frame.open();
			System.err.println(" about to open text editor finished");

			try {
				if (Platform.getOS() == OS.mac)
					ReflectionTools.findFirstMethodCalled(
							ReflectionTools.illegalGetObject(frame, "window")
									.getClass(), "setHidesOnDeactivate")
							.invoke(ReflectionTools.illegalGetObject(frame,
									"window"), true);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		} else
			frame.close();
		return this;
	}

	private void completionsForFilename(final String string,
			LinkedHashMap<String, iUpdateable> completions) {

		int lastSlash = string.lastIndexOf('/');
		String left;
		final String right;
		if (lastSlash == -1) {
			left = "./";
			right = string;
		} else {
			left = string.substring(0, lastSlash) + "/";
			right = string.substring(lastSlash + 1, string.length());
		}

		if (left.startsWith("file://"))
			left = left.substring("file://".length());

		final File[] allFiles = new File(left).listFiles(new FilenameFilter() {

			public boolean accept(File dir, String name) {
				return name.startsWith(right);
			}
		});
		for (final File f : allFiles) {
			completions
					.put((f.isDirectory() ? "\u2208 <b>" : "\u2802 <i>")
							+ f.getName() + (f.isDirectory() ? "</b>" : "</i>"),
							new iUpdateable() {
								public void update() {
									int pos = ed.getCaretOffset();
									ed.getContent().replaceTextRange(
											pos - string.length(),
											string.length(),
											f.getAbsolutePath()
													+ (f.isDirectory() ? "/"
															: ""));
									if (f.isDirectory())
										completionQuotedHandle();
								}
							});
		}

		if (allFiles.length == 1) {
			completions
					.put((allFiles[0].isDirectory() ? "\u2208 reveal in Finder - <b>"
							: "\u2802 <i> reveal in Finder - ")
							+ allFiles[0].getName()
							+ " "
							+ (allFiles[0].isDirectory() ? "</b>" : "</i"),
							new iUpdateable() {
								public void update() {
									UbiquitousLinks
											.showPathInFinder(allFiles[0]
													.getAbsolutePath());
								}
							});
		}

	}

	// public TextEditorDecoration getTextEditorDecoration() {
	// return textEditorDecoration;
	// }

	protected void completionQuotedHandle() {

		int pos = ed.getCaretOffset();
		Point line = null;
		line = ed.getLocationAtOffset(pos);

		String text = ed.getText();
		int a = text.lastIndexOf("\n", pos - 1);
		if (a == -1)
			a = 0;
		String leftText = text.substring(a, pos);

		LinkedHashMap<String, iUpdateable> items = new LinkedHashMap<String, iUpdateable>();

		iKeystrokeUpdate ks = new iKeystrokeUpdate() {

			@Override
			public boolean update(Event arg0) {
				if (!Character.isISOControl(arg0.character)) {
					;// System.out.println(" inserting character ");
					ed.insert("" + arg0.character);
					ed.setCaretOffset(ed.getCaretOffset() + 1);
					completionQuotedHandle();
					return true;
				}

				if (arg0.character == SWT.BS) {
					ed.replaceTextRange(ed.getCaretOffset() - 1, 1, "");
					completionQuotedHandle();
					return true;
				}

				return false;
			}
		};

		if (leftText.lastIndexOf("(\"") != -1
				&& leftText.lastIndexOf("(\"") == leftText.lastIndexOf("\"") - 1) {
			if (!completionKeyHandle(leftText, items, ks))
				completionsForFilename(
						leftText.substring(leftText.lastIndexOf("\"") + 1),
						items);
		} else
			completionsForFilename(
					leftText.substring(leftText.lastIndexOf("\"") + 1), items);

		menu = new SmallMenu().createMenu(items, frame, ks);
		menu.doneHook = new iUpdateable() {

			@Override
			public void update() {
				;// System.out.println(" forcing focus <" + ed + ">");
				frame.forceActive();
				ed.forceFocus();
			}
		};
		// menu.setMinimumSize(new Dimension(500, 50));
		menu.show(Launcher.display
				.map(ed, frame, new Point(line.x + 2, line.y)));

		menu.selectFirst();

		// final JPopupMenu m = new SmallMenu().createMenu(items, new
		// iKeystrokeUpdate() {
		// public void update(KeyEvent arg0) {
		// if (arg0.getID() == KeyEvent.KEY_TYPED || arg0.getKeyChar()
		// == '\b')
		// {
		// arg0.setSource(ed);
		// new MiscNative().disableScreenUpdates();
		// menu.setVisible(false);
		// try {
		// ReflectionTools.findFirstMethodCalled(JEditorPane.class,
		// "processKeyEvent").invoke(ed, new Object[] { arg0 });
		// } catch (IllegalArgumentException e) {
		// e.printStackTrace();
		// } catch (IllegalAccessException e) {
		// e.printStackTrace();
		// } catch (InvocationTargetException e) {
		// e.printStackTrace();
		// }
		// completionQuotedHandle();
		// }
		// }
		// });
		// menu = m;
		// m.show(ed, line.x, line.y);
	}

	protected boolean completionKeyHandle(String leftText,
			LinkedHashMap<String, iUpdateable> items, iKeystrokeUpdate ks) {
		return false;
	}

	public void executeArea(Area area) {
	}

	protected void executeAreaSpecial(Area area) {
	}

	protected void executeAreaAndRewrite(Area area, iFunction<String, String> up) {
	}

	protected void runAndReviseArea(Area area) {

	}

	protected LocalFuture<Boolean> runAndCheckArea(Area area) {
		LocalFuture<Boolean> lf = new LocalFuture<Boolean>();
		lf.set(false);
		return lf;
	}

	protected void executeBrowseHandle(String s) {
	}

	protected void executePrintHandle() {
	}

	protected void executeSpecial(int i) {
	}

	// JPopupMenu menu;

	protected void executeSpecial(String s) {
	}

	protected void executeSpecialPrintHandle() {
		// TODO Auto-generated method stub

	}

	protected String getBanner() {
		return "";
	}

	protected List<Completion> getCompletions(String leftText,
			boolean publicOnly, iKeystrokeUpdate ks) {
		ArrayList<Completion> comp = new ArrayList<Completion>();
		return comp;
	}

	protected List<Completion> getImportations(String leftText) {
		return new ArrayList<Completion>();
	}

	protected void getMenuItems(LinkedHashMap<String, iUpdateable> items) {
	}

	// protected void handleMouseEventOnArea(MouseEvent e, Area currentArea)
	// {
	// }

	protected void importHandle() {

		int pos = ed.getCaretOffset();
		Point line = null;
		line = ed.getLocationAtOffset(pos);

		String text = ed.getText();
		int a = text.lastIndexOf("\n", pos - 1);
		if (a == -1)
			a = 0;
		String leftText = text.substring(a, pos).trim();

		// String
		// leftText =
		// ed.getText().substring(0,
		// pos);

		List<Completion> completions = getImportations(leftText);

		if (completions.size() == 0)
			return;

		LinkedHashMap<String, iUpdateable> insert = new LinkedHashMap<String, iUpdateable>();
		for (Completion c : completions) {
			if (c.isDocumentation) {
				insert.put("" + c, new Documentation(c.text));
			} else if (c.enabled) {
				insert.put("\u21e0 " + c.text, c);
			} else {
				insert.put(c.text, null);
			}
		}

		iKeystrokeUpdate ks = new iKeystrokeUpdate() {

			public boolean update(Event arg0) {
				if (arg0 == null) {
					importHandle();
					return false;
				}

				if (!Character.isISOControl(arg0.character)) {
					;// System.out.println(" inserting character ");
					ed.insert("" + arg0.character);
					ed.setCaretOffset(ed.getCaretOffset() + 1);
					importHandle();
					return true;
				}

				if (arg0.character == SWT.BS) {
					ed.replaceTextRange(ed.getCaretOffset() - 1, 1, "");
					importHandle();
					return true;
				}

				return false;
			}
		};

		menu = new SmallMenu().createMenu(insert, frame, ks);
		menu.doneHook = new iUpdateable() {

			@Override
			public void update() {
				;// System.out.println(" forcing focus <" + ed + ">");
				frame.forceActive();
				ed.forceFocus();
			}
		};
		// menu.setMinimumSize(new Dimension(500, 50));
		menu.show(Launcher.display
				.map(ed, frame, new Point(line.x + 2, line.y)));

		menu.selectFirst();

	}

	protected void indentSelectionOrCurrentLine() {
		int start = ed.getSelectionRanges()[0];
		int end = ed.getSelectionRanges()[1] + start;

		String text = ed.getSelectionText();

		ed.getContent().replaceTextRange(start, 0, "\t");
		int in = 1;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '\n') {
				ed.getContent().replaceTextRange(start + i + in + 1, 0, "\t");
				in++;
			}
		}
		ed.setSelection(start);
	}

	protected void nextTab() {
	}

	protected void outdentSelectionOrCurrentLine() {
		int start = ed.getSelectionRanges()[0];
		int end = ed.getSelectionRanges()[1] + start;

		String text = ed.getSelectionText();

		if (!text.startsWith("\t"))
			return;

		ed.getContent().replaceTextRange(start, 1, "");
		int in = 1;
		boolean lastWasNL = false;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '\n') {
				lastWasNL = true;
			} else if (lastWasNL && c == '\t') {
				ed.getContent().replaceTextRange(start + i - in, 1, "");
				lastWasNL = false;
				in++;
			} else
				lastWasNL = false;
		}
	}

	protected void paintPositionAnnotations(GC g2, int width) {
		Iterator<iPositionAnnotation> ii = positionAnnotations.iterator();
		while (ii.hasNext()) {
			iPositionAnnotation pa = ii.next();
			Position start = pa.getStartPosition();
			Position end = pa.getEndPosition();

			int startAt = start.at;
			int endAt = end.at;

			try {

				Point leftp = ed.getLocationAtOffset(startAt);
				Point rightp = ed.getLocationAtOffset(endAt - 1);

				Rectangle left = new Rectangle(leftp.x, leftp.y, 0,
						ed.getLineHeight(startAt));
				Rectangle right = new Rectangle(rightp.x, rightp.y, 0,
						ed.getLineHeight(startAt));

				if (left.y == right.y)
					pa.drawRect(left.union(right), g2);
				else {
					left.width = width - left.x;
					pa.drawRect(left, g2);
					left.y += left.height;
					while (left.y < right.y) {
						left.x = 0;
						left.width = width;
						pa.drawRect(left, g2);
						left.y += left.height;
					}
					left.y = right.y;
					left.height = right.height;
					left.x = 0;
					left.width = 0;
					left.add(right);
					pa.drawRect(left, g2);
				}

			} catch (IllegalArgumentException e) {
				// e.printStackTrace();
				ii.remove();
			}
		}
	}

	protected void popUp(Event arg0) {

		java.util.LinkedHashMap<String, iUpdateable> items = new java.util.LinkedHashMap<String, iUpdateable>();
		getMenuItems(items);

		items.put("Syntax Colors", null);
		items.put(" \u26a1\tCustomize colors", new iUpdateable() {

			@Override
			public void update() {
				SyntaxHighlightingStyles2.openCustomizer(colors, ed,
						new iUpdateable() {

							@Override
							public void update() {
								cache.clear();
							}
						});
			}
		});

		;// System.out.println(" popping up <" + items + "> at <" + arg0.x + " "
			// + arg0.y + ">");

		BetterPopup m = new SmallMenu().createMenu(items, frame, null);
		m.show(Launcher.display.map(ed, frame, new Point(arg0.x, arg0.y)));
		m.doneHook = new iUpdateable() {

			@Override
			public void update() {
				;// System.out.println(" -- done ? ");
			}
		};

	}

	public ToolBar getToolbar() {
		return toolbar;
	}

	public void setName(String n) {
		// frame.setText("Text Editor - " + n);
	}

	public PythonCallableMap getTextDecoration() {
		return textDecoration;
	}

	public StyledTextUndo getUndoHelper() {
		return undoHelper;
	}

	public int highlightSearch(String text) {
		searchString = text;
		if (ed.isEnabled()) {
			String m = ed.getText();
			int s = 0;
			int i = m.indexOf(text, s);

			int match = 0;

			while (i != -1 && s < m.length()) {
				match++;
				s = i + text.length();
				i = m.indexOf(text, s);
			}
			return match;
		} else {
			return 0;
		}
	}

}
