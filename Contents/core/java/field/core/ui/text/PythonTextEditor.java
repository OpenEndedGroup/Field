package field.core.ui.text;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Shell;
import org.python.core.CodeFlag;
import org.python.core.Py;
import org.python.core.PyFunction;
import org.python.core.PyIterator;
import org.python.core.PyJavaPackage;
import org.python.core.PyJavaType;
import org.python.core.PyList;
import org.python.core.PyMethod;
import org.python.core.PyObject;
import org.python.core.PyObjectDerived;
import org.python.core.PyReflectedFunction;
import org.python.core.PyStringMap;
import org.python.core.PySystemState;
import org.python.core.PyTableCode;
import org.python.core.PyType;

import com.thoughtworks.qdox.JavaDocBuilder;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.Type;

import field.bytecode.protect.Trampoline2.MyClassLoader;
import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.HiddenInAutocomplete;
import field.bytecode.protect.annotations.NextUpdate;
import field.core.Constants;
import field.core.Platform;
import field.core.dispatch.iVisualElement;
import field.core.execution.PythonInterface;
import field.core.ui.SmallMenu.iKeystrokeUpdate;
import field.core.ui.text.protect.ClassDocumentationProtect.Comp;
import field.core.ui.text.protect.ClassDocumentationProtect.CompProxy;
import field.core.ui.text.rulers.ExecutedAreas.Area;
import field.core.ui.text.rulers.ExecutedAreas.State;
import field.core.ui.text.rulers.ExecutionRuler;
import field.core.ui.text.rulers.StyledTextPositionSystem;
import field.core.util.FieldPyObjectAdaptor.PyExtensibleJavaInstance;
import field.core.util.LocalFuture;
import field.launch.Launcher;
import field.launch.SystemProperties;
import field.launch.iUpdateable;
import field.math.linalg.Quaternion;
import field.math.linalg.Vector2;
import field.math.linalg.Vector3;
import field.math.linalg.Vector4;
import field.math.linalg.VectorN;
import field.namespace.generic.Bind.iFunction2;
import field.namespace.generic.ReflectionTools;
import field.util.PythonUtils;

@Woven
public class PythonTextEditor extends BaseTextEditor2 {

	static public class PickledCompletionInformation {
		List info;

		public PickledCompletionInformation(List info) {
			super();
			this.info = info;
		}
	}

	static public final HashMap<String, String> knownOperators = new HashMap<String, String>();
	static {
		knownOperators.put("__mul__", "*");
		knownOperators.put("__imul__", "*=");
		knownOperators.put("__add__", "+");
		knownOperators.put("__iadd__", "+=");
		knownOperators.put("__sub__", "-");
		knownOperators.put("__isub__", "-=");
		knownOperators.put("__div__", "/");
		knownOperators.put("__idiv__", "/=");
		knownOperators.put("__div__", "/");
		knownOperators.put("__idiv__", "/=");
		knownOperators.put("__lshift__", "<<");
		knownOperators.put("__rshift__", ">>");
		knownOperators.put("__ilshift__", "<<=");
		knownOperators.put("__irshift__", ">>=");
	}

	static public class ClassRecord {
		String firstName;

		String fullName;

		public ClassRecord(String firstName, String fullName) {
			this.firstName = firstName;
			this.fullName = fullName;
		}

		@Override
		public boolean equals(Object obj) {
			return firstName.equals(((ClassRecord) obj).firstName) && fullName.equals(((ClassRecord) obj).fullName);
		}

		@Override
		public int hashCode() {
			return firstName.hashCode() + fullName.hashCode();
		}
	}

	public interface EditorExecutionInterface {

		public void executeFragment(String fragment);

		public Object executeReturningValue(String string);
	}

	boolean scrollLock = false;

	@NextUpdate
	protected void scrollToBottomOfOutput() {
		// edOut.getParent().layout();
		// edOut.scrollRectToVisible(new Rectangle(0, 9000000, 0, 0));
		// scrollLock = false;

		edOut.setSelection(edOut.getCharCount());

	}

	iVisualElement inside;

	public void setInside(iVisualElement inside) {
		this.inside = inside;
	}

	public interface iOutputCapture {
		public void output(String s);
	}

	iOutputCapture outputCapture = null;

	public iOutputCapture setOutputCapture(iOutputCapture outputCapture) {
		iOutputCapture pref = this.outputCapture;
		this.outputCapture = outputCapture;
		return pref;
	}

	public boolean printFancy = true;
	public int outputLength = 1024 * 2;

	public final class OutputFlusher implements iUpdateable {

		TextStyle inputStyle;
		TextStyle outputStyle;
		TextStyle errorStyle;
		{
			inputStyle = new TextStyle();
			inputStyle.font = new Font(Launcher.display, ed.getFont().getFontData()[0].name, Constants.defaultFont == ed.getFont().getFontData()[0].name ? 10 : 12, SWT.ITALIC);
			inputStyle.foreground = new Color(Launcher.display, 255, 255, 255);
		}

		{
			outputStyle = new TextStyle();
			outputStyle.font = new Font(Launcher.display, ed.getFont().getFontData()[0].name, 12, SWT.NORMAL);
			outputStyle.foreground = Launcher.display.getSystemColor(SWT.COLOR_BLACK);
		}
		{
			errorStyle = new TextStyle();
			errorStyle.font = new Font(Launcher.display, ed.getFont().getFontData()[0].name, 12, SWT.NORMAL);
			errorStyle.foreground = new Color(Launcher.display, 100, 0, 20);
		}

		// Pattern
		// errorLineNoPattern
		// =
		// Pattern.compile("on
		// line
		// (\\d+)");
		Pattern errorLineNoPattern = Pattern.compile("'<string>', (\\d+)|on line (\\d+)|mfm.*?(\\d+)");
		Pattern syntaxErrorLineNoPattern = Pattern.compile("', (\\d+), (\\d+), ");

		@NextUpdate
		public void scrollToBottomOfOutput() {
			PythonTextEditor.this.scrollToBottomOfOutput();
		}

		boolean isInside = false;

		int uniq = 0;

		public void update() {
			if (isInside)
				return;
			isInside = true;

			try {
				boolean needsEnd = false;
				boolean didOutput = false;

				boolean go = input.getBuffer().length() > 0 || output.getBuffer().length() > 0 || error.getBuffer().length() > 0;

				if (!go)
					return;
				String inputText = input.getBuffer().toString();
				String outputText = output.getBuffer().toString();
				String errorText = error.getBuffer().toString();

				if (outputCapture != null) {
					try {
						outputCapture.output(outputText);
					} catch (Throwable t) {
						t.printStackTrace();
					}
				}

				if (inputText.length() > 0)
					append(edOut, inputText + "\n", inputStyle);
				if (outputText.length() > 0)
					append(edOut, limit(outputText, 12500) + "\n", outputStyle);
				if (errorText.length() > 0)
					append(edOut, errorText + "\n", errorStyle);

				if (errorText.length() > 0 || outputText.length() > 0 || inputText.length() > 0)
					append(edOut, "\n", errorStyle);

				if (edOut.getCharCount() > outputLength) {
					edOut.replaceTextRange(0, outputLength / 2, "");
				}

				scrollToBottomOfOutput();

				try {

					// ;//System.out.println(" searching <" +
					// error.toString() + "> with pattern <"
					// + errorLineNoPattern + ">");
					Matcher m = errorLineNoPattern.matcher(error.toString());
					if (m.find()) {
						String gg = m.group(1);
						if (gg == null)
							gg = m.group(2);
						if (gg == null)
							gg = m.group(3);

						;//System.out.println(" found it <" + gg + ">");

						int i = Integer.parseInt(gg);

						int cp = getInputEditor().getSelectionRange().x;
						if (cp >= getInputEditor().getText().length())
							return;

						if (getInputEditor().getText().charAt(cp) == '\n' && getInputEditor().getSelectionRange().x != getInputEditor().getSelectionRange().y)
							cp++;

						List<Integer> lineStarts = getLineStarts();
						int at = Collections.binarySearch(lineStarts, cp);
						if (at < 0) {
							at = -at - 1;
						} else
							at += 1;

						List<Integer> subLineStarts = lineStarts.subList(at - 1, lineStarts.size());
						int lineStartAts = subLineStarts.get(Math.min(subLineStarts.size() - 1, Math.max(0, i - 1)));
						int lineEndsAts = subLineStarts.get(Math.min(subLineStarts.size() - 1, Math.max(0, i - 1) + 1));

						addPositionAnnotation(new ErrorAnnotation(lineStartAts, lineEndsAts).setToolTip(error.toString() + "\n" + PythonInterface.getPythonInterface().getLastError()));
					} else {
					}

					m = syntaxErrorLineNoPattern.matcher(error.toString());
					if (m.find()) {
						String gg = m.group(1);
						String cc = m.group(2);
						int i = Integer.parseInt(gg);
						int i2 = Integer.parseInt(cc);

						int cp = getInputEditor().getSelectionRange().x;
						;//System.out.println(" cp = " + cp + "> <" + getInputEditor().getText().length() + ">");
						if (cp >= getInputEditor().getText().length())
							return;

						if (getInputEditor().getText().charAt(cp) == '\n' && getInputEditor().getSelectionRange().x != getInputEditor().getSelectionRange().y)
							cp++;

						List<Integer> lineStarts = getLineStarts();
						int at = Collections.binarySearch(lineStarts, cp);
						if (at < 0) {
							at = -at - 1;
						} else
							at += 1;

						List<Integer> subLineStarts = lineStarts.subList(at - 1, lineStarts.size());
						int lineStartAts = subLineStarts.get(Math.min(subLineStarts.size() - 1, Math.max(0, i - 1)));
						int lineEndsAts = subLineStarts.get(Math.min(subLineStarts.size() - 1, Math.max(0, i - 1) + 1));

						addPositionAnnotation(new ErrorAnnotation(lineStartAts, lineEndsAts).setToolTip(error.toString() + "\n" + PythonInterface.getPythonInterface().getLastError()));
						addPositionAnnotation(new ErrorAnnotation(lineStartAts + i2 - 1, lineStartAts + i2 + 2).setToolTip(error.toString() + "\n" + PythonInterface.getPythonInterface().getLastError()));

					} else {
					}

				} finally {
					error.getBuffer().setLength(0);
				}

			} finally {
				isInside = false;
				input.getBuffer().setLength(0);
				output.getBuffer().setLength(0);
				error.getBuffer().setLength(0);

			}
		}

