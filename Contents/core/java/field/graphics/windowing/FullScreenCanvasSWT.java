package field.graphics.windowing;

import static org.lwjgl.opengl.GL11.GL_BACK;
import static org.lwjgl.opengl.GL11.GL_BACK_LEFT;
import static org.lwjgl.opengl.GL11.GL_BACK_RIGHT;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glColorMask;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawBuffer;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glFinish;
import static org.lwjgl.opengl.GL11.glFlush;
import static org.lwjgl.opengl.GL11.glReadPixels;
import static org.lwjgl.opengl.GL13.GL_MULTISAMPLE;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.KHRDebug.GL_DEBUG_OUTPUT_SYNCHRONOUS;
import static org.lwjgl.opengl.KHRDebug.glDebugMessageCallback;
import static org.lwjgl.opengl.KHRDebug.glDebugMessageControl;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;

import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.opengl.GLCanvas;
import org.eclipse.swt.opengl.GLData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.KHRDebugCallback;
import org.lwjgl.opengl.KHRDebugCallback.Handler;
import org.lwjgl.opengl.OpenGLException;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.DispatchOverTopology;
import field.bytecode.protect.annotations.HiddenInAutocomplete;
import field.bytecode.protect.annotations.NextUpdate;
import field.bytecode.protect.dispatch.Cont;
import field.bytecode.protect.dispatch.Cont.ReturnCode;
import field.bytecode.protect.dispatch.Cont.aRun;
import field.core.Platform;
import field.core.plugins.drawing.opengl.OnCanvasLines;
import field.core.plugins.drawing.threed.ThreedContext.iThreedDrawingSurface;
import field.core.util.PythonCallableMap;
import field.core.windowing.LinuxCanvasInterface;
import field.core.windowing.iCanvasInterface;
import field.graphics.core.Base;
import field.graphics.core.Base.iAcceptsSceneListElement;
import field.graphics.core.Base.iSceneListElement;
import field.graphics.core.BasicCamera;
import field.graphics.core.BasicContextManager;
import field.graphics.core.BasicSceneList;
import field.graphics.core.BasicUtilities;
import field.graphics.core.BasicUtilities.Clear;
import field.graphics.core.CoreHelpers;
import field.graphics.core.DynamicFrameRateCuller;
import field.graphics.core.ResourceMonitor;
import field.graphics.core.StereoCamera;
import field.graphics.core.When;
import field.launch.Launcher;
import field.launch.SystemProperties;
import field.launch.iUpdateable;
import field.math.BaseMath;
import field.math.linalg.Vector2;
import field.math.linalg.Vector3;
import field.namespace.context.SimpleContextTopology;
import field.namespace.generic.ReflectionTools;
import field.util.MiscNative;
import field.util.TaskQueue;

@Woven
public class FullScreenCanvasSWT implements iUpdateable, iThreedDrawingSurface, iAcceptsSceneListElement {

	@HiddenInAutocomplete
	static public final Method method_beforeFlush = ReflectionTools.methodOf("beforeFlush", FullScreenCanvasSWT.class);
	@HiddenInAutocomplete
	static public final Method method_beforeLeftFlush = ReflectionTools.methodOf("beforeLeftFlush", FullScreenCanvasSWT.class);
	@HiddenInAutocomplete
	static public final Method method_beforeRightFlush = ReflectionTools.methodOf("beforeRightFlush", FullScreenCanvasSWT.class);

	@HiddenInAutocomplete
	static public final Method method_display = ReflectionTools.methodOf("display", FullScreenCanvasSWT.class);

	@HiddenInAutocomplete
	static public final SimpleContextTopology windowContextTree = new SimpleContextTopology();

	@HiddenInAutocomplete
	static public int uniq = 0;

	@HiddenInAutocomplete
	static public int[] dimensions = { 0, 0, 0, 0 };

	@HiddenInAutocomplete
	public String contextName = "fullScreenCanvas+" + (uniq++);

	@HiddenInAutocomplete
	public CoordinateFrameCamera keyboardCamera;

	private final Shell frame;

	protected BasicCamera camera;

	protected BasicSceneList rootSceneList;

