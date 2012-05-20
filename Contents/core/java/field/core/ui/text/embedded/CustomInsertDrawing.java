package field.core.ui.text.embedded;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DirectColorModel;
import java.awt.image.WritableRaster;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.JComponent;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.PaintObjectEvent;
import org.eclipse.swt.custom.PaintObjectListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import field.bytecode.protect.Woven;
import field.bytecode.protect.annotations.NextUpdate;
import field.launch.Launcher;
import field.namespace.generic.ReflectionTools;

@Woven
public class CustomInsertDrawing {

	float margin = 0;

	public interface iInsertRenderingContext {
		public StyledText getText();

		public Canvas getControl();
	}

	public interface iAcceptsInsertRenderingContext {
		public void setInsertRenderingContext(iInsertRenderingContext context);
	}

	static public Nub currentNub;

	public class Nub implements iInsertRenderingContext {
		JComponent component;
		Canvas canvas;
		BufferedImage input;
		Graphics inputGraphics;
		Image output;
		ImageData middle;
		StyledText target;
		private int width;
		private int height;
		protected int start;
		protected int length;

		boolean disposed = false;

		public Nub(final JComponent component, StyledText target, int width, int height) {
			this.component = component;
			this.target = target;
			this.width = width;
			this.height = height;

			input = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(width, height, Transparency.TRANSLUCENT);

			inputGraphics = input.getGraphics();

			((Graphics2D) inputGraphics).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			((Graphics2D) inputGraphics).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			DirectColorModel colorModel = (DirectColorModel) input.getColorModel();
			PaletteData palette = new PaletteData(colorModel.getRedMask(), colorModel.getGreenMask(), colorModel.getBlueMask());
			middle = new ImageData(width, height, colorModel.getPixelSize(), palette);

			canvas = new Canvas(target, SWT.NO_BACKGROUND);
			canvas.addPaintListener(new PaintListener() {

				@Override
				public void paintControl(PaintEvent e) {
//					;//System.out.println("        drawing background for sub canvas <" + Nub.this + ">");
					if (output != null)
						output.dispose();
					output = updageAndMakeImage(e.gc.getDevice(), Math.max(Nub.this.width, e.width), Math.max(Nub.this.height,e.height));

					;//System.out.println("        image is <" + output + ">");

					if (output != null)
					{
						Rectangle mapped = Launcher.getLauncher().display.map(canvas, canvas.getShell(), new Rectangle(0, 0, Math.max(Nub.this.width, e.width), Math.max(Nub.this.height,e.height)));
						;//System.out.println(" drawing image at origin <"+output+"> <"+e.gc+"> <"+e.x+" "+e.y+"> <"+canvas.getBounds()+"> <"+mapped+">");
						e.gc.drawImage(output, 0, 0);
					}
				}
			});

			Listener ll = new Listener() {

				boolean down;

				@Override
				public void handleEvent(Event event) {

//					;//System.out.println(" event in component <" + event + ">");

					currentNub = Nub.this;

					switch (event.type) {
					case (SWT.MouseMove):
						if (down) {
							java.awt.event.MouseEvent m = new java.awt.event.MouseEvent(Nub.this.component, java.awt.event.MouseEvent.MOUSE_DRAGGED, event.time, modifiers(event), event.x, event.y, event.x, event.y, 0, false, event.button);
							Nub.this.component.dispatchEvent(m);
							Nub.this.canvas.redraw();
							deferedRedraw(Nub.this.target);
						} else {
							java.awt.event.MouseEvent m = new java.awt.event.MouseEvent(Nub.this.component, java.awt.event.MouseEvent.MOUSE_MOVED, event.time, modifiers(event), event.x, event.y, event.x, event.y, 0, false, event.button);
							Nub.this.component.dispatchEvent(m);
							// Nub.this.target.redraw();
						}
						break;
					case (SWT.MouseUp): {
						down = false;
						java.awt.event.MouseEvent m = new java.awt.event.MouseEvent(Nub.this.component, java.awt.event.MouseEvent.MOUSE_RELEASED, event.time, modifiers(event), event.x, event.y, event.x, event.y, 0, false, event.button);
						Nub.this.component.dispatchEvent(m);
						Nub.this.canvas.redraw();
						deferedRedraw(Nub.this.target);
					}
						break;
					case (SWT.MouseDown): {
						java.awt.event.MouseEvent m = new java.awt.event.MouseEvent(Nub.this.component, java.awt.event.MouseEvent.MOUSE_PRESSED, event.time, modifiers(event), event.x, event.y, event.x, event.y, event.count, false, event.button);
						Nub.this.component.dispatchEvent(m);
						down = true;
						Nub.this.canvas.redraw();
						deferedRedraw(Nub.this.target);
					}
						break;
					case (SWT.MouseDoubleClick): {
						java.awt.event.MouseEvent m = new java.awt.event.MouseEvent(Nub.this.component, java.awt.event.MouseEvent.MOUSE_PRESSED, event.time, modifiers(event), event.x, event.y, event.x, event.y, 2, false, event.button);
						Nub.this.component.dispatchEvent(m);
						down = true;
						Nub.this.canvas.redraw();
						deferedRedraw(Nub.this.target);
					}
						break;

					}
				}

				private int modifiers(Event event) {
					int m = 0;
					if ((event.stateMask & SWT.SHIFT) != 0)
						m |= java.awt.Event.SHIFT_MASK;
					if ((event.stateMask & SWT.ALT) != 0)
						m |= java.awt.Event.ALT_MASK;
					if ((event.stateMask & SWT.COMMAND) != 0)
						m |= java.awt.Event.META_MASK;
					return m;

				}
			};
			canvas.addListener(SWT.MouseMove, ll);
			canvas.addListener(SWT.MouseDown, ll);
			canvas.addListener(SWT.MouseUp, ll);
			// canvas.addListener(SWT.MouseDoubleClick, ll);

			canvas.setData(this);
		}

