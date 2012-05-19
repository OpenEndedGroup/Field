package field.core.plugins.history;

import java.io.File;
import java.util.Arrays;

import field.core.util.ExecuteCommand;

public class GitVersioningSystem extends VersioningSystem {

	static public String gitCommand = "/usr/bin/git";
	
	static
	{
		if (!new File(gitCommand).exists())
			gitCommand = "/usr/bin/git";
		if (!new File(gitCommand).exists())
			gitCommand = "/opt/local/bin/git";
		if (!new File(gitCommand).exists())
			gitCommand = "/usr/local/bin/git";
		if (!new File(gitCommand).exists())
			gitCommand = "/bin/git";
		if (!new File(gitCommand).exists())
			gitCommand = System.getProperty("user.home") + "/bin/git";
		if (!new File(gitCommand).exists())
			System.err.println("ERROR: cant find a git installation anywhere");
	}

	public GitVersioningSystem(String fullPathToRepositoryDirector, String sheetsub, String xmlFilename) {
		super(fullPathToRepositoryDirector, sheetsub, xmlFilename);
	}

	@Override
	protected void scmAddDirectory(File file) {
		executeCommand(fullPathToRepositoryDirectory, gitCommand, "add", file.getAbsolutePath());
	}

	protected String executeCommand(String dir, String... command) {
		System.out.println(":: " + dir + " @ " + Arrays.asList(command));
		ExecuteCommand c = new ExecuteCommand(dir, command, true);
		c.waitFor(true);
		String output = c.getOutput();
		return output;
	}

	@Override
	protected void scmAddFile(File path) {
		executeCommand(fullPathToRepositoryDirectory, gitCommand, "add", path.getAbsolutePath());
	}

	@Override
	protected void scmCommitDirectory(File path) {
		executeCommand(fullPathToRepositoryDirectory, gitCommand, "commit", "-a", "-m", "\"" + currentLogMessage + "\"");
	}

	@Override
	protected void scmCommitFile(File path) {
		executeCommand(fullPathToRepositoryDirectory, gitCommand, "commit", "-a", "-m", "\"" + currentLogMessage + "\"");
	}

	@Override
	protected void scmCopyDirectory(File from, File to) {
		if (to.exists()) {
			executeCommand(fullPathToRepositoryDirectory, gitCommand, "add", to.getAbsolutePath());
		} else {
			executeCommand(fullPathToRepositoryDirectory, "/bin/cp", "-r", from.getAbsolutePath(), to.getAbsolutePath());
			executeCommand(fullPathToRepositoryDirectory, gitCommand, "add", to.getAbsolutePath());
		}
	}

	@Override
	protected void scmCopyFile(File from, File to) {
		if (to.exists()) {
			executeCommand(fullPathToRepositoryDirectory, gitCommand, "add", to.getAbsolutePath());
		} else {
			executeCommand(fullPathToRepositoryDirectory, "/bin/cp", "-r", from.getAbsolutePath(), to.getAbsolutePath());
			executeCommand(fullPathToRepositoryDirectory, gitCommand, "add", to.getAbsolutePath());
		}
	}

	@Override
	protected void scmDeleteDirectory(File path) {
		executeCommand(fullPathToRepositoryDirectory, gitCommand, "rm", path.getAbsolutePath());
	}

	@Override
	protected void scmDeleteFile(File path) {
		executeCommand(fullPathToRepositoryDirectory, gitCommand, "rm", path.getAbsolutePath());
	}

}
