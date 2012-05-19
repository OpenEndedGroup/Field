package field.core.ui.text.embedded;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.JTextField;

import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.GlyphMetrics;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.persistance.FluidPersistence;
import field.core.plugins.python.PythonPlugin;
import field.core.plugins.python.PythonPluginEditor;
import field.core.ui.text.PythonTextEditor.EditorExecutionInterface;
import field.core.ui.text.embedded.CustomInsertDrawing.iInsertRenderingContext;
import field.graphics.ci.MinimalImageHistogram;
import field.graphics.ci.MinimalStatsBox;
import field.launch.SystemProperties;
import field.namespace.generic.Generics.Pair;
import field.namespace.generic.Generics.Triple;
import field.util.ANSIColorUtils;

public class CustomInsertSystem implements iCustomInsertSystem {

	public enum ExecutesWhat {
		line, enclosingBlock, everything;
	}

	public enum ExecutesWhen {
		always, never, onMouseUp;
	}

	public interface ProvidesWidth {
		public int getWidthNow();
	}

	static public class iPossibleComponent {
		public String name;

		public Class<? extends ProvidedComponent> clazz;

		public iPossibleComponent(String name, Class<? extends ProvidedComponent> clazz) {
			this.name = name;
			this.clazz = clazz;
		}

		public void prep(ProvidedComponent pc, iVisualElement currentlyEditing, String currentSelection) {

		}
	}

	static public abstract class ProvidedComponent {
		public transient JComponent component;
		public transient Control swt_control;

		public int tagNumber;

		transient StyleRange style;
		transient iVisualElement inside;

		public ProvidedComponent() {
		}

		public ProvidedComponent(JComponent js) {
			component = js;
		}

		public void deserialize(iVisualElement inside) {

			System.out.println(" component has deserialized inside <" + inside + ">");

			new Exception().printStackTrace();

			this.inside = inside;
		}

		public void executeThisLine(iInsertRenderingContext irc, ExecutesWhat enclosing) {
			System.out.println(" execute this line :" + enclosing + " " + inside);

			StyledText component = irc.getText();
			String text = component.getText();
			PythonPluginEditor editor = (PythonPluginEditor) PythonPlugin.python_plugin.get(inside);
			int indexOf = text.indexOf(stringForTag(tagNumber));

			System.out.println(" indexof is <" + indexOf + ">");

			if (indexOf != -1) {
				String[] lines = text.split("\n");

				int start = 0;
				for (int n = 0; n < lines.length; n++) {
					int end = start + lines[n].length() + 1;
					if (start <= indexOf && end > indexOf) {
						System.out.println(" inside :" + inside);
						EditorExecutionInterface eei = ((PythonPluginEditor) PythonPluginEditor.python_plugin.get(inside)).getEditor().getInterface();

						if (enclosing == ExecutesWhat.enclosingBlock) {
							String substring = new ExecutableAreaFinder().findExecutableSubstringAnyKey(editor.getCustomInsertSystem().convertUserTextPositionToExecutableTextPosition(text, indexOf), editor.getCustomInsertSystem().convertUserTextToExecutableText(text));
							System.out.println(" foud <" + substring + ">");
							eei.executeFragment(substring);
						} else if (enclosing == ExecutesWhat.everything) {
							String substring = new ExecutableAreaFinder().findExecutableSubstring(indexOf, text, 0);
							eei.executeFragment(substring);
						} else {
							eei.executeFragment(lines[n]);
						}
					}
					start = end;
				}
			}
		}

		// TODO swt embed
		// public void executeThisLine() {
		// executeThisLine(ExecutesWhat.line);
		// }