	protected BasicSceneList sceneList;
	protected BasicSceneList leftSceneList;
	protected BasicSceneList rightSceneList;

	protected Clear clear;

	int frameNumber = 0;

	boolean stereo = SystemProperties.getIntProperty("stereo", 0) == 1;
	boolean leftOnly = SystemProperties.getIntProperty("leftOnly", 0) == 1;
	boolean anaglyph = SystemProperties.getIntProperty("anaglyph", 0) == 1;
	boolean sequentialStereo = SystemProperties.getIntProperty("sequentialStereo", 0) == 1;
	@HiddenInAutocomplete
	public boolean passiveStereo = SystemProperties.getIntProperty("passiveStereo", 0) == 1;
	@HiddenInAutocomplete
	static public boolean doMultisampling = SystemProperties.getIntProperty("needsFSAA", 0) == 1;

	@HiddenInAutocomplete
	public OnCanvasLines onCanvasPLine;

	@HiddenInAutocomplete
	static public FullScreenCanvasSWT currentCanvas = null;

	public FullScreenCanvasSWT() {
		this(false);
	}

	DynamicFrameRateCuller culler = new DynamicFrameRateCuller();
	// private GLCanvas_field canvas;
	private Canvas canvas;
	public iCanvasInterface canvasInterface;
	private final boolean inAWindow;

	public float extraShiftX = 0;

