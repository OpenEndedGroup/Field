package field;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationEvent;
import com.apple.eawt.ApplicationListener;

public class T_Open {

	static public void main(String[] s) {
		Shell ss = new Shell();
		ss.setVisible(true);

		Display display = ss.getDisplay();

		display.addFilter(SWT.MouseMove, new Listener() {
			
			@Override
			public void handleEvent(Event arg0) {
				System.out.println(" arg0 :"+arg0);
			}
		});
		
		display.addListener(SWT.OpenDocument, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				System.out.println(" arg0 :" + arg0);
			}
		});

		while (!display.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}

		}

	}

}
