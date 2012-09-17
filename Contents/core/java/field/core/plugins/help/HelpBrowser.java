package field.core.plugins.help;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.ref.Reference;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.OpenWindowListener;
import org.eclipse.swt.browser.WindowEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.pegdown.PegDownProcessor;
import org.python.core.PyObject;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.NextUpdate;
import field.core.Platform;
import field.core.Platform.OS;
import field.core.StandardFluidSheet;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.execution.PythonInterface;
import field.core.plugins.BaseSimplePlugin;
import field.core.plugins.drawing.SplineComputingOverride;
import field.core.plugins.help.NanoHTTPD.Response;
import field.core.plugins.python.PythonPlugin;
import field.core.plugins.python.PythonPluginEditor;
import field.core.plugins.selection.ToolBarFolder;
import field.core.ui.MacScrollbarHack;
import field.launch.Launcher;
import field.launch.iUpdateable;
import field.math.abstraction.iProvider;
import field.namespace.generic.Generics.Pair;

@Woven
public class HelpBrowser extends BaseSimplePlugin {

	static public final VisualElementProperty<HelpBrowser> helpBrowser = new VisualElementProperty<HelpBrowser>("helpBrowser");

	static public final VisualElementProperty<String> documentation = new VisualElementProperty<String>("documentation");

	private Browser browser;

	static PegDownProcessor proc;