	public FullScreenCanvasSWT(boolean inAWindow) {

		this.inAWindow = inAWindow;
		GraphicsDevice[] devs = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
		GraphicsDevice dev = devs[0];
		for (GraphicsDevice device : devs) {
			if (device != GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()) {
				dev = device;
				break;
			}
		}

		if (SystemProperties.getIntProperty("onMainScreen", 0) == 1)
			dev = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

		java.awt.Rectangle bb = dev.getConfigurations()[0].getBounds();

		frame = new Shell(Launcher.display, inAWindow ? (SWT.SHELL_TRIM | SWT.RESIZE) : SWT.NO_TRIM);
		frame.setBackground(Launcher.getLauncher().display.getSystemColor(SWT.COLOR_BLACK));
		frame.setBounds(new Rectangle(bb.x, bb.y, bb.width, bb.height));

		int h = bb.height;
		int w = bb.width;

		if (SystemProperties.getIntProperty("halfScreen", 0) == 1) {

			w = 1050;
			h = 1600;

			frame.setBounds(new Rectangle(0, 0, w, h));
		}
		if (SystemProperties.getIntProperty("centerPortrait", 0) == 1) {

			w = 1024;
			h = 2048;

			frame.setBounds(new Rectangle(-1200 + (1200 - 1024) / 2, frame.getSize().y - (2048 - 1920), w, h));
		}
		if (SystemProperties.getIntProperty("twoK", 0) == 1) {

			w = 2560;
			h = 1600;

			frame.setBounds(new Rectangle(0, 0, w, h));
		}
		if (SystemProperties.getProperty("rect") != null && !SystemProperties.getProperty("rect").equals("")) {
			String p = SystemProperties.getProperty("rect");
			String[] parts = p.split("\\.");

			int x = Integer.parseInt(parts[0]);
			int y = Integer.parseInt(parts[1]);
			w = Integer.parseInt(parts[2]);
			h = Integer.parseInt(parts[3]);

			frame.setBounds(new Rectangle(x, y, w, h));

		}

		;// System.out.println(" dev is <" + dev.getClass() + "> ");

		;// System.out.println(" rect is <" + frame.getBounds() + ">");
		boolean z = dev.isFullScreenSupported();

		;// System.out.println(" full screen is supported <" + z +
			// "> : stereo ? " + stereo);

		final Composite comp = new Composite(frame, SWT.NONE);
		comp.setLayout(new FillLayout());
		comp.setSize(frame.getSize().x, frame.getSize().y);

		GLData data = new GLData();
		data.doubleBuffer = true;
		data.depthSize = 24;

		data.stencilSize = 8;

		data.stereo = stereo;
		if (doMultisampling) {
			data.samples = 2;
			// data.sampleBuffers = 2;
		}

		if (Platform.isMac()) {

			// canvas = new GLCanvas_field(hsplit,
			// SWT.NO_BACKGROUND, data);
			// canvasInterface = new MacOSXCanvasInterface(canvas);

			// Composite parent, int style, GLData data
			try {
				canvas = (Canvas) this.getClass().getClassLoader().loadClass("field.core.windowing.GLCanvas_field").getConstructor(Composite.class, Integer.TYPE, GLData.class).newInstance(comp, SWT.NONE, data);
				canvasInterface = (iCanvasInterface) this.getClass().getClassLoader().loadClass("field.core.windowing.MacOSXCanvasInterface").getConstructor(Canvas.class).newInstance(canvas);
			} catch (Throwable t) {
				t.printStackTrace();
				throw new IllegalStateException("gl canvas failed to init. This is fatal");
			}

		} else {
			canvas = new GLCanvas(comp, SWT.NONE, data);
			canvasInterface = new LinuxCanvasInterface(canvas);
		}

		// canvas = new GLCanvas_field(comp, SWT.NONE, data);
		canvasInterface.setCurrent();

		try {
			GLContext.useContext(canvas);
		} catch (LWJGLException e1) {
			e1.printStackTrace();
		}

		// glClearColor(0, 0, 0, 1);
		// glClear(GL_COLOR_BUFFER_BIT);
		frame.addListener(SWT.Resize, new Listener() {

			@Override
			public void handleEvent(Event event) {
				;// System.out.println(" client area of frame is <"
					// + frame.getClientArea() + ">");
				comp.setSize(frame.getClientArea().width, frame.getClientArea().height);
				canvas.setSize(frame.getClientArea().width, frame.getClientArea().height);

				canvasInterface.setCurrent();
				try {
					GLContext.useContext(canvas);
				} catch (LWJGLException e) {
					e.printStackTrace();
				}

				Rectangle bounds = canvas.getBounds();
				FullScreenCanvasSWT.this.reshape(bounds.x, bounds.y, bounds.width, bounds.height);

			}
		});

		createInitialLists();

		if (SystemProperties.getIntProperty("	", 0) == 1)
			new MiscNative().enterKiosk_safe();

		dimensions[0] = 0;
		dimensions[1] = 0;
		dimensions[2] = w;
		dimensions[3] = h;

		if (SystemProperties.getIntProperty("nocursor", 0) == 1) {
			// getCanvas().setCursor(Launcher.display.getSystemCursor(SWT.));
			ImageData dd = new ImageData("/Users/marc/Desktop/black.png");
			Cursor c = new Cursor(Launcher.display, dd, 0, 0);
			getCanvas().setCursor(c);
			// int[] pixels = new int[16 * 16];
			// Image image =
			// Toolkit.getDefaultToolkit().createImage(new
			// MemoryImageSource(16, 16, pixels, 0, 16));
			// Cursor transparentCursor =
			// Toolkit.getDefaultToolkit().createCustomCursor(image,
			// new Point(0, 0), "nothing");
			// frame.setCursor(transparentCursor);
			// canvas.setCursor(transparentCursor);
		}

	}

	@HiddenInAutocomplete
	@DispatchOverTopology(topology = Cont.class)
	public void beforeFlush() {
		postQueue.update();
	}

	@HiddenInAutocomplete
	@DispatchOverTopology(topology = Cont.class)
	public void beforeLeftFlush() {
	}

	@HiddenInAutocomplete
	@DispatchOverTopology(topology = Cont.class)
	public void beforeRightFlush() {
	}

	@HiddenInAutocomplete
	static public int side = 0;

	@HiddenInAutocomplete
	public boolean doLeft = true;
	@HiddenInAutocomplete
	public boolean doRight = true;

	@HiddenInAutocomplete
	static public boolean totalFlip = false;

	@HiddenInAutocomplete
	public boolean totalFlip_instance = false;

	@HiddenInAutocomplete
	static public int vertexCount = 0;
	@HiddenInAutocomplete
	static public int triangleCount = 0;

	@HiddenInAutocomplete
	public static int dropFrame = 0;