		// public void executeThisLine(ExecutesWhat enclosing) {
		// if (component.isDisplayable()) {
		// Container p1 = component.getParent();
		// Container p2 = p1.getParent();
		//
		// assert p2 instanceof JEditorPane : p2.getClass();
		// try {
		// String text = ((JEditorPane) p2).getDocument().getText(0,
		// ((JEditorPane) p2).getDocument().getLength());
		//
		// PythonPluginEditor editor = (PythonPluginEditor)
		// PythonPlugin.python_plugin.get(inside);
		//
		// int indexOf = text.indexOf(stringForTag(tagNumber));
		// if (indexOf != -1) {
		// String[] lines = text.split("\n");
		//
		// int start = 0;
		// for (int n = 0; n < lines.length; n++) {
		// int end = start + lines[n].length() + 1;
		// if (start <= indexOf && end > indexOf) {
		// System.out.println(" inside :" + inside);
		// EditorExecutionInterface eei = ((PythonPluginEditor)
		// PythonPluginEditor.python_plugin.get(inside)).getEditor().getInterface();
		// System.out.println(" eei :" + eei);
		//
		// if (enclosing == ExecutesWhat.enclosingBlock) {
		// String substring = new
		// ExecutableAreaFinder().findExecutableSubstringAnyKey(editor.getCustomInsertSystem().convertUserTextPositionToExecutableTextPosition(text,
		// indexOf),
		// editor.getCustomInsertSystem().convertUserTextToExecutableText(text));
		// System.out.println(" foud <" + substring + ">");
		// eei.executeFragment(substring);
		// } else if (enclosing == ExecutesWhat.everything) {
		// String substring = new
		// ExecutableAreaFinder().findExecutableSubstring(indexOf, text,
		// 0);
		// eei.executeFragment(substring);
		// } else {
		// eei.executeFragment(lines[n]);
		// }
		//
		// break;
		// }
		// start = end;
		// }
		// }
		// } catch (BadLocationException e) {
		// e.printStackTrace();
		// }
		// }
		// }

		abstract public String getCurrentRepresentedString();

		public void makeNew() {
		}

		public void preserialize() {
		}

		public int getPositionFor(CustomInsertDrawing.iInsertRenderingContext context, boolean startPosition) {

			// String text = ((JEditorPane)
			// p2).getDocument().getText(0,
			// ((JEditorPane) p2).getDocument().getLength());

			String text = context.getText().getText();

			int indexOf = text.indexOf(stringForTag(tagNumber));
			if (indexOf != -1) {
				String[] lines = text.split("\n");

				int start = 0;
				boolean on = false;

				for (int n = 0; n < lines.length; n++) {
					int end = start + lines[n].length() + 1;
					if (start <= indexOf && end > indexOf) {
						if (startPosition)
							return start;
						else
							return end;
					}
					start = end;
				}
			}

			return 0;
		}

		public Rect boundsOfRange(CustomInsertDrawing.iInsertRenderingContext context, int start, int end) {
			
			System.out.println(" bounds of range <"+start+" "+end+">");
			
			if (start > end) {
				int a = start;
				start = end;
				end = a;
			}
			
			if (start==end)
			{
				end += 1;
				start -=1;
			}
			
			Control cc = context.getControl();

			
			StyledText ed = context.getText();
			if (start<0) start = 0;
			if (start>ed.getCharCount()) start = ed.getCharCount();
			if (end<0) end= 0;
			if (end>ed.getCharCount()) end= ed.getCharCount();
			
			System.out.println(" ed text is :"+ed.getText());

			Rectangle rr = null;
			for (int i = start; i < end; i++) {

				Point loc = ed.getLocationAtOffset(i);
				Rectangle r = new Rectangle(loc.x, loc.y, 10, 10);

				if (rr == null)
					rr = r;
				else
					rr = rr.union(r);
			}

			return new Rect(rr.x, rr.y, rr.width, rr.height);

		}

	}

	public static class Replace {
		public int from;

		public int to;

		public String with;

		public String was;

		public ProvidedComponent comp;

		@Override
		public String toString() {
			return "replacement<" + from + " -> " + to + "> <" + with + " / " + was + "> <" + comp + ">\n";
		}

	}

	static public List<iPossibleComponent> possibleComponents = new ArrayList<iPossibleComponent>();

	static public List<Triple<String, iPossibleComponent, iPossibleComponent>> possibleWrappers = new ArrayList<Triple<String, iPossibleComponent, iPossibleComponent>>();

	// static Pattern findText = Pattern.compile("XXX(.)XXX");
	static Pattern findText = Pattern.compile("([\uf800-\uff00])");