		private void append(StyledText target, String string, TextStyle style) {

			int a = target.getContent().getCharCount();
			target.append(string);
			int b = target.getContent().getCharCount();

			StyleRange s = new StyleRange(style);
			s.start = a;
			s.length = b - a;

			target.setStyleRanges(a, b - a, new int[] { a, b - a }, new StyleRange[] { s });
		}

		int currentIndentLevel = 0;

		public String indentOutput(String o) {
			return indents(currentIndentLevel) + o.replace("\n", "\n" + indents(currentIndentLevel)).trim() + "\n";
		}

		private String indents(int i) {
			StringBuffer s = new StringBuffer(i * 3);
			for (int n = 0; n < i; n++)
				s.append("    ");
			return s.toString();
		}

		public void increaseIndent() {
			currentIndentLevel++;
		}

		public void decreaseIndent() {
			currentIndentLevel--;
		}

		public int getIndentLevel() {
			return currentIndentLevel;
		}

	}

	private class ErrorAnnotation implements iPositionAnnotation {
		private String tt;

		StyledTextPositionSystem.Position start = null;

		StyledTextPositionSystem.Position end = null;

		// line is 1
		// offset
		public ErrorAnnotation(int start, int end) {

			this.start = StyledTextPositionSystem.get(ed).createPosition(start);
			this.end = StyledTextPositionSystem.get(ed).createPosition(end);
		}

		public void drawRect(Rectangle r, GC g) {
			g.setBackground(new Color(Launcher.display, 128, 0, 0));
			g.setAlpha(64);
			g.fillRectangle(r.x, r.y, r.width, r.height);
		}

		public StyledTextPositionSystem.Position getEndPosition() {
			return end;
		}

		public StyledTextPositionSystem.Position getStartPosition() {
			return start;
		}

		public iPositionAnnotation setToolTip(String error) {
			tt = error.toString();
			return this;
		}

		public String toolTipText() {
			return tt;
		}
	}

	private static String[] sourceDirs = SystemProperties.getProperty("java.source.paths", "").split(":");

	static public String smaller(String t) {
		return "<font size=-2>" + t.replace("\n", " ").replace("  ", " ").replace("\t", " ").replaceAll("( +)", " ") + "</font>";
	}

	protected void goButton(String uidDesc, int parseInt) {
	}

	private final OutputFlusher outputFlusher;

	protected EditorExecutionInterface inter;

	JavaDocBuilder db = new JavaDocBuilder();

	StringWriter output = new StringWriter();

	StringWriter input = new StringWriter();

	StringWriter error = new StringWriter() {
		@Override
		public void write(String str) {
			super.write(str);
		}

		public void write(int c) {
			super.write(c);
		};
	};

	protected int uniq = 0;

	List<ClassRecord> allClasses = null;

	Comparator<ClassRecord> sortClassRecord = new Comparator<ClassRecord>() {
		public int compare(ClassRecord o1, ClassRecord o2) {
			return o1.firstName.compareTo(o2.firstName);
		}
	};

	Object forceEvaluateTo = null;

	boolean shouldForceEvaluate = false;

	private String topicOutput;

	public PythonTextEditor() {

		outputFlusher = new OutputFlusher();
		Launcher.getLauncher().registerUpdateable(outputFlusher);
	}

	public List<Completion> buildCompletionsForClassName(final String cc) {
		if (allClasses == null) {
			allClasses = buildAllClasses();
		}

		ClassRecord find = new ClassRecord(cc, "");
		int i = Collections.binarySearch(allClasses, find, sortClassRecord);
		if (i < 0)
			i = -i - 1;
		List<Completion> r = new ArrayList<Completion>();
		while (i < allClasses.size()) {
			if (allClasses.get(i).firstName.startsWith(cc)) {
				final ClassRecord rr = allClasses.get(i);

				if (rr.firstName.contains("$")) {
					if (Character.isDigit(rr.firstName.charAt(rr.firstName.lastIndexOf("$") + 1))) {
						i++;
						continue;
					}
				}
				Completion comp = new Completion() {
					@Override
					public void update() {
						if (PythonInterface.getPythonInterface().getVariable(rr.firstName) == null) {
							if (rr.firstName.indexOf("$") == -1) {
								String aa = "from " + rr.fullName.substring(0, rr.fullName.lastIndexOf(".")) + " import " + rr.firstName + "\n";
								PythonInterface.getPythonInterface().execString(aa);
								ed.getContent().replaceTextRange(0, 0, aa);

								int old = ed.getCaretOffset();
								ed.getContent().replaceTextRange(ed.getCaretOffset(), 0, rr.firstName.substring(cc.length()));
								ed.setCaretOffset(old + rr.firstName.substring(cc.length()).length());

							} else {
								String[] xx = rr.firstName.split("\\$");

								String aa1 = "from " + rr.fullName.substring(0, rr.fullName.lastIndexOf(".")) + " import " + xx[0] + "\n";
								PythonInterface.getPythonInterface().execString(aa1);
								String aa2 = "from " + rr.fullName.substring(0, rr.fullName.lastIndexOf(".")) + "." + xx[0] + " import " + xx[1] + "\n";
								PythonInterface.getPythonInterface().execString(aa2);
								ed.getContent().replaceTextRange(0, 0, aa2);
								ed.getContent().replaceTextRange(0, 0, aa1);

								int old = ed.getCaretOffset();
								ed.getContent().replaceTextRange(ed.getCaretOffset(), 0, xx[1]);
								ed.setCaretOffset(old + xx[1].length());

							}
						} else {
							// ed.getContent().replaceTextRange(ed.getCaretOffset(),
							// 0,
							// rr.firstName.substring(cc.length()));

							int old = ed.getCaretOffset();
							ed.getContent().replaceTextRange(ed.getCaretOffset(), 0, rr.firstName.substring(cc.length()));
							ed.setCaretOffset(old + rr.firstName.substring(cc.length()).length());

						}
					}
				};
				comp.optionalPlainText = rr.firstName.substring(cc.length());
				comp.enabled = true;
				comp.text = dress(allClasses.get(i).fullName);
				r.add(comp);
			}
			i++;
		}

		Collections.sort(r, new Comparator<Completion>() {
			public int compare(Completion o1, Completion o2) {
				return o1.text.compareTo(o2.text);
			}
		});

		String old = "";
		String old2 = "";

		for (int x = 0; x < r.size(); x++) {
			Completion c = r.get(x);
			if (!c.text.substring(0, c.text.indexOf(".")).equals(old)) {
				Completion heading = new Completion() {

					@Override
					public void update() {
					}

				};
				heading.text = old = c.text.substring(0, c.text.indexOf(".")) + "</b>";
				heading.enabled = false;
				r.add(x, heading);
				x++;
			}
			if (!c.text.substring(0, c.text.lastIndexOf(".")).equals(old2)) {
				List<Comp> customcomp = new ClassDocumentation().getPackageCustomCompletion(c.text.substring(0, c.text.lastIndexOf(".")), c.text.substring(c.text.lastIndexOf(".")));

				if (customcomp != null) {
					for (Comp co : customcomp) {
						if (co.longDocumentation != null) {
							Completion cc2 = new Completion() {

								@Override
								public void update() {
								}
							};
							cc2.text = co.longDocumentation;
							cc2.isDocumentation = true;
							r.add(x, cc2);
							x++;
						} else if (co instanceof CompProxy) {
							completionsFromReflectingUpon(((CompProxy) co).proxyTo, cc, (ArrayList<Completion>) r, true);
						}
					}
				}
				old2 = c.text.substring(0, c.text.lastIndexOf("."));

			}
		}

		return r;
	}

	@Override
	public void executeHandle() {

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
			executionRuler.getExecutedAreas().execute(a + 2, b - 1, s);
		} else
			executionRuler.getExecutedAreas().execute(ed.getSelectionRanges()[0], ed.getSelectionRanges()[1] + ed.getSelectionRanges()[0] - 1, s);

		s = detab(s);

