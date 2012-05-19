package field.core.windowing;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.internal.cocoa.NSNotificationCenter;
import org.eclipse.swt.internal.cocoa.NSOpenGLContext;
import org.eclipse.swt.internal.cocoa.NSOpenGLPixelFormat;
import org.eclipse.swt.internal.cocoa.OS;
import org.eclipse.swt.opengl.GLData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;


import field.launch.SystemProperties;
import field.namespace.generic.ReflectionTools;

public class GLCanvas_field extends Canvas {
	NSOpenGLContext context;
	NSOpenGLPixelFormat pixelFormat;

	static final int MAX_ATTRIBUTES = 32;
	static final String GLCONTEXT_KEY = "org.eclipse.swt.internal.cocoa.glcontext"; //$NON-NLS-1$

	static GLCanvas_field firstCanvas = null;
	
	/**
	 * Create a GLCanvas widget using the attributes described in the GLData
	 * object provided.
	 * 
	 * @param parent
	 *                a composite widget
	 * @param style
	 *                the bitwise OR'ing of widget styles
	 * @param data
	 *                the requested attributes of the GLCanvas
	 * 
	 * @exception IllegalArgumentException
	 *                    <ul>
	 *                    <li>ERROR_NULL_ARGUMENT when the data is null
	 *                    <li>ERROR_UNSUPPORTED_DEPTH when the requested
	 *                    attributes cannot be provided
	 *                    </ul>
	 *                    </ul>
	 */
	public GLCanvas_field(Composite parent, int style, GLData data) {
		super(parent, style);
		if (data == null)
			SWT.error(SWT.ERROR_NULL_ARGUMENT);
		int attrib[] = new int[MAX_ATTRIBUTES];
		int pos = 0;

		if (SystemProperties.getIntProperty("forceSoftwareRenderer", 0) == 1) {
			attrib[pos++] = 70;
			attrib[pos++] = 0x00020200;
		}

		if (SystemProperties.getIntProperty("opengl32", 0) == 1) {
			attrib[pos++] = 99;
			attrib[pos++] = 0x3200;
		}
		
		

		if (data.doubleBuffer)
			attrib[pos++] = OS.NSOpenGLPFADoubleBuffer;

		if (data.stereo)
			attrib[pos++] = OS.NSOpenGLPFAStereo;

		
		/*
		 * Feature in Cocoa: NSOpenGL/CoreOpenGL only supports
		 * specifying the total number of bits in the size of the color
		 * component. If specified, the color size is the sum of the
		 * red, green and blue values in the GLData.
		 */
		if ((data.redSize + data.blueSize + data.greenSize) > 0) {
			attrib[pos++] = OS.NSOpenGLPFAColorSize;
			attrib[pos++] = data.redSize + data.greenSize + data.blueSize;
		}

		if (data.alphaSize > 0) {
			attrib[pos++] = OS.NSOpenGLPFAAlphaSize;
			attrib[pos++] = data.alphaSize;
		}

		if (data.depthSize > 0) {
			attrib[pos++] = OS.NSOpenGLPFADepthSize;
			attrib[pos++] = data.depthSize;
		}

		if (data.stencilSize > 0) {
			attrib[pos++] = OS.NSOpenGLPFAStencilSize;
			attrib[pos++] = data.stencilSize;
		}

		/*
		 * Feature in Cocoa: NSOpenGL/CoreOpenGL only supports
		 * specifying the total number of bits in the size of the color
		 * accumulator component. If specified, the color size is the
		 * sum of the red, green, blue and alpha accum values in the
		 * GLData.
		 */
		if ((data.accumRedSize + data.accumBlueSize + data.accumGreenSize) > 0) {
			attrib[pos++] = OS.NSOpenGLPFAAccumSize;
			attrib[pos++] = data.accumRedSize + data.accumGreenSize + data.accumBlueSize + data.accumAlphaSize;
		}

		if (data.sampleBuffers > 0) {
			attrib[pos++] = OS.NSOpenGLPFASampleBuffers;
			attrib[pos++] = data.sampleBuffers;
		}

		if (data.samples > 0) {
			attrib[pos++] = OS.NSOpenGLPFASamples;
			attrib[pos++] = data.samples;
		}

		attrib[pos++] = 0;

		pixelFormat = (NSOpenGLPixelFormat) new NSOpenGLPixelFormat().alloc();

		if (pixelFormat == null) {
			dispose();
			SWT.error(SWT.ERROR_UNSUPPORTED_DEPTH);
		}
		pixelFormat.initWithAttributes(attrib);

		NSOpenGLContext ctx = null;// data.shareContext != null ? data.shareContext.context : null;
			
//		ctx = firstCanvas == null ? null : firstCanvas.context;
		
		context = (NSOpenGLContext) new NSOpenGLContext().alloc();
		if (context == null) {
			dispose();
			SWT.error(SWT.ERROR_UNSUPPORTED_DEPTH);
		}
		context = context.initWithFormat(pixelFormat, ctx);
		setData(GLCONTEXT_KEY, context);

//		NSNotificationCenter.defaultCenter().addObserver(view, OS.sel_updateOpenGLContext_, OS.NSViewGlobalFrameDidChangeNotification, view);

		NSNotificationCenter d = NSNotificationCenter.defaultCenter();
		try {
			
			Object o1 = OS.class.getField("sel_updateOpenGLContext_").get(null);
			Object o2 = OS.class.getField("NSViewGlobalFrameDidChangeNotification").get(null);
			
			ReflectionTools.findFirstMethodCalled(d.getClass(), "addObserver").invoke(d, view, o1, o2, view);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		}
		
		Listener listener = new Listener() {
			public void handleEvent(Event event) {
				switch (event.type) {

				case SWT.Dispose:
					setData(GLCONTEXT_KEY, null);
					NSNotificationCenter.defaultCenter().removeObserver(view);

					if (context != null) {
						context.clearDrawable();
						context.release();
					}
					context = null;
					if (pixelFormat != null)
						pixelFormat.release();
					pixelFormat = null;
					break;
				}
			}
		};
		addListener(SWT.Dispose, listener);
		
		if (firstCanvas ==null) firstCanvas = this;
		
		
	}

