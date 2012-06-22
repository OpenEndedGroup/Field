package field.extras.typing;

import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;

import field.core.Platform;
import field.core.dispatch.iVisualElement;
import field.core.dispatch.iVisualElement.Rect;
import field.core.plugins.BaseSimplePlugin;
import field.core.windowing.GLComponentWindow;
import field.core.windowing.GLComponentWindow.ComponentContainer;
import field.core.windowing.components.DraggableComponent.Resize;
import field.core.windowing.components.iComponent;
import field.math.abstraction.iAcceptor;
import field.math.linalg.iCoordinateFrame;

public class TypingPlugin extends BaseSimplePlugin {

	private GLComponentWindow window;
	private TypingOverlay typingOverlay;

	boolean insert = false;
	String text = "";

	@Override
	public void registeredWith(iVisualElement root) {
		super.registeredWith(root);

		window = iVisualElement.enclosingFrame.get(root);
		typingOverlay = new TypingOverlay(window, window.textSystem) {
			@Override
			protected String getText() {

				if (!insert)
					return "";
				if (text.equals(""))
					return ">>";
				return text;
			}
		};

		;//;//System.out.println(" registering typing plugin ");

		window.getRoot().addAsAllEventHandler(new iComponent() {

			public iComponent setVisualElement(iVisualElement ve) {
				return null;
			}

			public void setSelected(boolean selected) {
			}

			public void setBounds(Rect r) {
			}

			public void paint(ComponentContainer inside,
					iCoordinateFrame frameSoFar, boolean visible) {
			}

			public void mouseReleased(ComponentContainer inside, Event arg0) {
			}

			public void mousePressed(ComponentContainer inside, Event arg0) {
			}

			public void mouseMoved(ComponentContainer inside, Event arg0) {
			}

			public void mouseExited(ComponentContainer inside, Event arg0) {
			}

			public void mouseEntered(ComponentContainer inside, Event arg0) {
			}

			public void mouseDragged(ComponentContainer inside, Event arg0) {
			}

			public void mouseClicked(ComponentContainer inside, Event arg0) {
			}

			public void keyTyped(final ComponentContainer inside, Event arg0) {

				if (arg0 == null)
					return;

				if (arg0.keyCode == '.' && (arg0.stateMask & Platform.getCommandModifier()) != 0) {
					arg0.doit = false;
					;//;//System.out.println(" completion hook ? ");

					TypingExecution.beginCompletion(TypingPlugin.this.root,
							text, this, (int) typingOverlay.popupPoint.x,
							(int) typingOverlay.popupPoint.y,
							new iAcceptor<String>() {

								public iAcceptor<String> set(String to) {
									text = text + to;
									inside.requestRedisplay();
									return this;
								}
							});

					return;
				}

				if (insert && arg0.keyCode == 13) {
					arg0.doit = false;
					
					;//;//System.out.println(" inserting <"+text+">");
					for(int i=0;i<text.length();i++)
					{
						;//;//System.out.println((int)text.charAt(i));
					}
					executeText(text);
					inside.requestRedisplay();
					text = "";
					insert = false;
					return;
				}

				if (!insert && arg0.keyCode == '\'') {
					arg0.doit = false;
					;//;//System.out.println(" insert on ");
					insert = true;
					inside.requestRedisplay();
				} else if (insert) {
					arg0.doit = false;

					if (insert && arg0.keyCode == '\b') {
						if (text.length() > 0) {
							text = text.substring(0, text.length() - 1);
							inside.requestRedisplay();
							return;
						} else {
							insert = false;
							inside.requestRedisplay();
							return;
						}
					}

					if (Character.isISOControl(arg0.keyCode))
						return;

					if (arg0.character > 0) {
						text += arg0.character;
						inside.requestRedisplay();
					}
				}
			}

			public void keyReleased(ComponentContainer inside, Event arg0) {
				if (arg0 == null)
					return;
				if (insert)
					arg0.doit = false;
			}

			public void keyPressed(ComponentContainer inside, Event arg0) {
				if (arg0 == null)
					return;

				if (insert)
					arg0.doit = false;

			}

			public boolean isSelected() {
				return false;
			}

			public float isHit(Event event) {
				return Float.NEGATIVE_INFINITY;
			}

			public iComponent hit(Event event) {
				return null;
			}

			public void handleResize(Set<Resize> currentResize, float dx,
					float dy) {
			}

			public iVisualElement getVisualElement() {
				return null;
			}

			public Rect getBounds() {
				return null;
			}

			public void endMouseFocus(ComponentContainer inside) {
			}

			public void beginMouseFocus(ComponentContainer inside) {
			}
		});
	}

	protected void executeText(String text) {
		TypingExecution.execute(root, text);
	}

	@Override
	protected String getPluginNameImpl() {
		return "typing";
	}

}
