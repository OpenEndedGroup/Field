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
		throw new SecurityException();
	}

	@Override
	public void checkWrite(
			String fd) {
		
		if (fd.startsWith(System.getProperty("java.io.tmpdir"))) return;
		
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