package field.core.persistance;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.Sun14ReflectionProvider;

import field.core.StandardFluidSheet;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.execution.PhantomFluidSheet;
import field.core.ui.FieldMenus2;
import field.core.util.AppleScript;
import field.core.windowing.components.SelectionGroup;
import field.core.windowing.components.iComponent;
import field.math.linalg.Vector2;
import field.util.PythonUtils;

/**
 * we'de like a single button that wraps selected elements up into a single
 * archive and opens them at the other end
 * 
 * @author marc
 * 
 */
public class PackageTools {

	static public final VisualElementProperty<String> isPackageImported = new VisualElementProperty<String>("isPackageImported");

	HashMap<String, String> packagesToSheets = new HashMap<String, String>();

	public PackageTools() {
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				new PythonUtils().persistAsXML(packagesToSheets, FieldMenus2.getFieldDir() + "/packagesToSheets");
			}
		}));

		try {
			packagesToSheets = (HashMap<String, String>) new PythonUtils().loadAsXML(FieldMenus2.getFieldDir() + "/packagesToSheets");
		} catch (Throwable e) {
		} finally {
			if (packagesToSheets == null)
				packagesToSheets = new HashMap<String, String>();
		}
	}

	public boolean isPackage(String file) {
		boolean endsWith = file.endsWith(".fieldpackage");
		try {
			if (endsWith && !new File(file).getCanonicalFile().getPath().startsWith(FieldMenus2.getCanonicalVersioningDir()))
				return true;
		} catch (IOException e) {
			return false;
		}
		return false;
	}

	public void handle(String openingFile) {
		
		//TODO swt \u2014 resolve issues with FieldMenus
		
//		String known = packagesToSheets.get(openingFile);
//		if (known == null) {
//			ArrayList<Sheet> sheets = FieldMenus.fieldMenus.getOpenSheets();
//			if (sheets.size() == 1) {
//				int option = JOptionPane.showOptionDialog(null, "You have asked Field to open a .fieldpackage file that has not previously been opened in your copy of Field. Import this file into this sheet ('" + sheets.get(0).filename + "') or make a new sheet specific for this file?", "Import .fieldpackage file", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[] { "New Sheet", "Use Existing Sheet", "Cancel" }, null);
//				System.out.println(" option selected is <" + option + ">");
//				if (option == 0) {
//					System.out.println(" make new sheet");
//					makeNewSheet(openingFile);
//				} else if (option == 1) {
//					System.out.println(" import into existing");
//					importFieldPackage(sheets.get(0).sheet.getRoot(), openingFile);
//				}
//			} else if (sheets.size() > 1) {
//				Object[] openSheets = new Object[sheets.size() + 1];
//				for (int i = 0; i < sheets.size(); i++) {
//					openSheets[i + 1] = "Import into '" + sheets.get(i).filename + "'";
//				}
//				openSheets[0] = "Make new sheet, called " + new File(openingFile).getName();
//				Object option = JOptionPane.showInputDialog(null, "You have asked Field to open a .fieldpackage source file that has not previously been opened in Field. Import this file into an exisiting sheet, or make a new sheet specific to this file?", "Import .py file", JOptionPane.QUESTION_MESSAGE, null, openSheets, openSheets[0]);
//				if (option == null)
//					return;
//				int found = -1;
//				for (int i = 0; i < openSheets.length; i++) {
//					if (option.equals(openSheets[i]))
//						found = i;
//				}
//				if (found == -1)
//					return;
//
//				if (found == 0) {
//					makeNewSheet(openingFile);
//				} else {
//					importFieldPackage(sheets.get(found - 1).sheet.getRoot(), openingFile);
//				}
//
//			} else {
//				makeNewSheet(openingFile);
//			}
//		} else {
//			ArrayList<Sheet> sheets = FieldMenus.fieldMenus.getOpenSheets();
//			for (Sheet s : sheets) {
//				if (s.filename.equals(known)) {
//					importFieldPackage(s.sheet.getRoot(), openingFile);
//					return;
//				}
//			}
//
//			PhantomFluidSheet ps = FieldMenus.fieldMenus.openSheet(known, false);
//			for (Sheet s : sheets) {
//				if (s.sheet == ps) {
//					importFieldPackage(s.sheet.getRoot(), openingFile);
//					return;
//				}
//			}
//			assert false;
//			return;
//		}

	}

	private void makeNewSheet(String openingFile) {
		String fn = getSuggestedFileName(openingFile);
		if (fn == null)
			fn = new File(openingFile).getName() + ".field";

		PhantomFluidSheet ps = FieldMenus2.fieldMenus.open(fn);
		importFieldPackage(ps.getRoot(), openingFile);

		packagesToSheets.put(openingFile, fn);
	}

	public File newTempFileWithSelected(iVisualElement root, String suggestedName) {
		FluidCopyPastePersistence copier = iVisualElement.copyPaste.get(root);

		SelectionGroup<iComponent> group = iVisualElement.selectionGroup.get(root);

		Set<iComponent> c = group.getSelection();
		Set<iVisualElement> v = new LinkedHashSet<iVisualElement>();
		for (iComponent cc : c) {
			iVisualElement vv = cc.getVisualElement();
			if (vv != null)
				v.add(vv);
		}

		return newTempFileWithSet(suggestedName, copier, v);
	}

	public File newTempFileWithSet(String suggestedName, FluidCopyPastePersistence copier, Set<iVisualElement> v) {
		System.out.println(" output is <" + v + ">");

		File tmpFile = null;
		try {
			tmpFile = File.createTempFile("fieldtmp_", ".fieldpackage");
			Writer temp = new BufferedWriter(new FileWriter(tmpFile));
			HashSet<iVisualElement> savedOut = new HashSet<iVisualElement>();
			ObjectOutputStream oos = copier.getObjectOutputStream(temp, savedOut, v);
			try {
				oos.writeObject(suggestedName);
				oos.writeObject(v);
				oos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return tmpFile;
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return null;
	}

	public String getSuggestedFileName(String filename) {
		XStream stream = new XStream(new Sun14ReflectionProvider());
		try {
			ObjectInputStream o = stream.createObjectInputStream(new BufferedReader(new FileReader(filename)));
			String suggested = (String) o.readObject();
			o.close();
			if (!suggested.endsWith(".field"))
				return suggested + ".field";
			return suggested;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void importFieldPackage(iVisualElement root, String filename) {
		FluidCopyPastePersistence copier = iVisualElement.copyPaste.get(root);

		SelectionGroup<iComponent> selectionGroup = iVisualElement.selectionGroup.get(root);
		selectionGroup.deselectAll();

		HashSet<iVisualElement> all = new HashSet<iVisualElement>(StandardFluidSheet.allVisualElements(root));

		try {
			HashSet<iVisualElement> ongoing = new HashSet<iVisualElement>();
			ObjectInputStream ois = copier.getObjectInputStream(new BufferedReader(new FileReader(filename)), ongoing, all);
			String suggestedFilename = (String) ois.readObject();
			Object in = ois.readObject();
			ois.close();

			for (iVisualElement o : ongoing) {
				iVisualElement.localView.get(o).setSelected(true);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void importFieldPackage(iVisualElement root, String filename, Vector2 centerOn) {
		FluidCopyPastePersistence copier = iVisualElement.copyPaste.get(root);

		SelectionGroup<iComponent> selectionGroup = iVisualElement.selectionGroup.get(root);
		selectionGroup.deselectAll();

		HashSet<iVisualElement> all = new HashSet<iVisualElement>(StandardFluidSheet.allVisualElements(root));

		try {
			HashSet<iVisualElement> ongoing = new HashSet<iVisualElement>();
			ObjectInputStream ois = copier.getObjectInputStream(new BufferedReader(new FileReader(filename)), ongoing, all);
			String suggestedFilename = (String) ois.readObject();
			Object in = ois.readObject();
			ois.close();

			Vector2 offsetBy = new Vector2();
			for (iVisualElement o : ongoing) {
				iVisualElement.localView.get(o).setSelected(true);
				offsetBy.add(o.getFrame(null).midpoint2());
			}
			offsetBy.scale(1f/ongoing.size());
			
			for (iVisualElement o : ongoing) {
				Rect f = o.getFrame(null);
				f.x+=centerOn.x-offsetBy.x;
				f.y+=centerOn.y-offsetBy.y;
				o.setFrame(f);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void copyFileReferenceToClipboard(final String file) {

		try {
			new AppleScript("set the clipboard to (\"" + new File(file).toURL().toExternalForm() + "\" as POSIX file)\n", true);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

}