		@Override
		public Canvas getControl() {
			return canvas;
		}

		@Override
		public StyledText getText() {
			return target;
		}

		protected Image updageAndMakeImage(Device d, int width, int height) {

			if (disposed)
				return null;

			;//System.out.println(" update and make image <"+d+"> <"+width+"> <"+height+">");
			
			component.setBounds(0, 0, width, height);

			Method pc = ReflectionTools.findFirstMethodCalled(component.getClass(), "paintComponent");
			try {
				inputGraphics.setColor(new java.awt.Color(0.5f, 0.5f, 0.5f, 1f));
				inputGraphics.fillRect(0, 0, width, height);
				pc.invoke(component, inputGraphics.create());

				Image image2 = new Image(d, convertToSWT(input, middle, 128));

				return image2;

			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}

			return null;
		}

		public void setDimensions(int w, int h) {
			if (w != width || h != height) {
				width = w;
				height = h;
				input = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(width, height, Transparency.TRANSLUCENT);

				inputGraphics = input.getGraphics();
				DirectColorModel colorModel = (DirectColorModel) input.getColorModel();
				PaletteData palette = new PaletteData(colorModel.getRedMask(), colorModel.getGreenMask(), colorModel.getBlueMask());
				middle = new ImageData(width, height, colorModel.getPixelSize(), palette);
			}
		}

		public void destroy() {
			this.component.putClientProperty("dead", true);
			this.output.dispose();
			this.canvas.dispose();
			disposed = true;
		}

		public JComponent getComponent() {
			return component;
		}

		public void updateAllStylesNow() {
			CustomInsertDrawing.this.updateAllStylesNow();
		}
	}

	HashMap<JComponent, Nub> nubs = new HashMap<JComponent, Nub>();

	public CustomInsertDrawing(final StyledText target)

	{
		// TODO verify listener for dispose
		target.addPaintListener(new PaintListener() {

			@Override
			public void paintControl(PaintEvent e) {
			}
		});

		target.addVerifyListener(new VerifyListener() {

			@Override
			public void verifyText(VerifyEvent e) {
				int start = e.start;
				int end = e.end;

//				;//System.out.println(" text event at <"+e.start+" -> "+e.end+">");
				
				Iterator<Map.Entry<JComponent, Nub>> i = nubs.entrySet().iterator();
				while (i.hasNext()) {
					Nub v = i.next().getValue();

//					;//System.out.println(" nub is at <"+v.start+" "+v.start+v.length+">");
					
					if (start < v.start + v.length && end > v.start) {
						;//System.out.println(" removing nub <" + v + ">");
						i.remove();
						v.destroy();
					}
					else if (start < v.start)
					{
						int delta = e.text.length()-(e.end-e.start);
						v.start+=delta;
					}
				}

			}
		});

		target.addPaintObjectListener(new PaintObjectListener() {
			public void paintObject(PaintObjectEvent event) {

				;//System.out.println(" paint object event at <"+event.x+", "+event.y+">");
//				new Exception().printStackTrace();
				
				if (event.style.data != null) {
//					;//System.out.println(" picked up an component embedded here <" + event.style.data + "> <" + event.x + " " + event.y + " " + event.ascent + " " + event.descent + " @ " + event.style + ">");

					int width = event.style.length * event.style.metrics.width;

					int height = event.ascent + event.descent;

					if (((JComponent) event.style.data).getClientProperty("dead") != null) {
//						;//System.out.println(" trying to draw dead component ");
						event.style.data = null;
						return;
					}

					((JComponent) event.style.data).setBounds(event.x, event.y, width, height);

					Nub nub = nubs.get(event.style.data);

					boolean found = false;
					if (nub != null) {
						
//						;//System.out.println(" control has <"+target.getChildren().length+"> children");
						
						for (Control cc : target.getChildren()) {
//							;//System.out.println(" checking :"+cc+" / "+nub.canvas);
							
							if (cc == nub.canvas)
								found = true;
						}
					}
					if (nub == null || !found) {
//						;//System.out.println(" :: needs a new nub ");
						nub = new Nub(((JComponent) event.style.data), target, width, height);
						nub.canvas.setBounds(event.x, event.y, width, height);

						nubs.put((JComponent) event.style.data, nub);

						if (event.style.data instanceof iAcceptsInsertRenderingContext) {
							((iAcceptsInsertRenderingContext) event.style.data).setInsertRenderingContext(nub);
						}

					} else {
						nub.setDimensions(width, height);
						nub.canvas.setBounds(event.x, event.y, width, height);
					}

					nub.start = event.style.start;
					nub.length = event.style.length;

//					;//System.out.println(" nub starts at <" + event.style.start + " " + event.style.length + ">");

					// BufferedImage image =
					// GraphicsEnvironment
					// .getLocalGraphicsEnvironment()
					// .getDefaultScreenDevice()
					// .getDefaultConfiguration()
					// .createCompatibleImage(width, height,
					// Transparency.TRANSLUCENT);
					//
					// Method pc =
					// ReflectionTools.findFirstMethodCalled(
					// ((JComponent)
					// event.style.data).getClass(),
					// "paintComponent");
					// try {
					// pc.invoke((JComponent)
					// event.style.data,
					// image.getGraphics());
					//
					// Image image2 = new
					// Image(event.gc.getDevice(),
					// convertToSWT(image));
					//
					// event.gc.drawImage(image2, event.x,
					// event.y);
					//
					// } catch (IllegalArgumentException e)
					// {
					// e.printStackTrace();
					// } catch (IllegalAccessException e) {
					// e.printStackTrace();
					// } catch (InvocationTargetException e)
					// {
					// e.printStackTrace();
					// }
				}
				// Control control = (Control)event.style.data;
				// Point pt = control.getSize();
				// int x = event.x + MARGIN;
				// int y = event.y + event.ascent - 2*pt.y/3;
				// control.setLocation(x, y);
			}
		});

	}

