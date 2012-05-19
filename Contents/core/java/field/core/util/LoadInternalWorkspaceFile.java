package field.core.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import field.core.StandardFluidSheet;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.plugins.history.HGTools;
import field.core.plugins.history.HGVersioningSystem;
import field.core.plugins.history.VersioningSystem;
import field.launch.SystemProperties;

public class LoadInternalWorkspaceFile {

	static public class Shadow {
		public String internalFile;
		public VisualElementProperty property;
		public iVisualElement inside;

		public void copyToProperty() {
			try {
				new LoadInternalWorkspaceFile().copyPlainTextToProperty(internalFile, inside, property);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public String getCurrentFile() {
			try {
				return new LoadInternalWorkspaceFile().loadPlainText(internalFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		public String getFullPath() {
			return new LoadInternalWorkspaceFile().internalRepositoryRoot + internalFile;
		}

		public boolean mergeToProperty(boolean visualOnConflict) throws IOException {
			String currentProperty = (String) property.get(inside);
			String currentFile = new LoadInternalWorkspaceFile().loadPlainText(internalFile);
			String ancestor = (String) new LoadInternalWorkspaceFile().getOriginalText(inside, property);

			// the fast cases
			if (currentProperty.equals(ancestor)) {
				copyToProperty();
				return true;
			} else if (currentFile.equals(ancestor)) {
				return true;
			} else {
				// we need to find out if there are conflicts
				boolean conflict = diff3DoesConflict(currentProperty, ancestor, currentFile);
				if (conflict) {
					if (visualOnConflict) {
						String m = new LoadInternalWorkspaceFile().performThreeWayMerge(currentProperty, currentFile, ancestor);
						copyToProperty();
						property.set(inside, inside, m);
						return true;
					} else {
						return false;
					}
				} else {
					String m = performThreeWayMergeNonVisually(currentProperty, currentFile, ancestor);
					copyToProperty();
					property.set(inside, inside, m);
					return true;
				}
			}

		}

	}

	public static boolean diff3DoesConflict(String left, String ancestor, String right) throws IOException {
		File f1 = File.createTempFile("currentProperty", "field");
		File f2 = File.createTempFile("currentFile", "field");
		File f3 = File.createTempFile("ancestor", "field");

		{
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f1));
			bos.write(left.getBytes());
			bos.close();
		}
		{
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f2));
			bos.write(right.getBytes());
			bos.close();
		}
		{
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f3));
			bos.write(ancestor.getBytes());
			bos.close();
		}

