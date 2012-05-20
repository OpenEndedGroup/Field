package field.core.plugins.python;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.undo.UndoableEdit;

import org.python.core.PyGenerator;
import org.python.core.PyObject;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.execution.PythonInterface;
import field.core.plugins.python.PythonPlugin.CapturedEnvironment;
import field.core.ui.text.embedded.HistogramLayerPainter;
import field.core.ui.text.embedded.MinimalButton;
import field.core.ui.text.embedded.MinimalJustLayerPainter;
import field.math.util.Histogram;

/**
 * so you can print a button that does something when it's pushed, these are
 * simpler than the "CustomInsertSystem" because they never need to be persisted
 * 
 * @author marc
 * 
 */
public class OutputInserts {

	public interface iHandlePrintClass {
		public boolean handle(Object toPrint, OutputInserts o, iVisualElement inside);
	}

	static public VisualElementProperty<OutputInserts> outputInserts = new VisualElementProperty<OutputInserts>("outputInserts_");

	static public final List<iHandlePrintClass> handlers = new ArrayList<iHandlePrintClass>();

	static {

		addPrintHandler(new iHandlePrintClass() {
			public boolean handle(Object toPrint, OutputInserts o, iVisualElement inside) {
				if (toPrint instanceof Histogram) {
					printHistogram("histogram", "histogram", inside, (Histogram<Number>) toPrint);
					return true;
				}
				return false;
			}
		});

		addPrintHandler(new iHandlePrintClass() {
			public boolean handle(Object toPrint, OutputInserts o, iVisualElement inside) {
				if (toPrint instanceof PyGenerator) {

					PyObject next = ((PyGenerator) toPrint).__iternext__();
					int num = 0;
					println("[ generator ...", o);
					while (next != null && num < 50) {
						println(num + ": " + next, o);
						next = ((PyGenerator) toPrint).__iternext__();
						num++;
					}
					if (num == 50) {
						println(" ... continues ... ", o);
					}
					println("... ends ]", o);

					return true;
				}
				return false;
			}
		});

		addPrintHandler(new iHandlePrintClass() {

			public boolean handle(Object toPrint, OutputInserts o, iVisualElement inside) {
				if (toPrint instanceof List) {
					List l = ((List) toPrint);
					if (l.size() == 0) {
						println("[--empy list of type " + l.getClass() + "--]", o);
					} else if (l.size() == 1) {
						println("[" + l.get(0) + "]", o);
					} else {
						println("[--" + l.size() + " elements in list of type " + l.getClass() + "...", o);
						for (int i = 0; i < Math.min(50, l.size()); i++) {
							println("     " + i + "\t\u2192\t" + l.get(i), o);
						}
						if (l.size() > 50) {
							println("   and " + (l.size() - 50) + " more", o);
						}
						println("]", o);
					}

					return true;
				}
				return false;
			}
		});

		addPrintHandler(new iHandlePrintClass() {

			public boolean handle(Object toPrint, OutputInserts o, iVisualElement inside) {
				if (toPrint instanceof Map) {
					Map l = ((Map) toPrint);
					if (l.size() == 0) {
						println("{--empy map of type " + l.getClass() + "--}", o);
					} else if (l.size() == 1) {
						println("{" + l.entrySet().iterator().next() + "}", o);
					} else {
						println("{--" + l.size() + " elements in map of type " + l.getClass() + "...", o);

						Iterator<Map.Entry> ii = l.entrySet().iterator();
						for (int i = 0; i < Math.min(50, l.size()); i++) {
							Map.Entry kk = ii.next();
							println("     " + kk.getKey() + "\t\u2192\t" + kk.getValue(), o);
						}
						if (l.size() > 50) {
							println("   and " + (l.size() - 50) + " more", o);
						}
						println("}", o);
					}

					return true;
				}
				return false;

			}
		});
	}

	static public void addPrintHandler(iHandlePrintClass c) {
		handlers.add(c);

	}