	static public void defaultPossibleInserts() {
		possibleComponents.add(new iPossibleComponent(" \u25a5\t<b>Label</b> (commented section with keyboard shortcut)", MinimalTextField.Component.class));
		// way too undocumented right now
		// possibleComponents.add(new
		// iPossibleComponent(" \u25a8\tBlock \u2014 <b>Freeze start</b>",
		// MinimalTextField.Component_blockStart.class));
		possibleComponents.add(new iPossibleComponent(" \u25a7\tBlock \u2014 <b>Text Transform begin</b>", MinimalTextField_blockMenu.Component_transformBlockStart.class));
		possibleComponents.add(new iPossibleComponent(" \u25a7\tBlock \u2014 <b>Text Transform end</b>", MinimalTextField_blockMenu.Component_transformBlockEnd.class));
		possibleComponents.add(new iPossibleComponent(" \u21a6\t<b>Slider</b> (returns <i>float</i>)", MinimalSlider.Component.class));
		possibleComponents.add(new iPossibleComponent(" \u21a3\t<b>Slider</b> (returns <i>Lazy / iFloatProvider</i>)", MinimalSlider.Component_iFloatProvider.class));
		possibleComponents.add(new iPossibleComponent(" \u2698\t<b>Color</b> (returns <i>Vector4</i> between 0 and 1)", MinimalColorWell.Component.class));
		possibleComponents.add(new iPossibleComponent(" \u2698\t<b>Color * 255</b> (returns <i>Vector4</i> between 0 and 255)", MinimalColorWell.Component255.class));
		// possibleComponents.add(new
		// iPossibleComponent(" \u22a1\t<b>File</b> (returns <i>java.io.File</i>)",
		// MinimalFileBox.Component.class));
		possibleComponents.add(new iPossibleComponent(" \u22a1\t<b>x,y</b> (returns <i>2-tuple</i>)", MinimalXYSlider.Component_tuple.class));
		possibleComponents.add(new iPossibleComponent(" \u22a1\t<b>x,y</b> (returns <i>Vector2</i>)", MinimalXYSlider.Component_vector2.class));
		possibleComponents.add(new iPossibleComponent(" \u22a1\t<b>x,y</b> (returns <i>Vector3</i>)", MinimalXYSlider.Component_vector3.class));
		possibleComponents.add(new iPossibleComponent(" \u22a1\t<b>x,y</b> (returns <i>Lazy / iProvider<Vector3></i>", MinimalXYSlider.Component_vector3_provider.class));
		// possibleComponents.add(new
		// iPossibleComponent(" \u223f\t<b>graph</b>",
		// MinimalGraphWidget.Component_tuple.class));
		possibleComponents.add(new iPossibleComponent(" \u223f\t<b>graph editor</b>", MinimalGraphWidget2.Component_tuple.class));
		possibleComponents.add(new iPossibleComponent(" \u223f\t<b>image histogram (experiemental)</b>", MinimalImageHistogram.Component.class));
		possibleComponents.add(new iPossibleComponent(" \u223f\t<b>stats box (experiemental)</b>", MinimalStatsBox.Component.class));
		// possibleComponents.add(new
		// iPossibleComponent(" \u223f\t<b>graph</b> (with experimental overlay)",
		// MinimalGraphWidget.Component_tuple2.class));
		// possibleComponents.add(new
		// iPossibleComponent(" \u00a7\t<b>popup selection</b>",
		// MinimalCombo.Component.class));
		// possibleComponents.add(new
		// iPossibleComponent(" \u2041\t<b>tree selection</b>",
		// MinimalTree.Component.class));
		possibleComponents.add(new iPossibleComponent(" \u21b7\t<b>stepper</b> for generators", MinimalStepper.Component_Stepper.class));

		// TODO swt emebed
		possibleComponents.add(new iPossibleComponent(" \u25a2\t<b>lazy</b> text box", MinimalLazyBox.Component.class) {
			@Override
			public void prep(ProvidedComponent arg0, iVisualElement arg1, String arg2) {
				if (arg2 == null)
					arg2 = "None";
				((MinimalLazyBox.Component) arg0).valueString = arg2;
				((MinimalLazyBox.Component) arg0).updateValue(arg2);
				if (((MinimalLazyBox.Component) arg0).component != null)
					((JTextField) ((MinimalLazyBox.Component) arg0).component).setText(arg2);
			}
		});
		//
		// possibleComponents.add(new
		// iPossibleComponent(" \u25a2\t<b>toggle</b> text box",
		// MinimalFlipBox.Component.class) {
		// @Override
		// public void prep(ProvidedComponent arg0, iVisualElement arg1,
		// String
		// arg2) {
		// if (arg2 == null)
		// arg2 = "";
		// ((MinimalFlipBox.Component) arg0).value1String = arg2;
		// ((MinimalFlipBox.Component) arg0).value2String = "";
		// ((MinimalFlipBox.Component) arg0).updateValue();
		// if (((MinimalFlipBox.Component) arg0).component != null)
		// ((JTextField) ((MinimalFlipBox.Component)
		// arg0).component).setText(arg2);
		// }
		// });

		// possibleComponents.add(new
		// iPossibleComponent(" \u203b\t<b>reference</b> to another <i>iVisualElement</i>",
		// MinimalReference.Component.class));
		//
		// possibleComponents.add(new
		// iPossibleComponent(" \u203b\t<b>reference</b> to marked <i>iVisualElements</i>",
		// MinimalReference.Component.class) {
		// @Override
		// public void prep(ProvidedComponent pc, iVisualElement
		// currentlyEditing, String currentSelection) {
		//
		// ((MinimalReference.Component) pc).algorithmName =
		// "Explicit(\"nothing marked\")";
		//
		// SelectionGroup<iComponent> marking =
		// iVisualElement.markingGroup.get(currentlyEditing);
		// if (marking != null) {
		// Set<iComponent> components = marking.getSelection();
		// if (components.size() != 0) {
		// iComponent component = components.iterator().next();
		// iVisualElement element = component.getVisualElement();
		// ((MinimalReference.Component) pc).algorithmName =
		// "Explicit(\"#" + element.getUniqueID().replace(":", ".") +
		// "#\")";
		// }
		// }
		//
		// }
		// });

		possibleWrappers.add(new Triple<String, iPossibleComponent, iPossibleComponent>(" \u25a8 wrap selected text in a <b>new labeled block</b> ", new iPossibleComponent("", MinimalTextField.Component.class) {
			@Override
			public void prep(ProvidedComponent pc, iVisualElement currentlyEditing, String currentSelection) {
				((MinimalTextField.Component) pc).valueString = "1. untitled";
			}
		}, new iPossibleComponent("", MinimalTextField.Component.class) {
			@Override
			public void prep(ProvidedComponent pc, iVisualElement currentlyEditing, String currentSelection) {
				((MinimalTextField.Component) pc).valueString = "1. untitled";
			}
		}));

		possibleWrappers.add(new Triple<String, iPossibleComponent, iPossibleComponent>(" \u25a7 wrap selected text in a <b>new transform block</b>", new iPossibleComponent("", MinimalTextField_blockMenu.Component_transformBlockStart.class) {
			@Override
			public void prep(ProvidedComponent pc, iVisualElement currentlyEditing, String currentSelection) {
				((MinimalTextField_blockMenu.Component_transformBlockStart) pc).valueString = "1. defaultTransform";
			}
		}, new iPossibleComponent("", MinimalTextField_blockMenu.Component_transformBlockEnd.class) {
			@Override
			public void prep(ProvidedComponent pc, iVisualElement currentlyEditing, String currentSelection) {
				((MinimalTextField_blockMenu.Component_transformBlockEnd) pc).valueString = "1. defaultTransform";
			}
		}));
	}

