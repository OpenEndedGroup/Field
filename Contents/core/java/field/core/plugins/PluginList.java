package field.core.plugins;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.TreeItem;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.NextUpdate;
import field.core.Constants;
import field.core.Platform;
import field.core.dispatch.iVisualElement;
import field.core.plugins.help.ContextualHelp;
import field.core.plugins.help.HelpBrowser;
import field.core.plugins.selection.SelectionSetUI;
import field.core.plugins.selection.ToolBarFolder;
import field.core.ui.BetterComboBox;
import field.core.ui.GraphNodeToTreeFancy;
import field.core.ui.UbiquitousLinks;
import field.core.util.ExecuteCommand;
import field.launch.Launcher;
import field.launch.SystemProperties;
import field.launch.iUpdateable;
import field.util.HashMapOfLists;

@Woven
public class PluginList {

	private final iVisualElement root;
	private SelectionSetUI selectionUI;

	public class PluginOrExtensionNode extends field.math.graph.NodeImpl<PluginOrExtensionNode> {
		protected final String rootname;
		protected final boolean enabled;
		protected File file;

		public PluginOrExtensionNode(String rootname, boolean enabled) {
			this.rootname = rootname;
			this.enabled = enabled;
		}

		public PluginOrExtensionNode associateFile(File f) {
			this.file = f;
			return this;
		}

		protected String getTypeDescription() {
			return "";
		}

		public boolean isEnableable() {
			return false;
		}

		public boolean canDelete() {
			return false;
		}

		public void delete() {
			if (this.file != null)
				this.file.delete();
		}

		public void enable() {
			invalidateAndRebuild();
		}

		public boolean isEnabled() {
			return enabled;
		}

		@Override
		public String toString() {
			String rootnamesplit = dressName();

			// System.out.println(" enabled :" + enabled);
			return rootnamesplit + " " + smaller(getTypeDescription());
		}

		protected String dressName() {
			String[] splat = rootname.split("\\.");
			return splat.length == 1 ? rootname : ((enabled ? "<b>" : "") + splat[0] + (enabled ? "</b>" : "") + rootname.substring(splat[0].length()));
		}

	}

	static protected String smaller(String text) {
		return "<font size=-3 color='#" + Constants.defaultTreeColorDim + "'>" + text + "</font>";
	}

	public void invalidateAndRebuild() {
		selectionUI.setLabel("<html><font color='#aa0000' face='" + Constants.defaultFont + "'>the plugins list has been modified, <b>restart</b> <i>Field</i> for these changes to take effect</font>");
		reconstructRootModel();
	}

	public class JARNode extends PluginOrExtensionNode {

		public JARNode(String rootname, boolean enabled) {
			super(rootname, enabled);
		}

		@Override
		protected String getTypeDescription() {
			return "(jar file)";
		}
	}

	public class MFNode extends PluginOrExtensionNode {

		public MFNode(String rootname, boolean enabled) {
			super(rootname, enabled);
			associateFile(new File(extensionsDir + "/" + rootname));
		}

		public boolean canDelete() {
			return file != null && file.exists();
		}

		protected String getTypeDescription() {
			return "(manifest file)";
		}
	}

	public class ClasspathNode extends PluginOrExtensionNode {

		public ClasspathNode(String rootname, boolean enabled) {
			super(rootname, enabled);
		}

		protected String getTypeDescription() {
			return "(classpath file)";
		}

		@Override
		protected String dressName() {
			// try {
			// String r = UbiquitousLinks.links.link(smaller(new
			// File(rootname).getParentFile().getCanonicalPath()) +
			// "/" + new File(rootname).getName(),
			// UbiquitousLinks.links.code_revealInFinder(new
			// File(rootname).getCanonicalPath()));
			return rootname;
			// } catch (IOException e) {
			// e.printStackTrace();
			// }
		}

	}

	public class InformationNode extends PluginOrExtensionNode {

		public InformationNode(String rootname, boolean enabled) {
			super(rootname, enabled);
		}

	}

	public class PluginNode extends PluginOrExtensionNode {

		public PluginNode(String rootname, boolean enabled) {
			super(rootname, enabled);
		}

		protected String getTypeDescription() {
			return "(plugin)";
		}

		@Override
		protected String dressName() {
			return rootname;
		}
	}

	public class ExtensionNode extends PluginOrExtensionNode {

