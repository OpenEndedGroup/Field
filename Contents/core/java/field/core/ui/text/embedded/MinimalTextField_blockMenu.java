package field.core.ui.text.embedded;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.python.core.Py;
import org.python.core.PyFloat;
import org.python.core.PyFunction;
import org.python.core.PyModule;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyTuple;

import field.core.Constants;
import field.core.Platform;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.execution.PythonInterface;
import field.core.plugins.python.PythonPlugin;
import field.core.ui.PopupTextBox;
import field.core.ui.SmallMenu;
import field.core.ui.text.PythonTextEditor;
import field.core.ui.text.embedded.CustomInsertDrawing.Nub;
import field.core.ui.text.embedded.CustomInsertDrawing.iAcceptsInsertRenderingContext;
import field.core.ui.text.embedded.CustomInsertDrawing.iInsertRenderingContext;
import field.core.ui.text.embedded.CustomInsertSystem.ProvidedComponent;
import field.core.util.FieldPyObjectAdaptor.iExtensible;
import field.launch.Launcher;
import field.launch.iUpdateable;
import field.math.abstraction.iAcceptor;
import field.math.abstraction.iPhasicAcceptor;
import field.namespace.generic.Generics.Pair;
import field.namespace.key.OKey;
import field.util.Dict;
import field.util.PythonUtils.OKeyByName;

public class MinimalTextField_blockMenu extends MinimalTextField implements iPhasicAcceptor<Object>, iAcceptsInsertRenderingContext {

	static public class Component_transformBlockEnd extends ProvidedComponent {
		protected String valueString = "";