	@HiddenInAutocomplete
	boolean neverDisplay = false;

	int flushStrobe = 1;

	boolean first = true;

	@DispatchOverTopology(topology = Cont.class)
	@HiddenInAutocomplete
	public void display() {

		if (neverDisplay)
			return;

		vertexCount = 0;
		triangleCount = 0;

		if (first) {
			System.out.println(" Setting up debug stream ");
			first = false;
			glEnable(GL_DEBUG_OUTPUT_SYNCHRONOUS);
			glDebugMessageCallback(new KHRDebugCallback(new Handler() {

				@Override
				public void handleMessage(int arg0, int arg1, int arg2, int arg3, String arg4) {
					System.out.println(" handle message :" + arg0 + " " + arg1 + " " + arg2 + " " + arg3 + " " + arg4);
				}
			}));
			glDebugMessageControl(GL11.GL_DONT_CARE, GL11.GL_DONT_CARE, GL11.GL_DONT_CARE, null, true);
		}

		if (doMultisampling) {
			glEnable(GL_MULTISAMPLE);
		}

		// ;//System.out.println(" frame number "+this.frameNumber);

		// if (this.frameNumber==10)
		// {
		// ;//System.out.println(" going multithreaded ?");
		// new MiscNative().goMultithreadedRenderer();
		// }

		// ;//System.out.println(" -- main display --");

		currentCanvas = this;
		try {

			// if (!frame.isVisible())
			// return;
			// ;//System.out.println(" -- in");

			try {

				if (stereo) {
					culler.enter();
					// ;//System.out.println(" -- in ");
					if (leftOnly)
						side = 1;

					for (int i = 0; i < 2; i++) {
						windowContextTree.begin(getContextName());
						side++;

						// arg0.getGL().
						try {
							if (!leftOnly)
								glEnable(GL11.GL_STEREO);
							else
								glDisable(GL11.GL_STEREO);
						} catch (OpenGLException e) {
						}

						if (side % 2 == 0)
							glDrawBuffer((totalFlip | totalFlip_instance) ? GL_BACK_RIGHT : GL_BACK_LEFT);
						else
							glDrawBuffer((totalFlip | totalFlip_instance) ? GL_BACK_LEFT : GL_BACK_RIGHT);

						if (leftOnly)
							glDrawBuffer(GL_BACK);

						BasicContextManager.setCurrentContextFor(this, new Object(), new Object());

						rootSceneList.update();

						if (side % 2 == 0) {
							// ;//System.out.println(" -- left");
							if (anaglyph)
								glColorMask(true, false, false, true);
							if (doLeft) {
								leftSceneList.update();
								beforeLeftFlush();
							}
						} else {
							// ;//System.out.println(" -- right");
							if (anaglyph)
								glColorMask(false, true, true, true);
							if (doRight) {
								rightSceneList.update();
								beforeRightFlush();
							}
						}

						// if (leftOnly)
						// beforeRightFlush();

						// ;//System.out.println(" -- both");

						beforeFlush();
						windowContextTree.end(getContextName());

						// byte[] bb = new byte[1];
						// arg0.getGL().glGetBooleanv(GL.GL_STEREO,
						// bb,
						// 0);
						// ;//System.out.println(" ?? stereo ? :"
						// + bb[0]);

						if (side % 2 == 0 || leftOnly)
							if (doFlush()) {
								canvasInterface.swapBuffers();
								// canvasInterface.swapBuffers();
								// arg0.getGL().glFinish();
							}
						if (leftOnly)
							break;
					}
					frameNumber++;
					culler.exit();
					// ;//System.out.println(" -- out");

				} else if (passiveStereo) {
					culler.enter();
					// ;//System.out.println(" -- in ");
					if (leftOnly)
						side = 1;

					for (int i = 0; i < 2; i++) {
						windowContextTree.begin(getContextName());
						side++;

						BasicContextManager.setCurrentContextFor(this, new Object(), new Object());
						// ;//System.out.println(" -- clear passive --");
						rootSceneList.update();

						if (side % 2 == 0) {
							if (doLeft) {
								// ;//System.out.println(" >> left ");
								leftSceneList.update();
								beforeLeftFlush();
								// ;//System.out.println(" << left ");
							}
						} else {
							if (doRight) {
								// ;//System.out.println(" >> right");
								rightSceneList.update();
								beforeRightFlush();
								// ;//System.out.println(" << right");
							}
						}

						beforeFlush();
						windowContextTree.end(getContextName());

						if (side % 2 == 0 || leftOnly)
							if (doFlush()) {
								// ;//System.out.println(" :: flush ");
								if (dropFrame > 0) {
									dropFrame--;
								} else {
									canvasInterface.swapBuffers();
								}
							}
						if (leftOnly)
							break;
					}
					frameNumber++;
					culler.exit();
					// ;//System.out.println(" -- out");

				} else {
					windowContextTree.begin(getContextName());
					BasicContextManager.setCurrentContextFor(this, new Object(), new Object());

					rootSceneList.update();
					sceneList.update();

					beforeLeftFlush();
					beforeFlush();
					windowContextTree.end(getContextName());

					// byte[] bb = new byte[1];
					// arg0.getGL().glGetBooleanv(GL.GL_STEREO,
					// bb,
					// 0);
					// ;//System.out.println(" ?? stereo ? :"
					// +
					// bb[0]);

					if (side % 2 == 0)
						if (doFlush()) {
							// canvasInterface.swapBuffers();
							canvasInterface.swapBuffers();
						}

					frameNumber++;
				}

			} catch (Throwable e) {
				e.printStackTrace();
				throw (IllegalArgumentException) new IllegalArgumentException().initCause(e);
			}

		} finally {
			currentCanvas = null;
			CoreHelpers.backOutStacks();
		}
	}

