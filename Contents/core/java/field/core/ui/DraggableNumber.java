package field.core.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.RoundRectangle2D.Float;
import java.text.NumberFormat;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import field.core.Constants;

/**
 * 
 * lifted wholesale out of the wonderful NodeBox 2 (GPL). (But the bugs are
 * ours, not theirs).
 * 
 */
public class DraggableNumber extends JComponent implements MouseListener, MouseMotionListener, ComponentListener {

	private static Image draggerLeft, draggerRight;
	private static int draggerLeftWidth, draggerRightWidth, draggerHeight;

	static {

		// draggerLeft = ImageIO.read(new File("res/dragger-left.png"));
		// draggerRight = ImageIO.read(new
		// File("res/dragger-right.png"));
		// draggerBackground = ImageIO.read(new
		// File("res/dragger-background.png"));

//		draggerLeft = ((ImageIcon) SmallMenu.makeIconFromCharacter('\u25c0', 20, 10, 0, new Color(0, 0, 0, 0.4f), new Color(0, 0, 0, 0.4f), 0.5f)).getImage();
//		draggerRight = ((ImageIcon) SmallMenu.makeIconFromCharacter('\u25b6', 20, 10, 0, new Color(0, 0, 0, 0.4f), new Color(0, 0, 0, 0.4f), 0.5f)).getImage();

		// draggerBackground = ((ImageIcon)
		// SmallMenu.makeIconFromCharacter(' ', 10, 8, 0, new Color(0,
		// 0, 0, 0.4f), new Color(0, 0, 0, 0.4f))).getImage();

//		draggerLeftWidth = draggerLeft.getWidth(null);
//		draggerRightWidth = draggerRight.getWidth(null);

		draggerLeftWidth = 30;
		draggerRightWidth = 30;
		draggerHeight = 20;
	}

	// todo: could use something like BoundedRangeModel (but then for
	// floats) for checking bounds.

	private JTextField numberField;
	private double oldValue, value;
	private int previousX;

	private Double minimumValue;
	private Double maximumValue;

	/**
	 * Only one <code>ChangeEvent</code> is needed per slider instance since
	 * the event's only (read-only) state is the source property. The source
	 * of events generated here is always "this". The event is lazily
	 * created the first time that an event notification is fired.
	 * 
	 * @see #fireStateChanged
	 */
	protected transient ChangeEvent changeEvent = null;

	private NumberFormat numberFormat;

	public DraggableNumber() {
		setLayout(null);
		addMouseListener(this);
		addMouseMotionListener(this);
		addComponentListener(this);
		Dimension d = new Dimension(87, 20);
		setPreferredSize(d);

		numberField = new JTextField();
		numberField.putClientProperty("JComponent.sizeVariant", "small");
		numberField.setFont(new Font(Constants.defaultFont, 0, 10));
		numberField.setHorizontalAlignment(JTextField.CENTER);
		numberField.setVisible(false);
		numberField.addKeyListener(new EscapeListener());
		numberField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				commitNumberField();
			}
		});
		add(numberField);

		numberFormat = NumberFormat.getNumberInstance();
		numberFormat.setMinimumFractionDigits(2);
		numberFormat.setMaximumFractionDigits(2);

		setValue(0);
		// Set the correct size for the numberField.
		componentResized(null);
	}

	// // Value ranges ////

	public Double getMinimumValue() {
		return minimumValue;
	}

	public boolean hasMinimumValue() {
		return minimumValue == null;
	}

	public void setMinimumValue(double minimumValue) {
		this.minimumValue = minimumValue;
	}

	public void clearMinimumValue() {
		this.minimumValue = null;
	}

	public Double getMaximumValue() {
		return maximumValue;
	}

	public boolean hasMaximumValue() {
		return maximumValue == null;
	}

	public void setMaximumValue(double maximumValue) {
		this.maximumValue = maximumValue;
	}

	public void clearMaximumValue() {
		this.maximumValue = null;
	}

	// // Value ////

	public double getValue() {
		return value;
	}

	public double clampValue(double value) {
		if (minimumValue != null && value < minimumValue)
			value = minimumValue;
		if (maximumValue != null && value > maximumValue)
			value = maximumValue;
		return value;
	}

	public void setValue(double value) {
		this.value = clampValue(value);
		String formattedNumber = numberFormat.format(getValue());
		numberField.setText(formattedNumber);
		repaint();
	}

	public void setStatusMixed(double value) {

		boolean changed = !numberField.getText().equals("\u2014");
		this.value = clampValue(value);
		String formattedNumber = numberFormat.format(getValue());
		numberField.setText("\u2014");
		if (changed)
			repaint();
	}

	public void setStatusUnset(double value) {
		boolean changed = !numberField.getText().equals(" ");
		this.value = clampValue(value);
		String formattedNumber = numberFormat.format(getValue());
		numberField.setText(" ");
		if (changed)
			repaint();
	}

	public void setValueFromString(String s) throws NumberFormatException {
		setValue(Double.parseDouble(s));
	}

	public String valueAsString() {
		return numberFormat.format(value);
	}

	// // Number formatting ////

	public NumberFormat getNumberFormat() {
		return numberFormat;
	}

	public void setNumberFormat(NumberFormat numberFormat) {
		this.numberFormat = numberFormat;
		// Refresh the label
		setValue(getValue());
	}

	private void commitNumberField() {
		numberField.setVisible(false);
		String s = numberField.getText();
		try {
			setValueFromString(s);
			fireStateChanged();
		} catch (NumberFormatException e) {
			Toolkit.getDefaultToolkit().beep();
		}
	}

	// // Component paint ////

	private Rectangle getLeftButtonRect(Rectangle r) {
		if (r == null)
			r = getBounds();
		return new Rectangle(r.x, r.y, draggerLeftWidth, draggerHeight);
	}

	private Rectangle getRightButtonRect(Rectangle r) {
		if (r == null)
			r = getBounds();
		return new Rectangle(r.width - draggerRightWidth, r.y, draggerRightWidth, draggerHeight);
	}

	@Override
	public void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Rectangle r = getBounds();

		Float inside = new RoundRectangle2D.Float(3, 4, r.width - 10, 16, 12, 12);
		g2.setColor(new Color(0, 0, 0, 0.25f));
		g2.setStroke(new BasicStroke(1));
		g2.draw(inside);
		g2.setColor(new Color(0.75f, 0.75f, 0.75f, 1f));
		g2.fill(inside);
		Float inside2 = new RoundRectangle2D.Float(3, 5, r.width - 10, 15, 12, 12);
		g2.setColor(new Color(0, 0, 0, 0.35f));
		g2.setStroke(new BasicStroke(1));
		g2.draw(inside2);

