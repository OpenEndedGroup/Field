package field.core.plugins.history;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.Sun14ReflectionProvider;
import com.thoughtworks.xstream.core.ReferenceByIdMarshallingStrategy;

import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides.Ref;
import field.core.plugins.history.Versionings.CopyElementOp;
import field.core.plugins.history.Versionings.CopyPropertyOp;
import field.core.plugins.history.Versionings.DeleteElementOp;
import field.core.plugins.history.Versionings.DeletePropertyOp;
import field.core.plugins.history.Versionings.Path;
import field.core.plugins.history.Versionings.iOperation;
import field.launch.SystemProperties;
import field.util.HashMapOfLists;

public abstract class VersioningSystem {

	static public PrintStream debugOut = null;

	static {
		// debugOut = new PrintStream(new File("/dev/ttyp2"));
		debugOut = System.err;
	}

	/* this is sheet local storage */

	static public boolean useGit = SystemProperties.getIntProperty("useGit", 0) == 1;

	// static public final OKey<VersioningSystem> versioningSystem = new
	// OKey<VersioningSystem>("versioningSystem").rootSet(null);

	public static VersioningSystem newDefault() {
		// VersioningSystem r = new
		// SVNVersioningSystem(SystemProperties.getDirProperty("versioning.dir"),
		// SystemProperties.getProperty("fluid.scratch",
		// SystemProperties.getProperty("main.class") + ".xml"),
		// "sheet.xml");

		if (useGit)
			return new GitVersioningSystem(SystemProperties.getDirProperty("versioning.dir"), SystemProperties.getProperty("fluid.scratch", SystemProperties.getProperty("main.class") + ".xml"), "sheet.xml");
		VersioningSystem r = new HGVersioningSystem(SystemProperties.getDirProperty("versioning.dir"), SystemProperties.getProperty("fluid.scratch", SystemProperties.getProperty("main.class") + ".xml"), "sheet.xml");
		// VersioningSystem.versioningSystem.set(r);
		return r;
	}

	public static VersioningSystem newDefault(String filename) {
		// VersioningSystem r = new
		// SVNVersioningSystem(SystemProperties.getDirProperty("versioning.dir"),
		// SystemProperties.getProperty("fluid.scratch",
		// SystemProperties.getProperty("main.class") + ".xml"),
		// "sheet.xml");
		if (useGit)
			return new GitVersioningSystem(SystemProperties.getDirProperty("versioning.dir"), filename, "sheet.xml");
		VersioningSystem r = new HGVersioningSystem(SystemProperties.getDirProperty("versioning.dir"), filename, "sheet.xml");
		// VersioningSystem.versioningSystem.set(r);
		return r;
	}

