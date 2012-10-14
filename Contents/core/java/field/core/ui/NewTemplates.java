package field.core.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.JTextField;
import javax.swing.text.BadLocationException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.Sun14ReflectionProvider;
import com.thoughtworks.xstream.core.ReferenceByIdMarshallingStrategy;

import field.core.dispatch.iVisualElement;
import field.core.plugins.pseudo.PseudoPropertiesPlugin;
import field.core.ui.SmallMenu.BetterPopup;
import field.core.ui.SmallMenu.Documentation;
import field.core.ui.SmallMenu.iKeystrokeUpdate;
import field.core.windowing.GLComponentWindow;
import field.launch.iUpdateable;
import field.math.abstraction.iAcceptor;
import field.namespace.generic.Generics.Pair;
import field.util.ChannelSerializer;
import field.util.FloatBufferSerializer;
import field.util.MarkerSerializer;

/**
 * a better templating mechanism
 * 
 * @author marc
 * 
 */
public class NewTemplates {

	private final iVisualElement root;
	public String templateFolder;

	public NewTemplates(iVisualElement root) {
		this.root = root;
		templateFolder = PseudoPropertiesPlugin.workspaceFolder.get(root) + "/templates/";
		if (!new File(templateFolder).exists())
			new File(templateFolder).mkdir();

	}

	public String suffix = ".template";

	public List<Pair<String, String>> getAllTemplates() {
		;// System.out.println(" all templates in <" + templateFolder +
			// ">");
		File[] all = new File(templateFolder).listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				;// System.out.println(" checking <" + name +
					// ">");
				return name.endsWith(suffix);
			}
		});

		if (all == null)
			return Collections.EMPTY_LIST;

		List<Pair<String, String>> aa = new ArrayList<Pair<String, String>>();
		for (File ff : all)
			aa.add(new Pair<String, String>(ff.getName().substring(0, ff.getName().length() - suffix.length()), infoForFile(ff)));

		return aa;
	}

	private String infoForFile(File ff) {
		XStream stream = new XStream(new Sun14ReflectionProvider());
		stream.registerConverter(new ChannelSerializer());
		stream.registerConverter(new FloatBufferSerializer());
		stream.registerConverter(new MarkerSerializer(stream.getClassMapper()));
		stream.setMarshallingStrategy(new ReferenceByIdMarshallingStrategy());

		try {
			ObjectInputStream input = stream.createObjectInputStream(new BufferedReader(new FileReader(ff)));
			Object o = input.readObject();
			input.close();
			return (String) o;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return "(can't load)";
	}

	public BetterPopup completionsFor(String left, iKeystrokeUpdate u, final iAcceptor<String> inserter) {
		LinkedHashMap<String, iUpdateable> up = new LinkedHashMap<String, iUpdateable>();
		up.put("<b>" + left + "</b>", null);
		up.put("Templates", null);
		List<Pair<String, String>> all = getAllTemplates();

		;// System.out.println(" all templates are <" + all + ">");

		int num = 0;
		for (final Pair<String, String> a : all) {
			if (a.left.startsWith(left)) {
				num++;
			}
		}

		for (final Pair<String, String> a : all) {
			;// System.out.println(" checking <" + a.left +
				// "> against <" + left + ">");
			if (a.left.startsWith(left)) {

				final String text = "\u1d40 <b>" + a.left + "</b> \u2014 <i>" + trim(a.right) + "</i>";
				up.put(text, new iUpdateable() {
					public void update() {
						inserter.set(a.left);
					}
				});
			}
		}

		if (all.size() == 0) {
			up.put("<i>(no templates are installed, right click on a selected box to make one)</i>", new iUpdateable() {
				public void update() {
				}
			});
		} else if (up.size() == 2) {
			up.put("(no templates start with <b>'" + left + "'</b>)", new iUpdateable() {
				public void update() {
				}
			});
		}

		GLComponentWindow r = iVisualElement.enclosingFrame.get(root);

		return new SmallMenu().createMenu(up, r.getFrame(), u);
	}

	private String trim(String right) {
		if (right.length() > 40)
			return right.substring(0, 40) + "\u2014";
		return right;
	}

	public void getTemplateName(final Point at, final iAcceptor<String> result) {
		final String[] left = { "" };
		final BetterPopup[] completions = { null };
		completions[0] = completionsFor(left[0], new iKeystrokeUpdate() {

			public void update(KeyEvent ke) {
			}

			@Override
			public boolean update(Event ke) {

				;// System.out.println(" key code is <" +
					// ke.keyCode + ">" +
					// Character.isISOControl(ke.character));

				if (ke.keyCode == 13) {
					// result.set(left[0]);
					return true;
				} else if (ke.keyCode == SWT.ESC) {
					return true;
				} else if (ke.character == SWT.BS) {
					left[0] = left[0].substring(0, left[0].length() - 1);
				} else if (Character.isISOControl(ke.character)) {
					return false;
				}

				else {
					left[0] += ke.character;
				}
				completions[0].shell.setVisible(false);
				completions[0] = completionsFor(left[0], this, result);
				completions[0].show(new Point(at.x, at.y));
				// completions[0].requestFocusInWindow();
				// ((BetterPopup)
				// completions[0]).getKey().down();

				return true;
			}
		}, result);

		;// System.out.println(" popping open completions menu");

		completions[0].show(new Point(at.x, at.y));
		// completions[0].requestFocusInWindow();
		// ((BetterPopup) completions[0]).getKey().down();
	}

	// public void getTemplateName(Point at, String label, String def, final
	// iAcceptor<String> result) {
	// new PopupTextBox(def, at, label) {
	// @Override
	// protected void changed(String text) {
	// result.set(text);
	// }
	//
	//
	//
	// BetterPopup m = null;
	//
	// @Override
	// protected void completion(final JTextField inside, final
	// iKeystrokeUpdate iKeystrokeUpdate) {
	// m = completionsFor(inside.getText(), new iKeystrokeUpdate() {
	//
	// public void update(KeyEvent ke) {
	// ;//System.out.println(" hiding menu ... ");
	// m.setVisible(false);
	// iKeystrokeUpdate.update(ke);
	// }
	// }, new iAcceptor<String>() {
	// public field.math.abstraction.iAcceptor<String> set(String to) {
	// inside.setText(to);
	// return this;
	// };
	// });
	//
	// try {
	// Rectangle pp;
	// pp = inside.modelToView(inside.getText().length());
	// m.show(inside, pp.x, pp.y);
	// m.requestFocusInWindow();
	// ((BetterPopup) m).getKey().down();
	//
	// } catch (BadLocationException e) {
	// e.printStackTrace();
	// }
	// }
	// }.doCompletions();
	// }
}
