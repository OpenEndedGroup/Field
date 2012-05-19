package field.extras.plugins.processing;

import java.applet.AppletContext;
import java.applet.AppletStub;
import java.applet.AudioClip;
import java.awt.AWTEvent;
import java.awt.AWTException;
import java.awt.AWTKeyStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.FocusTraversalPolicy;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.ImageCapabilities;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.MenuComponent;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Component.BaselineResizeBehavior;
import java.awt.dnd.DropTarget;
import java.awt.event.ComponentListener;
import java.awt.event.ContainerListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyListener;
import java.awt.event.InputMethodListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;
import java.awt.im.InputContext;
import java.awt.im.InputMethodRequests;
import java.awt.image.ColorModel;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.awt.image.VolatileImage;
import java.awt.peer.ComponentPeer;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.EventListener;
import java.util.Locale;
import java.util.Set;

import javax.accessibility.AccessibleContext;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PFont;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PMatrix;
import processing.core.PMatrix2D;
import processing.core.PMatrix3D;
import processing.core.PShape;
import processing.core.PStyle;
import field.bytecode.protect.SimplyWrappedInQueue.iProvidesWrapping;
import field.bytecode.protect.SimplyWrappedInQueue.iWrappedExit;

public class HollowPApplet implements PConstants, iProvidesWrapping {

	public PApplet delegateTo;
	public String javaVersionName;
	public float javaVersion;
	public int platform;
	public int MENU_SHORTCUT;
	public PGraphics g;
	public Frame frame;
	public Dimension screen;
	public PGraphics recorder;
	public String sketchPath;
	public int[] pixels;
	public int width;
	public int height;
	public int mouseX;
	public int mouseY;
	public int pmouseX;
	public int pmouseY;
	public int mouseButton;
	public boolean mousePressed;
	public MouseEvent mouseEvent;
	public char key;
	public int keyCode;
	public KeyEvent keyEvent;
	public boolean focused;

	public boolean action(Event evt, Object what) {
		return delegateTo.action(evt, what);
	}

	public Component add(Component comp, int index) {
		return delegateTo.add(comp, index);
	}

	public void add(Component comp, Object constraints, int index) {
		delegateTo.add(comp, constraints, index);
	}

	public void add(Component comp, Object constraints) {
		delegateTo.add(comp, constraints);
	}

	public Component add(Component comp) {
		return delegateTo.add(comp);
	}

	public void add(PopupMenu popup) {
		delegateTo.add(popup);
	}

	public Component add(String name, Component comp) {
		return delegateTo.add(name, comp);
	}

	public void addComponentListener(ComponentListener l) {
		delegateTo.addComponentListener(l);
	}

	public void addContainerListener(ContainerListener l) {
		delegateTo.addContainerListener(l);
	}

	public void addFocusListener(FocusListener l) {
		delegateTo.addFocusListener(l);
	}

	public void addHierarchyBoundsListener(HierarchyBoundsListener l) {
		delegateTo.addHierarchyBoundsListener(l);
	}

	public void addHierarchyListener(HierarchyListener l) {
		delegateTo.addHierarchyListener(l);
	}

	public void addInputMethodListener(InputMethodListener l) {
		delegateTo.addInputMethodListener(l);
	}

	public void addKeyListener(KeyListener l) {
		delegateTo.addKeyListener(l);
	}

	public void addListeners() {
		delegateTo.addListeners();
	}

	public void addMouseListener(MouseListener l) {
		delegateTo.addMouseListener(l);
	}

	public void addMouseMotionListener(MouseMotionListener l) {
		delegateTo.addMouseMotionListener(l);
	}

	public void addMouseWheelListener(MouseWheelListener l) {
		delegateTo.addMouseWheelListener(l);
	}

