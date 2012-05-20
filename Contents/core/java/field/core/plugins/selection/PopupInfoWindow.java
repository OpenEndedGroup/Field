package field.core.plugins.selection;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SpringLayout;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import field.core.Constants;
import field.core.ui.UbiquitousLinks;
import field.launch.Launcher;
import field.launch.iUpdateable;

public class PopupInfoWindow {

	static public String content(String s) {
		String errorText = "<p class='wellspacedIndentedWhite'><i>" + s.replace("\n", "<BR>") + "</i></p>";
		return errorText;
	}

	static public String stackTrace(StackTraceElement[] cause) {
		StackTraceElement[] stack = cause;
		String s = "<p class='wellspacedIndentedWhite'>";
		for (StackTraceElement e : stack) {
			;//System.out.println(" stack trace <" + e + ">");
			s += "<b>" + e.getClassName() + "</b>( <u><a href=\"'http://\" onclick=\"" + UbiquitousLinks.links.code_openInEclipse(e.getFileName(), e.getLineNumber()) + "\">" + e.getFileName() + ":<i>" + e.getLineNumber() + "</a></u></i>)";
			s += "<BR>";
		}
		s += "<BR></p>";
		return s;
	}

	static public String stackTrace(Throwable cause) {
		return stackTrace(cause.getStackTrace());
	}

	static public String title(String s) {
		String errorText = "<p class='invertedheading'><font bgcolor='#888888' color='#000000'>" + s + "</font></p>";
		return errorText;
	}

	private JFrame drawerFrame;

	private JPanel drawer;
	private JScrollPane textScrollPane;

	private JEditorPane infoText;

	int notVisibleVotes = 0;

	int visibleVotes = 0;

	public void becomeVisible(int x, int y, int width, int height, String contents) {
		visibleVotes++;
		drawerFrame.setBounds(x - 30, y - 30, width, height);
		// drawerFrame.setVisible(true);
		infoText.setText(contents);
		drawerFrame.pack();
		int w = Math.min(500, drawerFrame.getWidth() + 100);
		int h = Math.min(500, drawerFrame.getHeight());
		drawerFrame.setSize(w, h);
	}

