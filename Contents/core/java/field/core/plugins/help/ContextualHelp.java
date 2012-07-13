package field.core.plugins.help;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.NextUpdate;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.launch.Launcher;
import field.launch.iUpdateable;
import field.math.abstraction.iProvider;

@Woven
public class ContextualHelp {

	private final HelpBrowser helpBrowser;

	static public final VisualElementProperty<String> contextualHelp = new VisualElementProperty<String>("contextualHelp");

	public ContextualHelp(HelpBrowser helpBrowser) {
		this.helpBrowser = helpBrowser;

		Launcher.getLauncher().registerUpdateable(new iUpdateable() {

			@Override
			public void update() {
				updateContext();
			}
		});
	}

	HashMap<String, Context> contexts = new LinkedHashMap<String, Context>();
	String lastString = "";

	protected void updateContext() {

		String next = "";
		boolean hasContext = false;

		int m = 0;
		String q = "";
		
		for (Context c : contexts.values()) {
			c.inContext = Math.max(0, c.inContext - 1);
			
			
			if (c.inContext > m) {
				c.lastString = c.contents.get();
				hasContext = true;

				next = c.lastString + "\n\n";
				
				m = c.inContext;
			}
		}

		if (hasContext) {
			if (!lastString.equals(next) && next!=null) {
				lastString = next;
				offerHelp("", next);
			}
		} else {

		}
	}

	static public class Context {
		int inContext = 0;

		String lastString = "";

		iProvider<String> contents;

		public Context(int inContext, iProvider<String> contents) {
			super();
			this.inContext = inContext;
			this.contents = contents;
		}

	}

	public iProvider<String> providerForStaticMarkdownResource(final String name) {
		final File m = new File(HelpBrowser.documentationDirectory + name);
		return new iProvider<String>() {

			@Override
			public String get() {
				try {
					BufferedReader r = new BufferedReader(new FileReader(m));
					final StringBuffer s = new StringBuffer();
					while (r.ready()) {
						s.append(r.readLine() + "\n");
					}
					return s.toString();
				} catch (IOException e) {
//					System.err.println(" exception thrown while trying to load markdown documentation '" + name + " "+m.getAbsolutePath());
//					e.printStackTrace();
					return null;
				}
			}
			
			@Override
			public String toString() {
				return "provider:"+name;
			}
		};
	}

	public void addContextualHelpForWidget(final String name, final Widget w, iProvider<String> help, final int duration) {
		if (help == null)
			return;
		final Context c = new Context(0, help);
		contexts.put(name, c);

		w.addListener(SWT.MouseMove, new Listener() {

			@Override
			public void handleEvent(Event event) {

				;//System.out.println(" got mouse move over <" + w + "> <"+name+">");

				c.inContext = duration;
			}
		});
	}

	public void addContextualHelpForToolItem(String name, final ToolItem w, iProvider<String> help, final int duration) {
		if (help == null)
			return;
		final Context c = new Context(0, help);
		contexts.put(name, c);

		w.getParent().addListener(SWT.MouseMove, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				for (ToolItem cc : w.getParent().getItems()) {
					if (cc == w && cc.getBounds().contains(arg0.x, arg0.y))
						c.inContext = duration;
				}

				;//System.out.println(" got mouse move over <" + w + ">");

			}
		});
	}

	@NextUpdate
	public void offerHelp(String type, String markdown) {
		if (helpBrowser.isPinned())
			return;
		if (markdown == null)
			return;

		if (markdown.equals("null")) return;
		
		String x = helpBrowser.proc.markdownToHtml(markdown);

		x = x.replace("<p>[fold]</p>", "<div class=\"accordion\">");
		x = x.replace("<p>[/fold]</p>", "</div>");

		helpBrowser.setText(x);
	}

}