	public enum StereoSide {
		middle(0), left(-1), right(1);

		public float x;

		StereoSide(float x) {
			this.x = x;
		}
	}

	public Vector2 cameraTileShift = new Vector2();

	/**
	 * returns the stereo side that we're currently rendering (or
	 * StereoSide.middle if we aren't rendering stereo).
	 */
	public static StereoSide getSide() {

		if (currentCanvas == null) {
			return StereoSide.middle;
		}

		if (!(currentCanvas.stereo || currentCanvas.sequentialStereo || currentCanvas.passiveStereo))
			return StereoSide.middle;

		return (currentCanvas.side % 2 == 0 ? StereoSide.left : StereoSide.right);
	}

	/**
	 * returns the Camera associated with this canvas
	 */
	public BasicCamera getCamera() {
		return camera;
	}

	@HiddenInAutocomplete
	public Canvas getCanvas() {
		return canvas;
	}

	@HiddenInAutocomplete
	public String getContextName() {
		return contextName;
	}

	/**
	 * returns the SWT Shell (i.e. window) associated with this canvas
	 */
	public Shell getFrame() {
		return frame;
	}

	@HiddenInAutocomplete
	public int getFrameNumber() {
		return frameNumber;
	}

	/**
	 * returns the main scenelist � the list of things to draw
	 */
	public BasicSceneList getSceneList() {
		return sceneList;
	}

	@HiddenInAutocomplete
	public void overwriteCamera(BasicCamera c) {
		this.camera = c;
	}

	@HiddenInAutocomplete
	public void registerKeyboadEventDelegate(KeyListener listener) {
		canvas.addKeyListener(listener);
	}

	@HiddenInAutocomplete
	public void registerMouseEventDelegate(MouseEventDelegate delegate) {
		canvas.addMouseListener(delegate);
		canvas.addMouseMoveListener(delegate);
		canvas.addMouseWheelListener(delegate);

		// T_TransWindow.notDraggable(frame);

		// notDraggable(frame);
	}

	/**
	 * hides or shows this window
	 */
	public FullScreenCanvasSWT setVisible(boolean b) {
		frame.setRegion(new Region());
		frame.setVisible(b);
		andVisible();
		return this;
	}

	@NextUpdate(delay = 3)
	public void andVisible() {
		Region r = new Region();
		r.add(0, 0, width(), height());
		frame.setRegion(r);
	}

