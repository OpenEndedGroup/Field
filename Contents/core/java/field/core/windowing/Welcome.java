package field.core.windowing;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;

import field.launch.Launcher;

public class Welcome {

	private Shell s;

	public Welcome()
	{
		s = new Shell(Launcher.display);
		s.setText("Welcome to Field");
		Rectangle bounds = Launcher.display.getBounds();
		
		int width = 500;
		int height = 50;
		s.setSize(width, height);
		s.setLocation(bounds.x-width/2+bounds.width/2, bounds.y-height/2+bounds.height/2);
		
		s.setVisible(true);
		
		FillLayout fill = new FillLayout();
		s.setLayout(fill);

		fill.marginHeight = 5;
		fill.marginWidth= 5;
		fill.spacing = 5;
		
		Button b1 = new Button(s, SWT.PUSH);
		b1.setText("Open...");
		Button b2 = new Button(s, SWT.PUSH);
		b2.setText("New...");
		
	}
	
}