	public static String stringForTag(int tag) {

		return "" + (char) (0xf800 + tag);

		// return "XXX" + (char) ('0' + tag) + "XXX";

	}

	Map<Integer, ProvidedComponent> comps = new HashMap<Integer, ProvidedComponent>();

	int currentMaxTagNumber = 0;

	FluidPersistence persistance = new FluidPersistence(null);

	public ProvidedComponent addComponent(ProvidedComponent comp) {
		while (comps.containsKey(currentMaxTagNumber))
			currentMaxTagNumber++;

		comp.tagNumber = currentMaxTagNumber;
		comps.put(comp.tagNumber, comp);
		return comp;
	}

	public int convertUserTextPositionToExecutableTextPosition(String text, int caretPosition) {
		String input = text;

		Matcher matcher = findText.matcher(input);

		List<Replace> replacements = new ArrayList<Replace>();
		while (matcher.find()) {
			Replace replace = new Replace();
			replace.from = matcher.start();
			replace.to = matcher.end();
			ProvidedComponent c = componentForTag(parseTagNumber(matcher.group(1)));
			replace.with = (c == null ? "((missing component))" : c.getCurrentRepresentedString());
			replacements.add(replace);
		}
		for (int i = replacements.size() - 1; i >= 0; i--) {
			Replace replace = replacements.get(i);
			input = input.substring(0, replace.from) + replace.with + input.substring(replace.to, input.length());
			if (replace.from < caretPosition)
				caretPosition -= (replace.to - replace.from) - replace.with.length();
		}
		return caretPosition;
	}