		inter.executeFragment(s);

	}

	public void executeHandleSpecial(String preamble, String postamble) {

		String s = ed.getSelectionText();
		Area area;
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

			area = executionRuler.getExecutedAreas().execute(a + 2, b - 1, s);
		} else
			area = executionRuler.getExecutedAreas().execute(ed.getSelectionRanges()[0], ed.getSelectionRanges()[1] + ed.getSelectionRanges()[0] - 1, s);

		s = detab(s);

		// pad each line
		// of s with a
		// tab
		String[] lines = s.split("\n");
		String total = "";
		for (String l : lines) {
			total += "\t" + l + "\n";
		}
		total += preamble;
		total = "def __tmp" + uniq + "():\n" + total;
		total = "__env" + uniq + "=_environment\n" + total;
		total = "def __enter" + uniq + "():\n\t __env" + uniq + ".enter()\n" + total;
		total = "def __exit" + uniq + "():\n\t __env" + uniq + ".exit()\n" + total;
		total = total + "\nu.stackPrePost(__enter" + uniq + ", __tmp" + uniq + "(), __exit" + uniq + ")\n";
		total += postamble;
		inter.executeFragment(total);
		uniq++;

	}

	@Override
	public void executeHandleSpecial() {

		executeHandleSpecial("", "");
	}

	// public State getAreas() {
	// return executionRuler.getExecutedAreas().getState();
	// }

	public Writer getErrorOutput() {
		return error;
	}

	public Writer getInput() {
		return input;
	}

	public EditorExecutionInterface getInterface() {
		return inter;
	}

	public List<Integer> getLineStarts() {
		String t = getInputEditor().getText();
		List<Integer> r = new ArrayList<Integer>();

		String[] lines = t.split("\n");
		r.add(0);
		int at = 0;
		for (int i = 0; i < lines.length; i++) {

			at += lines[i].length() + 1;
			r.add(at);
		}
		return r;
	}

	public Writer getOutput() {
		return output;
	}

	public OutputFlusher getOutputFlusher() {
		return outputFlusher;
	}

	public String getText() {

		// final String[] text = {null};
		// Launcher.display.syncExec(new Runnable(){
		//
		// @Override
		// public void run() {
		// text[0] = ed.getText();
		// }});
		//
		// return text[0];
		return ed.getText();
	}

	// public void setAreas(State areaz) {
	// executionRuler.getExecutedAreas().setState(areaz);
	// ed.repaint();
	// }

	// public void setContentsForHistory(String stringAtSwapIn) {
	// historyRuler.updateCurrentContents(stringAtSwapIn);
	// }

	public void setEnabled(boolean b) {
		ed.setEditable(b);
		ed.setEnabled(b);
	}

	// public void setFilename(HistoryExplorerHG historyExplorerHG, String
	// string, String stringAtSwapIn) {
	// historyRuler.setFilename(historyExplorerHG, string, stringAtSwapIn);
	// }

	public PythonTextEditor setInterface(EditorExecutionInterface inter) {
		this.inter = inter;
		return this;
	}

	public void setText(String string) {
		ed.setText(string);
	}

	private void addClassToStaticallyImportedClasses(String name, Map<String, ClassRecord> allClassesMap) {
		try {
			Class<?> n = this.getClass().getClassLoader().loadClass(name);

			Field[] fields = n.getDeclaredFields();
			Method[] methods = n.getDeclaredMethods();

			for (Field f : fields) {
				ClassRecord cr = new ClassRecord(f.getName(), name + "." + f.getName());
				allClassesMap.put(f.getName(), cr);
			}

			for (Method f : methods) {
				if (!Modifier.isStatic(f.getModifiers()))
					continue;
				ClassRecord cr = new ClassRecord(f.getName(), name + "." + f.getName());
				allClassesMap.put(f.getName(), cr);
			}

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void addSourceDirToAllClasses(String root, String s, Map<String, ClassRecord> allClassesMap) {
		try {
			File d = new File(s);
			;//System.out.println(" add source dir to all classes :" + d);
			if (d.isDirectory()) {
				File[] classes = d.listFiles(new FilenameFilter() {

					public boolean accept(File dir, String name) {
						return name.endsWith(".java");
					}
				});

				for (File f : classes) {
					String n = f.getName();
					String n2 = n;
					if (n.contains("$")) {
						n2 = n.substring(n.lastIndexOf("$") + 1);
					}

					n2 = n2.substring(0, n2.length() - ".java".length());
					n = n.substring(0, n.length() - ".java".length());

					assert f.getPath().startsWith(root);
					String upTo = f.getPath().substring(root.length());
					upTo = upTo.replace('/', '.');

					if (upTo.endsWith(n2 + ".java"))
						upTo = upTo.substring(0, upTo.length() - (n2 + ".java").length());

					if (!upTo.endsWith("."))
						upTo = upTo + '.';

					if (upTo.startsWith("."))
						upTo = upTo.substring(1);

					allClassesMap.put(n2, new ClassRecord(n2, upTo + n));
				}

				File[] dirs = d.listFiles(new FileFilter() {
					public boolean accept(File pathname) {
						return pathname.isDirectory();
					}
				});

				for (File dd : dirs) {
					addSourceDirToAllClasses(root, dd.getPath(), allClassesMap);
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public void completionsFromLocalNamespace(final String cc, Map<String, Object> variables, ArrayList<Completion> comp) {
		Iterator<Entry<String, Object>> i = variables.entrySet().iterator();
		while (i.hasNext()) {
			final Entry<String, Object> e = i.next();
			if (e.getKey() == null)
				continue;

			try {
				if (comp.size() > 100)
					return;

				if (e.getKey().startsWith(cc)) {

					if (e.getValue() instanceof PyMethod) {
						final PyMethod f = ((PyMethod) e.getValue());

						Completion c = completionForPyMethod(cc, f, false);
						if (c == null)
							continue;
						;//System.out.println(" comp is <" + c + ">");
						comp.add(c);
					} else if (e.getValue() instanceof PyFunction) {

						final PyFunction f = ((PyFunction) e.getValue());

						Completion c = completionForPyFunction(cc, f);
						if (c == null)
							continue;
						comp.add(c);

					} else if (e.getValue() instanceof Class) {
						;//System.out.println(" we have a class <" + e.getKey() + "> <" + e.getValue() + "> <" + ((Class) e.getValue()).getSimpleName() + " " + cc + ">");
						completionsFromReflectingUponConstructors(cc, comp, ((Class) e.getValue()), false);
					} else if (e.getValue() instanceof PyType) {
						;//System.out.println(" found pytype <" + e.getValue() + ">");
						PyType t = ((PyType) e.getValue());
						Completion c = new Completion() {
							@Override
							public void update() {
								int old = ed.getCaretOffset();
								ed.getContent().replaceTextRange(ed.getCaretOffset() - cc.length(), cc.length(), e.getKey());

								if (cc.length() == 0)
									ed.setCaretOffset(old + e.getKey().length());

							}
						};
						c.optionalPlainText = e.getKey().substring(cc.length());

						String doc = "";

						PyObject d = t.getDoc();
						if (d != null && d != Py.None)
							doc = Py.tojava(d, String.class);

						doc = doc.trim();

						c.text = "\u03bd  <b>" + e.getKey() + "</b> = " + limit(e.getValue()) + smaller(" a python class ") + ((doc.length() > 0 ? ("- <i>" + limitDocumentation(doc) + "</i>") : ""));
						c.enabled = true;
						// c.optionalDocumentation =
						// convertPythonDoc(doc);

						PyObject constructor = t.getDict().__findattr__("__init__".intern());
						if (constructor != null && constructor != Py.None) {
							;//System.out.println(" constructor class is <" + constructor.getClass() + "> <" + constructor.getClass().getSuperclass() + ">");
							Completion ccc = completionForConstructor(cc, t);
							c.text = ccc.text;
						}

						comp.add(c);
					} else {

						Completion c = new Completion() {
							@Override
							public void update() {
								int old = ed.getCaretOffset();
								ed.getContent().replaceTextRange(ed.getCaretOffset() - cc.length(), cc.length(), e.getKey());

								if (cc.length() == 0)
									ed.setCaretOffset(old + e.getKey().length());
							}
						};
						c.optionalPlainText = e.getKey().substring(cc.length());
						c.text = "\u03bd  <b>" + e.getKey() + "</b> = " + limit(e.getValue()) + smaller(" of " + (e.getValue() == null ? null : e.getValue().getClass()) + " ");
						c.enabled = true;
						comp.add(c);
					}
				}
			} catch (NullPointerException ex) {
				// ;//System.out.println(" npe on <" + e + ">");
				// ex.printStackTrace();
			}
		}
	}

	private Completion completionForPyFunction(final String leftText, final PyFunction f) {
		return completionForPyFunction(leftText, f, false);
	}

	private Completion completionForPyFunction(final String leftText, final PyFunction f, boolean ignoreFirst) {
		;//System.out.println(" enter ");

		if ((((PyTableCode) f.__code__).co_flags.isFlagSet(CodeFlag.CO_VARARGS)) || (((PyTableCode) f.__code__).co_flags.isFlagSet(CodeFlag.CO_VARKEYWORDS)) || (f.func_defaults != null)) {

			;//System.out.println(" p1 ");

			// different, slower technique
			PythonInterface.getPythonInterface().setVariable("__f", f);
			;//System.out.println(" p1 ");
			String s = (String) PythonInterface.getPythonInterface().eval("inspect.formatargspec(*inspect.getargspec(__f))").toString();

			Completion c = new Completion() {
				@Override
				public void update() {
					int old = ed.getCaretOffset();
					ed.getContent().replaceTextRange(ed.getCaretOffset() - leftText.length(), leftText.length(), rewriteOperator(f.__name__));

					if (leftText.length() == 0)
						ed.setCaretOffset(old + rewriteOperator(f.__name__).length());

					// hint = this.text;
					// hintAlpha = 0.75f;
				}
			};
			c.optionalPlainText = rewriteOperator(f.__name__).substring(leftText.length());
			c.enabled = true;

			String ddoc = "";

			PyObject doc = f.__getattr__("__doc__");

			if (doc != null && !doc.toString().equals("None"))
				ddoc = "- " + doc.toString();

			s = s.replace("(", "");
			s = s.replace(")", "");
			s = s.replace("self, ", "");

			c.text = "\u1d3e  <b>" + rewriteOperator(f.__name__) + " (</b>" + s + "<b>) </b> " + smaller(limit(ddoc));

			c.optionalDocumentation = convertPythonDoc(ddoc);

			return c;

		}

		;//System.out.println(" P1 ");

		int argCount = ((PyTableCode) f.__code__).co_argcount;
		;//System.out.println(" P1 ");
		String[] v = ((PyTableCode) f.__code__).co_varnames;
		;//System.out.println(" P1 ");
		;//System.out.println(" function names <" + Arrays.asList(v) + "> <" + argCount + ">");

		Completion c = new Completion() {
			@Override
			public void update() {
				int old = ed.getCaretOffset();
				ed.getContent().replaceTextRange(ed.getCaretOffset() - leftText.length(), leftText.length(), rewriteOperator(f.__name__));

				if (leftText.length() == 0)
					ed.setCaretOffset(old + rewriteOperator(f.__name__).length());
			}
		};
		c.optionalPlainText = rewriteOperator(f.__name__).substring(leftText.length());
		c.enabled = true;
		List<String> a = Arrays.asList(v).subList(ignoreFirst ? 1 : 0, argCount);
		String aa = a.toString();
		aa = aa.substring(1, aa.length() - 1);

		String ddoc = "";

		PyObject doc = f.__getattr__("__doc__");

		;//System.out.println(" doc string is <" + ddoc + ">");

		if (doc != null && !doc.toString().equals("None"))
			ddoc = "- " + doc.toString();

		c.text = "\u1d3e  <b>" + rewriteOperator(f.__name__) + " (</b>" + aa + "<b>) </b> " + smaller(limit(ddoc));

		c.optionalDocumentation = convertPythonDoc(ddoc);

		;//System.out.println(" returning <" + c + ">");
		return c;
	}

	private Completion completionForConstructor(final String leftText, final PyType clazz) {
		// different technique
		PythonInterface.getPythonInterface().setVariable("__f", clazz);
		String s = (String) PythonInterface.getPythonInterface().eval("inspect.formatargspec(*inspect.getargspec(__f.__init__))").toString();

		Completion c = new Completion() {
			@Override
			public void update() {
				int old = ed.getCaretOffset();
				ed.getContent().replaceTextRange(ed.getCaretOffset() - leftText.length(), leftText.length(), clazz.getName());

				if (leftText.length() == 0)
					ed.setCaretOffset(old + clazz.getName().length());

				// hint = this.text;
				// hintAlpha = 0.75f;
			}
		};
		c.optionalPlainText = clazz.getName().substring(leftText.length());
		c.enabled = true;

		String ddoc = "";

		PyObject doc = clazz.__getattr__("__doc__");
		if (doc != null && !doc.toString().equals("None"))
			ddoc = "- " + doc.toString();

		s = s.replace("(", "");
		s = s.replace(")", "");
		s = s.replace("self, ", "");

		c.text = "\u1d3e  <b>" + clazz.getName() + " (</b>" + s + "<b>) </b> " + smaller(limit(ddoc));

		// c.optionalDocumentation = convertPythonDoc(ddoc);

		return c;
	}

	private Completion completionForPyMethod(final String leftText, final PyMethod f, boolean pythonOnly) {
		;//System.out.println(" py mthod :" + f + " " + f.im_func.getClass());
		if (f.im_func instanceof PyFunction) {
			Completion c = completionForPyFunction(leftText, (PyFunction) f.im_func, true);
			return c;
		} else if (!pythonOnly && f.im_func instanceof PyReflectedFunction) {
			PyReflectedFunction ff = ((PyReflectedFunction) f.im_func);
			Method m = (Method) ReflectionTools.illegalGetObject(ff.argslist[0], "data");
			;//System.out.println(" method is <" + m + "> <" + m.getDeclaringClass() + ">");

			if (m.getDeclaringClass() == Object.class)
				return null;

			return getCompletionFor(null, m.getDeclaringClass(), m, leftText);
		}
		return null;
	}

	private void completionsFromPythonList(Object ret, PyList list, final String right, ArrayList<Completion> comp, PyObject po, boolean publicOnly, boolean pythonOnly) {

		List<Object> alist = new ArrayList<Object>(list);
		Collections.sort(alist, new Comparator<Object>() {

			public int compare(Object o1, Object o2) {
				return o1.toString().compareTo(o2.toString());
			}

		});

		;//System.out.println(" completions from <" + ret + ">");

		for (Object p : alist) {

			if (comp.size() > 100)
				return;

			;//System.out.println(" object p :" + p + " " + p.getClass());

			p = PythonUtils.maybeToJava(p);

			if (p instanceof PyJavaType) {
				;//System.out.println(" ruc");
				completionsFromReflectingUponClass(right, comp, (Class) ((PyJavaType) p).__tojava__(Class.class), publicOnly);
				doContextualHelpFor((Class) ((PyJavaType) p).__tojava__(Class.class));
				continue;
			}
			if (p instanceof Class) {
				;//System.out.println(" ruc");
				completionsFromReflectingUponClass(right, comp, (Class) p, publicOnly);
				doContextualHelpFor((Class) p);
				continue;
			}
			if (p instanceof Comp) {
				if (p instanceof CompProxy) {
					Object to = ((CompProxy) p).proxyTo;

					;//System.out.println(" proxy to <" + to + ">");
					if (to instanceof Class)
						completionsFromReflectingUponClass(right, comp, (Class) ((CompProxy) p).proxyTo, true);
					else if (to instanceof Field)
						comp.add(getCompletionFor(ret, ret.getClass(), (Field) ((CompProxy) p).proxyTo, right));
					else if (to instanceof Method)
						comp.add(getCompletionFor(ret, ret.getClass(), (Method) ((CompProxy) p).proxyTo, right));
					else
						completionsFromReflectingUpon(((CompProxy) p).proxyTo, right, comp, true);
				} else
					makeCompletion(right, comp, ((Comp) p));
			} else {

				final String name = p.toString();
				if (name.indexOf("__") != -1)
					continue;

				if (name.startsWith(right) && (!name.startsWith("_") || !publicOnly || knownOperators.containsKey(name))) {
					String ddoc = "";
					Completion c = new Completion() {
						@Override
						public void update() {
							int old = ed.getCaretOffset();
							ed.getContent().replaceTextRange(ed.getCaretOffset() - right.length(), right.length(), rewriteOperator(name));

							if (right.length() == 0)
								ed.setCaretOffset(old + rewriteOperator(name).length());

						}
					};

					c.optionalPlainText = rewriteOperator(name).substring(right.length());
					c.text = "\u1d3e  <b>" + rewriteOperator(name) + "</b>";
					try {
						PyObject a = po.__getattr__(name);

						// ;//System.out.println(" a is a :"
						// + a + " " + a.getClass());
						if (a instanceof PyFunction) {
							;//System.out.println(" >> function");
							c = completionForPyFunction(right, ((PyFunction) a));
							if (c == null)
								continue;
						} else if (a instanceof PyMethod) {
							;//System.out.println(" >> method");
							c = completionForPyMethod(right, ((PyMethod) a), pythonOnly);
							if (c == null)
								continue;
						} else if (!pythonOnly && a instanceof PyObjectDerived) {
							Object o = Py.tojava(a, Object.class);
							c.text += "- " + limit(o) + " " + (o == null ? "" : smaller(strip("" + o.getClass())));
						} else {
							if (pythonOnly)
								continue;
							// PyObject doc =
							// a.__getattr__("__doc__");
							// ;//System.out.println(" >> doc"
							// + doc);
							// if (doc != null &&
							// !doc.toString().equals("None")
							// &&
							// !doc.toString().equals("The most base type")
							// &&
							// doc.toString().trim().length()
							// > 0) {
							// ddoc = "- " +
							// doc.toString();
							// c.text += " " +
							// smaller(limitDocumentation(ddoc));
							// c.optionalDocumentation
							// =
							// convertPythonDoc(ddoc);
							// }

						}

					} catch (Throwable t) {
						t.printStackTrace();
					}

					c.enabled = true;
					comp.add(c);
				}
			}
		}
	}

	protected void doContextualHelpFor(Class c) {
	}

	protected String rewriteOperator(String name) {
		return knownOperators.containsKey(name) ? knownOperators.get(name) : name;
	}

	public void completionsFromReflectingUpon(Object ret, final String right, ArrayList<Completion> comp, boolean publicOnly) {
		if (ret == null)
			return;

		;//System.out.println(" completions from reflecting upon " + ret + " " + ret.getClass());

		// ret = PythonUtils.maybeToJava(ret);

		boolean pythonOnly = false;

		if (ret instanceof PyObjectDerived) {

			PyObject a = ((PyObjectDerived) ret).__findattr__("completesAsPython__");

			;//System.out.println(" completes as python? " + a + "  " + ((PyObjectDerived) ret).__dir__());

			if (a == null || a == Py.None) {
				PyType type = ((PyObjectDerived) ret).getType();
				if (type instanceof PyJavaType)
					ret = PythonUtils.maybeToJava(ret);
			} else {
				pythonOnly = true;
			}
		}

		;//System.out.println(" go on <" + ret + "> <" + ret.getClass() + ">");

		if (ret instanceof PyExtensibleJavaInstance) {
			ret = ((PyExtensibleJavaInstance) ret).__tojava__(Object.class);
		}

		if (ret instanceof PyJavaType) {
			Class c = (Class) ((PyJavaType) ret).__tojava__(Class.class);

			String doc = new ClassDocumentation().getClassDocumentation(right, null, c);
			if (doc != null) {
				Completion cc = new Completion() {

					@Override
					public void update() {
					}
				};
				cc.text = doc;
				cc.isDocumentation = true;
				comp.add(cc);
			}

			List<Comp> customcomp = new ClassDocumentation().getClassCustomCompletion(right, null, c);
			if (customcomp != null) {
				for (Comp cc : customcomp) {
					makeCompletion(right, comp, cc);
				}
			}

			completionsFromReflectingUponClass(right, comp, c, publicOnly);
			doContextualHelpFor(c);

		} else if (ret instanceof PyObject) {
			PyObject po = (PyObject) ret;

			Class poClass = po.getClass();
			doContextualHelpFor(poClass);

			if (po.__findattr__("__completionProxy__") != null) {
				poClass = Py.tojava(po.__getattr__("__completionProxy__"), Class.class);
				;//System.out.println(" got completion proxy for <" + poClass + ">");
			}

			String doc = new ClassDocumentation().getClassDocumentation(right, ret, poClass);
			if (doc != null) {
				Completion cc = new Completion() {

					@Override
					public void update() {
					}
				};
				cc.text = doc;
				cc.isDocumentation = true;
				comp.add(cc);
			}

			List<Comp> customcomp = new ClassDocumentation().getClassCustomCompletion(right, ret, po.getClass());
			if (customcomp != null) {
				for (Comp c : customcomp) {
					makeCompletion(right, comp, c);
				}
			}

			if (right.equals("")) {
				PyObject pythonDoc = po.__findattr__("__doc__".intern());
				if (pythonDoc != null && pythonDoc != Py.None) {
					Completion cc = new Completion() {

						@Override
						public void update() {
						}
					};
					cc.text = convertPythonDoc((String) pythonDoc.__tojava__(String.class));
					cc.isDocumentation = true;
					comp.add(cc);

				}
			}
			try {
				PyList list = (PyList) po.__dir__();

				;//System.out.println("dir is <" + list + ">");
				completionsFromPythonList(ret, list, right, comp, po, publicOnly, pythonOnly);

				if (po.__findattr__("__completions__") != null) {
					PyObject a = po.__getattr__("__completions__");

					;//System.out.println(" __completions__ is <" + a + ">");

					if (a != null && a != Py.None && a instanceof PyList) {
						Completion c = new Completion() {
							@Override
							public void update() {
							}
						};
						c.enabled = false;
						c.text = "from '__completions__'";
						comp.add(c);
						completionsFromPythonList(ret, (PyList) a, right, comp, po, publicOnly, pythonOnly);
					} else if (a != null && a != Py.None && a instanceof PyObject) {

						;//System.out.println(" calling ... ");
						a = a.__call__();

						;//System.out.println(" got <" + a + ">");

						if (a instanceof PyList) {
							completionsFromPythonList(ret, (PyList) a, right, comp, po, publicOnly, pythonOnly);
						}
					}
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}

		} else {
			completionsFromReflectingUponInstance(ret, right, comp, ret.getClass(), publicOnly);
			doContextualHelpFor(ret.getClass());
		}
	}

	private String convertPythonDoc(String ascii) {
		return convertPythonDoc(ascii, "");
	}

	private String convertPythonDoc(String ascii, String defaultTitle) {
		ascii = ascii.trim();

		if (ascii == null)
			return "";

		if (ascii.equals("The most base type")) {
			ascii = "Completions...";
		}

		int firstLine = Math.min(ascii.indexOf(".") + 1, ascii.indexOf("\n\n"));
		if (firstLine == -1) {
			firstLine = defaultTitle.length() + 1;
			ascii = defaultTitle + "\n" + ascii;
		}

		String formatted = ascii.substring(firstLine).replace("\n\n", "\n").replace("-->", "\u2192").replace("->", "\u2192");
		formatted = formatted.replaceAll("C\\{(.*?)\\}", "<i>$1</i>");
		formatted = formatted.replaceAll("X\\{(.*?)\\}", "<b>$1</b>");
		formatted = formatted.replaceAll("(\\p{Alnum}*?)(\\(.*?\\))", "<b>$1</b>$2");
		formatted = formatted.replaceAll("\r", " ");
		formatted = formatted.replaceAll("\t", " ");
		for (int i = 0; i < 10; i++)
			formatted = formatted.replaceAll("  ", " ");

		String a = ((firstLine > 0 ? "<b>" + (ascii.substring(0, firstLine)).trim() + "</b>\n" + (formatted).trim() : formatted)).trim();

		// String a = formatted.replace("\n", " ").replace("  ",
		// " ").replace("<i></i>", "").replace("<b></b>", "");

		;//System.out.println(" formatted <" + ascii + "> to <" + a + ">");

		a = a.replace("<b></b>", "");
		return a;
	}

	protected void completionsFromReflectingUponClass(String right, ArrayList<Completion> comp, Class class1, boolean publicOnly) {
		Object ret = null;

		boolean added = false;
		Field[] fields = class1.getDeclaredFields();
		for (Field f : fields) {
			if (comp.size() > 100)
				return;
			if (Modifier.isStatic(f.getModifiers()))
				if (f.getName().startsWith(right) && (!publicOnly || notHidden(f))) {
					if (!added) {
						comp.add(getSeparatorFor(ret, class1, right));
						added = true;
					}
					comp.add(getCompletionFor(ret, class1, f, right));
				}
		}
		// methods
		Method[] methods = class1.getDeclaredMethods();
		for (Method m : methods) {
			if (comp.size() > 100)
				return;
			if (Modifier.isStatic(m.getModifiers()))
				if (m.getName().startsWith(right) && (!publicOnly || notHidden(m))) {
					if (!added) {
						comp.add(getSeparatorFor(ret, class1, right));
						added = true;
					}
					comp.add(getCompletionFor(ret, class1, m, right));
				}
		}
		Class c = class1.getSuperclass();
		if (c != null)
			completionsFromReflectingUponClass(right, comp, c, publicOnly);
	}

	private boolean notHidden(Field f) {
		return f.getAnnotation(HiddenInAutocomplete.class) == null && Modifier.isPublic(f.getModifiers());
	}

	private boolean notHidden(Method m) {
		boolean nh = m.getAnnotation(HiddenInAutocomplete.class) == null && Modifier.isPublic(m.getModifiers());

		;//System.out.println(" not hidden ? :" + m + " " + nh + " " + Arrays.asList(m.getAnnotations()));

		return nh;

	}

	protected void completionsFromReflectingUponConstructors(String right, ArrayList<Completion> comp, Class class1, boolean publicOnly) {
		Object ret = null;

		boolean added = false;

		Constructor[] methods = class1.getDeclaredConstructors();
		for (Constructor m : methods) {
			if (comp.size() > 100)
				return;
			{
				if (!added) {
					Completion setp = getSeparatorFor(ret, class1, right);
					setp.text += " (constructors)";
					comp.add(setp);
					added = true;
				}
				comp.add(getCompletionFor(class1, m, right));
			}
		}
	}

	private void completionsFromReflectingUponInstance(Object ret, final String right, ArrayList<Completion> comp, Class<? extends Object> class1, boolean publicOnly) {

		if ((!publicOnly || notHidden(class1))) {

			String doc = new ClassDocumentation().getClassDocumentation(right, ret, class1);
			if (doc != null) {
				Completion cc = new Completion() {

					@Override
					public void update() {
					}
				};
				cc.text = doc;
				cc.isDocumentation = true;
				comp.add(cc);
			}

			List<Comp> customcomp = new ClassDocumentation().getClassCustomCompletion(right, ret, class1);
			if (customcomp != null) {
				for (Comp c : customcomp) {
					makeCompletion(right, comp, c);
				}
			}

			boolean added = false;
			Field[] fields = class1.getDeclaredFields();
			for (Field f : fields) {
				if (comp.size() > 100)
					return;
				if (f.getName().startsWith(right) && (!publicOnly || notHidden(f))) {
					if (!added) {
						comp.add(getSeparatorFor(ret, class1, right));
						added = true;
					}
					comp.add(getCompletionFor(ret, class1, f, right));
				}
			}
			// methods
			Method[] methods = class1.getDeclaredMethods();

			;//System.out.println("-------------------- in class" + class1);

			for (Method m : methods) {
				if (m.isSynthetic())
					continue;

				if (comp.size() > 100)
					return;
				if (m.getName().startsWith(right) && (!publicOnly || notHidden(m))) {
					if (!added) {
						comp.add(getSeparatorFor(ret, class1, right));
						added = true;
					}
					comp.add(getCompletionFor(ret, class1, m, right));
				}
			}
		}
		Class c = class1.getSuperclass();
		if (c != null && c != Object.class)
			completionsFromReflectingUponInstance(ret, right, comp, c, publicOnly);
	}

	private boolean notHidden(Class<? extends Object> class1) {

		;//System.out.println(" checking class for annotation <" + class1 + " " + class1.getAnnotation(HiddenInAutocomplete.class) + ">");

		return class1.getAnnotation(HiddenInAutocomplete.class) == null;
	}

	private void makeCompletion(final String right, ArrayList<Completion> comp, Comp c) {
		if (c.isTitle) {
			Completion cc = new Completion() {

				@Override
				public void update() {
				}
			};
			cc.text = c.shortDocumentation;
			cc.enabled = false;
			cc.isDocumentation = false;
			comp.add(cc);

		} else if (c.completes != null) {
			final String f = c.completes;
			final Completion co = new Completion() {
				@Override
				public void update() {
					int old = ed.getCaretOffset();
					ed.getContent().replaceTextRange(ed.getCaretOffset() - right.length(), right.length(), f);

					if (right.length() == 0)
						ed.setCaretOffset(old + f.length());

				}
			};

			co.optionalPlainText = f.substring(right.length());

			co.text = ("\u22f6 ") + " <b>" + c.completes + "</b> - " + smaller(c.shortDocumentation);
			co.enabled = true;

			comp.add(co);
		} else if (c.longDocumentation != null) {
			Completion cc = new Completion() {

				@Override
				public void update() {
				}
			};
			cc.text = c.longDocumentation;
			cc.isDocumentation = true;
			comp.add(cc);
		}
	}

	protected String detab(String s) {
		int n = 0;
		while (n < s.length() && s.charAt(n) == '\n') {
			n++;
		}
		s = s.substring(n);
		n = 0;
		while (n < s.length() && s.charAt(n) == '\t') {
			n++;
		}
		if (n == 0)
			return s;
		s = s.substring(n);
		for (int i = 0; i < s.length() - 1; i++) {
			if (s.charAt(i) == '\n') {
				s = s.substring(0, i + 1) + s.substring(i + 1 + n, s.length());
			}
		}
		return s;
	}

	private String dress(String fullName) {
		int m = fullName.lastIndexOf('.');
		if (m != -1)
			return fullName.substring(0, m) + "<b>" + fullName.substring(m) + "</b>";
		else
			return fullName.substring(m);

	}

	JavaDocCache cache = new JavaDocCache();

	private Completion getCompletionFor(Object ret, Class<? extends Object> class1, final Field f, final String right) {
		// ;//System.out.println(" get completion for field <" + f + ">");
		final Completion c = new Completion() {
			@Override
			public void update() {
				int old = ed.getCaretOffset();
				ed.getContent().replaceTextRange(ed.getCaretOffset() - right.length(), right.length(), f.getName());

				if (right.length() == 0)
					ed.setCaretOffset(old + f.getName().length());

			}
		};
		c.optionalPlainText = f.getName().substring(right.length());

		String value = "";
		if (isSafeToPrint(f, ret)) {
			value = "= " + printValue(f, ret);
		}

		c.text = (Modifier.isPublic(f.getModifiers()) ? "\u22f6 " : "\u22f7 <grey>") + " <b>" + strip(f.getName().toString()) + "</b>" + " [" + strip(f.getType().toString()) + "] " + value;

		Class<?> declaringClass = f.getDeclaringClass();
		String name = Platform.getCanonicalName(declaringClass);
		if (name != null) {

			JavaClass jc = resolveJavaClass(declaringClass, name);

			if (jc != null) {
				JavaField method = jc.getFieldByName(f.getName());

				if (method != null) {

					String ctext = "<b>" + f.getName() + "</b>" + " [" + strip(f.getType().toString()) + "] " + value;

					String comment = method.getComment();
					if (comment == null)
						comment = "";

					c.text = (Modifier.isPublic(f.getModifiers()) ? "\u1d39  " : "\u1d50 <grey>") + ctext + (comment.length() > 0 ? " - " + smaller(limitDocumentation(comment)) : "");
					c.optionalDocumentation = convertPythonDoc(comment);

				} else {

				}
			}
		}

		c.enabled = true;
		return c;
	}

	private JavaClass resolveJavaClass(Class<?> declaringClass, String name) {
		JavaClass jc = cache.getClass(declaringClass);
		if (jc == null) {
			;//System.out.println(" resolve java class didn't find it, so we're looking for it " + declaringClass + " " + name);

			String fname = name;

			if (declaringClass.getName().contains("$")) {
				fname = declaringClass.getName().substring(0, declaringClass.getName().indexOf("$"));
			}

			String[] sd = getSourceDirs();
			for (int n = 0; n < sd.length; n++) {
				;//System.out.println(" source :" + sd[n]);
				try {
					if (sd[n].endsWith(".jar")) {
						cache.addJarFile(sd[n]);
						String p2 = fname.replace(".", "/") + ".java";
						String p1 = "src/" + p2;
						;//System.out.println(" trying :" + p1);
						jc = cache.loadNameFromJar(p1, name);
						if (jc != null)
							break;
						jc = cache.loadNameFromJar(p2, name);
						;//System.out.println(" trying :" + p2);
						if (jc != null)
							break;

					} else {
						jc = cache.loadFromFile(sd[n] + "/" + fname.replace(".", "/") + ".java", name);
						if (jc != null)
							break;
					}
				} catch (Exception e) {
					;//System.out.println(" resource is <" + sd[n] + ">");
					e.printStackTrace();
				}
			}
		}

		// ;//System.out.println(" found ? "+(jc!=null));
		return jc;
	}

	private Completion getCompletionFor(Object ret, Class<? extends Object> class1, final Method m, final String right) {

		// ;//System.out.println(" the scene, get completion for <" + ret +
		// "> <" + class1 + "> <" + m + "> <" + right + ">");

		// if (m.isSynthetic())
		// return null;

		Completion c = new Completion() {
			@Override
			public void update() {
				int old = ed.getCaretOffset();
				ed.getContent().replaceTextRange(old - right.length(), right.length(), m.getName());
				if (right.length() == 0) {
					ed.setCaretOffset(old + m.getName().length());
				}
			}
		};
		c.optionalPlainText = m.getName().substring(right.length());

		String parameterNames = Arrays.asList(m.getParameterTypes()).toString();
		parameterNames = parameterNames.substring(1, parameterNames.length() - 1);
		String staticprefix = Modifier.isStatic(m.getModifiers()) ? (strip(class1.getName()) + ".") : "";
		c.text = "\u1d39 " + staticprefix + strip(m.getName()) + "(" + (m.getParameterTypes().length == 0 ? "" : parameterNames) + ")";

		Class<?> declaringClass = m.getDeclaringClass();
		String name = Platform.getCanonicalName(declaringClass);
		if (name != null) {

			JavaClass jc = resolveJavaClass(declaringClass, name);

			if (jc != null) {
				Type[] types = new Type[m.getParameterTypes().length];
				for (int i = 0; i < m.getParameterTypes().length; i++) {
					String typeName = Platform.getCanonicalName(m.getParameterTypes()[i]).replace("[", "").replace("]", "");

					if (m.getParameterTypes()[i].isMemberClass()) {
						typeName = m.getParameterTypes()[i].getName();

						typeName = typeName.substring(m.getParameterTypes()[i].getDeclaringClass().getName().length() + 1);

						;//System.out.println("parameter type is member class <" + m.getParameterTypes()[i] + "> names are <" + typeName + "> <" + m.getParameterTypes()[i].getName() + "> <" + Platform.getCanonicalName(m.getParameterTypes()[i]) + ">");

						typeName = m.getParameterTypes()[i].getName();

						if (typeName.contains("$")) {
							String root = typeName.substring(0, typeName.indexOf("$"));

							;//System.out.println(" root = " + root + " " + typeName + " " + name);

							if (root.equals(name)) {
								;//System.out.println(" is inside class ");
							} else {
								;//System.out.println(" is outside class");
								typeName = typeName.replace("$", ".");
							}
						}
					}

					types[i] = new Type(typeName, m.getParameterTypes()[i].isArray() ? 1 : 0);
				}
				JavaMethod method = jc.getMethodBySignature(m.getName(), types);

				if (method != null) {
					if (method.getParameters() != null) {

						String ctext = "<b>" + staticprefix + m.getName() + "</b>" + " ( ";
						for (int i = 0; i < method.getParameters().length; i++) {
							ctext += strip(Platform.getCanonicalName(m.getParameterTypes()[i])) + " <b><i>" + method.getParameters()[i].getName() + "</i></b>" + (i != method.getParameters().length - 1 ? ", " : "");
						}
						ctext += " )";

						String comment = method.getComment();
						if (comment == null)
							comment = "";

						c.text = (Modifier.isPublic(m.getModifiers()) ? "\u1d39  " : "\u1d50 <grey>") + ctext + (comment.length() > 0 ? " - " + smaller(limitDocumentation(comment)) : "");
						c.optionalDocumentation = formatSource(method.getDeclarationSignature(true) + "\n{" + method.getSourceCode() + "}");
					} else {
						;//System.out.println(" does this ever happen ? ");
					}
				} else {
					;//System.out.println(" couldn't find method by signature <" + m.getName() + "> <" + Arrays.asList(types) + "> <" + m.isBridge() + " " + m.isSynthetic() + ">");
					;//System.out.println("methods are <" + Arrays.asList(jc.getMethods()) + ">");
				}
			}
		}

		c.enabled = true;
		return c;
	}

	private ZipEntry findEntry(JarFile jar, String simpleName) {
		Enumeration<JarEntry> e = jar.entries();
		while (e.hasMoreElements()) {
			JarEntry j = e.nextElement();
			String name = j.getName();
			if (name.endsWith(simpleName))
				return j;
		}
		return null;
	}

	public String formatSource(String source) {

		source = source.replace("\t", "  ");
		String[] ll = source.split("\n");
		boolean over = false;
		if (ll.length > 10) {
			StringBuffer s = new StringBuffer();
			for (int i = 0; i < 10; i++)
				s.append(ll[i] + "\n");
			over = true;
			source = s.toString();
		}
		return source + (over ? "\n<i>... method continues ...</i>" : "");
	}

	private Completion getCompletionFor(Class<? extends Object> class1, final Constructor m, final String right) {
		Completion c = new Completion() {
			@Override
			public void update() {

				int old = ed.getCaretOffset();
				ed.getContent().replaceTextRange(old - right.length(), right.length(), m.getDeclaringClass().getSimpleName());
				if (right.length() == 0) {
					ed.setCaretOffset(old + m.getDeclaringClass().getSimpleName().length());
				}

			}
		};
		c.optionalPlainText = m.getDeclaringClass().getSimpleName().substring(right.length());

		Class[] parametertypes = m.getParameterTypes();

		c.text = "\u1d39 " + strip(m.getName()) + "(" + (parametertypes.length == 0 ? "" : Arrays.asList(parametertypes)) + ")";

		Class<?> declaringClass = m.getDeclaringClass();
		String name = Platform.getCanonicalName(declaringClass);

		boolean isNonStatisInnerClass = false;

		if (declaringClass.isMemberClass() && !Modifier.isStatic(declaringClass.getModifiers())) {
			Class[] p2 = new Class[parametertypes.length - 1];
			System.arraycopy(parametertypes, 1, p2, 0, p2.length);
			parametertypes = p2;
			isNonStatisInnerClass = true;
		}

		if (name != null) {

			JavaClass jc = resolveJavaClass(declaringClass, name);

			if (jc != null) {
				Type[] types = new Type[parametertypes.length];
				for (int i = 0; i < parametertypes.length; i++) {
					types[i] = new Type(Platform.getCanonicalName(parametertypes[i]).replace("[", "").replace("]", ""), parametertypes[i].isArray() ? 1 : 0);
				}

				JavaMethod method = jc.getMethodBySignature(m.getDeclaringClass().getSimpleName(), types);

				if (method != null) {
					if (method.getParameters() != null) {

						String ctext = "<b>" + m.getDeclaringClass().getSimpleName() + "</b>" + " ( ";

						if (isNonStatisInnerClass)
							ctext += "<i>" + declaringClass.getDeclaringClass().getSimpleName() + "</i>, ";

						for (int i = 0; i < method.getParameters().length; i++) {
							ctext += strip(Platform.getCanonicalName(parametertypes[i])) + " <i>" + method.getParameters()[i].getName() + "</i>" + (i != method.getParameters().length - 1 ? ", " : "");
						}
						ctext += " )";

						String comment = method.getComment();
						if (comment == null)
							comment = "";

						c.text = (Modifier.isPublic(m.getModifiers()) ? "\u24d2  " : "\u24d2 <grey>") + ctext + (comment.length() > 0 ? " - " + smaller(limitDocumentation(comment)) : "");
						// c.optionalDocumentation =
						// convertPythonDoc(comment);
					} else {
						;//System.out.println(" does this ever happen ? ");
					}
				} else {

					;//System.out.println(" couldn't find constructor by signature ");
					;//System.out.println(" jc is <" + jc + "> <" + Arrays.asList(jc.getMethods()) + ">");
					;//System.out.println(" patameters are <" + Arrays.asList(types) + ">");
					;//System.out.println(" name is <" + m.getDeclaringClass().getSimpleName() + ">");
					;//System.out.println(" weird ? :" + m.getDeclaringClass().isMemberClass() + " " + m.getDeclaringClass().isLocalClass() + " " + Modifier.isStatic(m.getModifiers()));
				}
			} else {
				;//System.out.println(" couldn't find java class for <" + declaringClass + "> <" + name + ">");
			}
		}

		c.enabled = true;
		return c;
	}

	private Completion getSeparatorFor(Object ret, Class<? extends Object> class1, String right) {
		Completion c = new Completion() {
			@Override
			public void update() {
			};
		};
		c.text = "from " + class1.getName() + "";
		c.enabled = false;
		return c;
	}

	static public boolean isSafeToPrint(Field f, Object ret) {
		if (f.getType().isPrimitive())
			return true;
		if (f.getType().equals(String.class))
			return true;
		if (Number.class.isAssignableFrom(f.getType()))
			return true;
		if (f.getType().isAssignableFrom(Number.class))
			return true;
		if (f.getType().equals(Vector2.class))
			return true;
		if (f.getType().equals(Vector3.class))
			return true;
		if (f.getType().equals(Vector4.class))
			return true;
		if (f.getType().equals(VectorN.class))
			return true;
		if (f.getType().equals(Quaternion.class))
			return true;
		if (f.getType().equals(File.class))
			return true;
		if (f.getType().equals(Method.class))
			return true;
		if (f.getType().equals(Field.class))
			return true;
		if (f.getType().equals(Class.class))
			return true;

		return false;
	}

	static public String limit(Object value) {
		return limit(value, 250);
	}

	static public String limit(Object value, int i) {
		if (value == null)
			return "";

		try {
			String s = value.toString();
			if (s.length() > i)
				return s.substring(0, i) + " ... ";

			return s;
		} catch (Throwable t) {
			return "";
		}
	}

	static public String limitDocumentation(Object value) {
		String s = value.toString();
		int max = 100;
		if (s.length() > max) {
			String[] q = s.split("\n\n");
			if (q[0].length() > max) {
				String[] q2 = s.split("\n");
				if (q2[0].length() > max) {
					return q2[0].substring(0, max) + "...";
				}
				return q2[0] + "...";
			} else
				return q[0] + " ...";
		}
		return s;
	}

	private void print(PyJavaPackage topLevelPackage, String prefix, Map<String, ClassRecord> allClassesMap) {

		// + " has:");
		PyStringMap clsSet = topLevelPackage.clsSet;

		PyIterator iter = (PyIterator) clsSet.__iter__();
		while (true) {
			PyObject a = iter.__iternext__();
			if (a != null) {
				// " +
				// a);
				allClassesMap.put(topLevelPackage.__name__ + "." + a.toString(), new ClassRecord(a.toString(), topLevelPackage.__name__ + "." + a.toString()));
			} else {
				break;
			}
		}
		iter = (PyIterator) topLevelPackage.__dict__.__iter__();
		while (true) {
			PyObject n = iter.__iternext__();
			if (n != null) {
				PyObject a = topLevelPackage.__dict__.__getitem__(n);
				if (a instanceof PyJavaPackage) {
					print((PyJavaPackage) a, "", allClassesMap);
				}

				else {
				}
			} else {
				break;
			}
		}
	}

	private String printValue(Field f, Object ret) {
		;//System.out.println(" print value <" + f + "> <" + ret + ">");
		String v = "";
		try {
			f.setAccessible(true);
			v = "" + f.get(ret);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		if (v.length() > 50)
			v = v.substring(0, 50) + "...";
		return v;
	}

	private String strip(Method m) {
		Class<?>[] types = m.getParameterTypes();
		String s = "";
		for (int i = 0; i < types.length; i++) {
			s += strip(types[i].getName());
			if (i < types.length - 1)
				s += ", ";
		}
		return s;
	}

	private String strip(String name) {

		if (name.indexOf('.') != -1) {
			String[] sp = name.split("\\.");

			if (sp[sp.length - 1].contains("$")) {
				sp = sp[sp.length - 1].split("$");
			}

			return sp[sp.length - 1];
		}

		name = name.replaceAll("[<>]", "");

		return name;
	}

	protected List<ClassRecord> buildAllClasses() {

		Map<String, ClassRecord> allClassesMap = new HashMap<String, ClassRecord>();

		print(PySystemState.packageManager.topLevelPackage, "", allClassesMap);

		ClassLoader m = this.getClass().getClassLoader();
		assert m instanceof MyClassLoader : m.getClass();

		Set<Class> allLoadedClasses = ((MyClassLoader) m).getAllLoadedClasses();

		for (Class c : allLoadedClasses) {
			;//System.out.println(" class <" + c + ">");
			if (!allClassesMap.containsKey(c.getName())) {
				if (c.getName().indexOf(".") != -1)
					allClassesMap.put(c.getSimpleName(), new ClassRecord(c.getName().substring(c.getName().lastIndexOf(".") + 1, c.getName().length()), c.getName()));
			}
		}

		for (String s : getSourceDirs()) {
			addSourceDirToAllClasses(s, s, allClassesMap);
		}

		addClassToStaticallyImportedClasses("org.lwjgl.opengl.GL11", allClassesMap);
		addClassToStaticallyImportedClasses("org.lwjgl.opengl.GL12", allClassesMap);
		addClassToStaticallyImportedClasses("org.lwjgl.opengl.GL13", allClassesMap);
		addClassToStaticallyImportedClasses("org.lwjgl.opengl.GL14", allClassesMap);
		addClassToStaticallyImportedClasses("org.lwjgl.opengl.GL15", allClassesMap);
		addClassToStaticallyImportedClasses("org.lwjgl.opengl.GL20", allClassesMap);
		addClassToStaticallyImportedClasses("org.lwjgl.opengl.GL30", allClassesMap);

		allClasses = new ArrayList<ClassRecord>();
		allClasses.addAll(allClassesMap.values());
		Collections.sort(allClasses, sortClassRecord);

		return allClasses;
	}

	@Override
	protected void executePrintHandle() {

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

		if (s.startsWith("\n"))
			s = s.substring(1);

		inter.executeFragment(s.trim().startsWith("print") ? s : ("print " + s));
	}

	@Override
	protected boolean completionKeyHandle(String leftText, LinkedHashMap<String, iUpdateable> items, final iKeystrokeUpdate u) {
		leftText = leftText.trim();
		;//System.out.println(" completion key handle <" + leftText + ">");

		String[] b = leftText.split("\\(\"");
		String before = b[0];
		String after = b[1];

		String beforeExpression = balanceBack(before);

		;//System.out.println(" split <" + beforeExpression + "> <" + before + "> <" + after + ">");

		Object ex = null;
		try {
			if (shouldForceEvaluate) {
				ex = forceEvaluateTo;
			} else {
				ex = inter.executeReturningValue(beforeExpression);
				((StringWriter) getErrorOutput()).getBuffer().setLength(0);
			}

		} catch (Exception e) {
			return false;
		}
		if (ex == null)
			return false;
		shouldForceEvaluate = false;

		return interpretKeyHandleCompletionObject(ex, after, items, u);

	}

	private boolean interpretKeyHandleCompletionObject(final Object ex, final String after, LinkedHashMap<String, iUpdateable> items, final iKeystrokeUpdate u) {
		;//System.out.println(" interpret key handle completion object <" + ex + "> <" + (ex == null ? null : ex.getClass()) + ">");
		if (ex instanceof Collection) {
			for (Object o : ((Collection) ex)) {
				interpretKeyHandleCompletionObject(o, after, items, u);
			}
			return true;
		}
		if (ex instanceof Map) {
			for (Map.Entry o : ((Map<Object, Object>) ex).entrySet()) {
				interpretKeyHandleCompletionObject(o, after, items, u);
			}
			return true;
		}
		if (ex instanceof Map.Entry) {
			items.put("   <b>" + ((Map.Entry) ex).getKey() + "</b>", new iUpdateable() {

				@Override
				public void update() {
					int pos = ed.getCaretOffset();
					String s = ((Map.Entry) ex).getValue().toString();
					ed.getContent().replaceTextRange(pos - after.length(), after.length(), s);
				}
			});
			return true;
		}
		if (ex instanceof PyObject) {
			PyObject a = ((PyObject) ex).__findattr__("__completions__");
			if (a != null) {
				iFunction2 f = Py.tojava(a, iFunction2.class);
				Object res = f.f(ex, after);
				interpretKeyHandleCompletionObject(res, after, items, u);
				return true;
			} else {
				Object ex2 = Py.tojava((PyObject) ex, Object.class);
				if (ex2 != ex)
					return interpretKeyHandleCompletionObject(ex2, after, items, u);
				else
					return false;
			}
		}
		if (ex instanceof String) {

			items.put("   <b>" + ex + "</b>", new iUpdateable() {

				@Override
				public void update() {
					int pos = ed.getCaretOffset();
					String s = ex.toString();
					ed.getContent().replaceTextRange(pos - after.length(), after.length(), s);
				}
			});
			return true;
		}

		if (ex instanceof LocalFuture) {
			items.put("Waiting...", null);
			((LocalFuture) ex).addContinuation(new iUpdateable() {

				public void update() {
					forceEvaluateTo = ((LocalFuture) ex).get();
					shouldForceEvaluate = true;
					u.update(null);
				}
			});
			return true;
		}
		return false;
	}

	@Override
	public List<Completion> getCompletions(String leftText, boolean publicOnly, final iKeystrokeUpdate u) {
		ArrayList<Completion> comp = new ArrayList<Completion>();
		if (inter == null)
			return comp;

		// work back
		// balancing

		String cc = balanceBack(leftText);

		;//System.out.println(" looked at left text <" + leftText + "> got <" + cc + ">");

		if (cc.lastIndexOf('.') > 0) {

			String left = cc.substring(0, cc.lastIndexOf('.'));
			String right = cc.substring(cc.lastIndexOf('.') + 1, cc.length());

			final Object ret = shouldForceEvaluate ? forceEvaluateTo : inter.executeReturningValue(left);

			// ;//System.out.println(" completion for <" + ret + "> <"
			// + ret.getClass() + ">");

			shouldForceEvaluate = false;

			if (ret instanceof LocalFuture) {
				List<Completion> c = new ArrayList<Completion>();
				Completion ccc = new Completion() {

					@Override
					public void update() {
					}
				};

				ccc.enabled = false;
				ccc.text = "waiting...";

				((LocalFuture) ret).addContinuation(new iUpdateable() {

					public void update() {
						forceEvaluateTo = ((LocalFuture) ret).get();
						shouldForceEvaluate = true;
						u.update(null);
					}
				});

				c.add(ccc);
				return c;
			} else if (ret instanceof PickledCompletionInformation) {
				completionsFromPickledCompletionInfo(((PickledCompletionInformation) ret), right, comp, publicOnly);
			} else
				completionsFromReflectingUpon(ret, right, comp, publicOnly);
		} else {
			Map<String, Object> variables = PythonInterface.getPythonInterface().getVariables();
			cc = cc.trim();
			completionsFromLocalNamespace(cc, variables, comp);
		}

		if (comp.size() > 100)
			return comp.subList(0, 100);

		return comp;
	}

	protected String balanceBack(String leftText) {
		int round = 0;
		int square = 0;
		int curly = 0;

		String cc = "";
		for (int i = 0; i < leftText.length(); i++) {
			char c = leftText.charAt(leftText.length() - i - 1);
			if (c == ')')
				round++;
			if (c == '(')
				round--;
			if (c == ']')
				square++;
			if (c == '[')
				square--;
			if (c == '}')
				curly++;
			if (c == '{')
				curly--;
			if (c == ' ' || c == '=' || c == '@') {
				if (round >= 0 && square >= 0 && curly >= 0)
					break;
			}
			if (round >= 0 && square >= 0 && curly >= 0)
				cc = c + cc;
			else
				break;
		}
		return cc;
	}

	private void completionsFromPickledCompletionInfo(PickledCompletionInformation pickledCompletionInformation, final String right, ArrayList<Completion> comp, boolean publicOnly) {

		;//System.out.println(" pci <" + pickledCompletionInformation.info + "> <" + right + "> <" + comp + "> <" + publicOnly + ">");
		// pci <[['field', "<type 'NoneType'>", '__doc__', 'None'],
		// ['field', "<type 'str'>", '__module__', '__builtin__'],
		// ['field', "<type 'int'>", 'xx', '30']]> <x> <[]> <true>

		Completion c = new Completion() {

			@Override
			public void update() {
			}
		};
		c.enabled = false;
		c.text = "from remote process";

		comp.add(c);

		for (Object o : pickledCompletionInformation.info) {
			List i = ((List) o);
			String type = (String) i.get(0);

			if (type.equals("field")) {

				final String name = (String) i.get(2);
				if (name.startsWith(right) && (!publicOnly || !name.startsWith("_"))) {
					Completion cc = new Completion() {

						@Override
						public void update() {
							int old = ed.getCaretOffset();
							ed.getContent().replaceTextRange(old - right.length(), right.length(), name);
							if (right.length() == 0) {
								ed.setCaretOffset(old + name.length());
							}

						}
					};
					cc.enabled = true;
					cc.isDocumentation = false;
					cc.optionalDocumentation = "";
					cc.optionalPlainText = "";
					cc.text = "\u22f6  <b>" + name + "</b>" + " [" + ("" + i.get(1)) + "] " + i.get(3);

					comp.add(cc);
				}

			} else if (type.equals("javamethod")) {
				final String name = (String) i.get(1);
				if (name.startsWith(right) && (!publicOnly || !name.startsWith("_"))) {
					Completion cc = new Completion() {

						@Override
						public void update() {
							int old = ed.getCaretOffset();
							ed.getContent().replaceTextRange(old - right.length(), right.length(), name);
							if (right.length() == 0) {
								ed.setCaretOffset(old + name.length());
							}
						}
					};
					cc.enabled = true;
					cc.isDocumentation = false;
					cc.optionalDocumentation = "";
					cc.optionalPlainText = "";
					cc.text = "\u1d3e  <b>" + strip(rewriteOperator(name)) + " (</b>" + ("" + i.get(2)) + "<b>) </b> ";

					comp.add(cc);

				}
			} else if (type.equals("pythonmethod")) {
				final String name = (String) i.get(1);
				if (name.startsWith(right) && (!publicOnly || !name.startsWith("_"))) {
					Completion cc = new Completion() {

						@Override
						public void update() {
							int old = ed.getCaretOffset();
							ed.getContent().replaceTextRange(old - right.length(), right.length(), name);
							if (right.length() == 0) {
								ed.setCaretOffset(old + name.length());
							}
						}
					};
					cc.enabled = true;
					cc.isDocumentation = false;
					cc.optionalDocumentation = "";
					cc.optionalPlainText = "";
					cc.text = "\u1d3e  <b>" + strip(rewriteOperator(name)) + " (</b>" + ("" + i.get(2)) + "<b>) </b> ";

					comp.add(cc);
				}
			}
		}

	}

	@Override
	protected List<Completion> getImportations(String leftText) {
		String cc = "";
		for (int i = 0; i < leftText.length(); i++) {
			char c = leftText.charAt(leftText.length() - i - 1);
			if (!Character.isJavaIdentifierPart(c)) {
				break;
			}
			cc = c + cc;
		}
		if (cc.length() >= 2) {
			return buildCompletionsForClassName(cc);
		} else {
			return super.getImportations(leftText);
		}
	}

	public static String[] getSourceDirs() {
		String[] s = SystemProperties.getProperty("java.source.paths", "").split(":");
		ArrayList<String> rr = new ArrayList<String>();
		for (int i = 0; i < s.length; i++) {
			try {
				if (s[i].endsWith(".jar/"))
					s[i] = s[i].substring(0, s[i].length() - 1);

				File ff = new File(s[i]);
				if (ff.exists())
					rr.add(ff.getCanonicalPath());
			} catch (IOException e) {
			}
		}
		return rr.toArray(new String[rr.size()]);
	}

	public void setTopicString(String string) {
		if (string.length() > 100)
			string = string.substring(0, 100) + " ... ";
		topicOutput = string;
	}

	public Shell getFrame() {
		return frame;
	}

	public StyledText getOutputEditor() {
		return edOut;
	}

	public ExecutionRuler getExecutionRuler() {
		return executionRuler;
	}

	public void setAreas(State areaz) {
		executionRuler.getExecutedAreas().setState(areaz);
		ed.redraw();
	}

	public State getAreas() {
		return executionRuler.getExecutedAreas().getState();
	}

	public int doSearchAndReplace(String a, String b) {
		ed.setText(ed.getText().replace(a, b));
		return 0;
	}

	@Override
	protected String computeToolTip() {

		String t = ed.getSelectionText();
		if (t == null)
			return "";
		t = t.trim();

		Object o = PythonInterface.getPythonInterface().getVariable(t);
		if (o != null) {
			return "" + o;
		}
		return "";
	}

}