	@HiddenInAutocomplete
	public void update() {
		canvasInterface.setCurrent();
		try {
			GLContext.useContext(canvas);
		} catch (LWJGLException e) {
			e.printStackTrace();
		}
		display();

	}

	@HiddenInAutocomplete
	public FullScreenCanvasSWT withKeyboardControlledCamera_smooth() {
		keyboardCamera = new KeyboardControlledCamera_smooth(this.getCamera());
		((KeyboardControlledCamera_smooth) keyboardCamera).setCanvas(this);
		this.registerKeyboadEventDelegate((KeyboardControlledCamera_smooth) keyboardCamera);
		canvas.addGestureListener((KeyboardControlledCamera_smooth) keyboardCamera);
		Launcher.getLauncher().registerUpdateable(keyboardCamera);
		keyboardCamera.changeDoSync(true);
		return this;
	}

	protected void createInitialLists() {
		// defaults for color
		double r = SystemProperties.getDoubleProperty("background.red", 0.0);
		double g = SystemProperties.getDoubleProperty("background.green", 0.0);
		double b = SystemProperties.getDoubleProperty("background.blue", 0.0);
		double a = SystemProperties.getDoubleProperty("background.alpha", 0);

		boolean doClear = SystemProperties.getIntProperty("background.clear", 1) == 1;

		camera = (stereo || sequentialStereo || passiveStereo) ? new StereoCamera() : new BasicCamera();
		// camera = new BasicCamera();

		rootSceneList = new BasicSceneList();
		rootSceneList.addChild(new ResourceMonitor() {
			@Override
			public String toString() {
				return "resource monitor for fullscreen canvas";
			}
		});
		rootSceneList.addChild(new BasicUtilities.Standard());
		if (doClear) {
			clear = new BasicUtilities.Clear(new Vector3(r, g, b), (float) a);
			rootSceneList.addChild(clear);
		} else {
			rootSceneList.addChild(new BasicUtilities.ClearOnce(new Vector3(0, 0, 0), 1));
		}
		rootSceneList.addChild(camera);
		sceneList = leftSceneList = new BasicSceneList();
		rightSceneList = new BasicSceneList();
	}

	/**
	 * sets which scene list side we are currently building
	 */
	public void setSceneListSide(boolean left) {
		sceneList = left ? leftSceneList : rightSceneList;
	}

	@HiddenInAutocomplete
	protected void displayChanged(boolean arg1, boolean arg2) {
		glClearColor(0, 0, 0, 0);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
	}

	@HiddenInAutocomplete
	protected boolean doFlush() {
		return frameNumber % flushStrobe == 0;
	}

	@HiddenInAutocomplete
	protected void reshape(int x, int y, int width, int height) {
		camera.setViewport(x, y, width, height);
		if (passiveStereo)
			camera.setPerspective(camera.getFov(), width / (float) height, camera.getNear(), camera.getFar());
		else
			camera.setPerspective(camera.getFov(), width / (float) height, camera.getNear(), camera.getFar());

	}

	@HiddenInAutocomplete
	public void transformDrawingToWindow(Vector2 drawing) {
	}

	/**
	 * returns a scene list that you can use to attach things to "both eyes"
	 * in a stereo canvas
	 */
	public iAcceptsSceneListElement getBothEyes() {
		return new iAcceptsSceneListElement() {
			public void addChild(iSceneListElement e) {
				leftSceneList.addChild(e);
				rightSceneList.addChild(e);
			}

			public boolean isChild(iSceneListElement e) {
				return leftSceneList.getChildren().contains(e) || rightSceneList.getChildren().contains(e);
			}

			public void removeChild(iSceneListElement e) {
				leftSceneList.removeChild(e);
				rightSceneList.removeChild(e);

			}

		};
	}

	/**
	 * adds something to the list of things to be drawn. You can also use
	 * the << operator
	 */
	public void addChild(iSceneListElement e) {
		getSceneList().addChild(e);
	}

	/** returns true if this is already in the list of things to be drawn */
	public boolean isChild(iSceneListElement e) {
		return getSceneList().isChild(e);
	}

