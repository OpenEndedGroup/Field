package field.core.plugins.python;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JOptionPane;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;

import field.core.dispatch.iVisualElement;
import field.core.windowing.GLComponentWindow;
import field.core.windowing.overlay.OverlayAnimationManager;
import field.launch.SystemProperties;

public class AddToStandardLibrary {

	public static void addThis(iVisualElement from, String text, boolean plainText, StyledText fc) {
		String name = iVisualElement.name.get(from);
		String extensionsDir = SystemProperties.getProperty("extensions.dir", "../../extensions/");

		File newFile = new File(extensionsDir + name+".py");
		if (newFile.exists()) {
			// need to ask the user what to
			// do.
			
			MessageBox mb = new MessageBox(iVisualElement.enclosingFrame.get(from).getFrame(), SWT.OK | SWT.CANCEL);

			int oo = mb.open();
			
			if (oo == SWT.CANCEL) {
				return;
			}
		}

		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(newFile));
			if (plainText)
				out.write("#field-library\n");
			out.write(text);
			out.write("\n");
			out.close();
			
			OverlayAnimationManager.notifyAsText(from, "Wrote text to file '"+name+".py'", null);

		} catch (IOException e) {
			e.printStackTrace();
			;
		}
	}
}