	public String convertUserTextToExecutableText(String text) {

		String input = text;

		Matcher matcher = findText.matcher(input);

		List<Replace> replacements = new ArrayList<Replace>();
		while (matcher.find()) {
			Replace replace = new Replace();
			replace.from = matcher.start();
			replace.to = matcher.end();
			ProvidedComponent c = componentForTag(parseTagNumber(matcher.group(1)));
			replace.with = (c == null ? "((missing component))" : c.getCurrentRepresentedString());
			replacements.add(replace);
		}
		for (int i = replacements.size() - 1; i >= 0; i--) {
			Replace replace = replacements.get(i);
			input = input.substring(0, replace.from) + replace.with + input.substring(replace.to, input.length());
		}
		return input;
	}

	public String getStringForComponent(ProvidedComponent component) {
		return stringForTag(component.tagNumber);
	}

	public String mergeInText(Pair<String, Object> text) {

		if (text.right == null)
			return text.left;
		List<Replace> replacements = (List<Replace>) text.right;
		String input = text.left;

		try {
			for (int i = 0; i < replacements.size(); i++) {
				Replace replace = replacements.get(i);

				// this feels
				// wrong
				// replace.with
				// =
				// replace.comp==null
				// ?
				// replace.with
				// :
				// replace.comp.getCurrentRepresentedString();

				if (replace.comp == null || replace.comp.tagNumber != -1) {
					input = input.substring(0, replace.from) + (replace.comp == null ? replace.was : stringForTag(replace.comp.tagNumber)) + input.substring(replace.from + replace.with.length(), input.length());
					if (replace.comp != null)
						comps.put(replace.comp.tagNumber, replace.comp);
				} else {
					// need
					// to
					// allocate
					// it a
					// tag,
					// and
					// generate
					// a
					// string
					// for
					// it
					while (comps.containsKey(currentMaxTagNumber) || foundIn(replacements, currentMaxTagNumber))
						currentMaxTagNumber++;

					replace.comp.tagNumber = currentMaxTagNumber;
					replace.was = stringForTag(replace.comp.tagNumber);
					input = input.substring(0, replace.from) + replace.was + input.substring(replace.from + replace.with.length(), input.length());
					// input
					// =
					// input.substring(0,
					// replace.from)
					// +
					// replace.was
					// +
					// input.substring(replace.from
					// +
					// replace.with.length(),
					// input.length());
					comps.put(replace.comp.tagNumber, replace.comp);
				}

			}
		} catch (StringIndexOutOfBoundsException ex) {
			ex.printStackTrace();
		}
		return input;
	}

	public Pair<String, Object> mergeOutText(String text) {
		String input = text;

		Matcher matcher = findText.matcher(input);
		List<Replace> replacements = new ArrayList<Replace>();
		while (matcher.find()) {
			Replace replace = new Replace();
			replace.from = matcher.start();
			replace.to = matcher.end();
			replace.comp = duplicateComponent(componentForTag(parseTagNumber(matcher.group(1))));
			replace.with = (replace.comp == null ? "((missing component))" : replace.comp.getCurrentRepresentedString());
			replace.was = matcher.group();
			replacements.add(replace);
		}
		System.out.println(" merge out on <" + text + ">");

		for (int i = replacements.size() - 1; i >= 0; i--) {
			Replace replace = replacements.get(i);
			System.out.println(" replace <" + replace.from + " -> " + replace.to + ">");
			input = input.substring(0, replace.from) + replace.with + input.substring(replace.to, input.length());
			System.out.println(" input now <" + input + ">");
			if (replace.comp != null) {
				replace.comp.preserialize();
			}
		}
		return new Pair<String, Object>(input, replacements);
	}

