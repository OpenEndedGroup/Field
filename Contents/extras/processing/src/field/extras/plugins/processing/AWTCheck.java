package field.extras.plugins.processing;

import java.awt.Frame;

import org.eclipse.swt.widgets.Display;

import processing.core.PApplet;

public class AWTCheck {

	private static Display display;
	private static PApplet pa;

	static public void main(String[] a) {
//		System.out.println(" 1212");
//
////		display = new Display();
////		display.setAppName("Field");
//
//		final Frame f = new Frame("A");
//		f.setBounds(50, 50, 500, 500);
//		f.setVisible(true);
//
//		System.out.println(" ?? ");

		pa = new PApplet() {
			@Override
			public void setup() {
				System.out.println(" setup called ");
				size(500,500,this.OPENGL);
			}

			@Override
			public void draw() {
				System.out.println(" draw called ");
			}
		};

		PApplet.runSketch(new String[]{"",""}, pa);
		
//		display.asyncExec(new Runnable() {
//			
//			@Override
//			public void run() {
//				pa.init();
//				f.add(pa);
//			}
//		});
		
		
//		while (!display.isDisposed()) {
//
//			try {
//				if (!display.readAndDispatch()) {
//					display.sleep();
//				}
//			} catch (Throwable t) {
//				t.printStackTrace();
//			}
//		}
	}

}
