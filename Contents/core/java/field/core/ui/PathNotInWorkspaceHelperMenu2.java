package field.core.ui;

import java.io.File;
import java.text.DateFormat;
import java.util.Calendar;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import field.bytecode.protect.annotations.GenerateMethods;
import field.bytecode.protect.annotations.Mirror;
import field.core.plugins.history.HGVersioningSystem;
import field.core.plugins.history.VersioningSystem;
import field.core.util.ExecuteCommand;
import field.math.abstraction.iAcceptor;

/*
 * a helper for opening paths that aren't in the workspace.
 */
@GenerateMethods
public class PathNotInWorkspaceHelperMenu2 {

	private final FieldMenus2 manager;

	public PathNotInWorkspaceHelperMenu2(FieldMenus2 manager) {
		this.manager = manager;
	}

	String filename;

	public void open(Shell fc, final String filename) {
		this.filename = filename;

		;//System.out.println(" filename is <" + filename + "> and versioning dir is <" + FieldMenus2.getCanonicalVersioningDir() + "> therefore <" + filename.startsWith(FieldMenus2.getCanonicalVersioningDir()) + ">");

		if (!filename.startsWith(FieldMenus2.getCanonicalVersioningDir())) {
			File f = new File(filename);
			if (!f.exists()) {
				MessageBox mb = new MessageBox(fc, SWT.ICON_ERROR);
				mb.setMessage("Can't open file (doesn't exist ?)");
				mb.open();

				return;
			}

			MessageBox mb = new MessageBox(fc, SWT.ICON_QUESTION | SWT.YES | SWT.NO);
			mb.setMessage("You have selected a sheet that isn't in the repository\n(at '" + FieldMenus2.getCanonicalVersioningDir() + "'\nCopy sheet into repository?");
			int o = mb.open();

			if (o == SWT.NO) {
				return;
			} else if (o == SWT.YES) {
				File d = uniqName(fc, filename, PathNotInWorkspaceHelperMenu2_m.move_s.acceptor(this));
				if (d != null)
					manager.open(d.getAbsolutePath().substring(FieldMenus2.getCanonicalVersioningDir().length()));
			}
		} else {
			File f = new File(filename);
			if (f.exists()) {
				manager.open(f.getAbsolutePath().substring(FieldMenus2.getCanonicalVersioningDir().length()));
			} else {
				MessageBox mb = new MessageBox(fc, SWT.ICON_ERROR);
				mb.setMessage("Can't open file (doesn't exist ?)");
				mb.open();
			}
		}

	}

	@Mirror
	protected void move(File to) {
		new File(filename).renameTo(to);
	}

	@Mirror
	protected void copy(File to) {
		new ExecuteCommand(".", new String[] { "/bin/cp", "-r", filename, to.getAbsolutePath() }, true).waitFor();
	}

	private File uniqName(Shell fc, String filename, iAcceptor<File> acceptor) {

		String name = new File(filename).getName();
		if (!name.endsWith(".field"))
			name = name + ".field";

		File destination = new File(FieldMenus2.getCanonicalVersioningDir() + name);
		File originalDestination = destination;
		if (destination.exists()) {
			int n = 1;
			while (destination.exists()) {
				destination = new File(FieldMenus2.getCanonicalVersioningDir() + "/" + name.substring(0, name.length() - ".field".length()) + n + ".field");
				n++;
			}

			acceptor.set(destination);

			return destination;

		}

		acceptor.set(destination);

		HGVersioningSystem vs = (HGVersioningSystem) VersioningSystem.newDefault();
		vs.scmAddFile(destination);
		vs.scmCommitDirectory(destination);

		return destination;
	}

}