	public String swapInText(Pair<String, Object> text) {
		comps.clear();
		return mergeInText(text);
	}

	public Pair<String, Object> swapOutText(String text) {

		String input = text;

		Matcher matcher = findText.matcher(input);
		List<Replace> replacements = new ArrayList<Replace>();
		while (matcher.find()) {
			Replace replace = new Replace();
			replace.from = matcher.start();
			replace.to = matcher.end();
			replace.comp = componentForTag(parseTagNumber(matcher.group(1)));
			replace.with = (replace.comp == null ? "((missing component))" : replace.comp.getCurrentRepresentedString());
			replace.was = matcher.group();
			replacements.add(replace);
		}
		for (int i = replacements.size() - 1; i >= 0; i--) {
			Replace replace = replacements.get(i);
			input = input.substring(0, replace.from) + replace.with + input.substring(replace.to, input.length());
			if (replace.comp != null) {
				replace.comp.preserialize();
			}
		}

		return new Pair<String, Object>(input, replacements);
	}

	int styleWidth = SystemProperties.getIntProperty("defaultEmbeddedUIWidth", 300);

	public StyleRange styleForTag(String tag, iVisualElement inside, StyleRange in) {

		Matcher matcher = findText.matcher(tag);
		if (matcher.find()) {
			ProvidedComponent component = componentForTag(parseTagNumber(matcher.group(1)));
			if (component != null) {
				if (component.style == null) {
					component.deserialize(inside);
				}
				in.data = component.component;
				component.style = in;

				component.component.putClientProperty("dead", null);

				in.metrics = new GlyphMetrics(10, 5, styleWidth);

				if (component instanceof ProvidesWidth) {
					in.metrics.width = ((ProvidesWidth) component).getWidthNow();
				}

				System.out.println(" style for tag, width is <" + in.metrics.width + ">");

			}
		}
		return in;

	}

	public void updateAllStyles(StyledText document, iVisualElement inside) {
		String text = document.getText();

		Matcher matcher = findText.matcher(text);
		while (matcher.find()) {
			ProvidedComponent component = componentForTag(parseTagNumber(matcher.group(1)));
			if (component != null) {
				if (component.style == null) {
					component.deserialize(inside);

					StyleRange style = new StyleRange();
					style.start = matcher.start();
					style.length = matcher.end() - matcher.start();
					style.data = component.component;
					style.metrics = new GlyphMetrics(10, 5, styleWidth);

					if (component instanceof ProvidesWidth) {
						System.out.println(" provides width ");
						style.metrics.width = ((ProvidesWidth) component).getWidthNow();
					}
					component.style = style;

					// TODO swt emebed metrics, especially
					// for components that
					// can change size

					// document.setStyleRange(component.style);
				} else {
					component.style.start = matcher.start();
					component.style.length = matcher.end() - matcher.start();
					component.style.data = component.component;
					component.style.metrics = new GlyphMetrics(10, 5, styleWidth);

					if (component instanceof ProvidesWidth) {
						System.out.println(" provides width ");
						component.style.metrics.width = ((ProvidesWidth) component).getWidthNow();
					}

					// document.setStyleRange(component.style);
				}
			}
		}
	}

	private ProvidedComponent componentForTag(int i) {
		ProvidedComponent c = comps.get(i);
		if (c == null)
			System.err.println(ANSIColorUtils.red("no component for <" + i + "> <" + comps + ">"));
		return c;
	}

	private ProvidedComponent duplicateComponent(ProvidedComponent component) {

		ProvidedComponent cp2 = persistance.xmlDuplicate(component);
		cp2.makeNew();

		cp2.tagNumber = -1;
		return cp2;
	}

	private boolean foundIn(List<Replace> replacements, int tagnumber) {
		for (Replace r : replacements) {
			if (r.comp != null && r.comp.tagNumber == tagnumber)
				return true;
		}
		return false;
	}

	private int parseTagNumber(String group) {
		char c = group.charAt(0);
		// return (c - '0');
		return c - 0xf800;
	}
}
