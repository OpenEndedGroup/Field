package field.core.plugins.drawing.opengl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.internal.C;
import org.eclipse.swt.internal.cocoa.NSBitmapImageRep;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.HiddenInAutocomplete;
import field.bytecode.protect.annotations.NextUpdate;
import field.graphics.core.AdvancedTextures;
import field.graphics.core.AdvancedTextures.BaseSlowRawTexture;
import field.launch.Launcher;

@Woven
public class TextureBackedWebBrowser {

	private int w;
	private int h;
	private Shell window;
	private Browser browser;
	private Image image;
	private GC gc;
	private ByteBuffer buffer;
	private int[] row;
	private IntBuffer bufferInt;
	private BaseSlowRawTexture texture;
	private int[] all;

	volatile int needsUpdate = 0;
	private String url;

	public TextureBackedWebBrowser(int w, int h) {
		this.w = w;
		this.h = h;
		window = new Shell(Launcher.display, SWT.NO_TRIM);
		window.setSize(w, h);
		window.setLocation(0, 0);
		// window.setLocation(50, 50);

		window.setLayout(new FillLayout());
		browser = new Browser(window, SWT.NONE);

		window.setVisible(true);

		image = new Image(Launcher.display, w, h);
		gc = new GC(browser);

		buffer = ByteBuffer.allocateDirect(4 * w * h).order(ByteOrder.BIG_ENDIAN);
		bufferInt = buffer.asIntBuffer();
		row = new int[w];
		all = new int[w * h];
		texture = new AdvancedTextures.BaseSlowRawTexture("", bufferInt, w, h, null);
		texture.setGenMip(true);

		browser.addProgressListener(new ProgressListener() {

			@Override
			public void completed(ProgressEvent event) {
				;//System.out.println(" completed ");
				browser.execute("document.body.style.overflow='hidden'");
				needsUpdate++;
				updateTexture();
				// window.setVisible(false);
			}

			@Override
			public void changed(ProgressEvent event) {
				;//System.out.println(" changed ");
				needsUpdate++;
				updateTexture();
			}
		});

		browser.addPaintListener(new PaintListener() {

			@Override
			public void paintControl(PaintEvent e) {

				;//System.out.println(" got paint event ");

				needsUpdate++;
				updateTexture();
			}
		});

		browser.addListener(SWT.MouseDown, new Listener() {

			@Override
			public void handleEvent(Event event) {
				;//System.out.println(" browser get mouse down :" + event);
			}
		});

	}

	public void setURL(String r) {
		url = r;
		browser.setUrl(r);
		window.setVisible(true);
	}

	volatile int lastUpdate = 0;

	@HiddenInAutocomplete
	@NextUpdate(delay = 5)
	public void updateTexture() {

		;//System.out.println(" -- updating texture for texturebackedwebbroswer -- ");

		if (lastUpdate == needsUpdate)
			return;
		updateTextureNow();
	}

	@HiddenInAutocomplete
	public void updateTextureNow() {

		;//System.out.println(" -- updating texture for texturebackedwebbroswer -- ");

		if (lastUpdate == needsUpdate)
			return;
		lastUpdate = needsUpdate;
		long in = System.currentTimeMillis();
		gc.copyArea(image, 0, 0);
		long copy = System.currentTimeMillis();

		// bufferInt.rewind();
		// for (int y = 0; y < h; y++) {
		// image.getImageData().getPixels(0, y, w, row, 0);
		// bufferInt.put(row);
		// }
		// bufferInt.rewind();
		long out = System.currentTimeMillis();

		;//System.out.println(" timing " + (out - in) + " / " + (copy - in));

		long data = (new NSBitmapImageRep(image.handle.representations().objectAtIndex(0))).bitmapData();
		C.memmove(all, data, w * h * 4);

		bufferInt.rewind();
		bufferInt.put(all);
		bufferInt.rewind();

		texture.dirty();

		notifyUpdate();

	}

	protected void notifyUpdate() {

	}

	@HiddenInAutocomplete
	public void doClick(Event e) {
		// window.setVisible(false);

		browser.execute(" var evt = document.createEvent(\"MouseEvents\");\n" + "  evt.initMouseEvent(\"click\", true, true, window,\n" + "    0, 0, 0, 0, 0, false, false, false, false, 0, null);\n" + "document.elementFromPoint(" + e.x + "," + e.y + ").dispatchEvent(evt)" + "");
		needsUpdate++;
		updateTextureNow();
		// e.x += window.getLocation().x;
		// e.y += window.getLocation().y;
		//
		// ;//System.out.println(" posting synthetic event :" + e);
		// e.widget = browser;
		// Launcher.display.post(e);
	}

	/**
	 * scrolls the webpage inside it's container by ,x,y
	 */
	public void scroll(float x, float y) {
		browser.execute("window.scrollBy(" + x + ", " + y + ")");
		needsUpdate++;
		updateTexture();
	}

	@HiddenInAutocomplete
	public BaseSlowRawTexture getTexture() {
		return texture;
	}

	@HiddenInAutocomplete
	public void dispose() {
		gc.dispose();
		window.dispose();
		// texture.deallocate(atRenderTime);
	}

	/**
	 * executes javascript "on" the loaded page. You have full DOM access
	 * and can return from this function a variety of objects
	 */
	public Object executeJavaScript(String script) {
		return browser.evaluate(script);
	}

	public String getURL() {
		return url;
	}

}
