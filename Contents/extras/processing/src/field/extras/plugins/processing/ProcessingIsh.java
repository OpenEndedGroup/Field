package field.extras.plugins.processing;

import java.io.StringWriter;
import java.util.Arrays;

import processing.app.Preferences;
import processing.mode.java.preproc.PdePreprocessor;
import antlr.CharScanner;
import antlr.NoViableAltException;

/**
 * converts from processing ish to a class file
 * 
 * @author marc
 * 
 */
public class ProcessingIsh {

	static public class SyntaxError extends Exception {
		private String message;

		SyntaxError(String s) {
			this.message = s;
		}

		@Override
		public String toString() {
			return message;
		}
	}

	static public int uniq = 0;
	public String allCode;

	public ProcessingIsh(String code) throws NoViableAltException, SyntaxError {
		Preferences.setBoolean("preproc.substitute_floats", true);
		Preferences.setInteger("editor.tabs.size", 4);
		Preferences.set("preproc.imports.list", "java.applet.*, java.awt.*, java.awt.image.*, java.awt.event.*, java.io.*, java.net.*, java.text.*, java.util.*, java.util.zip.*, java.util.regex.*");
		Preferences.setBoolean("preproc.output_parse_tree", false);

		PdePreprocessor pdePreprocessor = new PdePreprocessor("FieldSketch");
		try {

			System.out.println(" constructors <" + Arrays.asList(CharScanner.class.getDeclaredConstructors()) + ">");

			StringWriter out = new StringWriter();

			pdePreprocessor.write(out, code);

			// pdePreprocessor.writePrefix(code, "/var/tmp/",
			// "ProcessingIsh"+(++uniq), new String[]{});
			// pdePreprocessor.write();

			// File f = new
			// File("/var/tmp/"+"ProcessingIsh"+(uniq)+".java");
			//
			// BufferedReader r = new BufferedReader(new
			// FileReader(f));
			// String all = "";
			// while(r.ready())
			// {
			// all += r.readLine()+"\n";
			// }
			// r.close();

			this.allCode = out.getBuffer().toString();

		} catch (NoViableAltException e) {
			throw new SyntaxError("Compilation error on line " + e.line + " :" + e);
		} catch (Exception e) {
			IllegalArgumentException a = new IllegalArgumentException();
			a.initCause(e);
			throw a;
		}
	}

}
