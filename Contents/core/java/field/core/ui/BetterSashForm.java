/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package field.core.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Sash;

import field.launch.Launcher;

/**
 * The BetterSashForm is a composite control that lays out its children in a row
 * or column arrangement (as specified by the orientation) and places a Sash
 * between each child. One child may be maximized to occupy the entire size of
 * the BetterSashForm. The relative sizes of the children may be specified using
 * weights.
 * <p>
 * <dl>
 * <dt><b>Styles:</b></dt>
 * <dd>HORIZONTAL, VERTICAL, SMOOTH</dd>
 * </dl>
 * </p>
 * 
 * @see <a href="http://www.eclipse.org/swt/snippets/#sashform">SashForm
 *      snippets</a>
 * @see <a href="http://www.eclipse.org/swt/examples.php">SWT Example:
 *      CustomControlExample</a>
 * @see <a href="http://www.eclipse.org/swt/">Sample code and further
 *      information</a>
 */
public class BetterSashForm extends Canvas {
	static class BetterSashFormData {

		long weight;

		String getName() {
			String string = getClass().getName();
			int index = string.lastIndexOf('.');
			if (index == -1)
				return string;
			return string.substring(index + 1, string.length());
		}

		/**
		 * Returns a string containing a concise, human-readable
		 * description of the receiver.
		 * 
		 * @return a string representation of the event
		 */
		public String toString() {
			return getName() + " {weight=" + weight + "}"; //$NON-NLS-2$
		}
	}

	static class BetterSashFormLayout extends Layout {
		protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {
			BetterSashForm BetterSashForm = (BetterSashForm) composite;
			Control[] cArray = BetterSashForm.getControls(true);
			int width = 0;
			int height = 0;
			if (cArray.length == 0) {
				if (wHint != SWT.DEFAULT)
					width = wHint;
				if (hHint != SWT.DEFAULT)
					height = hHint;
				return new Point(width, height);
			}
			// determine control sizes
			boolean vertical = BetterSashForm.getOrientation() == SWT.VERTICAL;
			int maxIndex = 0;
			int maxValue = 0;
			for (int i = 0; i < cArray.length; i++) {
				if (vertical) {
					Point size = cArray[i].computeSize(wHint, SWT.DEFAULT, flushCache);
					if (size.y > maxValue) {
						maxIndex = i;
						maxValue = size.y;
					}
					width = Math.max(width, size.x);
				} else {
					Point size = cArray[i].computeSize(SWT.DEFAULT, hHint, flushCache);
					if (size.x > maxValue) {
						maxIndex = i;
						maxValue = size.x;
					}
					height = Math.max(height, size.y);
				}
			}
			// get the ratios
			long[] ratios = new long[cArray.length];
			long total = 0;
			for (int i = 0; i < cArray.length; i++) {
				Object data = cArray[i].getLayoutData();
				if (data != null && data instanceof BetterSashFormData) {
					ratios[i] = ((BetterSashFormData) data).weight;
				} else {
					data = new BetterSashFormData();
					cArray[i].setLayoutData(data);
					((BetterSashFormData) data).weight = ratios[i] = ((200 << 16) + 999) / 1000;

				}
				total += ratios[i];
			}
			if (ratios[maxIndex] > 0) {
				int sashwidth = BetterSashForm.sashes.length > 0 ? BetterSashForm.SASH_WIDTH + BetterSashForm.sashes[0].getBorderWidth() * 2 : BetterSashForm.SASH_WIDTH;
				if (vertical) {
					height += (int) (total * maxValue / ratios[maxIndex]) + (cArray.length - 1) * sashwidth;
				} else {
					width += (int) (total * maxValue / ratios[maxIndex]) + (cArray.length - 1) * sashwidth;
				}
			}
			width += BetterSashForm.getBorderWidth() * 2;
			height += BetterSashForm.getBorderWidth() * 2;
			if (wHint != SWT.DEFAULT)
				width = wHint;
			if (hHint != SWT.DEFAULT)
				height = hHint;
			return new Point(width, height);
		}

		protected boolean flushCache(Control control) {
			return true;
		}