	/**
	 * removes something from the list of things to be drawn. You can also
	 * use the | operator
	 */
	public void removeChild(iSceneListElement e) {
		getSceneList().removeChild(e);
	}

	@HiddenInAutocomplete
	public OnCanvasLines getOnCanvasPLine() {
		if (onCanvasPLine == null) {
			onCanvasPLine = new OnCanvasLines(this.getBothEyes(), this);
		}
		return onCanvasPLine;
	}

	/**
	 * returns an interface to the FLine drawing system that draws to this
	 * canvas
	 */
	public OnCanvasLines getOnCanvasLines() {
		if (onCanvasPLine == null) {
			onCanvasPLine = new OnCanvasLines(this.getBothEyes(), this);
		}
		return onCanvasPLine;
	}

	/**
	 * returns an array of attached FLines that are being drawn on this
	 * canvas. Add to , remove from and clear this array to change what is
	 * being drawn
	 */
	public List<Object> lines() {
		return getOnCanvasPLine().submit;
	}

	/**
	 * use to register a callback that is called on mouse move over this
	 * canvas. It will be called with a MouseEvent argument
	 */
	public PythonCallableMap onMouseMove = new PythonCallableMap();
	/**
	 * use to register a callback that is called on mouse press over this
	 * canvas. It will be called with a MouseEvent argument
	 */
	public PythonCallableMap onMousePress = new PythonCallableMap();
	/**
	 * use to register a callback that is called on mouse double-click over
	 * this canvas. It will be called with a MouseEvent argument
	 */
	public PythonCallableMap onMouseDoubleClick = new PythonCallableMap();
	/**
	 * use to register a callback that is called on mouse drag over this
	 * canvas. It will be called with a MouseEvent argument
	 */
	public PythonCallableMap onMouseDrag = new PythonCallableMap();
	/**
	 * use to register a callback that is called on mouse release over this
	 * canvas. It will be called with a MouseEvent argument
	 */
	public PythonCallableMap onMouseRelease = new PythonCallableMap();
	/**
	 * use to register a callback that is called on mouse scroll over this
	 * canvas. It will be called with a MouseEvent argument
	 */
	public PythonCallableMap onMouseScroll = new PythonCallableMap();

	/**
	 * use to register a callback that is called on key press over this
	 * canvas. It will be called with a KeyEvent argument
	 */
	public PythonCallableMap onKeyPressed = new PythonCallableMap();
	/**
	 * use to register a callback that is called on key release over this
	 * canvas. It will be called with a KeyEvent argument
	 */
	public PythonCallableMap onKeyReleased = new PythonCallableMap();

	@HiddenInAutocomplete
	public void addDefaultHandlers() {
		this.registerMouseEventDelegate(new MouseEventDelegate() {

			boolean down;

			@Override
			public void mouseDoubleClick(MouseEvent arg0) {
				onMouseDoubleClick.invoke(arg0);
			}

			@Override
			public void mouseDown(MouseEvent arg0) {
				down = true;
				onMousePress.invoke(arg0);
			}

			@Override
			public void mouseUp(MouseEvent arg0) {
				down = false;
				onMouseRelease.invoke(arg0);
			}

			@Override
			public void mouseMove(MouseEvent arg0) {
				if (down)
					onMouseDrag.invoke(arg0);
				else
					onMouseMove.invoke(arg0);
			}

			@Override
			public void mouseScrolled(MouseEvent arg0) {
				onMouseScroll.invoke(arg0);
			}

		});

		this.registerKeyboadEventDelegate(new KeyListener() {

			@Override
			public void keyPressed(org.eclipse.swt.events.KeyEvent arg0) {
				onKeyPressed.invoke(arg0);
			}

			@Override
			public void keyReleased(org.eclipse.swt.events.KeyEvent arg0) {
				onKeyReleased.invoke(arg0);
			}

		});
	}

	When when;

	/**
	 * returns a decorator that schedules a function to be called at a
	 * particular point in the rendering. For example @canvas.add(0)
	 * installs a function at the start of the rendering cycle.
	 */
	public PythonCallableMap add(Base.StandardPass pass) {
		return getWhen().getMap(pass);
	}