	public void updateAllStylesNow() {
		
	}

	static public ImageData convertToSWT(BufferedImage bufferedImage, int background) {
		DirectColorModel colorModel = (DirectColorModel) bufferedImage.getColorModel();
		PaletteData palette = new PaletteData(colorModel.getRedMask(), colorModel.getGreenMask(), colorModel.getBlueMask());
		ImageData data = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(), colorModel.getPixelSize(), palette);

		WritableRaster raster = bufferedImage.getRaster();
		int[] pixelArray = new int[4];
		for (int y = 0; y < data.height; y++) {
			for (int x = 0; x < data.width; x++) {
				raster.getPixel(x, y, pixelArray);
				if (pixelArray[3] > 0) {
					float f = pixelArray[3] / 255f;

					pixelArray[0] = (int) Math.min(255, pixelArray[0] * f + background * (1 - f));
					pixelArray[1] = (int) Math.min(255, pixelArray[1] * f + background * (1 - f));
					pixelArray[2] = (int) Math.min(255, pixelArray[2] * f + background * (1 - f));
				}
				int pixel = palette.getPixel(new RGB(pixelArray[0], pixelArray[1], pixelArray[2]));

				// ;//System.out.println(x+" "+y+" pixel :"+pixelArray[0]+" "+pixelArray[1]+" "+pixelArray[2]);

				data.setPixel(x, y, pixel);
				data.setAlpha(x, y, 255);
			}
		}
		return data;
	}

	static public ImageData convertToSWT(BufferedImage bufferedImage, ImageData data, int background) {
		DirectColorModel colorModel = (DirectColorModel) bufferedImage.getColorModel();
		PaletteData palette = new PaletteData(colorModel.getRedMask(), colorModel.getGreenMask(), colorModel.getBlueMask());

		WritableRaster raster = bufferedImage.getRaster();
		int[] pixelArray = new int[4];
		for (int y = 0; y < data.height; y++) {
			for (int x = 0; x < data.width; x++) {
				raster.getPixel(x, y, pixelArray);

				if (pixelArray[3] > 0) {
					float f = pixelArray[3] / 255f;

					pixelArray[0] = (int) Math.min(255, pixelArray[0] * f + background * (1 - f));
					pixelArray[1] = (int) Math.min(255, pixelArray[1] * f + background * (1 - f));
					pixelArray[2] = (int) Math.min(255, pixelArray[2] * f + background * (1 - f));
				}
				int pixel = palette.getPixel(new RGB(pixelArray[0], pixelArray[1], pixelArray[2]));

				// ;//System.out.println(x+" "+y+" pixel :"+pixelArray[0]+" "+pixelArray[1]+" "+pixelArray[2]);

				data.setPixel(x, y, pixel);
				// data.setAlpha(x, y, pixelArray[3]);
				data.setAlpha(x, y, 255);
			}
		}
		return data;
	}

	@NextUpdate
	protected void deferedRedraw(StyledText target) {
		;//System.out.println(" -- inside defered redraw --");
		target.redraw();
	}

}