		protected void layout(Composite composite, boolean flushCache) {
			BetterSashForm sashForm = (BetterSashForm) composite;
			Rectangle area = sashForm.getClientArea();
			if (area.width <= 1 || area.height <= 1)
				return;

			Control[] newControls = sashForm.getControls(true);
			if (sashForm.controls.length == 0 && newControls.length == 0)
				return;
			sashForm.controls = newControls;

			Control[] controls = sashForm.controls;

			if (sashForm.maxControl != null && !sashForm.maxControl.isDisposed()) {
				for (int i = 0; i < controls.length; i++) {
					if (controls[i] != sashForm.maxControl) {
						controls[i].setBounds(-200, -200, 0, 0);
					} else {
						controls[i].setBounds(area);
					}
				}
				return;
			}

			// keep just the right number of sashes
			if (sashForm.sashes.length < controls.length - 1) {
				Sash[] newSashes = new Sash[controls.length - 1];
				System.arraycopy(sashForm.sashes, 0, newSashes, 0, sashForm.sashes.length);
				for (int i = sashForm.sashes.length; i < newSashes.length; i++) {
					newSashes[i] = sashForm.createSash();
				}
				sashForm.sashes = newSashes;
			}
			if (sashForm.sashes.length > controls.length - 1) {
				if (controls.length == 0) {
					for (int i = 0; i < sashForm.sashes.length; i++) {
						sashForm.sashes[i].dispose();
					}
					sashForm.sashes = new Sash[0];
				} else {
					Sash[] newSashes = new Sash[controls.length - 1];
					System.arraycopy(sashForm.sashes, 0, newSashes, 0, newSashes.length);
					for (int i = controls.length - 1; i < sashForm.sashes.length; i++) {
						sashForm.sashes[i].dispose();
					}
					sashForm.sashes = newSashes;
				}
			}
			if (controls.length == 0)
				return;
			Sash[] sashes = sashForm.sashes;
			// get the ratios
			long[] ratios = new long[controls.length];
			long total = 0;
			for (int i = 0; i < controls.length; i++) {
				Object data = controls[i].getLayoutData();
				if (data != null && data instanceof BetterSashFormData) {
					ratios[i] = ((BetterSashFormData) data).weight;
				} else {
					data = new BetterSashFormData();
					controls[i].setLayoutData(data);
					((BetterSashFormData) data).weight = ratios[i] = ((200 << 16) + 999) / 1000;

				}
				total += ratios[i];
			}

			int sashwidth = sashes.length > 0 ? sashForm.SASH_WIDTH + sashes[0].getBorderWidth() * 2 : sashForm.SASH_WIDTH;
			if (sashForm.getOrientation() == SWT.HORIZONTAL) {
				int width = (int) (ratios[0] * (area.width - sashes.length * sashwidth) / total);
				int x = area.x;
				controls[0].setBounds(x, area.y, width, area.height);
				x += width;
				for (int i = 1; i < controls.length - 1; i++) {
					sashes[i - 1].setBounds(x, area.y, sashwidth, area.height);
					x += sashwidth;
					width = (int) (ratios[i] * (area.width - sashes.length * sashwidth) / total);
					controls[i].setBounds(x, area.y, width, area.height);
					x += width;
				}
				if (controls.length > 1) {
					sashes[sashes.length - 1].setBounds(x, area.y, sashwidth, area.height);
					x += sashwidth;
					width = area.width - x;
					controls[controls.length - 1].setBounds(x, area.y, width, area.height);
				}
			} else {
				int height = (int) (ratios[0] * (area.height - sashes.length * sashwidth) / total);
				int y = area.y;
				controls[0].setBounds(area.x, y, area.width, height);
				y += height;
				for (int i = 1; i < controls.length - 1; i++) {
					sashes[i - 1].setBounds(area.x, y, area.width, sashwidth);
					y += sashwidth;
					height = (int) (ratios[i] * (area.height - sashes.length * sashwidth) / total);
					controls[i].setBounds(area.x, y, area.width, height);
					y += height;
				}
				if (controls.length > 1) {
					sashes[sashes.length - 1].setBounds(area.x, y, area.width, sashwidth);
					y += sashwidth;
					height = area.height - y;
					controls[controls.length - 1].setBounds(area.x, y, area.width, height);
				}

			}
		}
	}