	public JEditorPane getInfoText() {
		if (infoText == null) {
			infoText = new JEditorPane() {
				@Override
				public void paint(Graphics g) {
					Graphics2D g2 = ((Graphics2D) g);
					Graphics2D g2c = (Graphics2D) g2.create();

					g2c.setComposite(AlphaComposite.Src);
					g2c.setColor(new Color(0, 0, 0, 0.1f));
					g2c.fillRect(0, 0, this.getWidth(), this.getHeight());

					g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
					super.paint(g);

				}
			};

			HTMLEditorKit k = new HTMLEditorKit();
			UbiquitousLinks.links.injectCSS(k.getStyleSheet());
			k.install(infoText);

			infoText.setContentType("text/html");
			infoText.setDocument(new HTMLDocument());
			infoText.setEditable(false);
			infoText.setFont(new Font(Constants.defaultFont, Font.PLAIN, 12));
			infoText.setForeground(new Color(1f, 1f, 1f, 1f));
			infoText.setBackground(new Color(0.f, 0.f, 0.f, 0.4f));
			infoText.setBorder(new Border() {

				public Insets getBorderInsets(Component c) {
					return new Insets(5, 5, 5, 5);
				}

				public boolean isBorderOpaque() {
					return false;
				}

				public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
				}
			});

			UbiquitousLinks.links.install(infoText);
		}
		return infoText;
	}

	public PopupInfoWindow initialize(String title) {
		UbiquitousLinks links = UbiquitousLinks.links;

		drawerFrame = new JFrame("drawe+" + title) {

			iUpdateable fader = null;

			boolean fadingIn = false;

			float opacity = 0;

			@Override
			public void paint(Graphics g) {
				super.paint(g);
			}

			@Override
			public void setFocusable(boolean focusable) {
				super.setFocusable(false);
			}

			@Override
			public void setVisible(boolean b) {

				if (fader == null) {
					Launcher.getLauncher().registerUpdateable(fader = new iUpdateable() {
						public void update() {
							if (fadingIn) {
								opacity = (1 - (1 - opacity) * 0.9f);
							} else {
								opacity = opacity * 0.9f;
							}
							getRootPane().putClientProperty("Window.alpha", opacity);
							if (opacity > 1e-2)
								doSetVisible(true);
							else
								doSetVisible(false);
						}
					});
					fader.update();
				}

				if (b) {
					fadingIn = true;
					super.setVisible(true);
				} else {
					// super.setVisible(false);
					fadingIn = false;
					// opacity
					// = 0;
				}
			}

			protected void doSetVisible(boolean b) {
				super.setVisible(b);
			}
		};

		drawerFrame.setFocusable(false);
		drawerFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		drawerFrame.setTitle("Info");
		drawerFrame.setUndecorated(true);
		drawerFrame.setAlwaysOnTop(true);

		SpringLayout gb2 = new SpringLayout();
		drawerFrame.getContentPane().setLayout(gb2);

		// drawerFrame.getContentPane().add(topBoarder2);
		drawerFrame.getContentPane().add(getDrawer());
		drawerFrame.setBounds(new Rectangle(440 - 14, 80, 200, 400 - 80));
		// SelectionSetUI.this.setBackground(new
		// Color(0.3f,
		// 0.3f, 0.28f,
		// 1f));
		//SpringUtilities.makeCompactGrid(drawerFrame.getContentPane(), 1, 1, 0, 0, 0, 0);
		drawerFrame.getContentPane().validate();

		// tagPane.setBackground(new
		// Color(0.18f,
		// 0.18f, 0.20f,
		// 1f));
		// tagPane.setOpaque(true);

		drawerFrame.setBackground(new Color(0.4f, 0.4f, 0.4f, 0.5f));
		drawerFrame.getRootPane().setBackground(new Color(0.4f, 0.4f, 0.4f, 0.25f));

		Launcher.getLauncher().registerUpdateable(new iUpdateable() {

			public void update() {
				if (drawerFrame.isVisible()) {
					if (notVisibleVotes > 0 && visibleVotes == 0) {
						// if (drawerFrame.isVisible())
						// drawerFrame.setVisible(false);
					}
				} else {
					if (visibleVotes > 0) {
						if (!drawerFrame.isVisible()) {
							drawerFrame.pack();
							int w = Math.min(500, drawerFrame.getWidth() + 100);
							int h = Math.min(500, drawerFrame.getHeight());
							drawerFrame.setSize(w, h);
							drawerFrame.setVisible(true);
						}
					}
				}
				notVisibleVotes = 0;
				visibleVotes = 0;
			}
		});

		drawerFrame.addWindowFocusListener(new WindowFocusListener() {

			public void windowGainedFocus(WindowEvent e) {
			}

			public void windowLostFocus(WindowEvent e) {
				if (visibleVotes == 0) {
					if (drawerFrame.isVisible())
						drawerFrame.setVisible(false);
				}
			}
		});

		return this;
	}

	public void voteNotVisible() {
		notVisibleVotes++;
	}

	private JPanel getDrawer() {
		if (drawer == null) {
			GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
			gridBagConstraints3.fill = GridBagConstraints.BOTH;
			gridBagConstraints3.weighty = 1.0;
			gridBagConstraints3.insets = new Insets(14, 0, 0, 0);
			gridBagConstraints3.weightx = 1.0;
			drawer = new JPanel();
			drawer.setLayout(new GridBagLayout());
			drawer.setSize(new Dimension(208, 289));
			drawer.add(getTextScrollPane(), gridBagConstraints3);
			drawer.setBorder(new BevelBorder(BevelBorder.LOWERED, new Color(0.0f, 0.0f, 0.0f, 0.15f), new Color(0.0f, 0.0f, 0.0f, 0.15f), new Color(0.0f, 0.0f, 0.0f, 0.15f), new Color(0.0f, 0.0f, 0.0f, 0.15f)));
			drawer.setOpaque(false);
		}
		return drawer;
	}

	private JScrollPane getTextScrollPane() {
		if (textScrollPane == null) {
			textScrollPane = new JScrollPane() {
				@Override
				public void paint(Graphics g) {
					Graphics2D g2 = ((Graphics2D) g);
					Graphics2D g2c = (Graphics2D) g2.create();

					// g2c.setComposite(AlphaComposite.Clear);
					g2c.setColor(new Color(0, 0, 0, 0.1f));
					g2c.fillRect(0, 0, this.getWidth(), this.getHeight());
					super.paint(g);
				}
			};

			textScrollPane.setViewportView(getInfoText());

			textScrollPane.setBackground(new Color(0.1f, 0.1f, 0.1f, 1f));
			// textScrollPane.setOpaque(false);
			textScrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);

			textScrollPane.setOpaque(false);
			textScrollPane.getViewport().setOpaque(false);
			textScrollPane.getViewport().setBackground(new Color(0, 0, 0, 0.4f));

		}
		return textScrollPane;
	}

}