	public void addNotify() {
		delegateTo.addNotify();
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		delegateTo.addPropertyChangeListener(listener);
	}

	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		delegateTo.addPropertyChangeListener(propertyName, listener);
	}

	public final float alpha(int what) {
		return delegateTo.alpha(what);
	}

	public void ambient(float x, float y, float z) {
		delegateTo.ambient(x, y, z);
	}

	public void ambient(float gray) {
		delegateTo.ambient(gray);
	}

	public void ambient(int rgb) {
		delegateTo.ambient(rgb);
	}

	public void ambientLight(float red, float green, float blue, float x, float y, float z) {
		delegateTo.ambientLight(red, green, blue, x, y, z);
	}

	public void ambientLight(float red, float green, float blue) {
		delegateTo.ambientLight(red, green, blue);
	}

	public void applyComponentOrientation(ComponentOrientation o) {
		delegateTo.applyComponentOrientation(o);
	}

	public void applyMatrix(float n00, float n01, float n02, float n03, float n10, float n11, float n12, float n13, float n20, float n21, float n22, float n23, float n30, float n31, float n32, float n33) {
		delegateTo.applyMatrix(n00, n01, n02, n03, n10, n11, n12, n13, n20, n21, n22, n23, n30, n31, n32, n33);
	}

	public void applyMatrix(float n00, float n01, float n02, float n10, float n11, float n12) {
		delegateTo.applyMatrix(n00, n01, n02, n10, n11, n12);
	}

	public void applyMatrix(PMatrix source) {
		delegateTo.applyMatrix(source);
	}

	public void applyMatrix(PMatrix2D source) {
		delegateTo.applyMatrix(source);
	}

	public void applyMatrix(PMatrix3D source) {
		delegateTo.applyMatrix(source);
	}

	public void arc(float a, float b, float c, float d, float start, float stop) {
		delegateTo.arc(a, b, c, d, start, stop);
	}

	public boolean areFocusTraversalKeysSet(int id) {
		return delegateTo.areFocusTraversalKeysSet(id);
	}

	public void background(float x, float y, float z, float a) {
		delegateTo.background(x, y, z, a);
	}

	public void background(float x, float y, float z) {
		delegateTo.background(x, y, z);
	}

	public void background(float gray, float alpha) {
		delegateTo.background(gray, alpha);
	}

	public void background(float gray) {
		delegateTo.background(gray);
	}

	public void background(int rgb, float alpha) {
		delegateTo.background(rgb, alpha);
	}

	public void background(int rgb) {
		delegateTo.background(rgb);
	}

	public void background(PImage image) {
		delegateTo.background(image);
	}

	public void beginCamera() {
		delegateTo.beginCamera();
	}

	public void beginRaw(PGraphics rawGraphics) {
		delegateTo.beginRaw(rawGraphics);
	}

	public PGraphics beginRaw(String renderer, String filename) {
		return delegateTo.beginRaw(renderer, filename);
	}

	public void beginRecord(PGraphics recorder) {
		delegateTo.beginRecord(recorder);
	}

	public PGraphics beginRecord(String renderer, String filename) {
		return delegateTo.beginRecord(renderer, filename);
	}

	public void beginShape() {
		delegateTo.beginShape();
	}

	public void beginShape(int kind) {
		delegateTo.beginShape(kind);
	}

	public void bezier(float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4) {
		delegateTo.bezier(x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4);
	}

	public void bezier(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {
		delegateTo.bezier(x1, y1, x2, y2, x3, y3, x4, y4);
	}

	public void bezierDetail(int detail) {
		delegateTo.bezierDetail(detail);
	}

	public float bezierPoint(float a, float b, float c, float d, float t) {
		return delegateTo.bezierPoint(a, b, c, d, t);
	}

	public float bezierTangent(float a, float b, float c, float d, float t) {
		return delegateTo.bezierTangent(a, b, c, d, t);
	}

	public void bezierVertex(float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4) {
		delegateTo.bezierVertex(x2, y2, z2, x3, y3, z3, x4, y4, z4);
	}

	public void bezierVertex(float x2, float y2, float x3, float y3, float x4, float y4) {
		delegateTo.bezierVertex(x2, y2, x3, y3, x4, y4);
	}

	public void blend(int sx, int sy, int sw, int sh, int dx, int dy, int dw, int dh, int mode) {
		delegateTo.blend(sx, sy, sw, sh, dx, dy, dw, dh, mode);
	}

	public void blend(PImage src, int sx, int sy, int sw, int sh, int dx, int dy, int dw, int dh, int mode) {
		delegateTo.blend(src, sx, sy, sw, sh, dx, dy, dw, dh, mode);
	}

	public final float blue(int what) {
		return delegateTo.blue(what);
	}

	public Rectangle bounds() {
		return delegateTo.bounds();
	}

	public void box(float w, float h, float d) {
		delegateTo.box(w, h, d);
	}

	public void box(float size) {
		delegateTo.box(size);
	}

	public void breakShape() {
		delegateTo.breakShape();
	}

	public final float brightness(int what) {
		return delegateTo.brightness(what);
	}

	public void camera() {
		delegateTo.camera();
	}

	public void camera(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ) {
		delegateTo.camera(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
	}

	public int checkImage(Image image, ImageObserver observer) {
		return delegateTo.checkImage(image, observer);
	}

	public int checkImage(Image image, int width, int height, ImageObserver observer) {
		return delegateTo.checkImage(image, width, height, observer);
	}

	public final int color(float x, float y, float z, float a) {
		return delegateTo.color(x, y, z, a);
	}

	public final int color(float x, float y, float z) {
		return delegateTo.color(x, y, z);
	}

	public final int color(float fgray, float falpha) {
		return delegateTo.color(fgray, falpha);
	}

	public final int color(float fgray) {
		return delegateTo.color(fgray);
	}

	public final int color(int x, int y, int z, int a) {
		return delegateTo.color(x, y, z, a);
	}

	public final int color(int x, int y, int z) {
		return delegateTo.color(x, y, z);
	}

	public final int color(int gray, int alpha) {
		return delegateTo.color(gray, alpha);
	}

	public final int color(int gray) {
		return delegateTo.color(gray);
	}

	public void colorMode(int mode, float maxX, float maxY, float maxZ, float maxA) {
		delegateTo.colorMode(mode, maxX, maxY, maxZ, maxA);
	}

	public void colorMode(int mode, float maxX, float maxY, float maxZ) {
		delegateTo.colorMode(mode, maxX, maxY, maxZ);
	}

	public void colorMode(int mode, float max) {
		delegateTo.colorMode(mode, max);
	}

	public void colorMode(int mode) {
		delegateTo.colorMode(mode);
	}

	public boolean contains(int x, int y) {
		return delegateTo.contains(x, y);
	}

	public boolean contains(Point p) {
		return delegateTo.contains(p);
	}

	public void copy(int sx, int sy, int sw, int sh, int dx, int dy, int dw, int dh) {
		delegateTo.copy(sx, sy, sw, sh, dx, dy, dw, dh);
	}

	public void copy(PImage src, int sx, int sy, int sw, int sh, int dx, int dy, int dw, int dh) {
		delegateTo.copy(src, sx, sy, sw, sh, dx, dy, dw, dh);
	}

	public int countComponents() {
		return delegateTo.countComponents();
	}

	public PFont createFont(String name, float size, boolean smooth, char[] charset) {
		return delegateTo.createFont(name, size, smooth, charset);
	}

	public PFont createFont(String name, float size, boolean smooth) {
		return delegateTo.createFont(name, size, smooth);
	}

	public PFont createFont(String name, float size) {
		return delegateTo.createFont(name, size);
	}

	public PGraphics createGraphics(int iwidth, int iheight, String irenderer, String ipath) {
		return delegateTo.createGraphics(iwidth, iheight, irenderer, ipath);
	}

	public PGraphics createGraphics(int iwidth, int iheight, String irenderer) {
		return delegateTo.createGraphics(iwidth, iheight, irenderer);
	}

	public Image createImage(ImageProducer producer) {
		return delegateTo.createImage(producer);
	}

	public PImage createImage(int wide, int high, int format) {
		return delegateTo.createImage(wide, high, format);
	}

	public Image createImage(int width, int height) {
		return delegateTo.createImage(width, height);
	}

	public InputStream createInput(String filename) {
		return delegateTo.createInput(filename);
	}

	public InputStream createInputRaw(String filename) {
		return delegateTo.createInputRaw(filename);
	}

	public OutputStream createOutput(String filename) {
		return delegateTo.createOutput(filename);
	}

	public BufferedReader createReader(String filename) {
		return delegateTo.createReader(filename);
	}

	public VolatileImage createVolatileImage(int width, int height, ImageCapabilities caps) throws AWTException {
		return delegateTo.createVolatileImage(width, height, caps);
	}

	public VolatileImage createVolatileImage(int width, int height) {
		return delegateTo.createVolatileImage(width, height);
	}

	public PrintWriter createWriter(String filename) {
		return delegateTo.createWriter(filename);
	}

	public void cursor() {
		delegateTo.cursor();
	}

	public void cursor(int cursorType) {
		delegateTo.cursor(cursorType);
	}

	public void cursor(PImage image, int hotspotX, int hotspotY) {
		delegateTo.cursor(image, hotspotX, hotspotY);
	}

	public void cursor(PImage image) {
		delegateTo.cursor(image);
	}

	public void curve(float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4) {
		delegateTo.curve(x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4);
	}

	public void curve(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {
		delegateTo.curve(x1, y1, x2, y2, x3, y3, x4, y4);
	}

	public void curveDetail(int detail) {
		delegateTo.curveDetail(detail);
	}

	public float curvePoint(float a, float b, float c, float d, float t) {
		return delegateTo.curvePoint(a, b, c, d, t);
	}

	public float curveTangent(float a, float b, float c, float d, float t) {
		return delegateTo.curveTangent(a, b, c, d, t);
	}

	public void curveTightness(float tightness) {
		delegateTo.curveTightness(tightness);
	}

	public void curveVertex(float x, float y, float z) {
		delegateTo.curveVertex(x, y, z);
	}

	public void curveVertex(float x, float y) {
		delegateTo.curveVertex(x, y);
	}

	public File dataFile(String where) {
		return delegateTo.dataFile(where);
	}

	public String dataPath(String where) {
		return delegateTo.dataPath(where);
	}

	public void deliverEvent(Event e) {
		delegateTo.deliverEvent(e);
	}

	public void destroy() {
		delegateTo.destroy();
	}

	public void die(String what, Exception e) {
		delegateTo.die(what, e);
	}

	public void die(String what) {
		delegateTo.die(what);
	}

	public void directionalLight(float red, float green, float blue, float nx, float ny, float nz) {
		delegateTo.directionalLight(red, green, blue, nx, ny, nz);
	}

	public void disable() {
		delegateTo.disable();
	}

	public final void dispatchEvent(AWTEvent e) {
		delegateTo.dispatchEvent(e);
	}

	public boolean displayable() {
		return delegateTo.displayable();
	}

	public void doLayout() {
		delegateTo.doLayout();
	}

	public void draw() {
		delegateTo.draw();
	}

	public void edge(boolean edge) {
		delegateTo.edge(edge);
	}

	public void ellipse(float a, float b, float c, float d) {
		delegateTo.ellipse(a, b, c, d);
	}

	public void ellipseMode(int mode) {
		delegateTo.ellipseMode(mode);
	}

	public void emissive(float x, float y, float z) {
		delegateTo.emissive(x, y, z);
	}

	public void emissive(float gray) {
		delegateTo.emissive(gray);
	}

	public void emissive(int rgb) {
		delegateTo.emissive(rgb);
	}

	public void enable() {
		delegateTo.enable();
	}

	public void enable(boolean b) {
		delegateTo.enable(b);
	}

	public void enableInputMethods(boolean enable) {
		delegateTo.enableInputMethods(enable);
	}

	public void endCamera() {
		delegateTo.endCamera();
	}

	public void endRaw() {
		delegateTo.endRaw();
	}

	public void endRecord() {
		delegateTo.endRecord();
	}

	public void endShape() {
		delegateTo.endShape();
	}

	public void endShape(int mode) {
		delegateTo.endShape(mode);
	}

	public boolean equals(Object obj) {
		return delegateTo.equals(obj);
	}

	public void exit() {
		delegateTo.exit();
	}

	public void fill(float x, float y, float z, float a) {
		delegateTo.fill(x, y, z, a);
	}

	public void fill(float x, float y, float z) {
		delegateTo.fill(x, y, z);
	}

	public void fill(float gray, float alpha) {
		delegateTo.fill(gray, alpha);
	}

	public void fill(float gray) {
		delegateTo.fill(gray);
	}

	public void fill(int rgb, float alpha) {
		delegateTo.fill(rgb, alpha);
	}

	public void fill(int rgb) {
		delegateTo.fill(rgb);
	}

	public void filter(int kind, float param) {
		delegateTo.filter(kind, param);
	}

	public void filter(int kind) {
		delegateTo.filter(kind);
	}

	public Component findComponentAt(int x, int y) {
		return delegateTo.findComponentAt(x, y);
	}

	public Component findComponentAt(Point p) {
		return delegateTo.findComponentAt(p);
	}

	public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {
		delegateTo.firePropertyChange(propertyName, oldValue, newValue);
	}

	public void firePropertyChange(String propertyName, char oldValue, char newValue) {
		delegateTo.firePropertyChange(propertyName, oldValue, newValue);
	}

	public void firePropertyChange(String propertyName, double oldValue, double newValue) {
		delegateTo.firePropertyChange(propertyName, oldValue, newValue);
	}

	public void firePropertyChange(String propertyName, float oldValue, float newValue) {
		delegateTo.firePropertyChange(propertyName, oldValue, newValue);
	}

	public void firePropertyChange(String propertyName, long oldValue, long newValue) {
		delegateTo.firePropertyChange(propertyName, oldValue, newValue);
	}

	public void firePropertyChange(String propertyName, short oldValue, short newValue) {
		delegateTo.firePropertyChange(propertyName, oldValue, newValue);
	}

	public void flush() {
		delegateTo.flush();
	}

	public void focusGained() {
		delegateTo.focusGained();
	}

	public void focusGained(FocusEvent e) {
		delegateTo.focusGained(e);
	}

	public void focusLost() {
		delegateTo.focusLost();
	}

	public void focusLost(FocusEvent e) {
		delegateTo.focusLost(e);
	}

	public void frameRate(float newRateTarget) {
		delegateTo.frameRate(newRateTarget);
	}

	public void frustum(float left, float right, float bottom, float top, float near, float far) {
		delegateTo.frustum(left, right, bottom, top, near, far);
	}

	public PImage get() {
		return delegateTo.get();
	}

	public PImage get(int x, int y, int w, int h) {
		return delegateTo.get(x, y, w, h);
	}

	public int get(int x, int y) {
		return delegateTo.get(x, y);
	}

	public AccessibleContext getAccessibleContext() {
		return delegateTo.getAccessibleContext();
	}

	public float getAlignmentX() {
		return delegateTo.getAlignmentX();
	}

	public float getAlignmentY() {
		return delegateTo.getAlignmentY();
	}

	public AppletContext getAppletContext() {
		return delegateTo.getAppletContext();
	}

	public String getAppletInfo() {
		return delegateTo.getAppletInfo();
	}

	public AudioClip getAudioClip(URL url, String name) {
		return delegateTo.getAudioClip(url, name);
	}

	public AudioClip getAudioClip(URL url) {
		return delegateTo.getAudioClip(url);
	}

	public Color getBackground() {
		return delegateTo.getBackground();
	}

	public int getBaseline(int width, int height) {
		return delegateTo.getBaseline(width, height);
	}

	public BaselineResizeBehavior getBaselineResizeBehavior() {
		return delegateTo.getBaselineResizeBehavior();
	}

	public Rectangle getBounds() {
		return delegateTo.getBounds();
	}

	public Rectangle getBounds(Rectangle rv) {
		return delegateTo.getBounds(rv);
	}

	public URL getCodeBase() {
		return delegateTo.getCodeBase();
	}

	public ColorModel getColorModel() {
		return delegateTo.getColorModel();
	}

	public Component getComponent(int n) {
		return delegateTo.getComponent(n);
	}

	public Component getComponentAt(int x, int y) {
		return delegateTo.getComponentAt(x, y);
	}

	public Component getComponentAt(Point p) {
		return delegateTo.getComponentAt(p);
	}

	public int getComponentCount() {
		return delegateTo.getComponentCount();
	}

	public ComponentListener[] getComponentListeners() {
		return delegateTo.getComponentListeners();
	}

	public ComponentOrientation getComponentOrientation() {
		return delegateTo.getComponentOrientation();
	}

	public Component[] getComponents() {
		return delegateTo.getComponents();
	}

	public int getComponentZOrder(Component comp) {
		return delegateTo.getComponentZOrder(comp);
	}

	public ContainerListener[] getContainerListeners() {
		return delegateTo.getContainerListeners();
	}

	public Cursor getCursor() {
		return delegateTo.getCursor();
	}

	public URL getDocumentBase() {
		return delegateTo.getDocumentBase();
	}

	public DropTarget getDropTarget() {
		return delegateTo.getDropTarget();
	}

	public Container getFocusCycleRootAncestor() {
		return delegateTo.getFocusCycleRootAncestor();
	}

	public FocusListener[] getFocusListeners() {
		return delegateTo.getFocusListeners();
	}

	public Set<AWTKeyStroke> getFocusTraversalKeys(int id) {
		return delegateTo.getFocusTraversalKeys(id);
	}

	public boolean getFocusTraversalKeysEnabled() {
		return delegateTo.getFocusTraversalKeysEnabled();
	}

	public FocusTraversalPolicy getFocusTraversalPolicy() {
		return delegateTo.getFocusTraversalPolicy();
	}

	public Font getFont() {
		return delegateTo.getFont();
	}

	public FontMetrics getFontMetrics(Font font) {
		return delegateTo.getFontMetrics(font);
	}

	public Color getForeground() {
		return delegateTo.getForeground();
	}

	public Graphics getGraphics() {
		return delegateTo.getGraphics();
	}

	public GraphicsConfiguration getGraphicsConfiguration() {
		return delegateTo.getGraphicsConfiguration();
	}

	public int getHeight() {
		return delegateTo.getHeight();
	}

	public HierarchyBoundsListener[] getHierarchyBoundsListeners() {
		return delegateTo.getHierarchyBoundsListeners();
	}

	public HierarchyListener[] getHierarchyListeners() {
		return delegateTo.getHierarchyListeners();
	}

	public boolean getIgnoreRepaint() {
		return delegateTo.getIgnoreRepaint();
	}

	public Image getImage(URL url, String name) {
		return delegateTo.getImage(url, name);
	}

	public Image getImage(URL url) {
		return delegateTo.getImage(url);
	}

	public InputContext getInputContext() {
		return delegateTo.getInputContext();
	}

	public InputMethodListener[] getInputMethodListeners() {
		return delegateTo.getInputMethodListeners();
	}

	public InputMethodRequests getInputMethodRequests() {
		return delegateTo.getInputMethodRequests();
	}

	public Insets getInsets() {
		return delegateTo.getInsets();
	}

	public KeyListener[] getKeyListeners() {
		return delegateTo.getKeyListeners();
	}

	public LayoutManager getLayout() {
		return delegateTo.getLayout();
	}

	public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
		return delegateTo.getListeners(listenerType);
	}

	public Locale getLocale() {
		return delegateTo.getLocale();
	}

	public Point getLocation() {
		return delegateTo.getLocation();
	}

	public Point getLocation(Point rv) {
		return delegateTo.getLocation(rv);
	}

	public Point getLocationOnScreen() {
		return delegateTo.getLocationOnScreen();
	}

	public PMatrix getMatrix() {
		return delegateTo.getMatrix();
	}

	public PMatrix2D getMatrix(PMatrix2D target) {
		return delegateTo.getMatrix(target);
	}

	public PMatrix3D getMatrix(PMatrix3D target) {
		return delegateTo.getMatrix(target);
	}

	public Dimension getMaximumSize() {
		return delegateTo.getMaximumSize();
	}

	public Dimension getMinimumSize() {
		return delegateTo.getMinimumSize();
	}

	public MouseListener[] getMouseListeners() {
		return delegateTo.getMouseListeners();
	}

	public MouseMotionListener[] getMouseMotionListeners() {
		return delegateTo.getMouseMotionListeners();
	}

	public Point getMousePosition() throws HeadlessException {
		return delegateTo.getMousePosition();
	}

	public Point getMousePosition(boolean allowChildren) throws HeadlessException {
		return delegateTo.getMousePosition(allowChildren);
	}

	public MouseWheelListener[] getMouseWheelListeners() {
		return delegateTo.getMouseWheelListeners();
	}

	public String getName() {
		return delegateTo.getName();
	}

	public String getParameter(String name) {
		return delegateTo.getParameter(name);
	}

	public String[][] getParameterInfo() {
		return delegateTo.getParameterInfo();
	}

	public Container getParent() {
		return delegateTo.getParent();
	}

	public ComponentPeer getPeer() {
		return delegateTo.getPeer();
	}

	public Dimension getPreferredSize() {
		return delegateTo.getPreferredSize();
	}

	public PropertyChangeListener[] getPropertyChangeListeners() {
		return delegateTo.getPropertyChangeListeners();
	}

	public PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
		return delegateTo.getPropertyChangeListeners(propertyName);
	}

	public Dimension getSize() {
		return delegateTo.getSize();
	}

	public Dimension getSize(Dimension rv) {
		return delegateTo.getSize(rv);
	}

	public Toolkit getToolkit() {
		return delegateTo.getToolkit();
	}

	public final Object getTreeLock() {
		return delegateTo.getTreeLock();
	}

	public int getWidth() {
		return delegateTo.getWidth();
	}

	public int getX() {
		return delegateTo.getX();
	}

	public int getY() {
		return delegateTo.getY();
	}

	public boolean gotFocus(Event evt, Object what) {
		return delegateTo.gotFocus(evt, what);
	}

	public final float green(int what) {
		return delegateTo.green(what);
	}

	public void handleDraw() {
		delegateTo.handleDraw();
	}

	public boolean handleEvent(Event evt) {
		return delegateTo.handleEvent(evt);
	}

	public boolean hasFocus() {
		return delegateTo.hasFocus();
	}

	public int hashCode() {
		return delegateTo.hashCode();
	}

	public void hide() {
		delegateTo.hide();
	}

	public void hint(int which) {
		delegateTo.hint(which);
	}

	public final float hue(int what) {
		return delegateTo.hue(what);
	}

	public void image(PImage image, float a, float b, float c, float d, int u1, int v1, int u2, int v2) {
		delegateTo.image(image, a, b, c, d, u1, v1, u2, v2);
	}

	public void image(PImage image, float x, float y, float c, float d) {
		delegateTo.image(image, x, y, c, d);
	}

	public void image(PImage image, float x, float y) {
		delegateTo.image(image, x, y);
	}

	public void imageMode(int mode) {
		delegateTo.imageMode(mode);
	}

	public boolean imageUpdate(Image img, int infoflags, int x, int y, int w, int h) {
		return delegateTo.imageUpdate(img, infoflags, x, y, w, h);
	}

	public void init() {
		delegateTo.init();
	}

	public Insets insets() {
		return delegateTo.insets();
	}

	public boolean inside(int x, int y) {
		return delegateTo.inside(x, y);
	}

	public void invalidate() {
		delegateTo.invalidate();
	}

	public boolean isActive() {
		return delegateTo.isActive();
	}

	public boolean isAncestorOf(Component c) {
		return delegateTo.isAncestorOf(c);
	}

	public boolean isBackgroundSet() {
		return delegateTo.isBackgroundSet();
	}

	public boolean isCursorSet() {
		return delegateTo.isCursorSet();
	}

	public boolean isDisplayable() {
		return delegateTo.isDisplayable();
	}

	public boolean isDoubleBuffered() {
		return delegateTo.isDoubleBuffered();
	}

	public boolean isEnabled() {
		return delegateTo.isEnabled();
	}

	public boolean isFocusable() {
		return delegateTo.isFocusable();
	}

	public boolean isFocusCycleRoot() {
		return delegateTo.isFocusCycleRoot();
	}

	public boolean isFocusCycleRoot(Container container) {
		return delegateTo.isFocusCycleRoot(container);
	}

	public boolean isFocusOwner() {
		return delegateTo.isFocusOwner();
	}

	public boolean isFocusTraversable() {
		return delegateTo.isFocusTraversable();
	}

	public final boolean isFocusTraversalPolicyr() {
		return delegateTo.isFocusTraversalPolicyProvider();
	}

	public boolean isFocusTraversalPolicySet() {
		return delegateTo.isFocusTraversalPolicySet();
	}

	public boolean isFontSet() {
		return delegateTo.isFontSet();
	}

	public boolean isForegroundSet() {
		return delegateTo.isForegroundSet();
	}

	public boolean isLightweight() {
		return delegateTo.isLightweight();
	}

	public boolean isMaximumSizeSet() {
		return delegateTo.isMaximumSizeSet();
	}

	public boolean isMinimumSizeSet() {
		return delegateTo.isMinimumSizeSet();
	}

	public boolean isOpaque() {
		return delegateTo.isOpaque();
	}

	public boolean isPreferredSizeSet() {
		return delegateTo.isPreferredSizeSet();
	}

	public boolean isShowing() {
		return delegateTo.isShowing();
	}

	public boolean isValid() {
		return delegateTo.isValid();
	}

	public boolean isVisible() {
		return delegateTo.isVisible();
	}

	public boolean keyDown(Event evt, int key) {
		return delegateTo.keyDown(evt, key);
	}

	public void keyPressed() {
		delegateTo.keyPressed();
	}

	public void keyPressed(KeyEvent e) {
		delegateTo.keyPressed(e);
	}

	public void keyReleased() {
		delegateTo.keyReleased();
	}

	public void keyReleased(KeyEvent e) {
		delegateTo.keyReleased(e);
	}

	public void keyTyped() {
		delegateTo.keyTyped();
	}

	public void keyTyped(KeyEvent e) {
		delegateTo.keyTyped(e);
	}

	public boolean keyUp(Event evt, int key) {
		return delegateTo.keyUp(evt, key);
	}

	public void layout() {
		delegateTo.layout();
	}

	public int lerpColor(int c1, int c2, float amt) {
		return delegateTo.lerpColor(c1, c2, amt);
	}

	public void lightFalloff(float constant, float linear, float quadratic) {
		delegateTo.lightFalloff(constant, linear, quadratic);
	}

	public void lights() {
		delegateTo.lights();
	}

	public void lightSpecular(float x, float y, float z) {
		delegateTo.lightSpecular(x, y, z);
	}

	public void line(float x1, float y1, float z1, float x2, float y2, float z2) {
		delegateTo.line(x1, y1, z1, x2, y2, z2);
	}

	public void line(float x1, float y1, float x2, float y2) {
		delegateTo.line(x1, y1, x2, y2);
	}

	public void link(String url, String frameTitle) {
		delegateTo.link(url, frameTitle);
	}

	public void link(String here) {
		delegateTo.link(here);
	}

	public void list() {
		delegateTo.list();
	}

	public void list(PrintStream out, int indent) {
		delegateTo.list(out, indent);
	}

	public void list(PrintStream out) {
		delegateTo.list(out);
	}

	public void list(PrintWriter out, int indent) {
		delegateTo.list(out, indent);
	}

	public void list(PrintWriter out) {
		delegateTo.list(out);
	}

	public byte[] loadBytes(String filename) {
		return delegateTo.loadBytes(filename);
	}

	public PFont loadFont(String filename) {
		return delegateTo.loadFont(filename);
	}

	public PImage loadImage(String filename, String extension) {
		return delegateTo.loadImage(filename, extension);
	}

	public PImage loadImage(String filename) {
		return delegateTo.loadImage(filename);
	}

	public void loadPixels() {
		delegateTo.loadPixels();
	}

	public PShape loadShape(String filename) {
		return delegateTo.loadShape(filename);
	}

	public String[] loadStrings(String filename) {
		return delegateTo.loadStrings(filename);
	}

	public Component locate(int x, int y) {
		return delegateTo.locate(x, y);
	}

	public Point location() {
		return delegateTo.location();
	}

	public void loop() {
		delegateTo.loop();
	}

	public boolean lostFocus(Event evt, Object what) {
		return delegateTo.lostFocus(evt, what);
	}

	public void mask(int[] alpha) {
		delegateTo.mask(alpha);
	}

	public void mask(PImage alpha) {
		delegateTo.mask(alpha);
	}

	public int millis() {
		return delegateTo.millis();
	}

	public Dimension minimumSize() {
		return delegateTo.minimumSize();
	}

	public float modelX(float x, float y, float z) {
		return delegateTo.modelX(x, y, z);
	}

	public float modelY(float x, float y, float z) {
		return delegateTo.modelY(x, y, z);
	}

	public float modelZ(float x, float y, float z) {
		return delegateTo.modelZ(x, y, z);
	}

	public void mouseClicked() {
		delegateTo.mouseClicked();
	}

	public void mouseClicked(MouseEvent e) {
		delegateTo.mouseClicked(e);
	}

	public boolean mouseDown(Event evt, int x, int y) {
		return delegateTo.mouseDown(evt, x, y);
	}

	public boolean mouseDrag(Event evt, int x, int y) {
		return delegateTo.mouseDrag(evt, x, y);
	}

	public void mouseDragged() {
		delegateTo.mouseDragged();
	}

	public void mouseDragged(MouseEvent e) {
		delegateTo.mouseDragged(e);
	}

	public boolean mouseEnter(Event evt, int x, int y) {
		return delegateTo.mouseEnter(evt, x, y);
	}

	public void mouseEntered(MouseEvent e) {
		delegateTo.mouseEntered(e);
	}

	public boolean mouseExit(Event evt, int x, int y) {
		return delegateTo.mouseExit(evt, x, y);
	}

	public void mouseExited(MouseEvent e) {
		delegateTo.mouseExited(e);
	}

	public boolean mouseMove(Event evt, int x, int y) {
		return delegateTo.mouseMove(evt, x, y);
	}

	public void mouseMoved() {
		delegateTo.mouseMoved();
	}

	public void mouseMoved(MouseEvent e) {
		delegateTo.mouseMoved(e);
	}

	public void mousePressed() {
		delegateTo.mousePressed();
	}

	public void mousePressed(MouseEvent e) {
		delegateTo.mousePressed(e);
	}

	public void mouseReleased() {
		delegateTo.mouseReleased();
	}

	public void mouseReleased(MouseEvent e) {
		delegateTo.mouseReleased(e);
	}

	public boolean mouseUp(Event evt, int x, int y) {
		return delegateTo.mouseUp(evt, x, y);
	}

	public void move(int x, int y) {
		delegateTo.move(x, y);
	}

	public void nextFocus() {
		delegateTo.nextFocus();
	}

	public void noCursor() {
		delegateTo.noCursor();
	}

	public void noFill() {
		delegateTo.noFill();
	}

	public float noise(float x, float y, float z) {
		return delegateTo.noise(x, y, z);
	}

	public float noise(float x, float y) {
		return delegateTo.noise(x, y);
	}

	public float noise(float x) {
		return delegateTo.noise(x);
	}

	public void noiseDetail(int lod, float falloff) {
		delegateTo.noiseDetail(lod, falloff);
	}

	public void noiseDetail(int lod) {
		delegateTo.noiseDetail(lod);
	}

	public void noiseSeed(long what) {
		delegateTo.noiseSeed(what);
	}

	public void noLights() {
		delegateTo.noLights();
	}

	public void noLoop() {
		delegateTo.noLoop();
	}

	public void normal(float nx, float ny, float nz) {
		delegateTo.normal(nx, ny, nz);
	}

	public void noSmooth() {
		delegateTo.noSmooth();
	}

	public void noStroke() {
		delegateTo.noStroke();
	}

	public void noTint() {
		delegateTo.noTint();
	}

	public InputStream openStream(String filename) {
		return delegateTo.openStream(filename);
	}

	public void ortho() {
		delegateTo.ortho();
	}

	public void ortho(float left, float right, float bottom, float top, float near, float far) {
		delegateTo.ortho(left, right, bottom, top, near, far);
	}

	public void paint(Graphics screen) {
		delegateTo.paint(screen);
	}

	public void paintAll(Graphics g) {
		delegateTo.paintAll(g);
	}

	public void paintComponents(Graphics g) {
		delegateTo.paintComponents(g);
	}

	public String param(String what) {
		return delegateTo.param(what);
	}

	public void perspective() {
		delegateTo.perspective();
	}

	public void perspective(float fovy, float aspect, float zNear, float zFar) {
		delegateTo.perspective(fovy, aspect, zNear, zFar);
	}

	public void play(URL url, String name) {
		delegateTo.play(url, name);
	}

	public void play(URL url) {
		delegateTo.play(url);
	}

	public void point(float x, float y, float z) {
		delegateTo.point(x, y, z);
	}

	public void point(float x, float y) {
		delegateTo.point(x, y);
	}

	public void pointLight(float red, float green, float blue, float x, float y, float z) {
		delegateTo.pointLight(red, green, blue, x, y, z);
	}

	public void popMatrix() {
		delegateTo.popMatrix();
	}

	public void popStyle() {
		delegateTo.popStyle();
	}

	public boolean postEvent(Event e) {
		return delegateTo.postEvent(e);
	}

	public Dimension preferredSize() {
		return delegateTo.preferredSize();
	}

	public boolean prepareImage(Image image, ImageObserver observer) {
		return delegateTo.prepareImage(image, observer);
	}

	public boolean prepareImage(Image image, int width, int height, ImageObserver observer) {
		return delegateTo.prepareImage(image, width, height, observer);
	}

	public void print(Graphics g) {
		delegateTo.print(g);
	}

	public void printAll(Graphics g) {
		delegateTo.printAll(g);
	}

	public void printCamera() {
		delegateTo.printCamera();
	}

	public void printComponents(Graphics g) {
		delegateTo.printComponents(g);
	}

	public void printMatrix() {
		delegateTo.printMatrix();
	}

	public void printProjection() {
		delegateTo.printProjection();
	}

	public void pushMatrix() {
		delegateTo.pushMatrix();
	}

	public void pushStyle() {
		delegateTo.pushStyle();
	}

	public void quad(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {
		delegateTo.quad(x1, y1, x2, y2, x3, y3, x4, y4);
	}

	public final float random(float howsmall, float howbig) {
		return delegateTo.random(howsmall, howbig);
	}

	public final float random(float howbig) {
		return delegateTo.random(howbig);
	}

	public final void randomSeed(long what) {
		delegateTo.randomSeed(what);
	}

	public void rect(float a, float b, float c, float d) {
		delegateTo.rect(a, b, c, d);
	}

	public void rectMode(int mode) {
		delegateTo.rectMode(mode);
	}

	public final float red(int what) {
		return delegateTo.red(what);
	}

	public void redraw() {
		delegateTo.redraw();
	}

	public void registerDispose(Object o) {
		delegateTo.registerDispose(o);
	}

	public void registerDraw(Object o) {
		delegateTo.registerDraw(o);
	}

	public void registerKeyEvent(Object o) {
		delegateTo.registerKeyEvent(o);
	}

	public void registerMouseEvent(Object o) {
		delegateTo.registerMouseEvent(o);
	}

	public void registerPost(Object o) {
		delegateTo.registerPost(o);
	}

	public void registerPre(Object o) {
		delegateTo.registerPre(o);
	}

	public void registerSize(Object o) {
		delegateTo.registerSize(o);
	}

	public void remove(Component comp) {
		delegateTo.remove(comp);
	}

	public void remove(int index) {
		delegateTo.remove(index);
	}

	public void remove(MenuComponent popup) {
		delegateTo.remove(popup);
	}

	public void removeAll() {
		delegateTo.removeAll();
	}


	public void removeComponentListener(ComponentListener l) {
		delegateTo.removeComponentListener(l);
	}

	public void removeContainerListener(ContainerListener l) {
		delegateTo.removeContainerListener(l);
	}

	public void removeFocusListener(FocusListener l) {
		delegateTo.removeFocusListener(l);
	}

	public void removeHierarchyBoundsListener(HierarchyBoundsListener l) {
		delegateTo.removeHierarchyBoundsListener(l);
	}

	public void removeHierarchyListener(HierarchyListener l) {
		delegateTo.removeHierarchyListener(l);
	}

	public void removeInputMethodListener(InputMethodListener l) {
		delegateTo.removeInputMethodListener(l);
	}

	public void removeKeyListener(KeyListener l) {
		delegateTo.removeKeyListener(l);
	}

	public void removeMouseListener(MouseListener l) {
		delegateTo.removeMouseListener(l);
	}

	public void removeMouseMotionListener(MouseMotionListener l) {
		delegateTo.removeMouseMotionListener(l);
	}

	public void removeMouseWheelListener(MouseWheelListener l) {
		delegateTo.removeMouseWheelListener(l);
	}

	public void removeNotify() {
		delegateTo.removeNotify();
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		delegateTo.removePropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		delegateTo.removePropertyChangeListener(propertyName, listener);
	}

	public void repaint() {
		delegateTo.repaint();
	}

	public void repaint(int x, int y, int width, int height) {
		delegateTo.repaint(x, y, width, height);
	}

	public void repaint(long tm, int x, int y, int width, int height) {
		delegateTo.repaint(tm, x, y, width, height);
	}

	public void repaint(long tm) {
		delegateTo.repaint(tm);
	}

	public void requestFocus() {
		delegateTo.requestFocus();
	}

	public boolean requestFocusInWindow() {
		return delegateTo.requestFocusInWindow();
	}

	public PImage requestImage(String filename, String extension) {
		return delegateTo.requestImage(filename, extension);
	}

	public PImage requestImage(String filename) {
		return delegateTo.requestImage(filename);
	}

	public void resetMatrix() {
		delegateTo.resetMatrix();
	}

	public void reshape(int x, int y, int width, int height) {
		delegateTo.reshape(x, y, width, height);
	}

	public void resize(Dimension d) {
		delegateTo.resize(d);
	}

	public void resize(int width, int height) {
		delegateTo.resize(width, height);
	}

	public void rotate(float angle, float vx, float vy, float vz) {
		delegateTo.rotate(angle, vx, vy, vz);
	}

	public void rotate(float angle) {
		delegateTo.rotate(angle);
	}

	public void rotateX(float angle) {
		delegateTo.rotateX(angle);
	}

	public void rotateY(float angle) {
		delegateTo.rotateY(angle);
	}

	public void rotateZ(float angle) {
		delegateTo.rotateZ(angle);
	}

	public void run() {
		delegateTo.run();
	}

	public final float saturation(int what) {
		return delegateTo.saturation(what);
	}

	public void save(String filename) {
		delegateTo.save(filename);
	}

	public void saveBytes(String filename, byte[] buffer) {
		delegateTo.saveBytes(filename, buffer);
	}

	public File saveFile(String where) {
		return delegateTo.saveFile(where);
	}

	public void saveFrame() {
		delegateTo.saveFrame();
	}

	public void saveFrame(String what) {
		delegateTo.saveFrame(what);
	}

	public String savePath(String where) {
		return delegateTo.savePath(where);
	}

	public void saveStream(File targetFile, String sourceLocation) {
		delegateTo.saveStream(targetFile, sourceLocation);
	}

	public void saveStream(String targetFilename, String sourceLocation) {
		delegateTo.saveStream(targetFilename, sourceLocation);
	}

	public void saveStrings(String filename, String[] strings) {
		delegateTo.saveStrings(filename, strings);
	}

	public void scale(float x, float y, float z) {
		delegateTo.scale(x, y, z);
	}

	public void scale(float sx, float sy) {
		delegateTo.scale(sx, sy);
	}

	public void scale(float s) {
		delegateTo.scale(s);
	}

	public float screenX(float x, float y, float z) {
		return delegateTo.screenX(x, y, z);
	}

	public float screenX(float x, float y) {
		return delegateTo.screenX(x, y);
	}

	public float screenY(float x, float y, float z) {
		return delegateTo.screenY(x, y, z);
	}

	public float screenY(float x, float y) {
		return delegateTo.screenY(x, y);
	}

	public float screenZ(float x, float y, float z) {
		return delegateTo.screenZ(x, y, z);
	}

	public String selectFolder() {
		return delegateTo.selectFolder();
	}

	public String selectFolder(String prompt) {
		return delegateTo.selectFolder(prompt);
	}

	public String selectInput() {
		return delegateTo.selectInput();
	}

	public String selectInput(String prompt) {
		return delegateTo.selectInput(prompt);
	}

	public String selectOutput() {
		return delegateTo.selectOutput();
	}

	public String selectOutput(String prompt) {
		return delegateTo.selectOutput(prompt);
	}

	public void set(int x, int y, int c) {
		delegateTo.set(x, y, c);
	}

	public void set(int x, int y, PImage src) {
		delegateTo.set(x, y, src);
	}

	public void setBackground(Color c) {
		delegateTo.setBackground(c);
	}

	public void setBounds(int x, int y, int width, int height) {
		delegateTo.setBounds(x, y, width, height);
	}

	public void setBounds(Rectangle r) {
		delegateTo.setBounds(r);
	}

	public void setComponentOrientation(ComponentOrientation o) {
		delegateTo.setComponentOrientation(o);
	}

	public void setComponentZOrder(Component comp, int index) {
		delegateTo.setComponentZOrder(comp, index);
	}

	public void setCursor(Cursor cursor) {
		delegateTo.setCursor(cursor);
	}

	public void setDropTarget(DropTarget dt) {
		delegateTo.setDropTarget(dt);
	}

	public void setEnabled(boolean b) {
		delegateTo.setEnabled(b);
	}

	public void setFocusable(boolean focusable) {
		delegateTo.setFocusable(focusable);
	}

	public void setFocusCycleRoot(boolean focusCycleRoot) {
		delegateTo.setFocusCycleRoot(focusCycleRoot);
	}

	public void setFocusTraversalKeys(int id, Set<? extends AWTKeyStroke> keystrokes) {
		delegateTo.setFocusTraversalKeys(id, keystrokes);
	}

	public void setFocusTraversalKeysEnabled(boolean focusTraversalKeysEnabled) {
		delegateTo.setFocusTraversalKeysEnabled(focusTraversalKeysEnabled);
	}

	public void setFocusTraversalPolicy(FocusTraversalPolicy policy) {
		delegateTo.setFocusTraversalPolicy(policy);
	}

	public final void setFocusTraversalPolicyProvider(boolean provider) {
		delegateTo.setFocusTraversalPolicyProvider(provider);
	}

	public void setFont(Font f) {
		delegateTo.setFont(f);
	}

	public void setForeground(Color c) {
		delegateTo.setForeground(c);
	}

	public void setIgnoreRepaint(boolean ignoreRepaint) {
		delegateTo.setIgnoreRepaint(ignoreRepaint);
	}

	public void setLayout(LayoutManager mgr) {
		delegateTo.setLayout(mgr);
	}

	public void setLocale(Locale l) {
		delegateTo.setLocale(l);
	}

	public void setLocation(int x, int y) {
		delegateTo.setLocation(x, y);
	}

	public void setLocation(Point p) {
		delegateTo.setLocation(p);
	}

	public void setMatrix(PMatrix source) {
		delegateTo.setMatrix(source);
	}

	public void setMatrix(PMatrix2D source) {
		delegateTo.setMatrix(source);
	}

	public void setMatrix(PMatrix3D source) {
		delegateTo.setMatrix(source);
	}

	public void setMaximumSize(Dimension maximumSize) {
		delegateTo.setMaximumSize(maximumSize);
	}

	public void setMinimumSize(Dimension minimumSize) {
		delegateTo.setMinimumSize(minimumSize);
	}

	public void setName(String name) {
		delegateTo.setName(name);
	}

	public void setPreferredSize(Dimension preferredSize) {
		delegateTo.setPreferredSize(preferredSize);
	}

	public void setSize(Dimension d) {
		delegateTo.setSize(d);
	}

	public void setSize(int width, int height) {
		delegateTo.setSize(width, height);
	}

	public final void setStub(AppletStub stub) {
		delegateTo.setStub(stub);
	}

	public void setup() {
		delegateTo.setup();
	}

	public void setupExternalMessages() {
		delegateTo.setupExternalMessages();
	}

	public void setupFrameResizeListener() {
		delegateTo.setupFrameResizeListener();
	}

	public void setVisible(boolean b) {
		delegateTo.setVisible(b);
	}

	public void shape(PShape shape, float x, float y, float c, float d) {
		delegateTo.shape(shape, x, y, c, d);
	}

	public void shape(PShape shape, float x, float y) {
		delegateTo.shape(shape, x, y);
	}

	public void shape(PShape shape) {
		delegateTo.shape(shape);
	}

	public void shapeMode(int mode) {
		delegateTo.shapeMode(mode);
	}

	public void shininess(float shine) {
		delegateTo.shininess(shine);
	}

	public void show() {
		delegateTo.show();
	}

	public void show(boolean b) {
		delegateTo.show(b);
	}

	public void showStatus(String msg) {
		delegateTo.showStatus(msg);
	}

	public Dimension size() {
		return delegateTo.size();
	}

	public void size(int iwidth, int iheight, String irenderer, String ipath) {
		delegateTo.size(iwidth, iheight, irenderer, ipath);
	}

	public void size(int iwidth, int iheight, String irenderer) {
		delegateTo.size(iwidth, iheight, irenderer);
	}

	public void size(int iwidth, int iheight) {
		delegateTo.size(iwidth, iheight);
	}

	public File sketchFile(String where) {
		return delegateTo.sketchFile(where);
	}

	public String sketchPath(String where) {
		return delegateTo.sketchPath(where);
	}

	public void smooth() {
		delegateTo.smooth();
	}

	public void specular(float x, float y, float z) {
		delegateTo.specular(x, y, z);
	}

	public void specular(float gray) {
		delegateTo.specular(gray);
	}

	public void specular(int rgb) {
		delegateTo.specular(rgb);
	}

	public void sphere(float r) {
		delegateTo.sphere(r);
	}

	public void sphereDetail(int ures, int vres) {
		delegateTo.sphereDetail(ures, vres);
	}

	public void sphereDetail(int res) {
		delegateTo.sphereDetail(res);
	}

	public void spotLight(float red, float green, float blue, float x, float y, float z, float nx, float ny, float nz, float angle, float concentration) {
		delegateTo.spotLight(red, green, blue, x, y, z, nx, ny, nz, angle, concentration);
	}

	public void start() {
		delegateTo.start();
	}

	public void status(String what) {
		delegateTo.status(what);
	}

	public void stop() {
		delegateTo.stop();
	}

	public void stroke(float x, float y, float z, float a) {
		delegateTo.stroke(x, y, z, a);
	}

	public void stroke(float x, float y, float z) {
		delegateTo.stroke(x, y, z);
	}

	public void stroke(float gray, float alpha) {
		delegateTo.stroke(gray, alpha);
	}

	public void stroke(float gray) {
		delegateTo.stroke(gray);
	}

	public void stroke(int rgb, float alpha) {
		delegateTo.stroke(rgb, alpha);
	}

	public void stroke(int rgb) {
		delegateTo.stroke(rgb);
	}

	public void strokeCap(int cap) {
		delegateTo.strokeCap(cap);
	}

	public void strokeJoin(int join) {
		delegateTo.strokeJoin(join);
	}

	public void strokeWeight(float weight) {
		delegateTo.strokeWeight(weight);
	}

	public void style(PStyle s) {
		delegateTo.style(s);
	}

	public void text(char c, float x, float y, float z) {
		delegateTo.text(c, x, y, z);
	}

	public void text(char c, float x, float y) {
		delegateTo.text(c, x, y);
	}

	public void text(char c) {
		delegateTo.text(c);
	}

	public void text(float num, float x, float y, float z) {
		delegateTo.text(num, x, y, z);
	}

	public void text(float num, float x, float y) {
		delegateTo.text(num, x, y);
	}

	public void text(int num, float x, float y, float z) {
		delegateTo.text(num, x, y, z);
	}

	public void text(int num, float x, float y) {
		delegateTo.text(num, x, y);
	}

	public void text(String s, float x1, float y1, float x2, float y2, float z) {
		delegateTo.text(s, x1, y1, x2, y2, z);
	}

	public void text(String str, float x1, float y1, float x2, float y2) {
		delegateTo.text(str, x1, y1, x2, y2);
	}

	public void text(String str, float x, float y, float z) {
		delegateTo.text(str, x, y, z);
	}

	public void text(String str, float x, float y) {
		delegateTo.text(str, x, y);
	}

	public void text(String str) {
		delegateTo.text(str);
	}

	public void textAlign(int alignX, int alignY) {
		delegateTo.textAlign(alignX, alignY);
	}

	public void textAlign(int align) {
		delegateTo.textAlign(align);
	}

	public float textAscent() {
		return delegateTo.textAscent();
	}

	public float textDescent() {
		return delegateTo.textDescent();
	}

	public void textFont(PFont which, float size) {
		delegateTo.textFont(which, size);
	}

	public void textFont(PFont which) {
		delegateTo.textFont(which);
	}

	public void textLeading(float leading) {
		delegateTo.textLeading(leading);
	}

	public void textMode(int mode) {
		delegateTo.textMode(mode);
	}

	public void textSize(float size) {
		delegateTo.textSize(size);
	}

	public void texture(PImage image) {
		delegateTo.texture(image);
	}

	public void textureMode(int mode) {
		delegateTo.textureMode(mode);
	}

	public float textWidth(char c) {
		return delegateTo.textWidth(c);
	}

	public float textWidth(String str) {
		return delegateTo.textWidth(str);
	}

	public void tint(float x, float y, float z, float a) {
		delegateTo.tint(x, y, z, a);
	}

	public void tint(float x, float y, float z) {
		delegateTo.tint(x, y, z);
	}

	public void tint(float gray, float alpha) {
		delegateTo.tint(gray, alpha);
	}

	public void tint(float gray) {
		delegateTo.tint(gray);
	}

	public void tint(int rgb, float alpha) {
		delegateTo.tint(rgb, alpha);
	}

	public void tint(int rgb) {
		delegateTo.tint(rgb);
	}

	public String toString() {
		return delegateTo.toString();
	}

	public void transferFocus() {
		delegateTo.transferFocus();
	}

	public void transferFocusBackward() {
		delegateTo.transferFocusBackward();
	}

	public void transferFocusDownCycle() {
		delegateTo.transferFocusDownCycle();
	}

	public void transferFocusUpCycle() {
		delegateTo.transferFocusUpCycle();
	}

	public void translate(float tx, float ty, float tz) {
		delegateTo.translate(tx, ty, tz);
	}

	public void translate(float tx, float ty) {
		delegateTo.translate(tx, ty);
	}

	public void triangle(float x1, float y1, float x2, float y2, float x3, float y3) {
		delegateTo.triangle(x1, y1, x2, y2, x3, y3);
	}

	public void unregisterDispose(Object o) {
		delegateTo.unregisterDispose(o);
	}

	public void unregisterDraw(Object o) {
		delegateTo.unregisterDraw(o);
	}

	public void unregisterKeyEvent(Object o) {
		delegateTo.unregisterKeyEvent(o);
	}

	public void unregisterMouseEvent(Object o) {
		delegateTo.unregisterMouseEvent(o);
	}

	public void unregisterPost(Object o) {
		delegateTo.unregisterPost(o);
	}

	public void unregisterPre(Object o) {
		delegateTo.unregisterPre(o);
	}

	public void unregisterSize(Object o) {
		delegateTo.unregisterSize(o);
	}

	public void update(Graphics screen) {
		delegateTo.update(screen);
	}

	public void updatePixels() {
		delegateTo.updatePixels();
	}

	public void updatePixels(int x1, int y1, int x2, int y2) {
		delegateTo.updatePixels(x1, y1, x2, y2);
	}

	public void validate() {
		delegateTo.validate();
	}

	public void vertex(float x, float y, float z, float u, float v) {
		delegateTo.vertex(x, y, z, u, v);
	}

	public void vertex(float x, float y, float u, float v) {
		delegateTo.vertex(x, y, u, v);
	}

	public void vertex(float x, float y, float z) {
		delegateTo.vertex(x, y, z);
	}

	public void vertex(float x, float y) {
		delegateTo.vertex(x, y);
	}

	public void vertex(float[] v) {
		delegateTo.vertex(v);
	}

	public HollowPApplet()
	{
		this(ProcessingLoader.theApplet);
	}
	protected HollowPApplet(PApplet delegateTo) {
		this.delegateTo = delegateTo;
		this.javaVersionName = PApplet.javaVersionName;
		this.javaVersion = PApplet.javaVersion;
		this.platform = PApplet.platform;
		this.MENU_SHORTCUT = PApplet.MENU_SHORTCUT;
	}

	protected void updateState() {

		this.g = delegateTo.g;
		this.frame = delegateTo.frame;
		this.recorder = delegateTo.recorder;

		this.sketchPath = delegateTo.sketchPath;
		this.pixels = delegateTo.pixels;
		this.width = delegateTo.width;
		this.height = delegateTo.height;
		this.mouseX = delegateTo.mouseX;
		this.mouseY = delegateTo.mouseY;
		this.pmouseX = delegateTo.pmouseX;
		this.pmouseY = delegateTo.pmouseY;
		this.mouseButton = delegateTo.mouseButton;
		this.mousePressed = delegateTo.mousePressed;
		this.mouseEvent = delegateTo.mouseEvent;
		this.key = delegateTo.key;
		this.keyCode = delegateTo.keyCode;
		this.keyEvent = delegateTo.keyEvent;
		this.focused = delegateTo.focused;
	}

	public iWrappedExit enter(Method m) {
		updateState();
		return null;
	}

}