	static String preamble = "<html><head> <script type=\"text/javascript\" src=\"http://localhost:10010/documentation/scripts/mootools.js\"></script><style type=\"text/css\">\n" + "body\n" + "{\n" + "	line-height:18px; font-size:12px; background-color:#d3d3d3;\n" + "	font-family:\"Gill Sans\";\n" + "}\n" + "h1\n" + "{\n" + "	font-size:18px;\n" + "	background-color:#bbb;\n" + "	margin-left:-10px;\n" + "	padding-left:10px;\n" + "	padding:10px;\n" + "	text-align:center;\n" + "}\n" + "h2\n" + "{\n" + "	font-size:14px;\n" + "	background-color:#bbb;\n" + "	margin-left:-10px;\n" + "	padding-left:10px;\n" + "	padding:5px;\n" + "	text-align:center;\n" + "}\n" + "h3\n" + "{\n" + "	font-size:14px;\n" + "	background-color:#bbb;\n" + "	margin-left:-10px;\n" + "	padding-left:10px;\n"
			+ "	padding:5px;\n margin-top:5px; margin-bottom:5px;" + "	text-align:center;\n " + "}\n" + "h4\n" + "{\n" + "	font-size:14px;\n" + "	background-color:#bbb; text-decoration:none; font-weight:100;\n" + "	margin-left:-10px;\n" + "	padding-left:10px;\n" + "	padding:5px;\n margin-top:5px; margin-bottom:5px;" + "	text-align:left;\n text-decoration: none !important;" + "}\n" + "A:link {text-decoration: underline; color: black; background-color:#bbb; text-underline-style: dotted;}\n" + "h3 {text-decoration: none !important;}" + "A:visited {text-decoration: none; color: grey;}\n" + "A:active {text-decoration: none; color: dark-red; background-color: #ebb;}\n" + "A:hover {text-decoration: underline; background-color: #cbb;}\n" + "\n"
			+ "pre{background:#888;}code{font-family:'gill sans'; font-size:11px;line-height:13px;} .hll { background-color: #ffffcc }\n" + ".c { color: #5F5A60; font-style: italic } /* Comment */\n" + ".err { border:#B22518; } /* Error */\n" + ".k { color: #CDA869 } /* Keyword */\n" + ".cm { color: #5F5A60; font-style: italic } /* Comment.Multiline */\n" + ".cp { color: #5F5A60 } /* Comment.Preproc */\n" + ".c1 { color: #5F5A60; font-style: italic } /* Comment.Single */\n" + ".cs { color: #5F5A60; font-style: italic } /* Comment.Special */\n" + ".gd { background: #420E09 } /* Generic.Deleted */\n" + ".ge { font-style: italic } /* Generic.Emph */\n" + ".gr { background: #B22518 } /* Generic.Error */\n" + ".gh { color: #000080; font-weight: bold } /* Generic.Heading */\n"
			+ ".gi { background: #253B22 } /* Generic.Inserted */\n" + ".go { } /* Generic.Output */\n" + ".gp { font-weight: bold } /* Generic.Prompt */\n" + ".gs { font-weight: bold } /* Generic.Strong */\n" + ".gu { color: #800080; font-weight: bold } /* Generic.Subheading */\n" + ".gt { } /* Generic.Traceback */\n" + ".kc { } /* Keyword.Constant */\n" + ".kd { color: #e9df8f; } /* Keyword.Declaration */\n" + ".kn { } /* Keyword.Namespace */\n" + ".kp { color: #9B703F } /* Keyword.Pseudo */\n" + ".kr { } /* Keyword.Reserved */\n" + ".kt { } /* Keyword.Type */\n" + ".m { } /* Literal.Number */\n" + ".s { color: #8F9D6A } /* Literal.String */\n" + ".na { color: #F9EE98 } /* Name.Attribute */\n" + ".nb { } /* Name.Builtin */\n"
			+ ".nc { color: #9B859D; font-weight: bold } /* Name.Class */\n" + ".no { color: #9B859D } /* Name.Constant */\n" + ".nd { color: #7587A6 } /* Name.Decorator */\n" + ".ni { color: #CF6A4C; font-weight: bold } /* Name.Entity */\n" + ".nf { } /* Name.Function */\n" + ".nn { color: #9B859D; font-weight: bold } /* Name.Namespace */\n" + ".nt { color: #CDA869; font-weight: bold } /* Name.Tag */\n" + ".nv { color: #7587A6 } /* Name.Variable */\n" + ".ow { color: #aaaaaa; font-weight: bold } /* Operator.Word */\n" + ".w { color: #141414 } /* Text.Whitespace */\n" + ".mf { color: #CF6A4C } /* Literal.Number.Float */\n" + ".mh { color: #CF6A4C } /* Literal.Number.Hex */\n" + ".mi { color: #CF6A4C } /* Literal.Number.Integer */\n"
			+ ".mo { color: #CF6A4C } /* Literal.Number.Oct */\n" + ".sb { color: #8F9D6A } /* Literal.String.Backtick */\n" + ".sc { color: #8F9D6A } /* Literal.String.Char */\n" + ".sd { color: #8F9D6A; font-style: italic; } /* Literal.String.Doc */\n" + ".s2 { color: #8F9D6A } /* Literal.String.Double */\n" + ".se { color: #F9EE98; font-weight: bold; } /* Literal.String.Escape */\n" + ".sh { color: #8F9D6A } /* Literal.String.Heredoc */\n" + ".si { color: #DAEFA3; font-weight: bold; } /* Literal.String.Interpol */\n" + ".sx { color: #8F9D6A } /* Literal.String.Other */\n" + ".sr { color: #E9C062 } /* Literal.String.Regex */\n" + ".s1 { color: #8F9D6A } /* Literal.String.Single */\n" + ".ss { color: #CF6A4C } /* Literal.String.Symbol */\n"
			+ ".bp { color: #00aaaa } /* Name.Builtin.Pseudo */\n" + ".vc { color: #7587A6 } /* Name.Variable.Class */\n" + ".vg { color: #7587A6 } /* Name.Variable.Global */\n" + ".vi { color: #7587A6 } /* Name.Variable.Instance */\n" + ".il { color: #009999 } /* Literal.Number.Integer.Long */\n" + "</style></head><body>";

	static String postamble = "<script type=\"text/javascript\">\n" + "\n" + "      //scripts have been put here as a courtesy to you, the sourcecode viewer.\n" + "\n" + "      //this script uses mootools: http://mootools.net\n" + "\n" + "      var stretchers = $$('div.accordion');\n" + "      var togglers = $$('h4');\n" + "\n" + "      stretchers.setStyles({'height': '0', 'overflow': 'hidden'});\n" + "\n" + "      window.addEvent('load', function(){\n" + "\n" + "      //initialization of togglers effects\n" + "\n" + "      togglers.each(function(toggler, i){\n" + "      toggler.color = toggler.getStyle('background-color');\n" + "      toggler.$tmp.first = toggler.getFirst();\n"
			+ "      toggler.$tmp.fx = new Fx.Style(toggler, 'background-color', {'wait': false, 'transition': Fx.Transitions.Quart.easeOut});\n" + "      });\n" + "\n" + "      //the accordion\n" + "\n" + "      var myAccordion = new Accordion(togglers, stretchers, {\n" + "\n" + "      'opacity': false,\n" + "\n" + "      'start': false,\n" + "\n" + "      'transition': Fx.Transitions.Quad.easeOut,\n" + "\n" + "      onActive: function(toggler){\n" + "      toggler.$tmp.fx.start('#e555555');\n" + "      toggler.$tmp.first.setStyle('color', '#000');\n" + "      },\n" + "\n" + "      onBackground: function(toggler){\n" + "      toggler.$tmp.fx.stop();\n" + "      toggler.setStyle('background-color', toggler.color).$tmp.first.setStyle('color', '#222');\n" + "      }\n"
			+ "      });\n" + "\n" + "      //open the accordion section relative to the url\n" + "\n" + "      var found = 0;\n" + "      $$('h3.toggler a').each(function(link, i){\n" + "      if (window.location.hash.test(link.hash)) found = i;\n" + "      });\n" +

