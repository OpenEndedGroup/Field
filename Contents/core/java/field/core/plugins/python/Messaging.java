package field.core.plugins.python;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.python.core.PyFile;

import field.core.dispatch.iVisualElement.VisualElementProperty;

public class Messaging {

	public static VisualElementProperty<Messaging> messaging = new VisualElementProperty<Messaging>("messaging");
	public static VisualElementProperty<PyFile> feedback = new VisualElementProperty<PyFile>("feedback");
	private PyFile file;

	public Messaging()
	{
		file = new PyFile(new OutputStream() {
			
			StringBuilder s = new StringBuilder();
			
			@Override
			public void write(int b) throws IOException {
				s.append((char)b);
				if ((char)b=='\n')
				{
					message(s.toString().trim());
					s.setLength(0);
				}
			}
		});
	}

	Deque<String> messageQueue = new ArrayDeque<String>();

	public void message(String m) {
		synchronized (messageQueue) {
			messageQueue.push(m);
		}
	}

	public List<String> getMessages() {
		synchronized (messageQueue) {
			ArrayList<String> m = new ArrayList<String>(messageQueue);
			messageQueue.clear();
			
			return m;
		}
	}
	
	public PyFile getFile() {
		return file;
	}

}