	/**
	 * The width of all sashes in the form.
	 */
	public int SASH_WIDTH = 3;

	int sashStyle;
	Sash[] sashes = new Sash[0];
	// Remember background and foreground
	// colors to determine whether to set
	// sashes to the default color (null) or
	// a specific color
	Color background = null;
	Color foreground = null;
	Control[] controls = new Control[0];
	Control maxControl = null;
	Listener sashListener;
	static final int DRAG_MINIMUM = 20;

	/**
	 * Constructs a new instance of this class given its parent and a style
	 * value describing its behavior and appearance.
	 * <p>
	 * The style value is either one of the style constants defined in class
	 * <code>SWT</code> which is applicable to instances of this class, or
	 * must be built by <em>bitwise OR</em>'ing together (that is, using the
	 * <code>int</code> "|" operator) two or more of those <code>SWT</code>
	 * style constants. The class description lists the style constants that
	 * are applicable to the class. Style bits are also inherited from
	 * superclasses.
	 * </p>
	 * 
	 * @param parent
	 *                a widget which will be the parent of the new instance
	 *                (cannot be null)
	 * @param style
	 *                the style of widget to construct
	 * 
	 * @exception IllegalArgumentException
	 *                    <ul>
	 *                    <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
	 *                    </ul>
	 * @exception SWTException
	 *                    <ul>
	 *                    <li>ERROR_THREAD_INVALID_ACCESS - if not called
	 *                    from the thread that created the parent</li>
	 *                    </ul>
	 * 
	 * @see SWT#HORIZONTAL
	 * @see SWT#VERTICAL
	 * @see #getStyle()
	 */
	public BetterSashForm(Composite parent, int style) {
		super(parent, checkStyle(style));
		super.setLayout(new BetterSashFormLayout());
		sashStyle = ((style & SWT.VERTICAL) != 0) ? SWT.HORIZONTAL : SWT.VERTICAL;
		if ((style & SWT.BORDER) != 0)
			sashStyle |= SWT.BORDER;
		if ((style & SWT.SMOOTH) != 0)
			sashStyle |= SWT.SMOOTH;
		sashListener = new Listener() {
			public void handleEvent(Event e) {
				onDragSash(e);
			}
		};


	}

	static int checkStyle(int style) {
		int mask = SWT.BORDER | SWT.LEFT_TO_RIGHT | SWT.RIGHT_TO_LEFT;
		return style & mask;
	}

	Sash createSash() {
		Sash sash = new Sash(this, sashStyle);
		sash.setBackground(background);
		sash.setForeground(foreground);
		sash.setToolTipText(getToolTipText());
		sash.addListener(SWT.Selection, sashListener);
		return sash;
	}

	/**
	 * Returns SWT.HORIZONTAL if the controls in the BetterSashForm are laid
	 * out side by side or SWT.VERTICAL if the controls in the
	 * BetterSashForm are laid out top to bottom.
	 * 
	 * @return SWT.HORIZONTAL or SWT.VERTICAL
	 */
	public int getOrientation() {
		// checkWidget();
		return (sashStyle & SWT.VERTICAL) != 0 ? SWT.HORIZONTAL : SWT.VERTICAL;
	}

	/**
	 * Returns the width of the sashes when the controls in the
	 * BetterSashForm are laid out.
	 * 
	 * @return the width of the sashes
	 * 
	 * @exception SWTException
	 *                    <ul>
	 *                    <li>ERROR_WIDGET_DISPOSED - if the receiver has
	 *                    been disposed</li>
	 *                    <li>ERROR_THREAD_INVALID_ACCESS - if not called
	 *                    from the thread that created the receiver</li>
	 *                    </ul>
	 * 
	 * @since 3.4
	 */
	public int getSashWidth() {
		checkWidget();
		return SASH_WIDTH;
	}

	public int getStyle() {
		int style = super.getStyle();
		style |= getOrientation() == SWT.VERTICAL ? SWT.VERTICAL : SWT.HORIZONTAL;
		if ((sashStyle & SWT.SMOOTH) != 0)
			style |= SWT.SMOOTH;
		return style;
	}

