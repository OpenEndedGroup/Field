package field.core.ui.text.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import field.core.dispatch.VisualElement;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElementOverrides;
import field.core.dispatch.iVisualElement.Rect;
import field.core.dispatch.iVisualElement.VisualElementProperty;
import field.core.dispatch.iVisualElementOverrides.DefaultOverride;
import field.core.plugins.python.PythonPluginEditor;
import field.core.windowing.components.DraggableComponent;
import field.graphics.core.BasicGLSLangProgram;
import field.graphics.core.BasicGLSLangProgram.BasicGLSLangElement;
import field.launch.iUpdateable;
import field.namespace.generic.Generics.Pair;
import field.namespace.generic.Generics.Triple;

public class EchoOndiskFile {

	public static final VisualElementProperty<String> fileOnDisk = new VisualElementProperty<String>("contents_v");

	static {
		PythonPluginEditor.knownPythonProperties.put("<html><b>Contents</b> \u2014 <font size=-3>( fileOnDisk<i>_v</i> )</font>", fileOnDisk);
	}

	static public void echoShaderFiles(iVisualElement root, String prefix, final BasicGLSLangProgram program, int startat) {
		List<BasicGLSLangElement> p = program.getPrograms();
		int n = startat;
		for (BasicGLSLangElement pp : p) {
			File[] f = pp.getFiles();
			
			;//System.out.println(" files for program are <"+Arrays.asList(f)+">");
			
			if (f != null && f.length != 0) {
				final EchoOndiskFile [] ee = {null};
				EchoOndiskFile e = new EchoOndiskFile(root, f[0].getAbsolutePath(), n, new iUpdateable() {
					public void update() {
						program.reload();
						iVisualElement.dirty.set(ee[0].created.left,ee[0]. created.left, true);
					}
				});
				ee[0] = e;
				iVisualElement.name.set(e.created.left, e.created.left, prefix+"("+pp.isFragment+") :"+f[0].getName());
			}
			n++;
		}
	}

	private static String readFile(File code) {
		BufferedReader reader = null;
		String s = "";
		try {
			reader = new BufferedReader(new FileReader(code));
			while (reader.ready()) {
				s += reader.readLine() + "\n";
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return s;
	}

	private final Triple<VisualElement, DraggableComponent, DefaultOverride> created;
	private final String filename;

	private final iVisualElement root;

	@SuppressWarnings("unchecked")
	public EchoOndiskFile(iVisualElement root, String filename, int n, final iUpdateable onSync) {

		this.root = root;
		this.filename = filename;

		created = VisualElement.createWithToken(filename, root, new Rect(30, 100 + n * 40, 30, 30), VisualElement.class, DraggableComponent.class, iVisualElementOverrides.DefaultOverride.class);
		VisualElement.name.set(created.left, created.left, filename);
		PythonPluginEditor.python_customToolbar.addToList(ArrayList.class, created.left, new Pair<String, iUpdateable>("sync from filesystem", new iUpdateable() {
			public void update() {
				syncFromFilesystem();
			}
		}));

		PythonPluginEditor.python_customToolbar.addToList(ArrayList.class, created.left, new Pair<String, iUpdateable>("sync to filesystem and update", new iUpdateable() {
			public void update() {
				;//System.out.println(" syncing to file system");
				syncToFilesystem();
				if (onSync != null)
					onSync.update();
			}
		}));

		syncFromFilesystem();
	}

	protected void syncFromFilesystem() {
		String f;
		if (!new File(filename).exists()) {
			f = "(no file)";
		}
		f = readFile(new File(filename));

		fileOnDisk.set(created.left, created.left, f);
	}

	protected void syncToFilesystem() {
		String m = fileOnDisk.get(created.left);
		;//System.out.println(" syncing to file system");
		;//System.out.println(m);
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(new File(filename)));
			writer.append(m);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