	/**
	 * Returns a GLData object describing the created context.
	 * 
	 * @return GLData description of the OpenGL context attributes
	 * @exception SWTException
	 *                    <ul>
	 *                    <li>ERROR_WIDGET_DISPOSED - if the receiver has
	 *                    been disposed</li>
	 *                    <li>ERROR_THREAD_INVALID_ACCESS - if not called
	 *                    from the thread that created the receiver</li>
	 *                    </ul>
	 */
	public GLData getGLData() {
		checkWidget();
		// // GLData data = new GLData();
		// //
		// //// long /* int */[] value = new long /* int */[1];
		// // long /* int */[] value = new long /* int */[1];
		// //
		// // pixelFormat.getValues(value, OS.NSOpenGLPFADoubleBuffer,
		// 0);
		// // data.doubleBuffer = value[0] != 0;
		// // pixelFormat.getValues(value, OS.NSOpenGLPFAStereo, 0);
		// // data.stereo = value[0] != 0;
		// //
		// // pixelFormat.getValues(value, OS.NSOpenGLPFAAlphaSize, 0);
		// // data.alphaSize = (int/* 64 */) value[0];
		// //
		// // /*
		// // * Feature in Cocoa: NSOpenGL/CoreOpenGL only supports
		// // * specifying the total number of bits in the size of the
		// color
		// // * component. For compatibility we split the color size
		// less any
		// // * alpha into thirds and allocate a third to each color.
		// // */
		// // pixelFormat.getValues(value, OS.NSOpenGLPFAColorSize, 0);
		// //
		// // int colorSize = ((int/* 64 */) (value[0] -
		// data.alphaSize)) / 3;
		// //
		// // data.redSize = colorSize;
		// // data.greenSize = colorSize;
		// // data.blueSize = colorSize;
		// //
		// // pixelFormat.getValues(value, OS.NSOpenGLPFADepthSize, 0);
		// // data.depthSize = (int/* 64 */) value[0];
		// // pixelFormat.getValues(value, OS.NSOpenGLPFAStencilSize,
		// 0);
		// // data.stencilSize = (int/* 64 */) value[0];
		// //
		// // /*
		// // * Feature(?) in Cocoa: NSOpenGL/CoreOpenGL doesn't support
		// // * setting an accumulation buffer alpha, but has an alpha
		// if the
		// // * color values for the accumulation buffer were set.
		// Allocate
		// // * the values evenly in that case.
		// // */
		// // pixelFormat.getValues(value, OS.NSOpenGLPFAAccumSize, 0);
		// //
		// // int accumColorSize = (int/* 64 */) (value[0]) / 4;
		// // data.accumRedSize = accumColorSize;
		// // data.accumGreenSize = accumColorSize;
		// // data.accumBlueSize = accumColorSize;
		// // data.accumAlphaSize = accumColorSize;
		// //
		// // pixelFormat.getValues(value, OS.NSOpenGLPFASampleBuffers,
		// 0);
		// // data.sampleBuffers = (int/* 64 */) value[0];
		// // pixelFormat.getValues(value, OS.NSOpenGLPFASamples, 0);
		// // data.samples = (int/* 64 */) value[0];
		// return data;
		return null;

	}

	/**
	 * Returns a boolean indicating whether the receiver's OpenGL context is
	 * the current context.
	 * 
	 * @return true if the receiver holds the current OpenGL context, false
	 *         otherwise
	 * @exception SWTException
	 *                    <ul>
	 *                    <li>ERROR_WIDGET_DISPOSED - if the receiver has
	 *                    been disposed</li>
	 *                    <li>ERROR_THREAD_INVALID_ACCESS - if not called
	 *                    from the thread that created the receiver</li>
	 *                    </ul>
	 */
	public boolean isCurrent() {
		checkWidget();
		NSOpenGLContext current = NSOpenGLContext.currentContext();
		return current != null && current.id == context.id;
	}

	/**
	 * Sets the OpenGL context associated with this GLCanvas to be the
	 * current GL context.
	 * 
	 * @exception SWTException
	 *                    <ul>
	 *                    <li>ERROR_WIDGET_DISPOSED - if the receiver has
	 *                    been disposed</li>
	 *                    <li>ERROR_THREAD_INVALID_ACCESS - if not called
	 *                    from the thread that created the receiver</li>
	 *                    </ul>
	 */
	public void setCurrent() {
//		checkWidget();
		context.makeCurrentContext();
//		System.out.println(GLContext.getCapabilities());
	}

	/**
	 * Swaps the front and back color buffers.
	 * 
	 * @exception SWTException
	 *                    <ul>
	 *                    <li>ERROR_WIDGET_DISPOSED - if the receiver has
	 *                    been disposed</li>
	 *                    <li>ERROR_THREAD_INVALID_ACCESS - if not called
	 *                    from the thread that created the receiver</li>
	 *                    </ul>
	 */
	public void swapBuffers() {
//		checkWidget();
		context.flushBuffer();
	}
	
	
	
}