		@Override
		public void deserialize(iVisualElement inside) {
			component = new MinimalTextField_blockMenu() {
				@Override
				public void setText(String to) {
					super.setText(to);
					Component_transformBlockEnd.this.valueString = to;
				};

				@Override
				protected boolean isStart() {
					return false;
				}

				@Override
				public int getRealWidth() {

					int p1 = getPositionFor(true);
					MinimalTextField com = findMatchingComponent(false);
					if (com == null)
						return super.getRealWidth();
					int p2 = com.getPositionFor(false);
					
					return 500;
//
//					if (insertRenderingContext!=null)
//					{
//						Rect boundrect = boundsOfRange(insertRenderingContext, p1, p2);
//						return (int) Math.max(130, boundrect.w + 20);
//					}
//					else
//					{
//						return 300;
//					}
				}

				@Override
				public int getPositionFor(boolean starting) {
					if (insertRenderingContext!=null)
					
						return Component_transformBlockEnd.this.getPositionFor(insertRenderingContext, starting);
					else
						return -1;
				}
			};

			((MinimalTextField_blockMenu) component).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					Component_transformBlockEnd.this.valueString = ((MinimalTextField) component).getText();
				}
			});

			((MinimalTextField_blockMenu) component).setText(valueString);
			((MinimalTextField_blockMenu) component).setCaption("(transform block end, click to edit)");

		}

		@Override
		public String getCurrentRepresentedString() {
			return "\"\"\", globals())\n#--{\\" + valueString + "}";
		}

		public String name() {
			String[] q = valueString.split(" ");
			if (q.length == 0)
				return "untitled";
			String m = q[q.length - 1];
			if (m.length() == 0)
				return "unititled";
			if (!Character.isJavaIdentifierStart(m.charAt(0))) {
				m = "_" + m;
			}
			StringBuffer cm = new StringBuffer(m);
			for (int i = 0; i < m.length(); i++) {
				if (!Character.isJavaIdentifierPart(m.charAt(i)) && m.charAt(i) != '(' && m.charAt(i) != ')') {
					cm.setCharAt(i, '_');
				}
				if (m.charAt(i)=='(')
					break;
			}
			return cm.toString();
		}

		@Override
		public void preserialize() {
		}

		protected void updateValue() {
		}
	}

	static public class Component_transformBlockStart extends ProvidedComponent {
		protected String valueString = "";

		String name;

		OKey<MinimalTextFieldReference> localKey;

		MinimalTextFieldReference reference;

		@Override
		public void deserialize(final iVisualElement inside) {
			if (name == null)
				name = "Component_transformBlockStart:" + new UID().toString() + ".transient";
			component = new MinimalTextField_blockMenu() {
				@Override
				public void setText(String to) {
					super.setText(to);
					Component_transformBlockStart.this.valueString = to;
				};

				@Override
				protected void callPainter(PyObject painter, Graphics2D g, Rect r) {
					PythonInterface.getPythonInterface().setVariable("_self", inside);
					PythonPlugin.toolsModule.__dict__.__setitem__("_self".intern(), Py.java2py(inside));
					PyObject[] args = { Py.java2py(new OKeyByName(name, null)), Py.java2py(g), Py.java2py(r) };
					painter.__call__(args);
				}

				@Override
				protected Map<String, iUpdateable> callMenu(PyObject painter) {
					PythonInterface.getPythonInterface().setVariable("_self", inside);
					PythonPlugin.toolsModule.__dict__.__setitem__("_self".intern(), Py.java2py(inside));
					PyObject[] args = { Py.java2py(new OKeyByName(name, null)) };
					PyObject o = painter.__call__(args);
					if (o == null)
						return null;
					if (o.equals(Py.None))
						return null;
					return (Map<String, iUpdateable>) o.__tojava__(Object.class);
				}

				@Override
				public int getRealWidth() {

					int p1 = getPositionFor(false);
					MinimalTextField com = findMatchingComponent(true);

					if (com == null)
						return super.getRealWidth();

					int p2 = com.getPositionFor(true);

					return 500;
					
//					if (insertRenderingContext!=null)
//					{
//						Rect boundrect = boundsOfRange(insertRenderingContext, p1, p2);
//						return (int) Math.max(130, boundrect.w + 20);
//					}
//					else
//					{
//						return 300;
//					}
				}

				@Override
				public int getPositionFor(boolean starting) {
					if (insertRenderingContext!=null)
						return Component_transformBlockStart.this.getPositionFor(insertRenderingContext, starting);
					else
						return -1;
				}
			};

			((MinimalTextField_blockMenu) component).addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					Component_transformBlockStart.this.valueString = ((MinimalTextField) component).getText();
					updateValue();
				}
			});

			((MinimalTextField_blockMenu) component).setText(valueString);
			((MinimalTextField_blockMenu) component).setCaption("(transform block start, click to edit)");

			if (reference == null) {
				reference = new MinimalTextFieldReference();
			}

			reference.reference = (MinimalTextField_blockMenu) component;
			updateValue();
		}

		@Override
		public String getCurrentRepresentedString() {

			// return "#--{" + valueString +
			// "}";

			// return "\n" + "if (not
			// _a.__localFreeze_):\n" + "
			// _a.__localFreeze_ =
			// FreezeProperties()\n" + "
			// FreezeProperties.standardCloneHelpers(_a.__localFreeze_)\n"
			// + "\n" + "_a.__localFreeze_"
			// + name() + " =
			// Freeze(_a.__localFreeze_).freeze(_self)\n"
			// + "\n" + "#--{" + valueString
			// + "}\n" + "\n" +
			// "_a.__localFreeze_" + name()
			// + ".thaw(_self)";

			return "#--{" + valueString + "}\n\u000b" + name() + "(OKeyByName(\"" + name + "\", None), r\"\"\"\n";
			// return "#--{" + valueString +
			// "}\n" + getPreamble(name()) +
			// "\n" + "def __transformer_" +
			// name() + "(_text, g):\n" +
			// IndentationUtils.indentTo(1,
			// getTransformerBody(name())) +
			// "\n" + "__transformer_" +
			// name() + "(r\"\"\"\n";

		}

		public String name() {
			return MinimalTextField_blockMenu.name(valueString);
		}

		@Override
		public void preserialize() {
		}

		protected void updateValue() {
			if (localKey == null)
				localKey = new OKey<MinimalTextFieldReference>(name).rootSet(reference);
			localKey.rootSet(reference);
		}

	}

	static public String name(String valueString) {
		String[] q = valueString.split("[ \\.]");
		if (q.length == 0)
			return "untitled";
		String m = q[q.length - 1];
		if (m.length() == 0)
			return "unititled";
		if (!Character.isJavaIdentifierStart(m.charAt(0))) {
			m = "_" + m;
		}
		StringBuffer cm = new StringBuffer(m);
		for (int i = 0; i < m.length(); i++) {
			if (!Character.isJavaIdentifierPart(m.charAt(i)) && m.charAt(i) != '(' && m.charAt(i) != ')') {
				cm.setCharAt(i, '_');
			}
			if (m.charAt(i)=='(')
				break;
		}
		return cm.toString();
	}

	static public class MinimalTextFieldReference implements Serializable, iExtensible {
		transient MinimalTextField_blockMenu reference;
		Dict dict;

		public Dict getDict() {
			if (dict == null)
				dict = new Dict();
			return dict;
		}
	}

	static public List<Pair<String, String>> knownTextTransforms = new ArrayList<Pair<String, String>>();

	String[] options;

	String label = "";

	int isExecuting = 0;

	float progress = 1;


	public void begin() {
		isExecuting++;
		causeRepaint();
	}

	public void end() {
		isExecuting--;
		causeRepaint();
	}

	public MinimalTextField_blockMenu() {
		this.setFont(new Font(Constants.defaultFont, Font.BOLD, 11));
	}

	public String getCurrentNumberPart() {
		String q = getText();
		String[] c = q.split("[\\\\ \\.]");
		for (int i = 0; i < c.length; i++) {
			try {
				int n = Integer.parseInt(c[i]);
				return "" + n + ". ";
			} catch (NumberFormatException e) {
			}
		}
		return "";
	}

	public String getSlash() {
		if (getText().startsWith("\\"))
			return "\\";
		return "";
	}

	public iAcceptor set(Object to) {

		if (to instanceof Number) {
			float f = (float) ((Number) to).floatValue();
			if (Math.abs(progress - f) > 1e-3 || (f == 1 && progress != 1)) {
				progress = f;
				causeRepaint();
			}
		} else if (to instanceof PyFloat) {
			float f = (float) ((PyFloat) to).getValue();
			if (Math.abs(progress - f) > 1e-3 || (f == 1 && progress != 1)) {
				progress = f;
				causeRepaint();
			}
		} else if (to instanceof PyString) {
			Object c = ((PyObject) to).__tojava__(String.class);
			if (!label.equals(c)) {
				label = (String) c;
				causeRepaint();
			}
		} else if (to instanceof PyTuple) {
			for (int i = 0; i < ((PyObject) to).__len__(); i++) {
				set(((PyTuple) to).__getitem__(i));
			}
		}
		return this;
	}

	private void causeRepaint() {
		if (this.getParent() != null && this.getParent().getParent() != null)
			this.getParent().getParent().repaint();
	}

	private void paintProgress(Graphics2D g2, float percentage, Rect upper, Rect lower) {

		if (progress == 1)
			return;
		float r = 16;

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		java.awt.geom.Arc2D.Double d = new Arc2D.Double(new Rectangle2D.Double(11 + upper.x + upper.w - r * 2 - 2, 10 + lower.y - r - 2, r, r), 90, -percentage * 360, Arc2D.PIE);
		java.awt.geom.Arc2D.Double d3 = new Arc2D.Double(new Rectangle2D.Double(11 + upper.x + upper.w - r * 2 - 2, 10 + lower.y - r - 2, r, r), 90, -percentage * 360, Arc2D.OPEN);
		java.awt.geom.Ellipse2D.Double d2 = new Ellipse2D.Double(11 + upper.x + upper.w - r * 2 - 2, 10 + lower.y - r - 2, r, r);
		g2.setColor(new Color(0.3f / 8, 0.5f / 8, 0.39f / 8, 0.5f));
		g2.fill(d);
		g2.setStroke(new BasicStroke(0.5f));
		g2.setColor(new Color(0.3f / 8, 0.5f / 8, 0.39f / 8, 0.1f));
		g2.draw(d3);
		g2.draw(d2);

		g2.setFont(new Font(Constants.defaultFont, Font.PLAIN | Font.BOLD, 16));

		if (label.length() > 0) {
			int width = g2.getFontMetrics().bytesWidth(label.getBytes(), 0, label.length());
			g2.drawString(label, (float) (upper.x + upper.w - r * 2 - 2 - 5 - width), (float) (lower.y - 2));
		}
	}

	protected boolean isStart() {
		return true;
	}

	@Override
	protected boolean isBottom() {
		return !isStart();
	}

	@Override
	protected void paintBorderPath(Graphics2D g2, GeneralPath path) {
		if (true)
			return;

		Rectangle b = path.getBounds();

		// g2.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND,
		// BasicStroke.JOIN_ROUND, 0.5f, new float[] { 3, 3 }, 0));

		g2.setColor(new Color(0.5f, 0.5f, 0.5f, 0.1f));
		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
		g2.fill(path);
	}

	@Override
	protected void paintSpace(Graphics2D g2, Rect loc, Rect lower, boolean missing) {

		int width = getRealWidth();
		loc.w = width;

		if (isExecuting > 0)
			g2.setColor(new Color(0.3f, 0.5f, 0.39f, 0.15f));
		else
			g2.setPaint(new GradientPaint(new Point2D.Double(loc.x, loc.y), new Color(1, 1, 1, 0.1f), new Point2D.Double(lower.x, lower.y), new Color(0, 0, 0, 0.1f)));

		g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

		{
			GeneralPath path = new GeneralPath();
			Rectangle bounds = this.getBounds();
			path.moveTo((float) (loc.x), (float) (loc.y + 1));
			path.lineTo((float) (loc.x + loc.w), (float) (loc.y + 1));
			path.lineTo((float) (loc.x + loc.w), (float) (lower.y + lower.h));
			path.lineTo((float) (loc.x), (float) (lower.y + lower.h));
			path.lineTo((float) (loc.x), (float) (loc.y + 1));
			if (!missing)
				g2.fill(path);
		}
		g2.setColor(new Color(0, 0, 0, 0.1f));
		{
			GeneralPath path = new GeneralPath();
			path.moveTo((float) (loc.x + loc.w - 15), (float) (loc.y + 3));
			path.lineTo((float) (loc.x + loc.w - 15), (float) (lower.y + lower.h - 3));

			g2.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0.5f, new float[] { 6, 6 }, 0));
			if (!missing)
				g2.draw(path);
		}

		int r = 3;
		Graphics2D g3 = (Graphics2D) g2.create();
		g3.clipRect(0, (int) loc.y, (int) (loc.x + loc.w), (int) (lower.y + lower.h - loc.y));
		g3.fillOval((int) (loc.x + loc.w - 15 - r), (int) (loc.y - r), r * 2, r * 2);
		g3.fillOval((int) (loc.x + loc.w - 15 - r), (int) (lower.y + lower.h - 1 - r), r * 2, r * 2);

		if (isExecuting>0)
			paintProgress(g2, progress, loc, lower);

		try {
			Object method = PythonInterface.getPythonInterface().getVariable(name(this.getText()));
			if (method != null && method instanceof PyObject) {
				PyObject found = ((PyObject) method).__findattr__("paint".intern());
				if (found != null) {
					PyObject painter = ((PyObject) method).__getattr__("paint".intern());
					if (painter != null) {
						callPainter(painter, g2, new Rect(loc.x, loc.y + loc.h, loc.w, lower.y - loc.y - loc.h));
					}
				}
			}

		} catch (Exception e) {
			System.err.println(" harmless exception thrown in custom paint code");
			e.printStackTrace();
		}
	}

	protected void callPainter(PyObject painter, Graphics2D g2, Rect rect) {
	}

	protected Map<String, iUpdateable> callMenu(PyObject menu) {
		return null;
	}

	@Override
	protected void processMouseEvent(MouseEvent e) {
		super.processMouseEvent(e);
		
		if (e.getClickCount()==1 && !Platform.isPopupTrigger(e))
		{
			Point loc = Launcher.display.getCursorLocation();
			
			
			final Nub inside = CustomInsertDrawing.currentNub;
			new PopupTextBox.Modal().getString(new java.awt.Point(loc.x, loc.y), "label: ", getText(), new iAcceptor<String>() {

				@Override
				public iAcceptor<String> set(String to) {
				
					MinimalTextField_blockMenu.this.setText(to);
					if (inside!=null)
					{
						inside.canvas.redraw();
					}
					
					return this;
				}
			});
			
		}
		
		if (Platform.isPopupTrigger(e)) {

			Map<String, iUpdateable> additional = null;

			try {
				Object method = PythonInterface.getPythonInterface().getVariable(name(this.getText()));
				if (method != null && method instanceof PyObject) {
					PyObject found = ((PyObject) method).__findattr__("menu".intern());

					if (found != null) {
						PyObject painter = ((PyObject) method).__getattr__("menu".intern());
						if (painter != null) {
							additional = callMenu(painter);
						}
					}
				}

			} catch (Exception ee) {
				System.err.println(" harmless exception thrown in custom paint code");
				ee.printStackTrace();
			}

			LinkedHashMap<String, iUpdateable> items = new LinkedHashMap<String, iUpdateable>();

			if (additional != null) {
				items.put("Menu for '" + name(getText()) + "'", null);
				Set<Entry<String, iUpdateable>> ex = additional.entrySet();
				for (final Entry<String, iUpdateable> ee : ex) {
					items.put(ee.getKey(), new iUpdateable() {

						public void update() {
							ee.getValue().update();
							causeRepaint();
						}
					});
				}
			}

			PyModule q = (PyModule) PythonInterface.getPythonInterface().getVariable("TextTransforms");

			items.put("Available transforms, from TextTransforms.*", null);

			
			List l = (List) q.__dir__();
			for (int i = 0; i < l.size(); i++) {
				final String name = (String) l.get(i);
				;//System.out.println(" :: "+q.__getattr__(name)+" "+q.__getattr__(name).getClass());
				if (!name.startsWith("__") && name.length()>3 && q.__getattr__(name).getClass().isAssignableFrom(PyFunction.class)) {

					try {
						PyObject doc = (PyObject) PythonInterface.getPythonInterface().eval("TextTransforms." + name);
						String d = (String) doc.__getattr__("__doc__").__tojava__(String.class);

						if (d.length() > 0)
							d = "" + PythonTextEditor.limitDocumentation(d);

						;//System.out.println(name+" <"+d+">");
						
						
						items.put(" \u223d  <b>" + name + "</b> \u2014 <i>" + d.replace("\n", " ").trim() + "</i>", new iUpdateable() {

							public void update() {

								String ss = getSlash();
								String sn;
								if (ss.length() == 0)
									sn = "\\";
								else
									sn = "";
								MinimalTextField c = MinimalTextField_blockMenu.this.findMatchingComponent(ss.length() == 0 ? true : false);
								if (c != null)
									c.setText(sn + getCurrentNumberPart() + name);

								MinimalTextField_blockMenu.this.setText(getSlash() + getCurrentNumberPart() + name);
							}
						});
					} catch (Exception ex) {
					}
				}
			}

			if (knownTextTransforms.size() > 0) {
				items.put("Available transforms, from knownTextTransforms", null);
				for (final Pair<String, String> s : knownTextTransforms) {
					items.put(" \u223d  <b>" + s.left + "</b> \u2014 <i>" + s.right + "</i>", new iUpdateable() {

						public void update() {

							String ss = getSlash();
							String sn;
							if (ss.length() == 0)
								sn = "\\";
							else
								sn = "";
							MinimalTextField c = MinimalTextField_blockMenu.this.findMatchingComponent(ss.length() == 0 ? true : false);
							if (c != null)
								c.setText(sn + getCurrentNumberPart() + s.left);

							MinimalTextField_blockMenu.this.setText(ss + getCurrentNumberPart() + s.left);

						}
					});
				}
			}

			Canvas canvas = insertRenderingContext.getControl();
			Point p = Launcher.display.map(canvas, insertRenderingContext.getControl().getShell(), new Point(e.getX(), e.getY()));
			new SmallMenu().createMenu(items, insertRenderingContext.getControl().getShell(), null).show(p);

		}
	}


}
