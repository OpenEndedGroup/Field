package field.graphics.jfbxlib;

import java.net.URL;

import field.bytecode.protect.Trampoline2;

public class JFBXMain {

	static
	{
//		URL libAt = Trampoline2.trampoline.getClassLoader().getResource("libjfbxlib.dylib");
//		String path = libAt.getPath();
//		System.load(path);
		
		System.loadLibrary("jfbxlib");
		
	}

	public native void acceptTime(long time, JFBXVisitor visitor);

	public native int getNumTakes();

	public native long getTakeEndTime();

	public native long getTakeStartTime();

	public native void importFile(String filename);

	public native void noMoreGeometry();
	public native void moreGeometry();

	public native int setTake(int take);

}