		public ExtensionNode(String rootname, boolean enabled) {
			super(rootname, enabled);
		}

		protected String getTypeDescription() {
			return "(.py file)";
		}

	}

	public class DirNode extends PluginOrExtensionNode {

		public DirNode(String rootname, boolean enabled) {
			super(rootname, enabled);
		}

		protected String getTypeDescription() {
			return "(sub-directory)";
		}

	}

	protected void handleDrop(String string) throws IOException {
		File f = new File(string);

		System.out.println(" file is <" + f + " " + f.exists() + " " + f.isDirectory() + " " + f.getName());

		if (!f.exists())
			return;

		if (f.isDirectory()) {
			String n = f.getName();
			n = f.getParentFile().getName()+"_"+n;
			if (n.indexOf('.') != -1) {
				n = n.replace('.', '_');
			}

			
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(new File(extensionsDir + "/" + n + "_extension.mf")));
				writer.write("Field-RedirectionPath: " + f.getCanonicalPath() + (f.isDirectory() ? "/**" : "") + "\n");
				writer.close();
			} catch (IOException ee) {
				BufferedWriter writer = new BufferedWriter(new FileWriter(new File(extensionsDir2 + "/" + n + "_extension.mf")));
				writer.write("Field-RedirectionPath: " + f.getCanonicalPath() + (f.isDirectory() ? "/**" : "") + "\n");
				writer.close();

			}
			invalidateAndRebuild();
		} else if (f.getName().endsWith(".jar")) {
			String n = f.getName();
			n = f.getParentFile().getName()+"_"+n;
			if (n.indexOf('.') != -1) {
				n = n.replace('.', '_');
			}

			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(new File(extensionsDir + "/" + n + "_extension.mf")));
				writer.write("Field-RedirectionPath: " + f.getCanonicalPath() + (f.isDirectory() ? "/" : "") + "\n");
				writer.close();
			} catch (IOException ee) {
				BufferedWriter writer = new BufferedWriter(new FileWriter(new File(extensionsDir2 + "/" + n + "_extension.mf")));
				writer.write("Field-RedirectionPath: " + f.getCanonicalPath() + (f.isDirectory() ? "/" : "") + "\n");
				writer.close();
			}
			invalidateAndRebuild();
		}

	}

	public PluginList(iVisualElement root, ToolBarFolder parent) {
		this.root = root;

		selectionUI = new SelectionSetUI("icons/cog_32x32.png", parent) {

			@Override
			protected LinkedHashMap<String, iUpdateable> getMenuItems() {
				LinkedHashMap<String, iUpdateable> u = new LinkedHashMap<String, iUpdateable>();
				u.put("Extending Field", null);
				u.put(" \u271A  Add jar/directory to classpath", new iUpdateable() {
					public void update() {

						File f = getFile("Add Jar / Directory", true, new String[] { ".jar", ".jar_" });
						if (f != null) {
							String n = f.getName();
							n = f.getParentFile().getName()+"_"+n;
							if (n.indexOf('.') != -1) {
								n = n.replace('.', '_');
							}
							try {
								BufferedWriter writer = new BufferedWriter(new FileWriter(new File(extensionsDir2 + "/" + n + "_extension.mf")));
								writer.write("Field-RedirectionPath: " + f.getCanonicalPath() + (f.isDirectory() ? "/" : "") + "\n");
								writer.close();

								invalidateAndRebuild();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}

					}
				});
				u.put(" \u271A  Add directory full of jars to classpath", new iUpdateable() {
					public void update() {

						File f = getFile("Add Jar / Directory", true, new String[] { ".jar", ".jar_" });
						if (f != null) {
							String n = f.getName();
							n = f.getParentFile().getName()+"_"+n;
							if (n.indexOf('.') != -1) {
								n = n.replace('.', '_');
							}
							try {
								BufferedWriter writer = new BufferedWriter(new FileWriter(new File(extensionsDir2 + "/" + n + "_extension.mf")));
								writer.write("Field-RedirectionPath: " + f.getCanonicalPath() + (f.isDirectory() ? "/**" : "") + "\n");
								writer.close();

								invalidateAndRebuild();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}

					}
				});
				u.put(" \u21e5 Copy jar into application extensions directory", new iUpdateable() {
					public void update() {

						File f = getFile("Install Jar", false, new String[] { ".jar", ".jar_" });
						if (f != null) {
							try {
								new ExecuteCommand(".", new String[] { "/bin/cp", f.getCanonicalPath(), extensionsDir + "/" }, true).waitFor();
								invalidateAndRebuild();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}

					}
				});
				u.put(" \u21e5 Copy jar into user extensions directory", new iUpdateable() {
					public void update() {

						File f = getFile("Install Jar", false, new String[] { ".jar", ".jar_" });
						if (f != null) {
							try {
								new ExecuteCommand(".", new String[] { "/bin/cp", f.getCanonicalPath(), extensionsDir2 + "/" }, true).waitFor();
								invalidateAndRebuild();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}

					}
				});
				u.put(" \u271A  Add directory to Python library", new iUpdateable() {
					public void update() {

						File f = getFile("Add Python directory", true, new String[] {});
						if (f != null) {
							String n = f.getName();
							n = f.getParentFile().getName()+"_"+n;
							if (n.indexOf('.') != -1) {
								n = n.substring(0, n.indexOf("."));
							}
							try {
								BufferedWriter writer = new BufferedWriter(new FileWriter(new File(extensionsDir2 + "/" + n + "_extension.mf")));
								writer.write("Field-Property-Append-python-path: " + f.getCanonicalPath() + "\n");
								writer.close();
							} catch (IOException e) {
								e.printStackTrace();
							}

							invalidateAndRebuild();
						}

					}
				});
				u.put(" \u271A  Add jar/directory to source search path", new iUpdateable() {
					public void update() {
						File f = getFile("Add Source Jar / Directory", true, new String[] { ".jar", ".jar_" });
						if (f != null) {
							String n = f.getName();
							n = f.getParentFile().getName()+"_"+n;
							if (n.indexOf('.') != -1) {
								n = n.substring(0, n.indexOf("."));
							}
							try {
								BufferedWriter writer = new BufferedWriter(new FileWriter(new File(extensionsDir2 + "/" + n + "_extension.mf")));
								writer.write("Field-Property-Append-java-source-paths: " + f.getCanonicalPath() + "\n");
								writer.close();
							} catch (IOException e) {
								e.printStackTrace();
							}

							invalidateAndRebuild();
						}

					}

				});

				// u.put("Plugin & Extensions", null);
				// u.put(" \u275d Dump plugin list to clipboard",
				// new iUpdateable() {
				//
				// public void update() {
				//
				// //TODO swt clipboard
				//
				// // Clipboard c =
				// Toolkit.getDefaultToolkit().getSystemClipboard();
				// // String t =
				// dumpNode(((PluginOrExtensionNode)
				// getRevTree().getModel().getRoot()));
				// // t = "<html><body>" + t + "</body><html>";
				// // c.setContents(new StringSelection(t),
				// null);
				// }
				// });

				return u;
			}

			@Override
			protected void popUpMenu(Event ev, LinkedHashMap<String, iUpdateable> m) {

				final TreeItem[] path = tree.getSelection();

				if (path == null || path.length == 0)
					return;

				boolean found = false;
				for (TreeItem pp : path) {
					final Object o = pp.getData();

					if (((PluginOrExtensionNode) o).isEnableable()) {
						found = true;
					}
				}

				LinkedHashMap<String, iUpdateable> was = new LinkedHashMap<String, iUpdateable>(m);
				m.clear();

				if (found) {
					m.put("Modify", null);
					m.put("\u26aa " + (((PluginOrExtensionNode) path[0].getData()).isEnabled() ? "Disable" : "Enable"), new iUpdateable() {

						public void update() {
							for (TreeItem pp : path) {
								final Object o = pp.getData();
								((PluginOrExtensionNode) o).enable();
							}
						}
					});
					m.put("\u26aa Reveal in Finder", new iUpdateable() {
						public void update() {
							for (TreeItem pp : path) {
								final Object o = pp.getData();
								try {
									UbiquitousLinks.showPathInFinder(((PluginOrExtensionNode) o).file.getCanonicalPath());
								} catch (IOException e) {
								}
							}
						}
					});
					// m.put("Delete file", new
					// iUpdateable() {
					// public void update() {
					// for (TreePath pp : path) {
					// final Object o =
					// pp.getLastPathComponent();
					// if (((PluginOrExtensionNode)
					// o).canDelete())
					// ((PluginOrExtensionNode) o).delete();
					// }
					// }
					// });
					// BetterPopup menu = new
					// SmallMenu().createMenu(m,
					// tree.getShell(), null);
					// menu.show(Launcher.display.map(tree,
					// tree.getShell(), new Point(ev.x,
					// ev.y)));

				}

				m.putAll(was);
			}

			protected void handleDrop(String string) {
				try {
					PluginList.this.handleDrop(string);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			};
		};
		selectionUI.setWaterText("Plugins & Extensions");

		reconstructRootModel();
//
//		BetterComboBox combo = new BetterComboBox(selectionUI.getToolbarPanel(), new String[] { "<html>Java <b>1.7</b> \u2014 faster, less compatible", "<html>Java <b>1.6</b> \u2014 slower, more compatible" }) {
//			@Override
//			public void updateSelection(int index, String text) {
//				if (index == 0)
//					toJDK("1.6");
//				else if (index == 1)
//					toJDK("1.5");
//			}
//		};
//
//		if (!is16())
//			combo.selectNext();

		Link label = new Link(selectionUI.getToolbarPanel(), SWT.MULTI | SWT.NO_BACKGROUND | SWT.CENTER);
		label.setText("Field plugins on " + System.getProperty("java.version") + " / " + System.getProperty("os.arch"));
		label.setFont(new Font(Launcher.display, label.getFont().getFontData()[0].getName(), GraphNodeToTreeFancy.baseFontHeight(label) + 2, SWT.NORMAL));
		label.setBackground(ToolBarFolder.firstLineBackground);

		selectionUI.getToolbarPanel().setLayout(new GridLayout(1, true));
		GridData gd = new GridData(SWT.CENTER, SWT.CENTER, true, true);
		gd.verticalIndent = 1;
		label.setLayoutData(gd);

		// selectionUI.getToolbarPanel().setLayout(new GridBagLayout());
		// GridBagConstraints focusLabelConstraints = new
		// GridBagConstraints();
		// focusLabelConstraints.gridx = 0;
		// focusLabelConstraints.gridy = 0;
		// focusLabelConstraints.anchor = GridBagConstraints.CENTER;
		// focusLabelConstraints.gridwidth = 3;
		// focusLabelConstraints.fill = GridBagConstraints.HORIZONTAL;
		// focusLabelConstraints.insets = new Insets(3, 0, 0, 0);
		// selectionUI.getToolbarPanel().add(label,
		// focusLabelConstraints);
		//
		// UbiquitousLinks.links.install(selectionUI.getFocusLabel());
		// try {
		//
		// selectionUI.setLabel("<html><font face='" +
		// Constants.defaultFont +
		// "'>Plugins have been registered from the '" +
		// UbiquitousLinks.links.link("extensions",
		// UbiquitousLinks.links.code_revealInFinder(extensionsDir.getCanonicalPath()),
		// UbiquitousLinks.links.code_revealInFinder(extensionsDir.getCanonicalPath()))
		// + "' directory");
		//
		// } catch (IOException e) {
		// e.printStackTrace();
		// }

		installHelpBrowser(root);
	}

	@NextUpdate(delay = 3)
	private void installHelpBrowser(final iVisualElement root) {
		HelpBrowser h = HelpBrowser.helpBrowser.get(root);
		ContextualHelp ch = h.getContextualHelp();
		ch.addContextualHelpForWidget("plugins", selectionUI.getTree(), ch.providerForStaticMarkdownResource("contextual/plugins.md"), 50);
	}

	protected void toJDK(String string) {
		String ll = loadInfoPList();

		try {
			File f = new File("../../Info.plist");
			ll = ll.replaceFirst("(<key>JVMVersion</key>\\s*?<string>)(\\d\\.\\d)(</string>)", "$1" + string + "$3");

			System.out.println(" new plist <" + ll + ">");

			BufferedWriter writer = new BufferedWriter(new FileWriter(f));
			writer.append(ll);
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		invalidateAndRebuild();
	}

	protected boolean is16() {
		String plist = loadInfoPList();
		Matcher m = Pattern.compile("(<key>JVMVersion</key>\\s*?<string>)(\\d\\.\\d)(</string>)").matcher(plist);
		boolean found = m.find();
		if (found) {
			return m.group(2).startsWith("1.6");
		}
		return false;
	}

	private String loadInfoPList() {
		String ll = "";
		try {
			File f = new File("../../Info.plist");
			BufferedReader reader = new BufferedReader(new FileReader(f));
			while (reader.ready()) {
				ll += reader.readLine() + "\n";
			}
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ll;
	}

	String lastdir = SystemProperties.getDirProperty("versioning.dir");

	private File getFile(String title, final boolean b, final String[] strings) {

		if (b) {
			DirectoryDialog d = new DirectoryDialog(iVisualElement.enclosingFrame.get(root).getFrame(), SWT.SHEET);
			String name = d.open();

			if (name != null)
				return new File(name);
			return null;

		} else {

			FileDialog d = new FileDialog(iVisualElement.enclosingFrame.get(root).getFrame(), SWT.SHEET);
			d.setOverwrite(false);
			String name = d.open();

			if (name != null)
				return new File(name);
			return null;
		}
		// TODO SWT filechooser
		// final JFileChooser fc = new JFileChooser(new File(lastdir)) {
		// @Override
		// protected JDialog createDialog(Component parent) throws
		// HeadlessException {
		// JDialog d = super.createDialog(parent);
		// d.setAlwaysOnTop(true);
		// return d;
		// }
		// };
		// Quaqua15TigerLookAndFeelField.deRound(fc);
		//
		// fc.setDialogType(JFileChooser.OPEN_DIALOG);
		// fc.setAcceptAllFileFilterUsed(false);
		// fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		//
		// Quaqua15TigerLookAndFeelField.deRound(fc);
		//
		// fc.setFileFilter(new FileFilter() {
		// @Override
		// public boolean accept(File f) {
		// if (b && f.isDirectory())
		// return tr7ue;
		// for (String s : strings) {
		// if (f.getName().endsWith(s))
		// return true;
		// }
		// return false;
		// }
		//
		// @Override
		// public String getDescription() {
		// return "";
		// }
		// });
		//
		// int r = fc.showDialog(null, title);
		// if (r == JFileChooser.APPROVE_OPTION) {
		// File f = fc.getSelectedFile();
		// lastdir = f.isDirectory() ? f.getPath() : f.getParent();
		//
		// return f;
		// } else if (r == JFileChooser.CANCEL_OPTION) {
		// }
	}

	static public HashMapOfLists<String, String> pluginNotification = new HashMapOfLists<String, String>() {
		@Override
		protected Collection<String> newList() {
			return new LinkedHashSet<String>();
		}
	};

	static public void addStringToPluginEntry(Class c, String html) {
		pluginNotification.addToList(Platform.getCanonicalName(c), html);
	}

	File extensionsDir = new File(SystemProperties.getProperty("extensions.dir", "../../extensions/"));
	File extensionsDir2 = new File(System.getProperty("user.home") + "/Library/Application Support/Field/extensions");

	private void reconstructRootModel() {
		PluginOrExtensionNode root = new PluginOrExtensionNode("root", true);
		reconstructRootModel(extensionsDir, root, true);
		reconstructRootModel(extensionsDir2, root, true);

		// selectionUI.getRevTree().setModel(new
		// JTreeModelForGraphNodes<PluginOrExtensionNode>(root));

		new GraphNodeToTreeFancy(selectionUI.getTree()).reset(root);
	}

	private void reconstructRootModel(File extensionsDir, PluginOrExtensionNode root, boolean on) {

		File[] list = extensionsDir.listFiles(new FilenameFilter() {

			public boolean accept(File dir, String name) {
				return !name.startsWith(".");
			}
		});

		if (list == null)
			return;

		for (final File f : list) {
			String name = f.getName();

			if (f.isDirectory()) {
				DirNode d = new DirNode(name, f.getName().endsWith("_")) {
					@Override
					public boolean isEnableable() {
						return true;
					}

					@Override
					public void enable() {
						flipFile(f);
						super.enable();
					}
				};

				root.addChild(d);
				reconstructRootModel(f, d, true);
			}

			if (name.indexOf(".") == -1)
				continue;
			String[] parts = name.split("\\.");
			if (parts.length != 2)
				continue;

			if (parts[1].equals("jar") || parts[1].equals("jar_")) {
				JARNode j = new JARNode(f.getName(), parts[1].equals("jar")) {
					@Override
					public boolean isEnableable() {
						return true;
					}

					@Override
					public void enable() {
						flipFile(f);
						super.enable();
					}
				};
				root.addChild(j);

				try {
					JarFile m = new JarFile(f);
					Manifest manifest = m.getManifest();
					if (manifest != null) {
						MFNode n = new MFNode(f.getName() + ":manifest", parts[1].equals("jar")) {
							@Override
							public boolean isEnableable() {
								return true;
							}

							@Override
							public void enable() {
								flipFile(f);
								super.enable();
							}
						};
						j.addChild(n);
						parseManifest(manifest, n);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

			} else if (parts[1].equals("mf") || parts[1].equals("mf_")) {
				MFNode n = new MFNode(f.getName(), parts[1].equals("mf")) {
					@Override
					public boolean isEnableable() {
						return true;
					}

					@Override
					public void enable() {
						flipFile(f);
						super.enable();
					}
				};
				Manifest m;
				try {
					FileInputStream s = new FileInputStream(f);
					m = new Manifest(new BufferedInputStream(s));
					parseManifest(m, n);
					s.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				root.addChild(n);
			} else if (parts[1].equals("py") || parts[1].equals("py_")) {
				root.addChild(new ExtensionNode(f.getName(), parts[1].equals("py")) {
					@Override
					public boolean isEnableable() {
						return true;
					}

					@Override
					public void enable() {
						flipFile(f);
						super.enable();
					}
				});
			}

		}

	}

	protected void flipFile(File f) {

		System.out.println(" flipping file :" + f);

		String ap = f.getAbsolutePath();
		if (f.getName().endsWith("_")) {
			if (!f.renameTo(new File(ap.substring(0, ap.length() - 1)))) {

				System.err.println(" problem flipping file, using local extensions dir instead");
				new ExecuteCommand("/bin/cp", new String[] { ap, System.getProperty("user.home") + "/Library/Application Support/Field/extensions/" + ap.substring(0, ap.length() - 1) }, true).waitFor();
			}
		} else {
			System.out.println(" rename to " + new File(ap + "_"));
			f.renameTo(new File(ap + "_"));
		}
	}

	protected void parseManifest(Manifest m, MFNode n) {
		String aa = (String) m.getMainAttributes().get(new Attributes.Name("Field-RedirectionPath"));
		if (aa != null) {
			if (aa.endsWith("**")) {
				File dir = new File(aa.replace("**", ""));
				if (dir.exists()) {
					String[] ll = dir.list(new FilenameFilter() {
						public boolean accept(File dir, String name) {
							return name.endsWith(".jar");
						}
					});
					if (ll != null)
						for (String a : ll) {
							ClasspathNode c = new ClasspathNode(new File(dir.getAbsolutePath() + "/" + a).getAbsolutePath(), n.enabled);
							n.addChild(c);
							if (!new File(dir.getAbsolutePath() + "/" + a).exists()) {
								System.out.println(" no file <" + a + ">");
								c.addChild(new InformationNode("<b>warning</b>: file does not exist", n.enabled));
							}
						}
				} else {
					// add error / information node;
					n.addChild(new InformationNode("<b>warning</b>: directory does not exist", n.enabled));
				}
			} else
				for (String a : aa.split(":")) {
					File f = new File(a);
					if (!f.isAbsolute())
						f = new File(extensionsDir + "/" + a);

					ClasspathNode newChild = new ClasspathNode(f.getAbsolutePath(), n.enabled);
					n.addChild(newChild);
					if (!f.exists()) {
						System.out.println(" no file    <" + a + ">");
						newChild.addChild(new InformationNode("<b>warning</b>: file does not exist", n.enabled));
					}
				}
		}

		String b = (String) m.getMainAttributes().get(new Attributes.Name("Field-PluginClass"));
		if (b != null) {
			PluginNode newChild = new PluginNode(b, n.enabled);
			n.addChild(newChild);
			Collection<String> c = pluginNotification.get(b);
			if (c != null)
				for (String s : c)
					newChild.addChild(new InformationNode(s, n.enabled));

		}
	}

	protected String dumpNode(PluginOrExtensionNode pluginOrExtensionNode) {
		String s = pluginOrExtensionNode.toString();
		for (PluginOrExtensionNode e : pluginOrExtensionNode.getChildren()) {
			s += "\n" + dumpNode(e);
		}

		s = s.replace("<HTML>", "");
		s = s.replace("\n", "<p>");
		return s;
	}

}