			"      myAccordion.display(found);\n" + "\n" + "\n" + "      });\n" + "\n" + "    </script></body></html>\n" + "";

	@Override
	protected String getPluginNameImpl() {
		return "help";
	}

	NanoHTTPD server;

	private Composite toolbar;

	private Button pin;

	private Button forward;

	private Button back;

	public ContextualHelp help = new ContextualHelp(this);

	ObjectToMarkDown otmd = new ObjectToMarkDown();

	static public final String documentationDirectory = "../documentation/";

	@Override
	public void registeredWith(final iVisualElement root) {
		super.registeredWith(root);

		try {
			server = new NanoHTTPD(10010) {
				public Response serve(String uri, String method, java.util.Properties header, java.util.Properties parms) {

					;// System.out.println(" -- " + uri +
						// " -- with params <" + parms +
						// ">");
					try {
						if (uri.startsWith("/field/attachment/")) {
							try {
								return rewriteOnlineResource(uri + "?format=raw");
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						if (uri.contains("images/")) {
							try {
								return rewriteOnlineResource(uri);
							} catch (IOException e) {
								e.printStackTrace();
							}
						} else if (uri.startsWith("/otmd/")) {

							Reference ref = otmd.forward.get(uri.substring("/otmd/".length()));

							;// System.out.println(" ref :"
								// + ref + " " +
								// (ref == null
								// ? null :
								// ref.get()));

							;// System.out.println(otmd.forward);

							String t = otmd.convert(ref.get());
							return new Response(HTTP_OK, "text/html", textFromMarkdown(t));

						} else if (uri.startsWith("/browsed/")) {

							String s = uri.substring("/browsed/".length());
							String ref = otmd.browsed.get(s);

							;// System.out.println(" looking up <"
								// + s +
								// "> got <" +
								// ref +
								// "> from <" +
								// otmd.browsed
								// + ">");

							return new Response(HTTP_OK, "text/html", ref);

						} else if (uri.startsWith("/field/wiki/")) {
							try {
								return rewriteOnlineWikipage(uri);
							} catch (IOException e) {
								e.printStackTrace();
							}
						} else if (uri.startsWith("/documentation")) {
							try {

								;// System.out.println(" loading docs <"
									// + new
									// File("../"
									// +
									// uri).exists()
									// +
									// " ("
									// + new
									// File("../"
									// +
									// uri).getAbsolutePath()
									// +
									// ")>");

								FileInputStream fis = new FileInputStream(new File("../" + uri));
								return new Response(HTTP_OK, uri.endsWith(".js") ? "application/javascript" : "text/html", fis);
							} catch (IOException e) {
								e.printStackTrace();
							}
						} else if (uri.startsWith("/field/run/")) {
							String name = uri.replace("/field/run/", "");
							InputStream ans = findAndRun(name, parms);
							if (ans == null)
								return new Response(HTTP_NOTFOUND, null, "no element called <" + name + ">");
							return new Response(HTTP_OK, null, ans);
						} else if (uri.startsWith("/field/invoke/")) {

							;// System.out.println(" found it");

							String name = uri.replace("/field/invoke/", "");
							InputStream ans = findAndInvoke(name, parms);
							if (ans == null)
								return new Response(HTTP_NOTFOUND, null, "no element called <" + name + ">");
							return new Response(HTTP_OK, null, ans);
						} else if (uri.startsWith("")) {
							try {
								return rewriteOnlineWikipage(uri);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						// else if (uri.startsWith("/")
						// && uri.split("/").length ==
						// 2) {
						//
						// iVisualElement e =
						// StandardFluidSheet.findVisualElementWithName(root,
						// uri.split("/")[1]);
						//
						// if (e != null) {
						// // loadFromElement(e);
						// String t =
						// textFromElement(e);
						// return new Response(HTTP_OK,
						// "text/html", t);
						// }
						// }
					} catch (Exception e) {
						e.printStackTrace();
					}
					return new Response(HTTP_NOTFOUND, null, "Invalid link or object");
				};
			};
		} catch (IOException e) {
			e.printStackTrace();
		}

		helpBrowser.set(root, root, this);

		ToolBarFolder.helpFolder = new ToolBarFolder(new Rectangle(50, 50, 300, 600), false);

		Composite container = new Composite(ToolBarFolder.helpFolder.getContainer(), SWT.NO_BACKGROUND);
		ToolBarFolder.helpFolder.add("icons/read_more_16x16.png", container);

		toolbar = new Composite(container, SWT.BACKGROUND);
		Color backgroundColor = ToolBarFolder.helpFolder.background;
		toolbar.setBackground(backgroundColor);

		browser = new Browser(container, 0);

		// browser.setBackground(Launcher.display.getSystemColor(SWT.COLOR_RED));
		browser.setBackgroundMode(SWT.INHERIT_FORCE);

		// browser.setBackground(ToolBarFolder.firstLineBackground);
		// browser.setText("<html><body>Welcome to Field</body><html>");
		setText(preamble);
		proc = new PegDownProcessor();

		configureBridge(browser);

		GridLayout gl = new GridLayout(1, false);
		gl.marginHeight = 0;
		gl.marginWidth = 0;

		if (Platform.getOS() == OS.linux) {
			gl.verticalSpacing = 0;
			gl.horizontalSpacing = 0;
		}

		container.setLayout(gl);
		{
			GridData data = new GridData();
			data.heightHint = 28;
			if (Platform.getOS() == OS.linux) {
				data.heightHint = 38;
			}
			data.widthHint = 1000;
			data.horizontalAlignment = SWT.FILL;
			data.grabExcessHorizontalSpace = true;
			toolbar.setLayoutData(data);
		}
		{
			GridData data = new GridData();
			data.grabExcessVerticalSpace = true;
			data.grabExcessHorizontalSpace = true;
			data.verticalAlignment = SWT.FILL;
			data.horizontalAlignment = SWT.FILL;
			browser.setLayoutData(data);
		}

		toolbar.setLayout(new RowLayout(SWT.HORIZONTAL));

		back = new Button(toolbar, SWT.FLAT);
		back.setImage(new Image(Launcher.display, "icons/grey/arrow_left_16x16.png"));

		back.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				browser.back();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		forward = new Button(toolbar, SWT.FLAT);
		forward.setImage(new Image(Launcher.display, "icons/grey/arrow_right_16x16.png"));

		forward.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				browser.forward();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		Button home = new Button(toolbar, SWT.FLAT);
		home.setImage(new Image(Launcher.display, "icons/grey/home_16x16.png"));

		home.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				goHome();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		pin = new Button(toolbar, SWT.TOGGLE | SWT.FLAT);
		pin.setImage(new Image(Launcher.display, "icons/grey/pin_16x16.png"));

		back.setBackground(back.getParent().getBackground());
		forward.setBackground(back.getParent().getBackground());
		home.setBackground(back.getParent().getBackground());
		pin.setBackground(back.getParent().getBackground());

		new MacScrollbarHack(browser);

		goHome();

		browser.addOpenWindowListener(new OpenWindowListener() {
			public void open(WindowEvent event) {
				Shell shell = new Shell(Launcher.getLauncher().display);
				shell.setText("Field Help");
				shell.setLayout(new FillLayout());
				Browser browser = new Browser(shell, SWT.NONE);
				event.browser = browser;
				shell.setVisible(true);
				System.out.println(" opened a browser on <"+event+">");
			}
		});
	}

	public String HOME = "http://localhost:10010/field_2/StandardLibrary.html";

	private void configureBridge(Browser browser) {

		BrowserFunction f = new BrowserFunction(browser, "executeInField") {
			@Override
			public Object function(Object[] arguments) {

				;// System.out.println(" -- woo hoo --" +
					// Arrays.asList(arguments));

				return super.function(arguments);
			}
		};

	}

	// protected Response rewriteOnlineWikipage(String uri) throws
	// IOException {
	//
	// URL u = new URL("http://openendedgroup.com/" + uri);
	// URLConnection connect = u.openConnection();
	// InputStream s = connect.getInputStream();
	// BufferedReader r = new BufferedReader(new InputStreamReader(s));
	// String all = "<html><head><base href=\"http://localhost:10010/" + uri
	// + "\"></head><body>" + preamble;
	// String sub = "";
	// while (r.ready()) {
	// sub += r.readLine() + "\n";
	// }
	//
	// try {
	// Thread.sleep(1000);
	// } catch (InterruptedException e) {
	// e.printStackTrace();
	// }
	//
	// while (r.ready()) {
	// sub += r.readLine() + "\n";
	// }
	//
	// ;//System.out.println(" ------- \n");
	// ;//System.out.println(sub);
	// ;//System.out.println(" ------- \n");
	//
	// String startMarker = "<div class=\"wikipage\">";
	// int start = sub.indexOf(startMarker);
	//
	// String endMarker = "<div id=\"footer\"";
	// int end = sub.indexOf(endMarker);
	//
	// if (start != -1 && end != -1) {
	// sub = sub.substring(start + startMarker.length(), end);
	// }
	//
	// all = all + sub;
	//
	// ;//System.out.println(" subsection <" + start + " -> " + end);
	// ;//System.out.println(" rewrote wiki page to <" + all + ">");
	//
	// return server.new Response(NanoHTTPD.HTTP_OK,
	// "text/html;charset=utf-8", all);
	// }

	protected Response rewriteOnlineWikipage(String uri) throws IOException {

		URL u = new URL("http://openendedgroup.com/" + uri);
		URLConnection connect = u.openConnection();
		InputStream s = connect.getInputStream();
		BufferedReader r = new BufferedReader(new InputStreamReader(s));
		String all = "<html><head><base href=\"http://localhost:10010/" + uri + "\"></head><body>" + preamble;
		String sub = "";
		while (r.ready()) {
			sub += r.readLine() + "\n";
		}

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		while (r.ready()) {
			sub += r.readLine() + "\n";
		}

		;// System.out.println(" ------- \n");
		;// System.out.println(sub);
		;// System.out.println(" ------- \n");

		String startMarker = "<div id=\"middle\" class=\"middle\">";
		int start = sub.indexOf(startMarker);

		String endMarker = "<div style=\"clear:both; margin-bottom:50px\">&nbsp;</div>";
		int end = sub.indexOf(endMarker);

		if (start != -1 && end != -1) {
			sub = sub.substring(start + startMarker.length(), end);
		}

		sub = Pattern.compile("<div style=\"height(.*?)</div>", Pattern.DOTALL).matcher(sub).replaceAll("");

		;// System.out.println(" ------- replaced \n");
		;// System.out.println(sub);
		;// System.out.println(" ------- \n");

		all = all + sub;

		;// System.out.println(" subsection <" + start + " -> " + end);
		;// System.out.println(" rewrote wiki page to <" + all + ">");

		return server.new Response(NanoHTTPD.HTTP_OK, "text/html;charset=utf-8", all);
	}

	protected Response rewriteOnlineResource(String uri) throws IOException {

		URL u = new URL("http://openendedgroup.com/" + uri);
		URLConnection connect = u.openConnection();
		;// System.out.println(" headers for resource :" +
			// connect.getHeaderFields());
		InputStream s = connect.getInputStream();

		return server.new Response(NanoHTTPD.HTTP_OK, "image/png", s);
	}

	protected void goHome() {
		browser.setUrl(HOME);
	}

	public boolean isPinned() {
		return pin.getSelection();
	}

	static {
		PythonPluginEditor.knownPythonProperties.put("Documentation (markdown)", documentation);
	}

	@Override
	public void update() {
	}

	public void documentFromElement(final iVisualElement e) {
		if (documentation.get(e) == null)
			documentation.set(e, e, "");
		PythonPluginEditor.python_customToolbar.addToList(ArrayList.class, e, new Pair<String, iUpdateable>("Load document", new iUpdateable() {

			@Override
			public void update() {
				// loadFromElement(e);

				browser.setUrl("http://localhost:10010/" + iVisualElement.name.get(e));

			}
		}));
	}

	@NextUpdate
	public void loadFromElement(iVisualElement e) {
		try {
			Object xx = browser.evaluate("return pythonSource");
			;// System.out.println(" eval got :" + xx);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		String d = documentation.get(e);
		String x = proc.markdownToHtml(d);

		x = x.replace("<p>[fold]</p>", "<div class=\"accordion\">");
		x = x.replace("<p>[/fold]</p>", "</div>");

		setText(x);
	}

	public String textFromElement(iVisualElement e) {

		String d = documentation.get(e);
		return textFromMarkdown(d);
	}

	static public String textFromMarkdown(String d) {
		String x = proc.markdownToHtml(d);

		x = x.replace("<p>[fold]</p>", "<div class=\"accordion\">");
		x = x.replace("<p>[/fold]</p>", "</div>");

		String tfe = preamble + x + postamble;

		;// System.out.println(" text form element <\n" + tfe + "\n>");

		return tfe;
	}

	protected void setText(String x) {
		if (x == null || x.equals("null"))
			return;
		if (x.trim().equals("<p>null</p>"))
			return;

		;// System.out.println(" SETTING << " + x + " >>");

		MacScrollbarHack.skipAFrame(iVisualElement.enclosingFrame.get(root).getFrame());
		browser.setText(preamble + x + postamble);
	}

	public void goToWiki(String sel) {
		browser.setUrl("http://localhost:10010/field/wiki/" + sel);
	}

	public ContextualHelp getContextualHelp() {
		return this.help;
	}

	public void browseObject(Object o) {
		String md = otmd.convert(o);
		String t = textFromMarkdown(md);
		String q = otmd.makeUniq(t);
		otmd.browsed.put(q, t);
		browser.setUrl("http://localhost:10010/browsed/" + q);
		// setText(t);
	}

	protected java.io.InputStream findAndRun(String replace, Properties parms) {
		iVisualElement found = StandardFluidSheet.findVisualElementWithName(root, replace);
		if (found != null) {

			Iterator<String> ss = ((Set) parms.keySet()).iterator();

			Map<String, Object> was = new HashMap<String, Object>();
			while (ss.hasNext()) {
				String name = ss.next();
				Object var = PythonInterface.getPythonInterface().getVariable(name);
				was.put(name, var);
				PythonInterface.getPythonInterface().setVariable(name, parseObject(parms.get(name)));
			}

			try {
				SplineComputingOverride.executeMain(found);

				PythonPlugin ed = PythonPluginEditor.python_plugin.get(root);

				String x = ((PythonPluginEditor) ed).getEditor().getOutput().toString();

				return new ReaderInputStream(new StringReader(x));
			} finally {
				Iterator<String> w = was.keySet().iterator();
				while (w.hasNext()) {
					String name = w.next();
					Object val = was.get(name);
					if (val != null)
						PythonInterface.getPythonInterface().setVariable(name, val);
				}
			}
		} else {
			return null;
		}
	}

	protected java.io.InputStream findAndInvoke(String replace, Properties parms) {
		Object m = ObjectToMarkDown.invokeMap.get(replace);
		if (m == null)
			m = PythonInterface.getPythonInterface().getVariable(replace);

		if (m == null) {
			System.err.println(" couldn't find <" + replace + "> in <" + ObjectToMarkDown.invokeMap);
		}

		final Object fm = m;

		Callable u = null;
		if (m instanceof PyObject) {
			u = (Callable) ((PyObject) m).__tojava__(Callable.class);
		} else if (m instanceof iUpdateable) {
			u = new Callable() {

				@Override
				public Object call() throws Exception {
					((iUpdateable) fm).update();
					return "(no result)";
				}
			};
		} else if (m instanceof iProvider) {
			u = new Callable() {
				@Override
				public Object call() throws Exception {
					return ((iProvider) fm).get();
				}
			};
		}
		if (u == null)
			return new ReaderInputStream(new StringReader("didn't know how to invoke object " + m));

		Iterator<String> ss = ((Set) parms.keySet()).iterator();

		Map<String, Object> was = new HashMap<String, Object>();
		while (ss.hasNext()) {
			String name = ss.next();
			Object var = PythonInterface.getPythonInterface().getVariable(name);
			was.put(name, var);
			PythonInterface.getPythonInterface().setVariable(name, parseObject(parms.get(name)));
		}
		try {
			return new ReaderInputStream(new StringReader("" + u.call()));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			Iterator<String> w = was.keySet().iterator();
			while (w.hasNext()) {
				String name = w.next();
				Object val = was.get(name);
				if (val != null)
					PythonInterface.getPythonInterface().setVariable(name, val);
			}
		}
		return null;
	};

	private Object parseObject(Object object) {
		try {
			String s = ((String) object);
			try {
				return Long.parseLong(s);
			} catch (NumberFormatException e) {
			}
			try {
				return Double.parseDouble(s);
			} catch (NumberFormatException e) {
			}
			return s;
		} catch (Exception e) {
			return "" + object;
		}
	};
}