	/**
	 * Answer the control that currently is maximized in the BetterSashForm.
	 * This value may be null.
	 * 
	 * @return the control that currently is maximized or null
	 */
	public Control getMaximizedControl() {
		// checkWidget();
		return this.maxControl;
	}

	/**
	 * Answer the relative weight of each child in the BetterSashForm. The
	 * weight represents the percent of the total width (if BetterSashForm
	 * has Horizontal orientation) or total height (if BetterSashForm has
	 * Vertical orientation) each control occupies. The weights are returned
	 * in order of the creation of the widgets (weight[0] corresponds to the
	 * weight of the first child created).
	 * 
	 * @return the relative weight of each child
	 * 
	 * @exception SWTException
	 *                    <ul>
	 *                    <li>ERROR_WIDGET_DISPOSED - if the receiver has
	 *                    been disposed</li>
	 *                    <li>ERROR_THREAD_INVALID_ACCESS - if not called
	 *                    from the thread that created the receiver</li>
	 *                    </ul>
	 */

	public int[] getWeights() {
		checkWidget();
		Control[] cArray = getControls(false);
		int[] ratios = new int[cArray.length];
		for (int i = 0; i < cArray.length; i++) {
			Object data = cArray[i].getLayoutData();
			if (data != null && data instanceof BetterSashFormData) {
				ratios[i] = (int) (((BetterSashFormData) data).weight * 1000 >> 16);
			} else {
				ratios[i] = 200;
			}
		}
		return ratios;
	}

	Control[] getControls(boolean onlyVisible) {
		Control[] children = getChildren();
		Control[] result = new Control[0];
		for (int i = 0; i < children.length; i++) {
			if (children[i] instanceof Sash)
				continue;
			if (onlyVisible && !children[i].getVisible())
				continue;

			Control[] newResult = new Control[result.length + 1];
			System.arraycopy(result, 0, newResult, 0, result.length);
			newResult[result.length] = children[i];
			result = newResult;
		}
		return result;
	}

	void onDragSash(Event event) {
		Sash sash = (Sash) event.widget;
		int sashIndex = -1;
		for (int i = 0; i < sashes.length; i++) {
			if (sashes[i] == sash) {
				sashIndex = i;
				break;
			}
		}
		if (sashIndex == -1)
			return;

		Control c1 = controls[sashIndex];
		Control c2 = controls[sashIndex + 1];
		Rectangle b1 = c1.getBounds();
		Rectangle b2 = c2.getBounds();

		Rectangle sashBounds = sash.getBounds();
		Rectangle area = getClientArea();
		boolean correction = false;
		if (getOrientation() == SWT.HORIZONTAL) {
			correction = b1.width < DRAG_MINIMUM || b2.width < DRAG_MINIMUM;
			int totalWidth = b2.x + b2.width - b1.x;
			int shift = event.x - sashBounds.x;
			b1.width += shift;
			b2.x += shift;
			b2.width -= shift;
			if (b1.width < DRAG_MINIMUM) {
				b1.width = DRAG_MINIMUM;
				b2.x = b1.x + b1.width + sashBounds.width;
				b2.width = totalWidth - b2.x;
				event.x = b1.x + b1.width;
				event.doit = false;
			}
			if (b2.width < DRAG_MINIMUM) {
				b1.width = totalWidth - DRAG_MINIMUM - sashBounds.width;
				b2.x = b1.x + b1.width + sashBounds.width;
				b2.width = DRAG_MINIMUM;
				event.x = b1.x + b1.width;
				event.doit = false;
			}
			Object data1 = c1.getLayoutData();
			if (data1 == null || !(data1 instanceof BetterSashFormData)) {
				data1 = new BetterSashFormData();
				c1.setLayoutData(data1);
			}
			Object data2 = c2.getLayoutData();
			if (data2 == null || !(data2 instanceof BetterSashFormData)) {
				data2 = new BetterSashFormData();
				c2.setLayoutData(data2);
			}
			((BetterSashFormData) data1).weight = (((long) b1.width << 16) + area.width - 1) / area.width;
			((BetterSashFormData) data2).weight = (((long) b2.width << 16) + area.width - 1) / area.width;
		} else {
			correction = b1.height < DRAG_MINIMUM || b2.height < DRAG_MINIMUM;
			int totalHeight = b2.y + b2.height - b1.y;
			int shift = event.y - sashBounds.y;
			b1.height += shift;
			b2.y += shift;
			b2.height -= shift;
			if (b1.height < DRAG_MINIMUM) {
				b1.height = DRAG_MINIMUM;
				b2.y = b1.y + b1.height + sashBounds.height;
				b2.height = totalHeight - b2.y;
				event.y = b1.y + b1.height;
				event.doit = false;
			}
			if (b2.height < DRAG_MINIMUM) {
				b1.height = totalHeight - DRAG_MINIMUM - sashBounds.height;
				b2.y = b1.y + b1.height + sashBounds.height;
				b2.height = DRAG_MINIMUM;
				event.y = b1.y + b1.height;
				event.doit = false;
			}
			Object data1 = c1.getLayoutData();
			if (data1 == null || !(data1 instanceof BetterSashFormData)) {
				data1 = new BetterSashFormData();
				c1.setLayoutData(data1);
			}
			Object data2 = c2.getLayoutData();
			if (data2 == null || !(data2 instanceof BetterSashFormData)) {
				data2 = new BetterSashFormData();
				c2.setLayoutData(data2);
			}
			((BetterSashFormData) data1).weight = (((long) b1.height << 16) + area.height - 1) / area.height;
			((BetterSashFormData) data2).weight = (((long) b2.height << 16) + area.height - 1) / area.height;
		}
		if (correction || (event.doit && event.detail != SWT.DRAG)) {
			c1.setBounds(b1);
			sash.setBounds(event.x, event.y, event.width, event.height);
			c2.setBounds(b2);
		}
	}