	/**
	 * returns a decorator that schedules a function to be called at a
	 * particular point in the rendering. For example @canvas.add(0)
	 * installs a function at the start of the rendering cycle.
	 */
	public PythonCallableMap add(int pass) {
		return getWhen().getMap(pass);
	}

	@HiddenInAutocomplete
	public When getWhen() {
		if (when == null)
			when = new When(this.getSceneList());
		return when;
	}

	TaskQueue postQueue = new TaskQueue();

	@HiddenInAutocomplete
	public TaskQueue getPostQueue() {
		return postQueue;
	}

	/**
	 * returns which stereo side we are building (or middle if we are not a
	 * stereo canvas
	 */
	public StereoSide getSceneListSide() {
		if (!(this.stereo || this.sequentialStereo || this.passiveStereo))
			return StereoSide.middle;
		return sceneList == leftSceneList ? StereoSide.left : StereoSide.right;
	}

	@HiddenInAutocomplete
	public Method getCurrentFlushMethod() {
		if (getSceneListSide() == StereoSide.middle)
			return method_beforeFlush;
		return getSceneListSide() == StereoSide.left ? method_beforeLeftFlush : method_beforeRightFlush;
	}

	@HiddenInAutocomplete
	public FullScreenCanvasSWT pressSpaceToSave(final String directory) {
		registerKeyboadEventDelegate(new KeyListener() {
			int n = 0;

			@Override
			public void keyPressed(org.eclipse.swt.events.KeyEvent e) {
				if (e.keyCode == ' ' && ((e.stateMask & SWT.ALT) != 0)) {
					String filename = directory + "save" + pad(n) + ".png";
					while (new File(filename).exists()) {
						n++;
						filename = directory + "save" + pad(n) + ".png";
					}
					;// System.out.println(" filename is <"
						// + filename + ">");
					saveAsPNG(filename);
				}
			}

			private String pad(int n) {
				String q = "" + n;
				while (q.length() < 5)
					q = "0" + q;
				return q;
			}

			@Override
			public void keyReleased(org.eclipse.swt.events.KeyEvent e) {
			}
		});
		return this;
	}

	/** saves the canvas as a png file */
	public void saveAsPNG(final String filename) {
		aRun r = new aRun() {
			@Override
			public ReturnCode tail(Object calledOn, Object[] args, Object returnWas) {

				;// System.out.println(" saving to <" + filename
					// + ">");
				try {

					glBindFramebuffer(GL_FRAMEBUFFER, 0);
					glFlush();
					glFinish();

					int width = canvas.getSize().x;
					int height = canvas.getSize().y;
					ByteBuffer storage = ByteBuffer.allocateDirect(width * height * 4);

					glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, storage);
					BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

					WritableRaster tile = bi.getWritableTile(0, 0);
					DataBuffer buffer = tile.getDataBuffer();

					IntBuffer storagei = storage.asIntBuffer();
					for (int y = 0; y < height; y++) {
						for (int x = 0; x < width; x++) {
							int r = BaseMath.intify(storage.get());
							int g = BaseMath.intify(storage.get());
							int b = BaseMath.intify(storage.get());
							int a = BaseMath.intify(storage.get());

							a = 255;

							int d = (g << 8) | (r << 16) | (b) | (a << 24);
							buffer.setElem((height - 1 - y) * width + x, d);
						}
					}

					FileOutputStream fos;
					RenderedOp op = JAI.create("filestore", bi, filename, "PNG");
				} finally {
					Cont.unlinkWith(FullScreenCanvasSWT.this, method_beforeFlush, this);
				}
				return ReturnCode.cont;
			}
		};

		if (!(stereo || sequentialStereo || passiveStereo))
			Cont.linkWith(this, method_beforeLeftFlush, r);
		else
			Cont.linkWith(this, method_beforeFlush, r);
	}

	/**
	 * same as getFrame().getSize().x
	 */
	public int width() {
		return getCanvas().getSize().x;
	}

	/**
	 * same as getFrame().getSize().y
	 */
	public int height() {
		return getCanvas().getSize().y;
	}

}