	static public Object objectRepresentationFor(String read) {

		if (read.startsWith("string>"))
			return read.substring("string>".length());
		if (read.length() == 0)
			return null;
		if (read.startsWith("xml>")) {
			read = read.substring(("xml>").length());
			StringReader reader = new StringReader(read);
			XStream stream = new XStream(new Sun14ReflectionProvider());
			stream.setMarshallingStrategy(new ReferenceByIdMarshallingStrategy());
			ObjectInputStream input;
			Object o = null;
			try {
				input = stream.createObjectInputStream(reader);
				o = input.readObject();
				input.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			return o;
		}
		return null;
	}

	private String xmlFilename;

	protected final String fullPathToSheetDirectory;

	protected String currentLogMessage = "(no message)";

	protected List<Versionings.iOperation> journal = new ArrayList<Versionings.iOperation>();

	final String fullPathToRepositoryDirectory;

	HashMapOfLists<iVisualElement, VisualElementProperty<?>> dirty = new HashMapOfLists<iVisualElement, VisualElementProperty<?>>() {
		@Override
		protected Collection<VisualElementProperty<?>> newList() {
			return new HashSet<VisualElementProperty<?>>();
		}
	};

	int journalingDisabled = 0;

	public VersioningSystem(String fullPathToRepositoryDirectory, String sheetSubdirectory, String xmlFilename) {

		;// System.out.println(" inside versioning system constructor <"+fullPathToRepositoryDirectory+"> <"+sheetSubdirectory+"> <"+xmlFilename+">");

		this.fullPathToRepositoryDirectory = fullPathToRepositoryDirectory;
		this.fullPathToSheetDirectory = fullPathToRepositoryDirectory + "/" + sheetSubdirectory;
		this.xmlFilename = xmlFilename;
		// versioningSystem.set(this);

		// find out if the .xml is out of sync with the
		// file structure
		// if so, synchronizeElementWithFileStructure
		// will do something

		if (!new File(fullPathToRepositoryDirectory + "/" + sheetSubdirectory).exists()) {
			new File(fullPathToRepositoryDirectory + "/" + sheetSubdirectory).mkdir();
			scmAddDirectory(new File(fullPathToRepositoryDirectory + "/" + sheetSubdirectory));
		}
		if (!new File(fullPathToSheetDirectory + "/" + xmlFilename).exists()) {
			try {
				new File(fullPathToSheetDirectory + "/" + xmlFilename).createNewFile();
				scmAddFile(new File(fullPathToSheetDirectory + "/" + xmlFilename));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		this.xmlFilename = fullPathToSheetDirectory + "/" + xmlFilename;
	}

	/**
	 * works through the journal, and the dirty list
	 */
	public void commitAll(Collection<iVisualElement> elements) {

		Iterator<iOperation> i = journal.iterator();
		while (i.hasNext()) {
			iOperation op = i.next();

			if (op instanceof DeleteElementOp) {
				if (new File(((DeleteElementOp) op).oldElement.getPath()).exists())
					scmDeleteDirectory(((DeleteElementOp) op).oldElement.getFile());
			}
			if (op instanceof DeletePropertyOp) {

				;// System.out.println(" delete property <"+((DeletePropertyOp)
					// op).oldProperty.getPath()+">");

				if (new File(((DeletePropertyOp) op).oldProperty.getPath()).exists())
					scmDeleteFile(((DeletePropertyOp) op).oldProperty.getFile());
				new File(((DeletePropertyOp) op).oldProperty.getPath()).delete();
			}
		}

		for (iVisualElement element : elements) {
			commitOutToFileStructureAndRepository(element);
		}

		scmCommitDirectory(new File(fullPathToSheetDirectory));
	}

	public void enableJournaling() {
		journalingDisabled--;
		assert journalingDisabled >= 0;
	}

	// in version 1.1 we'll just save the journal, and hand this
	// over to another program, which will commit everything in that
	// .xml file
	public void fastCommit(List<iVisualElement> elements) {
		commitAll(elements);
	}

	public String getSheetPathName() {
		return this.xmlFilename;
	}

	public void notifyCopyFileToProperty(String filename, iVisualElement targetElement, VisualElementProperty<String> targetProperty) {
		File f = new File(fullPathToSheetDirectory + "/" + targetElement.getUniqueID());
		if (!f.exists())
			f.mkdir();
		String dest = fullPathToSheetDirectory + "/" + targetElement.getUniqueID() + "/" + targetProperty.getName() + ".property";
		scmCopyFile(new File(filename), new File(dest));
	}

	public void notifyElementCopied(iVisualElement copy, iVisualElement to) {
		if (copy == null || to == null)
			return;

		commitOutToFileStructureAndRepository(copy);

		CopyElementOp op = new CopyElementOp();
		op.newElement = pathFor(to, null);
		op.oldElement = pathFor(copy, null);
		journal.add(op);

		commitOutToFileStructureAndRepository(to);
	}

	public void notifyElementDeleted(iVisualElement deleted) {
		DeleteElementOp op = new DeleteElementOp();
		op.oldElement = pathFor(deleted, null);
		journal.add(op);
	}

	public <T> void notifyPropertyDeleted(VisualElementProperty<T> property, iVisualElement deleted) {
		DeletePropertyOp op = new DeletePropertyOp();
		op.oldProperty = pathFor(deleted, property);
		journal.add(op);
	}

	public <T> void notifyPropertySet(VisualElementProperty<T> property, Ref<T> was, iVisualElement dest) {
		if (journalingDisabled > 0)
			return;
		if (was.getStorageSource() == null || was.getStorageSource() == dest) {
			dirty.addToList(dest, property);
			return;
		}

		// damn. even this needs to commit (what if
		// we've changed the source property before
		// copyingit?). Perhaps we need a finer grained
		// commit.
		commitOutToFileStructureAndRepository(was.getStorageSource(), property);

		CopyPropertyOp op = new CopyPropertyOp();
		op.newProperty = pathFor(dest, property);
		op.oldProperty = pathFor(was.getStorageSource(), property);
		journal.add(op);
	}

	public void setCurrentLogMessage(String currentLogMessage) {
		this.currentLogMessage = currentLogMessage;
		if (currentLogMessage.trim().equals(""))
			currentLogMessage = "(no message)";
	}

	public void suspendJournaling() {
		journalingDisabled++;
	}

	// called on all elements when first loaded?
	public void synchronizeElementWithFileStructure(iVisualElement to) {

		if (true)
			return;

		// there needs to be a way to check to see if we
		// need to do this. It will only need to be done
		// if the hash of the dates of these files are
		// different from that stored in the xml file
		// (perhaps as persistance info)

		Path path = pathFor(to, null);
		File f = new File(path.getPath());
		if (f.exists()) {
			assert f.isDirectory() : f;

			String[] properties = f.list(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.endsWith(".property");
				}
			});
			if (properties != null)
				for (String p : properties) {
					File load = new File(p);
					try {
						BufferedReader reader = new BufferedReader(new FileReader(path.getPath() + "/" + load));
						StringBuffer read = new StringBuffer();
						while (reader.ready()) {
							read.append(reader.readLine() + "\n");
						}
						reader.close();
						Object o = objectRepresentationFor(read.toString());

						suspendJournaling();
						to.setProperty(new VisualElementProperty(propertyNameFor(p)), o);
						enableJournaling();

					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
		}
	}

	private void commitOutToFileStructureAndRepository(iVisualElement to, VisualElementProperty<?> property) {

		Path path = pathFor(to, property);

		Iterator<iOperation> i = journal.iterator();
		while (i.hasNext()) {
			iOperation op = i.next();

			if (op instanceof CopyElementOp) {
				if ((((CopyElementOp) op).newElement).equals(path)) {
					scmCopyDirectory(((CopyElementOp) op).oldElement.getFile(), ((CopyElementOp) op).newElement.getFile());
					i.remove();
				}
			}
			if (op instanceof CopyPropertyOp) {
				if ((((CopyPropertyOp) op).newProperty).equals(path)) {
					scmCopyFile(((CopyElementOp) op).oldElement.getFile(), ((CopyElementOp) op).newElement.getFile());
					i.remove();
				}
			}
		}

		ensureUnderVersionControl(to);

		boolean first = true;

		Collection<VisualElementProperty<?>> c = dirty.getCollection(to);
		if (c != null) {
			Iterator<VisualElementProperty<?>> a = c.iterator();
			while (a.hasNext()) {
				VisualElementProperty<?> p = a.next();
				if (pathFor(to, p).equals(path)) {
					if (first) {
						setFileFromString(pathFor(to, p), stringRepresentationFor(to.getProperty(p)), true);
						// svnCommitFile(pathFor(to,
						// p));
						first = false;
					}
					a.remove();
				}
			}
		}

	}

	private String propertyNameFor(String p) {
		String[] s = p.split("/");
		return s[s.length - 1].replace(".property", "");
	}

	protected void commitOutToFileStructureAndRepository(iVisualElement to) {
		if (to == null)
			return;

		// go through journal and see if it has any
		// structural modifications that apply to it.
		// in particular, is it the _destination_ of any
		// copyPropertyOp's

		Path path = pathFor(to, null);

		Iterator<iOperation> i = journal.iterator();
		while (i.hasNext()) {
			iOperation op = i.next();

			if (op instanceof CopyElementOp) {
				if ((((CopyElementOp) op).newElement).equals(path)) {
					scmCopyDirectory(((CopyElementOp) op).oldElement.getFile(), ((CopyElementOp) op).newElement.getFile());
					i.remove();
				}
			}
			if (op instanceof CopyPropertyOp) {
				if ((((CopyPropertyOp) op).newProperty).isSameElementAs(path)) {
					scmCopyFile(((CopyPropertyOp) op).oldProperty.getFile(), ((CopyPropertyOp) op).newProperty.getFile());
					i.remove();
				}
			}
		}

		ensureUnderVersionControl(to);

		// go through dirty list and see if it has any

		Collection<VisualElementProperty<?>> c = dirty.getCollection(to);
		if (c != null) {
			Iterator<VisualElementProperty<?>> a = c.iterator();
			while (a.hasNext()) {
				VisualElementProperty<?> p = a.next();
				setFileFromString(pathFor(to, p), stringRepresentationFor(to.getProperty(p)), true);

				// svnCommitFile(pathFor(to, p));

				a.remove();
			}
		}
		// modified properties
		// perform these and commit them
	}

	protected void ensureUnderVersionControl(iVisualElement to) {

		Path path = pathFor(to, null);

		System.out.println(" ensure under version control <" + path + ">");

		boolean needsSVNSupport = false;
		boolean justCreated = false;

		if (!new File(path.getPath()).exists()) {
			new File(path.getPath()).mkdir();
			justCreated = true;
		}
		Map<Object, Object> properties = to.payload();
		for (Map.Entry<Object, Object> e : properties.entrySet()) {
			Object key = e.getKey();
			if (key instanceof VisualElementProperty) {
				if (((VisualElementProperty) key).containsSuffix("v")) {
					Path pathProperty = pathFor(to, ((VisualElementProperty) key));
					justCreated |= setFileFromString(pathProperty, stringRepresentationFor(to.getProperty((VisualElementProperty) key)), false);
					needsSVNSupport = true;
				}
			}
		}

		if (iVisualElement.doNotSave.getBoolean(to, false))
			return;

		if (useGit)
			scmAddDirectory(path.getFile());

		if (needsSVNSupport && justCreated) {

			if (!useGit)
				scmAddDirectory(path.getFile());

			properties = to.payload();
			for (Map.Entry<Object, Object> e : properties.entrySet()) {
				Object key = e.getKey();
				if (key instanceof VisualElementProperty) {
					if (((VisualElementProperty) key).containsSuffix("v")) {
						Path pathProperty = pathFor(to, ((VisualElementProperty) key));
						scmAddFile(pathProperty.getFile());
					}
				}
			}
		}

	}

	protected Versionings.Path pathFor(iVisualElement element, VisualElementProperty<?> property) {
		return new Versionings.Path(fullPathToSheetDirectory, element.getUniqueID(), property == null ? Versionings.NoProperty : property.getName());
	}

	abstract protected void scmAddDirectory(File file);

	abstract protected void scmAddFile(File path);

	abstract protected void scmCommitDirectory(File path);

	abstract protected void scmCommitFile(File path);

	abstract protected void scmCopyDirectory(File from, File to);

	abstract protected void scmCopyFile(File from, File to);

	abstract protected void scmDeleteDirectory(File path);

	abstract protected void scmDeleteFile(File path);

	protected boolean setFileFromString(Path path, String string, boolean shouldAdd) {
		if (string != null) {
			try {
				boolean newFile = !new File(path.getPath()).exists();

				BufferedWriter writer = new BufferedWriter(new FileWriter(new File(path.getPath())));
				writer.write(string);
				writer.close();

				if (shouldAdd && !new File(path.getPath()).exists())
					scmAddFile(path.getFile());
				return newFile;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	protected String stringRepresentationFor(Object property) {
		if (property instanceof String) {
			return "string>" + property;
		} else {
			XStream stream = new XStream(new Sun14ReflectionProvider());
			stream.setMarshallingStrategy(new ReferenceByIdMarshallingStrategy());

			StringWriter writer = new StringWriter();
			try {
				ObjectOutputStream out;
				out = stream.createObjectOutputStream(writer);
				out.writeObject(property);
				out.close();
			} catch (IOException e) {
				System.err.println(" failure to serialize <" + property + ">");
				e.printStackTrace();
				return null;
			}
			return "xml>" + writer.toString();
		}
	}
}
