package field.core.ui.text;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import field.core.ui.text.protect.ClassDocumentationProtect.Comp;

public class ClassDocumentation {

	

	public interface CompletionDocumentation {
		public List<Comp> getCompletionDocumentationFor(String prefix);
	}

	public List<Comp> getPackageCustomCompletion(String packageName, String prefix) {
		if (!packageName.endsWith("."))
			packageName = packageName + ".";
		packageName = packageName.replace("<b>", "");
		try {
			Class<?> c = Class.forName(packageName + "__CompletionDocumentation__");
			List<Comp> cc = ((CompletionDocumentation) c.newInstance()).getCompletionDocumentationFor(prefix);
			return cc;
		} catch (ClassNotFoundException e) {
			// e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (Throwable t) {
		}
		return null;
	}

	public List<Comp> getClassCustomCompletion(String prefix, Object ret, Class c) {
		;//System.out.println(" looking for custom completion on class <" + c + ">");
		try {
			Method method = c.getDeclaredMethod("getClassCustomCompletion", String.class, Object.class);
			;//System.out.println(" method <" + method + ">");
			List<Comp> documentation = (List<Comp>) method.invoke(null, prefix, ret);
			;//System.out.println(" returns <" + documentation + ">");
			return documentation;
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
		} catch (Throwable t) {
		}
		return null;
	}

	public String getClassDocumentation(String prefix, Object ret, Class c) {
		try {
			Method method = c.getMethod("getClassDocumentation", String.class, Object.class);
			String documentation = (String) method.invoke(null, prefix, ret);
			return documentation;
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// e.printStackTrace();
		} catch (Throwable t) {
			// t.printStackTrace();
		}
		return null;
	}

}