	protected static UndoableEdit closeFold(String name, OutputInserts oi) {

		try {
			String text = oi.document.getText(0, oi.document.getLength());
			String n1 = oi.styleNameForName(name + ":start");
			String n2 = oi.styleNameForName(name + ":end");
			int start = text.indexOf(n1) + n1.length();
			int end = text.indexOf(n2, start);
			final UndoableEdit[] ee = { null };
			UndoableEditListener uel = new UndoableEditListener() {
				public void undoableEditHappened(UndoableEditEvent e) {
					;//System.out.println(" got edit <" + e + ">");
					ee[0] = e.getEdit();
				}
			};

			oi.document.addUndoableEditListener(uel);
			;//System.out.println(" about to get edit ? ");
			oi.document.remove(start, end - start);
			oi.document.removeUndoableEditListener(uel);

			;//System.out.println(" edit is <" + ee[0] + ">");
			return ee[0];
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		return null;
	}

	static public void printButton(String name, iVisualElement inside, int width, final PyObject call) {
		OutputInserts oi = outputInserts.get(inside);

		PythonPlugin ed = PythonPlugin.python_plugin.get(inside);
		if (ed == null || oi == null || (!(ed instanceof PythonPluginEditor)))
			return;

		final CapturedEnvironment captured = ed.new CapturedEnvironment(inside);

		((PythonPluginEditor) ed).getEditor().getOutputFlusher().update();

		oi.printComponent(new MinimalButton(name, width) {
			@Override
			public void execute() {
				captured.enter();
				try {
					call.__call__();
				} finally {
					captured.exit();
				}
			}
		});
	}


	static public void printButton(String name, iVisualElement inside, final PyObject call) {
		OutputInserts oi = outputInserts.get(inside);

		PythonPlugin ed = PythonPlugin.python_plugin.get(inside);
		if (ed == null || oi == null || (!(ed instanceof PythonPluginEditor)))
			return;

		final CapturedEnvironment captured = ed.new CapturedEnvironment(inside);

		((PythonPluginEditor) ed).getEditor().getOutputFlusher().update();

		oi.printComponent(new MinimalButton(name) {
			@Override
			public void execute() {
				captured.enter();
				try {
					call.__call__();
				} finally {
					captured.exit();
				}
			}
		});
	}

	static public void printFold(String name, iVisualElement inside, final String text) {
		OutputInserts oi = outputInserts.get(inside);

		PythonPlugin ed = PythonPlugin.python_plugin.get(inside);
		if (ed == null || oi == null || (!(ed instanceof PythonPluginEditor)))
			return;

		final CapturedEnvironment captured = ed.new CapturedEnvironment(inside);

		((PythonPluginEditor) ed).getEditor().getOutputFlusher().update();

		oi.printComponent(new MinimalButton(name, 200) {
			@Override
			public void execute() {
				captured.enter();
				try {
					PythonInterface.getPythonInterface().getOutputRedirects().peek().write("\n" + text + "\n");
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					captured.exit();
				}
			}
		});
	}

	static public void printHistogram(String text, String name, iVisualElement inside, field.math.util.Histogram<Number> n) {
		OutputInserts oi = outputInserts.get(inside);

		PythonPlugin ed = PythonPlugin.python_plugin.get(inside);
		if (ed == null || oi == null || (!(ed instanceof PythonPluginEditor)))
			return;

		final CapturedEnvironment captured = ed.new CapturedEnvironment(inside);

		((PythonPluginEditor) ed).getEditor().getOutputFlusher().update();
		MinimalJustLayerPainter mi = new MinimalJustLayerPainter(30);
		mi.setPainterForName(name, new HistogramLayerPainter().setHistogram(n));
		oi.print(text);
		oi.printComponent(mi);
	}

	static public void specialPrint(Object o, iVisualElement inside) {
		PythonPlugin ed = PythonPlugin.python_plugin.get(inside);
		OutputInserts oi = outputInserts.get(inside);
		if (ed == null || oi == null || (!(ed instanceof PythonPluginEditor)))
			return;

		try {
			for (iHandlePrintClass c : handlers) {
				if (c.handle(o, oi, inside))
					return;
			}
			println(o, oi);
		} finally {
			((PythonPluginEditor) ed).getEditor().getOutputFlusher().update();
			((PythonPluginEditor) ed).getEditor().getOutputFlusher().scrollToBottomOfOutput();
		}
	}

	private static void println(Object o, OutputInserts oi) {
		try {
			oi.document.insertString(oi.document.getEndPosition().getOffset() - 1, "" + o + "\n", oi.document.getStyle("regular"));
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	};

	public final DefaultStyledDocument document;

	public OutputInserts(DefaultStyledDocument document) {
		this.document = document;
	}

	public void print(String t) {
		try {
			document.insertString(document.getEndPosition().getOffset() - 1, t + "\n", document.getStyle("regular"));
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	public void printComponent(JComponent component) {
		try {
			document.insertString(document.getEndPosition().getOffset() - 1, "((printed component))", getAttributeSetForComponent(component));
			document.insertString(document.getEndPosition().getOffset(), "\n", null);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	private void printComponent(JComponent component, String name) {
		try {
			document.insertString(document.getEndPosition().getOffset() - 1, styleNameForName(name), getAttributeSetForComponent(component));
			document.insertString(document.getEndPosition().getOffset(), "\n", null);

		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}

	private String styleNameForName(String name) {
		return "((printed_" + name + "_component))";
	}

	private AttributeSet getAttributeSetForComponent(JComponent component) {
		Style style = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);
		StyleConstants.setComponent(style, component);
		return style;
	}

}