		ExecuteCommand c = new ExecuteCommand(".", new String[] { "/usr/bin/diff3", f1.getAbsolutePath(), f3.getAbsolutePath(), f2.getAbsolutePath()}, true);
		int o = c.waitFor(true);
		return o==0;
	}

	public  static String performThreeWayMergeNonVisually(String left, String right, String ancestor) throws IOException {
		File f1 = File.createTempFile("currentProperty", "field");
		File f2 = File.createTempFile("currentFile", "field");
		File f3 = File.createTempFile("ancestor", "field");

		{
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f1));
			bos.write(left.getBytes());
			bos.close();
		}
		{
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f2));
			bos.write(right.getBytes());
			bos.close();
		}
		{
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f3));
			bos.write(ancestor.getBytes());
			bos.close();
		}

		ExecuteCommand c = new ExecuteCommand(".", new String[] { "/usr/bin/diff3", "-m", f1.getAbsolutePath(), f3.getAbsolutePath(), f2.getAbsolutePath()}, true);
		int o = c.waitFor(true);
		if (o != 0)
			return null;

		return c.getOutput();
	}


	private final String internalRepositoryRoot;

	public LoadInternalWorkspaceFile() {
		internalRepositoryRoot = SystemProperties.getDirProperty("versioning.dir") + "/internal/";
	}

	public void copyPlainTextToProperty(String filename, iVisualElement targetElement, VisualElementProperty<String> targetProperty) throws IOException {
		if (targetProperty.containsSuffix("v")) {
			VersioningSystem vs = StandardFluidSheet.versioningSystem.get(targetElement);
			if (vs != null) {
				vs.notifyCopyFileToProperty(internalRepositoryRoot + filename, targetElement, targetProperty);
			}
		}
		targetProperty.set(targetElement, targetElement, loadPlainText(filename));
	}

	public Object  getOriginalText(iVisualElement targetElement, VisualElementProperty<String> targetProperty) {
		VersioningSystem vs = StandardFluidSheet.versioningSystem.get(targetElement);
		Object o = new HGTools(vs).getOriginatingCopyFor(targetElement.getUniqueID() + "/" + targetProperty.getName() + ".property");
		return o;
	}

	public String loadPlainText(String name) throws IOException {
		return loadPlainText(name, false, null);
	}

	public String loadPlainText(String name, boolean createIfNecessary, iVisualElement e) throws IOException {

		File f = new File(internalRepositoryRoot + name);
		if (createIfNecessary) {
			if (!f.exists()) {
				f.getParentFile().mkdirs();
				f.createNewFile();
				VersioningSystem vs = StandardFluidSheet.versioningSystem.get(e);
				((HGVersioningSystem) vs).scmAddFile(f);
				((HGVersioningSystem) vs).scmCommitFile(f);
			}
		}

		BufferedReader reader = new BufferedReader(new FileReader(f));
		StringBuffer s = new StringBuffer();
		while (reader.ready()) {
			s.append(reader.readLine() + "\n");
		}
		return s.toString();
	}

	public Shadow newShadow(String filename, iVisualElement inside, String property) throws IOException {
		String source = loadPlainText(filename, true, inside);

		VisualElementProperty<String> p = new VisualElementProperty<String>(property);
		p.set(inside, inside, source);

		Shadow s = new Shadow();
		s.internalFile = filename;
		s.property = p;
		s.inside = inside;

		return s;
	}

	public String performThreeWayMerge(String filename, iVisualElement targetElement, VisualElementProperty<String> targetProperty) throws IOException {
		String currentProperty = targetProperty.get(targetElement);
		String currentFile = loadPlainText(filename);
		String ancestor = (String) getOriginalText(targetElement, targetProperty);

		if (currentProperty != null && currentFile != null && ancestor != null) {

			String newProperty = performThreeWayMerge(currentProperty, currentFile, ancestor);
			if (newProperty != null) {
				targetProperty.set(targetElement, targetElement, newProperty);
				return newProperty;
			}
		}
		return currentProperty;

	}

	public String performThreeWayMerge(String left, String right, String ancestor) throws IOException {
		File f1 = File.createTempFile("currentProperty", "field");
		File f2 = File.createTempFile("currentFile", "field");
		File f3 = File.createTempFile("ancestor", "field");
		File f4 = File.createTempFile("output", "field");

		{
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f1));
			bos.write(left.getBytes());
			bos.close();
		}
		{
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f2));
			bos.write(right.getBytes());
			bos.close();
		}
		{
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f3));
			bos.write(ancestor.getBytes());
			bos.close();
		}

		ExecuteCommand c = new ExecuteCommand(".", new String[] { "/usr/bin/opendiff", f1.getAbsolutePath(), f2.getAbsolutePath(), "-ancestor", f3.getAbsolutePath(), "-merge", f4.getAbsolutePath() }, true);
		int o = c.waitFor(true);
		if (o != 0)
			return null;

		BufferedReader reader = new BufferedReader(new FileReader(f4));
		StringBuffer s = new StringBuffer();
		while (reader.ready()) {
			s.append(reader.readLine() + "\n");
		}
		return s.toString();
	}

	public File findTemplateCalled(final String templateName) {
		File dir = new File(SystemProperties.getDirProperty("versioning.dir"));
		File[] files = dir.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				try {
					if (pathname.isDirectory() && new File(pathname.getCanonicalPath() + "/sheet.xml").exists())
						return true;
				} catch (IOException e) {
				}
				return false;
			}
		});

		final String[] found = {""};
		for (File f : files) {
			f.listFiles(new FileFilter() {

				public boolean accept(File pathname) {
					try {
						File ff = new File(pathname.getCanonicalPath() + "/python_isTemplate_v.property");
						File ffh = new File(pathname.getCanonicalPath() + "/python_isTemplateHead_v.property");
						if (pathname.isDirectory() && ff.exists()) {
							String name = textForFile(ff);
							boolean isHead = ffh.exists() ? !textForFile(ffh).equals("") : false;
							if (name.equals(templateName))
							{
								found[0] = pathname.getCanonicalPath();
							}
						}
					} catch (IOException e) {
					}
					return false;
				}

				private String textForFile(File ff) throws FileNotFoundException, IOException {
					BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(ff)));
					StringBuffer read = new StringBuffer();
					while (reader.ready()) {
						read.append(reader.readLine() + "\n");
					}
					reader.close();
					String name = (String) VersioningSystem.objectRepresentationFor(read.toString());
					return name;
				}
			});
		}
		return new File(found[0]);
	}

}
