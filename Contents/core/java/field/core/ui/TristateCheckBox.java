package field.core.ui;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ActionMapUIResource;

public class TristateCheckBox extends JCheckBox {

	public enum TristateState {
		SELECTED {
			public TristateState next() {
				return DESELECTED;
			}
		},
		INDETERMINATE {
			public TristateState next() {
				return DESELECTED;
			}
		},
		DESELECTED {
			public TristateState next() {
				return SELECTED;
			}
		};

		public abstract TristateState next();
	}

	public class TristateButtonModel extends ToggleButtonModel {
		private TristateState state = TristateState.DESELECTED;

		public TristateButtonModel(TristateState state) {
			setState(state);
		}

		public TristateButtonModel() {
			this(TristateState.DESELECTED);
		}

		public void setIndeterminate() {
			setState(TristateState.INDETERMINATE);
		}

		public boolean isIndeterminate() {
			return state == TristateState.INDETERMINATE;
		}

		// Overrides of superclass methods
		public void setEnabled(boolean enabled) {
			super.setEnabled(enabled);
			// Restore state display
			displayState();
		}

		public void setSelected(boolean selected) {
			setState(selected ? TristateState.SELECTED : TristateState.DESELECTED);
		}
		
		@Override
		public boolean isSelected() {
			return getState()==TristateState.SELECTED;
		}

		// Empty overrides of superclass methods
		public void setArmed(boolean b) {
		}

		public void setPressed(boolean b) {
		}

		void iterateState() {
			setState(state.next());
		}

		private void setState(TristateState state) {
			// Set internal state
			this.state = state;
			displayState();
			if (state == TristateState.INDETERMINATE && isEnabled()) {
				// force the events to fire

				// Send ChangeEvent
				fireStateChanged();

				// Send ItemEvent
				int indeterminate = 3;
				fireItemStateChanged(new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED, this, indeterminate));
			}
		}

		private void displayState() {
			super.setSelected(state != TristateState.DESELECTED);
			super.setArmed(state == TristateState.INDETERMINATE);
			super.setPressed(state == TristateState.INDETERMINATE);

		}

		public TristateState getState() {
			return state;
		}
	}

	// Listener on model changes to maintain correct focusability
	private final ChangeListener enableListener = new ChangeListener() {
		public void stateChanged(ChangeEvent e) {
			TristateCheckBox.this.setFocusable(getModel().isEnabled());
		}
	};

	public TristateCheckBox(String text) {
		this(text, null, TristateState.DESELECTED);
	}

	public TristateCheckBox(String text, Icon icon, TristateState initial) {
		super(text, icon);

		// Set default single model
		setModel(new TristateButtonModel(initial));

		// override action behaviour
		super.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				TristateCheckBox.this.iterateState();
			}
		});
		ActionMap actions = new ActionMapUIResource();
		actions.put("pressed", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				TristateCheckBox.this.iterateState();
			}
		});
		actions.put("released", null);
		SwingUtilities.replaceUIActionMap(this, actions);
	}

	// Next two methods implement new API by delegation to model
	public void setIndeterminate() {
		getTristateModel().setIndeterminate();
		
	}

	public boolean isIndeterminate() {
		return getTristateModel().isIndeterminate();
	}

	public TristateState getState() {
		return getTristateModel().getState();
	}

	// Overrides superclass method
	public void setModel(ButtonModel newModel) {
		super.setModel(newModel);

		// Listen for enable changes
		if (model instanceof TristateButtonModel)
			model.addChangeListener(enableListener);
	}

	// Empty override of superclass method
	public void addMouseListener(MouseListener l) {
	}

	// Mostly delegates to model
	private void iterateState() {
		// Maybe do nothing at all?
		if (!getModel().isEnabled())
			return;

		repaint();
		
		grabFocus();

		// Iterate state
		getTristateModel().iterateState();

		// Fire ActionEvent
		int modifiers = 0;
		AWTEvent currentEvent = EventQueue.getCurrentEvent();
		if (currentEvent instanceof InputEvent) {
			modifiers = ((InputEvent) currentEvent).getModifiers();
		} else if (currentEvent instanceof ActionEvent) {
			modifiers = ((ActionEvent) currentEvent).getModifiers();
		}
		fireActionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, getText(), System.currentTimeMillis(), modifiers));
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		if (((TristateButtonModel) model).getState() == TristateState.INDETERMINATE) {
			Graphics2D g2 = (Graphics2D) g;

			int cx = 9;
			int cy = 15;

			g2.setColor(new Color(0, 0, 0, 0.5f));
			g2.fillRect(cx - 2, cy - 2, 4, 4);
		}
	}

	// Convenience cast
	public TristateButtonModel getTristateModel() {
		return (TristateButtonModel) super.getModel();
	}
}