	/**
	 * If orientation is SWT.HORIZONTAL, lay the controls in the
	 * BetterSashForm out side by side. If orientation is SWT.VERTICAL, lay
	 * the controls in the BetterSashForm out top to bottom.
	 * 
	 * @param orientation
	 *                SWT.HORIZONTAL or SWT.VERTICAL
	 * 
	 * @exception SWTException
	 *                    <ul>
	 *                    <li>ERROR_WIDGET_DISPOSED - if the receiver has
	 *                    been disposed</li>
	 *                    <li>ERROR_THREAD_INVALID_ACCESS - if not called
	 *                    from the thread that created the receiver</li>
	 *                    <li>ERROR_INVALID_ARGUMENT - if the value of
	 *                    orientation is not SWT.HORIZONTAL or SWT.VERTICAL
	 *                    </ul>
	 */
	public void setOrientation(int orientation) {
		checkWidget();
		if (getOrientation() == orientation)
			return;
		if (orientation != SWT.HORIZONTAL && orientation != SWT.VERTICAL) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		sashStyle &= ~(SWT.HORIZONTAL | SWT.VERTICAL);
		sashStyle |= orientation == SWT.VERTICAL ? SWT.HORIZONTAL : SWT.VERTICAL;
		for (int i = 0; i < sashes.length; i++) {
			sashes[i].dispose();
			sashes[i] = createSash();
		}
		layout(false);
	}

	public void setBackground(Color color) {
		super.setBackground(color);
		background = color;
		for (int i = 0; i < sashes.length; i++) {
			sashes[i].setBackground(background);
		}
	}

	public void setForeground(Color color) {
		super.setForeground(color);
		foreground = color;
		for (int i = 0; i < sashes.length; i++) {
			sashes[i].setForeground(foreground);
		}
	}

	/**
	 * Sets the layout which is associated with the receiver to be the
	 * argument which may be null.
	 * <p>
	 * Note: No Layout can be set on this Control because it already manages
	 * the size and position of its children.
	 * </p>
	 * 
	 * @param layout
	 *                the receiver's new layout or null
	 * 
	 * @exception SWTException
	 *                    <ul>
	 *                    <li>ERROR_WIDGET_DISPOSED - if the receiver has
	 *                    been disposed</li>
	 *                    <li>ERROR_THREAD_INVALID_ACCESS - if not called
	 *                    from the thread that created the receiver</li>
	 *                    </ul>
	 */
	public void setLayout(Layout layout) {
		checkWidget();
		return;
	}

