package field.extras.reload;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import field.launch.iUpdateable;

//17 only
public class ReloadWatchSupport {

	WatchService ws;
	{
		try {
			ws = FileSystems.getDefault().newWatchService();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		new Thread() {
			public void run() {
				while (true) {
					WatchKey w;
					try {
						w = ws.poll(1000, TimeUnit.MILLISECONDS);
						if (w == null)
							continue;

						List<WatchEvent<?>> ev = w.pollEvents();
						for (WatchEvent we : ev) {
							Object context = we.context();
							if (context instanceof Path) {
								Path pp = parents.get(w).resolve( ((Path) context)  );
								File f = pp.toFile();
								System.out.println(" context is :" + pp+ " -> " + f.getAbsolutePath());
								iUpdateable c = callback.get(f.getAbsolutePath());
								if (c != null) {
									c.update();
								} else {
									System.out.println(" no callback for :" + callback.keySet());
								}
							}
						}
						w.reset();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			};
		}.start();
	}

	Map<String, iUpdateable> callback = new LinkedHashMap<String, iUpdateable>();
	LinkedHashSet<String> already = new LinkedHashSet<String>();
	LinkedHashMap<WatchKey, Path> parents = new LinkedHashMap<WatchKey, Path>();

	public void install(String path, iUpdateable callback) {
		if (ws == null)
			return;

		try {
			Path p = FileSystems.getDefault().getPath(path).getParent();
			if (already.add(p.toAbsolutePath().toUri().toString())) {
				System.out.println(" installing watch on <" + p + ">");
				WatchKey k = p.register(ws, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE);
				this.callback.put(path, callback);
				this.parents.put(k, p);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
