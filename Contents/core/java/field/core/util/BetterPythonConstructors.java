package field.core.util;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.Arrays;

import field.bytecode.protect.Trampoline2;
import field.bytecode.protect.Trampoline2.ClassLoadedNotification;
import field.core.Platform;
import field.core.execution.PythonInterface;
import field.launch.iUpdateable;

public class BetterPythonConstructors implements ClassLoadedNotification {

	@Target(value = { ElementType.TYPE })
	@Retention(value = RetentionPolicy.RUNTIME)
	public @interface SynthesizeFactory {
	}

	static {
		Trampoline2.notifications.add(new BetterPythonConstructors());
	}

	boolean inside = false;

	public void notify(final Class loaded) {

		field.launch.Launcher.getLauncher().registerUpdateable(new iUpdateable() {

			@Override
			public void update() {
				field.launch.Launcher.getLauncher().deregisterUpdateable(this);
				
				Annotation an = loaded.getAnnotation(SynthesizeFactory.class);
				if (an == null)
					return;

				if (inside)
					return;

				inside = true;
				try {

					;//System.out.println(" found synthetize factory <" + loaded + ">");

					String name = loaded.getSimpleName();
					String upperName = name;
					name = Character.toLowerCase(name.charAt(0)) + name.substring(1);

					String doc = "Construct a" + (name.startsWith("a") | name.startsWith("e") | name.startsWith("i") | name.startsWith("o") | name.startsWith("u") ? "n" : "") + " " + name + ".\n\n Automatically constructs a " + Platform.getCanonicalName(loaded) + " using keyword arg style.\n\n Keys are:\n\n";
					Field[] f = loaded.getDeclaredFields();
					for (Field ff : f) {
						doc += "\tX{" + ff.getName() + "} (C{" + ff.getType().getName() + "})\n\n";
					}

					String s = "def " + name + "(**args):\n" + "\tr = " + upperName + "()\n" + "\tfor k,v in args.items():\n" + "\t\tsetattr(r, k, v)\n" + "\treturn r\n" + name + ".__doc__=\"\"\"" + doc + "\"\"\"";

					;//System.out.println(" eval :\n" + s);

					String n = loaded.getName();
					String clazz = "";
					String pack = "";

					;//System.out.println(" name is <" + n + ">");

					if (n.contains("$")) {
						;//System.out.println(" split <" + Arrays.asList(n.split("\\$")) + ">");
						clazz = n.split("\\$")[1];
						pack = n.split("\\$")[0];

						String pack2 = pack.substring(0, pack.lastIndexOf("."));
						String clazz2 = pack.substring(pack.lastIndexOf(".") + 1);

						;//System.out.println(" import <" + clazz2 + "> <" + pack2 + ">");
						PythonInterface.getPythonInterface().importJava(pack2, clazz2);
					} else {
						pack = n.substring(0, n.lastIndexOf("."));
						clazz = n.substring(n.lastIndexOf(".") + 1);
					}
					;//System.out.println(" import2 <" + clazz + "> <" + pack + ">");

					PythonInterface.getPythonInterface().importJava(pack, clazz);
					PythonInterface.getPythonInterface().execString(s);

				} finally {
					inside = false;
				}
			}
		});
	}

	public static void load() {
		// already done in the static constructor;
	}

}