	/**
	 * Specify the control that should take up the entire client area of the
	 * BetterSashForm. If one control has been maximized, and this method is
	 * called with a different control, the previous control will be
	 * minimized and the new control will be maximized. If the value of
	 * control is null, the BetterSashForm will minimize all controls and
	 * return to the default layout where all controls are laid out
	 * separated by sashes.
	 * 
	 * @param control
	 *                the control to be maximized or null
	 * 
	 * @exception SWTException
	 *                    <ul>
	 *                    <li>ERROR_WIDGET_DISPOSED - if the receiver has
	 *                    been disposed</li>
	 *                    <li>ERROR_THREAD_INVALID_ACCESS - if not called
	 *                    from the thread that created the receiver</li>
	 *                    </ul>
	 */
	public void setMaximizedControl(Control control) {
		checkWidget();
		if (control == null) {
			if (maxControl != null) {
				this.maxControl = null;
				layout(false);
				for (int i = 0; i < sashes.length; i++) {
					sashes[i].setVisible(true);
				}
			}
			return;
		}

		for (int i = 0; i < sashes.length; i++) {
			sashes[i].setVisible(false);
		}
		maxControl = control;
		layout(false);
	}

	/**
	 * Specify the width of the sashes when the controls in the
	 * BetterSashForm are laid out.
	 * 
	 * @param width
	 *                the width of the sashes
	 * 
	 * @exception SWTException
	 *                    <ul>
	 *                    <li>ERROR_WIDGET_DISPOSED - if the receiver has
	 *                    been disposed</li>
	 *                    <li>ERROR_THREAD_INVALID_ACCESS - if not called
	 *                    from the thread that created the receiver</li>
	 *                    </ul>
	 * 
	 * @since 3.4
	 */
	public void setSashWidth(int width) {
		checkWidget();
		if (SASH_WIDTH == width)
			return;
		SASH_WIDTH = width;
		layout(false);
	}

	public void setToolTipText(String string) {
		super.setToolTipText(string);
		for (int i = 0; i < sashes.length; i++) {
			sashes[i].setToolTipText(string);
		}
	}

	/**
	 * Specify the relative weight of each child in the BetterSashForm. This
	 * will determine what percent of the total width (if BetterSashForm has
	 * Horizontal orientation) or total height (if BetterSashForm has
	 * Vertical orientation) each control will occupy. The weights must be
	 * positive values and there must be an entry for each non-sash child of
	 * the BetterSashForm.
	 * 
	 * @param weights
	 *                the relative weight of each child
	 * 
	 * @exception SWTException
	 *                    <ul>
	 *                    <li>ERROR_WIDGET_DISPOSED - if the receiver has
	 *                    been disposed</li>
	 *                    <li>ERROR_THREAD_INVALID_ACCESS - if not called
	 *                    from the thread that created the receiver</li>
	 *                    <li>ERROR_INVALID_ARGUMENT - if the weights value
	 *                    is null or of incorrect length (must match the
	 *                    number of children)</li>
	 *                    </ul>
	 */
	public void setWeights(int[] weights) {
		checkWidget();
		Control[] cArray = getControls(false);
		if (weights == null || weights.length != cArray.length) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}

		int total = 0;
		for (int i = 0; i < weights.length; i++) {
			if (weights[i] < 0) {
				SWT.error(SWT.ERROR_INVALID_ARGUMENT);
			}
			total += weights[i];
		}
		if (total == 0) {
			SWT.error(SWT.ERROR_INVALID_ARGUMENT);
		}
		for (int i = 0; i < cArray.length; i++) {
			Object data = cArray[i].getLayoutData();
			if (data == null || !(data instanceof BetterSashFormData)) {
				data = new BetterSashFormData();
				cArray[i].setLayoutData(data);
			}
			((BetterSashFormData) data).weight = (((long) weights[i] << 16) + total - 1) / total;
		}

		layout(false);
	}

	

	@Override
	public void drawBackground(GC gc, int x, int y, int width, int height) {
		System.out.println(" db 2");
		super.drawBackground(gc, x, y, width, height);
	}
	
	@Override
	public void drawBackground(GC gc, int x, int y, int width, int height, int offsetX, int offsetY) {
		System.out.println(" db 1");
		super.drawBackground(gc, x, y, width, height, offsetX, offsetY);
	}

}
