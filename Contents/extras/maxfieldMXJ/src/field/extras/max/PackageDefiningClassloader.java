package field.extras.max;

import java.util.LinkedHashSet;

public class PackageDefiningClassloader extends ClassLoader {
	public PackageDefiningClassloader(ClassLoader parent) {
		super(parent);
	}

	LinkedHashSet<String> knownPackages = new LinkedHashSet<String>();

	@Override
	protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		if (name.lastIndexOf(".") != -1) {
			String packageName = name.substring(0, name.lastIndexOf("."));
			if (!knownPackages.contains(packageName)) {
				try {
					System.out.println(" defining package <" + packageName + ">");
					definePackage(packageName, null, null, null, null, null, null, null);
				} catch (IllegalArgumentException e) {
					// e.printStackTrace();
				}
			}
		}

		return super.loadClass(name, resolve);
	}
}
