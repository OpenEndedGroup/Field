package field.bytecode.protect;

import java.io.FileDescriptor;
import java.net.InetAddress;
import java.security.Permission;

public class NoopSecurityManager extends SecurityManager {

	@Override
	public void checkAccept(String arg0, int arg1) {
	}

	@Override
	public void checkAccess(Thread arg0) {
	}

	@Override
	public void checkAccess(ThreadGroup arg0) {
	}

	@Override
	public void checkAwtEventQueueAccess() {
	}

	@Override
	public void checkConnect(String arg0, int arg1, Object arg2) {
	}

	@Override
	public void checkConnect(String arg0, int arg1) {
	}

	@Override
	public void checkCreateClassLoader() {
	}

	@Override
	public void checkDelete(String arg0) {
	}

	@Override
	public void checkExec(String arg0) {
	}

	@Override
	public void checkExit(int arg0) {
	}

	@Override
	public void checkLink(String arg0) {
	}

	@Override
	public void checkListen(int arg0) {
	}

	@Override
	public void checkMemberAccess(Class<?> arg0, int arg1) {
	}

	@Override
	public void checkMulticast(InetAddress arg0, byte arg1) {
	}

	@Override
	public void checkMulticast(InetAddress arg0) {
	}

	@Override
	public void checkPackageAccess(String arg0) {
	}

	@Override
	public void checkPackageDefinition(String arg0) {
	}

	@Override
	public void checkPermission(Permission arg0, Object arg1) {
	}

	@Override
	public void checkPermission(Permission arg0) {
	}

	@Override
	public void checkPrintJobAccess() {
	}

	@Override
	public void checkPropertiesAccess() {
	}

	@Override
	public void checkPropertyAccess(String arg0) {
	}

	@Override
	public void checkRead(FileDescriptor arg0) {
	}

	@Override
	public void checkRead(String arg0, Object arg1) {
	}

	@Override
	public void checkRead(String arg0) {
	}

	@Override
	public void checkSecurityAccess(String arg0) {
	}

	@Override
	public void checkSetFactory() {
	}

	@Override
	public void checkSystemClipboardAccess() {
	}

	@Override
	public boolean checkTopLevelWindow(Object arg0) {
		return true;
	}

	@Override
	public void checkWrite(FileDescriptor arg0) {
	}

	@Override
	public void checkWrite(String arg0) {
	}

	@Override
	protected int classDepth(String arg0) {
		return super.classDepth(arg0);
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
		return true;
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
	protected boolean inClass(String arg0) {
		return true;
	}

	@Override
	protected boolean inClassLoader() {
		return true;
	}

}