//		g2.drawImage(draggerLeft, 6, 2, null);
//		g2.drawImage(draggerRight, r.width - draggerRightWidth + 1, 2, null);

		// g2.drawImage(draggerBackground, draggerLeftWidth, 0,
		// centerWidth, draggerHeight, null);

		g2.setFont(new Font(Constants.defaultFont, 0, 10));
		g2.setColor(new Color(0, 0, 0, 0.8f));

		int ww = g2.getFontMetrics().stringWidth(valueAsString());
		g2.drawString(numberField.getText(), r.width / 2 - ww / 2, 16);
	}

	// // Component size ////

	@Override
	public Dimension getPreferredSize() {
		// The control is actually 20 pixels high, but setting the
		// height to 30 will leave a nice margin.
		return new Dimension(120, 25);
	}

	// // Component listeners

	public void componentResized(ComponentEvent e) {
		numberField.setBounds(19, 3, getWidth() - 43, 25);
	}

	public void componentMoved(ComponentEvent e) {
	}

	public void componentShown(ComponentEvent e) {
	}

	public void componentHidden(ComponentEvent e) {
	}

	// // Mouse listeners ////

	public void mousePressed(MouseEvent e) {
		
		;//System.out.println(" DN mouse pressed !"+e);
		
		oldValue = getValue();
		previousX = e.getX();
//		if (e.getButton() == MouseEvent.BUTTON1) {
//		}
	}

	public void mouseClicked(MouseEvent e) {
		float dx = 1.0F;
		if ((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) > 0) {
			dx = 10F;
		} else if ((e.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) > 0) {
			dx = 0.01F;
		}
		if (getLeftButtonRect(null).contains(e.getPoint())) {
			setValue(getValue() - dx);
			fireStateChanged();
		} else if (getRightButtonRect(null).contains(e.getPoint())) {
			setValue(getValue() + dx);
			fireStateChanged();
		} else if (e.getClickCount() >= 2) {
			numberField.setText(valueAsString());
			numberField.setVisible(true);
			numberField.requestFocus();
			numberField.selectAll();
			componentResized(null);
			repaint();
		}
	}

	public void mouseReleased(MouseEvent e) {
		if (oldValue != value)
			fireStateChanged();
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mouseMoved(MouseEvent e) {
	}

	public void mouseDragged(MouseEvent e) {
		

		float deltaX = e.getX() - previousX;
		;//System.out.println(" DN mouse dragged !"+e.getX()+" "+previousX+" -> "+deltaX);
		if (deltaX == 0F)
			return;
		if ((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) > 0) {
			deltaX *= 10;
		} else if ((e.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) > 0) {
			deltaX *= 0.01;
		}
		setValue(getValue() + deltaX);
		previousX = e.getX();
		fireStateChanged();
	}

	/**
	 * Adds a ChangeListener to the slider.
	 * 
	 * @param l
	 *                the ChangeListener to add
	 * @see #fireStateChanged
	 * @see #removeChangeListener
	 */
	public void addChangeListener(ChangeListener l) {
		listenerList.add(ChangeListener.class, l);
	}

	/**
	 * Removes a ChangeListener from the slider.
	 * 
	 * @param l
	 *                the ChangeListener to remove
	 * @see #fireStateChanged
	 * @see #addChangeListener
	 */
	public void removeChangeListener(ChangeListener l) {
		listenerList.remove(ChangeListener.class, l);
	}

	/**
	 * Send a ChangeEvent, whose source is this Slider, to each listener.
	 * This method method is called each time a ChangeEvent is received from
	 * the model.
	 * 
	 * @see #addChangeListener
	 * @see javax.swing.event.EventListenerList
	 */
	protected void fireStateChanged() {
		Object[] listeners = listenerList.getListenerList();
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ChangeListener.class) {
				if (changeEvent == null) {
					changeEvent = new ChangeEvent(this);
				}
				((ChangeListener) listeners[i + 1]).stateChanged(changeEvent);
			}
		}
	}

	public static void main(String[] args) {
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(new DraggableNumber());
		frame.pack();
		frame.setVisible(true);
	}

	/**
	 * When the escape key is pressed in the numberField, ignore the change
	 * and "close" the field.
	 */
	private class EscapeListener extends KeyAdapter {
		@Override
		public void keyPressed(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
				numberField.setVisible(false);
		}
	}
}
