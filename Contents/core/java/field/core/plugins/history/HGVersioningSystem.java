package field.core.plugins.history;

import java.io.File;

import field.core.util.ExecuteCommand;
import field.util.WorkspaceDirectory;

public class HGVersioningSystem extends VersioningSystem {

	static public String hgCommand = "/usr/local/bin/hg";

	static {
		computeHGCommand();
	}

	public static void computeHGCommand() {
		if (WorkspaceDirectory.hgbinary != null && WorkspaceDirectory.hgbinary[0] != null) {
			if (new File(WorkspaceDirectory.hgbinary[0]).exists()) {
				hgCommand = WorkspaceDirectory.hgbinary[0];
			}
		}

		if (!new File(hgCommand).exists())
			hgCommand = "/usr/bin/hg";
		if (!new File(hgCommand).exists())
			hgCommand = "/opt/local/bin/hg";
		if (!new File(hgCommand).exists())
			hgCommand = "/usr/local/bin/hg";
		if (!new File(hgCommand).exists())
			hgCommand = "/bin/hg";
		if (!new File(hgCommand).exists())
			hgCommand = "/Library/Frameworks/Python.framework/Versions/Current/bin/hg";
		if (!new File(hgCommand).exists())
			hgCommand = "/System/Library/Frameworks/Python.framework/Versions/Current/bin/hg";
		if (!new File(hgCommand).exists())
			hgCommand = System.getProperty("user.home") + "/bin/hg";
		if (!new File(hgCommand).exists())
			hgCommand = "C:/Program Files/Mercurial/hg.exe";
		if (!new File(hgCommand).exists())
			System.err.println("ERROR: cant find a mercurial installation anywhere");
	}

	public HGVersioningSystem(String fullPathToRepositoryDirectory, String sheetsub, String xmlFilename) {
		super(fullPathToRepositoryDirectory, sheetsub, xmlFilename);
	}

	public int getLastVersion() {
		String e = executeCommand(fullPathToSheetDirectory, new String[] { hgCommand, "head", "--template", "{rev}," });

		String[] ee = e.split(",");
		if (ee.length == 0)
			return 0;

		try {
			int v = Integer.parseInt(ee[0]);
			return v;
		} catch (NumberFormatException eee) {
			eee.printStackTrace();
		}
		return 0;
	}

	public void rollbackDirectory(String string, long revision) {
		executeCommand(fullPathToSheetDirectory, new String[] { hgCommand, "revert", "-r", "" + revision, new File(string).getAbsolutePath() });
	}

	@Override
	public void scmAddFile(File path) {
		executeCommand(fullPathToSheetDirectory, new String[] { hgCommand, "add", path.getAbsolutePath() });
	}

	@Override
	public void scmCommitDirectory(File path) {
		scmAddFile(path);
		executeCommand(fullPathToSheetDirectory, new String[] { hgCommand, "commit", "-m", "\"" + currentLogMessage + "\"", path.getAbsolutePath() });
	}

	@Override
	public void scmCommitFile(File path) {
		;//System.out.println(" commit file, log message is <" + currentLogMessage + ">");
		executeCommand(fullPathToSheetDirectory, new String[] { hgCommand, "commit", "-m", "\"" + currentLogMessage + "\"", path.getAbsolutePath() });
	}

	@Override
	public void scmCopyDirectory(File from, File to) {
		if (to.exists())
			executeCommand(fullPathToSheetDirectory, new String[] { hgCommand, "copy", "--after", from.getAbsolutePath(), to.getAbsolutePath() });
		else
			executeCommand(fullPathToSheetDirectory, new String[] { hgCommand, "copy", from.getAbsolutePath(), to.getAbsolutePath() });
	}

	@Override
	public void scmCopyFile(File from, File to) {
		if (to.exists())
			executeCommand(fullPathToSheetDirectory, new String[] { hgCommand, "copy", "--after", from.getAbsolutePath(), to.getAbsolutePath() });
		else
			executeCommand(fullPathToSheetDirectory, new String[] { hgCommand, "copy", from.getAbsolutePath(), to.getAbsolutePath() });
	}

	@Override
	public void scmDeleteDirectory(File path) {
		;//System.out.println(" scm del <" + path + ">");
		if (!path.exists())
			executeCommand(fullPathToSheetDirectory, new String[] { hgCommand, "remove", "--after", path.getAbsolutePath() });
		else
			executeCommand(fullPathToSheetDirectory, new String[] { hgCommand, "remove", path.getAbsolutePath() });

	}

	@Override
	public void scmDeleteFile(File path) {
		;//System.out.println(" scm del <" + path + ">");
		if (!path.exists())
			executeCommand(fullPathToSheetDirectory, new String[] { hgCommand, "remove", "--after", path.getAbsolutePath() });
		else
			executeCommand(fullPathToSheetDirectory, new String[] { hgCommand, "remove", path.getAbsolutePath() });
	}

	public void scmMoveFile(File from, File to) {
		executeCommand(fullPathToSheetDirectory, new String[] { hgCommand, "rename", from.getAbsolutePath(), to.getAbsolutePath() });
	}

	protected String executeCommand(String dir, String[] command) {
		ExecuteCommand c = new ExecuteCommand(dir, command, true);
		c.waitFor(true);
		String output = c.getOutput();
		return output;
	}

	@Override
	protected void scmAddDirectory(File file) {
		executeCommand(fullPathToSheetDirectory, new String[] { hgCommand, "add", file.getAbsolutePath() });
	}

}
