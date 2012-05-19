package field.util;

import java.awt.Component;
import java.awt.HeadlessException;
import java.io.File;
import java.io.IOException;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import field.core.plugins.history.GitVersioningQueries;
import field.core.plugins.history.GitVersioningSystem;
import field.core.plugins.history.HGVersioningSystem;
import field.core.ui.FieldMenus2;
import field.core.util.ExecuteCommand;
import field.launch.SystemProperties;

public class WorkspaceDirectory {

	static public String[] dir = { null };

	static public final String[] hgbinary = new AutoPersist().persist("hgbinary", new String[] { null });
	static public final String[] gitBinary = new AutoPersist().persist("gitbinary", new String[] { null });
	static public boolean useGit = SystemProperties.getIntProperty("useGit", 0) == 1;

	public WorkspaceDirectory() {

		String d = SystemProperties.getProperty("versioning.dir", null);
		if (d != null) {
			System.err.println(" versioning dir already set to <" + d + ">");
			return;
		}

		if (useGit)
			checkGitInstalled();
		else
			checkHGInstalled();

		System.err.println("--- into workspace directory, in search of bug #142:");

		dir = new AutoPersist().persist("workspacedirectory", new String[] { System.getProperty("user.home") + "/Documents/FieldWorkspace" });

		d = dir[0];
		System.err.println("   versioning dir is set to <" + d + "> which exists? <" + new File(d).exists() + ">");
		System.err.println(" user home is <" + System.getProperty("user.home") + ">");

		if (!new File(d).exists()) {
			System.err.println(" since this file doesn't exist, we're goping to ask for one");
			// JOptionPane.showMessageDialog(null,
			// "Field needs to store all of your files in one directory, the 'workspace'.\nThis is so that it can keep track of them properly and automatically back them up.\nThis directory needs to be set now \u2014 just this once.\nWe recommend Documents/FieldWorkspace");

			Shell s = FieldMenus2.fieldMenus.hiddenWindow;

			MessageBox mb = new MessageBox(s, SWT.OK);
			mb.setText("Select a Workspace Directory");
			mb.setMessage("Field needs to store all of your files in one directory, the 'workspace'.\nThis is so that it can keep track of them properly and automatically back them up.\nThis directory needs to be set now \u2014 just this once.\nWe recommend ~/Documents/FieldWorkspace");
			mb.open();

			// let's ask the user where they'd like to put it

			final JFileChooser fc = new JFileChooser(new File(System.getProperty("user.	dir") + "/Documents/FieldWorkspace")) {
				@Override
				protected JDialog createDialog(Component parent) throws HeadlessException {
					JDialog d = super.createDialog(parent);
					d.setAlwaysOnTop(true);
					return d;
				}
			};

			DirectoryDialog dd = new DirectoryDialog(s);
			String fn = dd.open();

			if (fn != null) {

				System.out.println(" got ok <" + fn + ">");

				File f = new File(fn);

				try {
					System.out.println(" got file <" + f + "> <" + f.getAbsolutePath() + "> <" + f.getCanonicalPath() + ">");
				} catch (IOException e1) {
					e1.printStackTrace();
				}

				if (!f.exists()) {
					boolean made = f.mkdir();
					if (!made) {
						mb = new MessageBox(s, SWT.ABORT | SWT.ICON_ERROR);
						mb.setMessage("Field tried and failed to create the directory '" + f.getAbsolutePath() + "'. Check permissions on the enclosing directory?");
						mb.open();
						System.err.println(" failed to make 1 <" + f + ">");

						System.exit(1);
					} else {
						System.out.println(" directory made successfully <" + f.exists() + ">");
					}
				} else {
					if (!new File(f, ".hg").exists()) {
						f = new File(f, "FieldWorkspace");
						boolean made = f.mkdir();
						if (!made) {
							mb = new MessageBox(s, SWT.ABORT | SWT.ICON_ERROR);
							mb.setMessage("Field tried and failed to create the directory '" + f.getAbsolutePath() + "'. Check permissions on the enclosing directory?");
							mb.open();

							System.exit(1);
						} else {
							System.out.println(" directory made successfully 2 <" + f.exists() + ">");
						}
					} else {
						System.err.println(" mercurial repository already there");
					}
				}

				System.err.println(" directory already exists ");

				// check to see if this is a repository
				boolean isRep = new File(f, ".hg").exists();
				if (!isRep) {
					System.err.println(" making it into a repository ");
					makeIntoRepository(f);
				}

				try {
					dir[0] = f.getCanonicalPath();
				} catch (IOException e) {
					e.printStackTrace();
				}

			} else {
				System.exit(1);
			}
		}

		if (dir[0] == null) {
			System.exit(1);
		}

		if (!new File(dir[0]).exists()) {
			Shell s = FieldMenus2.fieldMenus.hiddenWindow;

			MessageBox mb = new MessageBox(s, SWT.ABORT | SWT.ICON_ERROR);
			mb.setMessage("Field tried and failed to create the workspace directory. Check permissions on the enclosing directory?");
			mb.open();
			System.exit(1);
		}

		System.err.println(" and we're out of workspacedirectory(), still looking for #142: dir is <" + dir[0] + "> and new <" + new File(dir[0]).exists() + ">");
		SystemProperties.setProperty("versioning.dir", dir[0]);
	}

	private void makeIntoRepository(File f) {
		if (useGit) {
			ExecuteCommand c = new ExecuteCommand(f.getAbsolutePath(), new String[] { GitVersioningSystem.gitCommand, "init" }, true);
			int w = c.waitFor();
			System.out.println(" return code <" + w + ">");
			System.out.println(" output <" + c.getOutput() + ">");
		} else {
			ExecuteCommand c = new ExecuteCommand(f.getAbsolutePath(), new String[] { HGVersioningSystem.hgCommand, "init" }, true);
			int w = c.waitFor();
			System.out.println(" return code <" + w + ">");
			System.out.println(" output <" + c.getOutput() + ">");
		}
	}

	protected void checkHGInstalled() {

		if (hgbinary != null && hgbinary[0] != null) {
			if (new File(hgbinary[0]).exists())
				return;
		}

		if (new File("/usr/local/bin/hg").exists())
			return;
		if (new File("/usr/bin/hg").exists())
			return;
		if (new File("/opt/local/bin/hg").exists())
			return;
		if (new File("/bin/hg").exists())
			return;
		if (new File("/Library/Frameworks/Python.framework/Versions/Current/bin/hg").exists())
			return;
		if (new File("/System/Library/Frameworks/Python.framework/Versions/Current/bin/hg").exists())
			return;
		if (new File("C:/Program Files/Mercurial/hg.exe").exists())
			return;
		if (new File(System.getProperty("user.home") + "/bin/hg").exists())
			return;

		JOptionPane.showMessageDialog(null, "Field can't find a Mercurial installation on this computer, please install one for development purposes");
		System.exit(0);

	}

	protected void checkGitInstalled() {

		if (gitBinary != null && gitBinary[0] != null) {
			if (new File(gitBinary[0]).exists())
				return;
		}

		if (!new File(GitVersioningSystem.gitCommand).exists()) {
			JOptionPane.showMessageDialog(null, "Field can't find a Git installation on this computer, please install one for development purposes");
			System.exit(0);
		}
	}

}
