/**
 * 
 */
package field.bytecode.protect;

import java.io.FileDescriptor;
import java.net.InetAddress;
import java.security.Permission;

import field.launch.SystemProperties;

public class NoWriteSecurityManager extends
		SecurityManager {
	@Override
	public void checkWrite(
			FileDescriptor fd) {
		System.out.println("NoWriteSecurityManager.checkWrite(fd)");
		//	Allow file descriptor writes:
		//throw new SecurityException();
	}

	@Override
	public void checkWrite(
			String fd) {
		
		//	Allow writes to temporary files, or to anywhere with (directory) prefix in property "writeableDirPrefixes".
		
		final String prop = "writeableDirPrefixes";
		
		String tmpdir = System.getProperty("java.io.tmpdir");
		String[] whitelistedDirs = SystemProperties.getDirProperties(prop);

		boolean ok = fd.startsWith(tmpdir);
		
		for (String d: whitelistedDirs) {
			System.out.println(String.format("Checking %s against whitelist entry %s", fd, d));
			ok = ok || fd.startsWith(d);
		}
		
		System.out.println(String.format("NoWriteSecurityManager.checkWrite('%s', tmp='%s', wl='%s') -> %s)",
										 fd, tmpdir, SystemProperties.getProperty(prop), (ok ? "TRUE" : "FALSE")));

		if (ok) return;
		throw new SecurityException();
	}

	@Override
	public void checkAccept(
			String host,
			int port) {
	}

	@Override
	public void checkAccess(Thread t) {
	}

	@Override
	public void checkAccess(
			ThreadGroup g) {
	}

	@Override
	public void checkAwtEventQueueAccess() {
	}

	@Override
	public void checkConnect(
			String host,
			int port,
			Object context) {
	}

	@Override
	public void checkConnect(
			String host,
			int port) {
	}

	@Override
	public void checkCreateClassLoader() {
	}

	@Override
	public void checkDelete(
			String file) {
		throw new SecurityException();
	}

	@Override
	public void checkExec(String cmd) {
	}

	@Override
	public void checkExit(int status) {
	}

	@Override
	public void checkLink(String lib) {
	}

	@Override
	public void checkListen(int port) {
	}

	@Override
	public void checkMemberAccess(
			Class<?> clazz,
			int which) {
	}

	@Override
	public void checkMulticast(
			InetAddress maddr,
			byte ttl) {
	}

	@Override
	public void checkMulticast(
			InetAddress maddr) {
	}

	@Override
	public void checkPackageAccess(
			String pkg) {
	}

	@Override
	public void checkPackageDefinition(
			String pkg) {
	}

	@Override
	public void checkPermission(
			Permission perm,
			Object context) {
	}

	@Override
	public void checkPermission(
			Permission perm) {
	}

	@Override
	public void checkPrintJobAccess() {
	}

	@Override
	public void checkPropertiesAccess() {
	}

	@Override
	public void checkPropertyAccess(
			String key) {
	}

	@Override
	public void checkRead(
			FileDescriptor fd) {
	}

	@Override
	public void checkRead(
			String file,
			Object context) {
	}

	@Override
	public void checkRead(
			String file) {
	}

	@Override
	public void checkSecurityAccess(
			String target) {
	}

	@Override
	public void checkSetFactory() {
	}

	@Override
	public void checkSystemClipboardAccess() {
	}

	@Override
	public boolean checkTopLevelWindow(
			Object window) {
		return super.checkTopLevelWindow(window);
	}

	@Override
	protected int classDepth(
			String name) {
		return super.classDepth(name);
	}

	@Override
	protected int classLoaderDepth() {
		return super.classLoaderDepth();
	}

	@Override
	protected ClassLoader currentClassLoader() {
		return super.currentClassLoader();
	}

	@Override
	protected Class<?> currentLoadedClass() {
		return super.currentLoadedClass();
	}

	@Override
	protected Class[] getClassContext() {
		return super.getClassContext();
	}

	@Override
	public boolean getInCheck() {
		return super.getInCheck();
	}

	@Override
	public Object getSecurityContext() {
		return super.getSecurityContext();
	}

	@Override
	public ThreadGroup getThreadGroup() {
		return super.getThreadGroup();
	}

	@Override
	protected boolean inClass(
			String name) {
		return super.inClass(name);
	}

	@Override
	protected boolean inClassLoader() {
		return super.inClassLoader();
	}
}