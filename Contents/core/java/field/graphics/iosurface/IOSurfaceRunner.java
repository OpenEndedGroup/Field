package field.graphics.iosurface;

import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL31.GL_TEXTURE_RECTANGLE;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import field.graphics.core.Base;
import field.graphics.core.BasicContextManager;
import field.graphics.core.BasicUtilities.TwoPassElement;
import field.launch.SystemProperties;
import field.util.MiscNative;
public class IOSurfaceRunner {

	static public final String iosurfacer_executable = SystemProperties.getProperty("iosurfacer", "./iosurfacer");
	private Process process;
	private BufferedReader in;
	private final String filename;
	private IOSurfaceElement element;
	private OutputStreamWriter out;

	public IOSurfaceRunner(String filename) throws IOException {

		this.filename = filename;
		if (!new File(filename).exists())
			throw new IllegalArgumentException(" file <" + filename + "> does not exist");

		ProcessBuilder p = new ProcessBuilder(iosurfacer_executable, filename);
		p.directory(new File("."));
		p.redirectErrorStream(true);
		process = p.start();

		in = new BufferedReader(new InputStreamReader(process.getInputStream()));

		out = new OutputStreamWriter(process.getOutputStream());
		new Thread() {
			public void run() {
				IOSurfaceRunner.this.run();
			};
		}.start();

		element = new IOSurfaceElement();

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					process.destroy();
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		});
	}

	public void dispose() {
		running = false;
		try {
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		process.destroy();
	}

	boolean running = true;
	private double duration;

	private int id = -1;
	private double currentTime;

	// HashMap<Integer, Integer> id = new HashMap<Integer, Integer>();
	// HashMap<Integer, Double> currentTime= new HashMap<Integer, Double>();

	private double width;
	private double height;

	public void setTime(double seconds) {
		try {
			out.append("setposition#" + seconds + "\n");
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setRate(double fractionOfRealtime) {
		try {
			out.append("setrate#" + fractionOfRealtime + "\n");
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void run() {

		boolean thrown = false;

		while (running) {
			try {
				String line = in.readLine();
				if (line == null)
					continue;
				parse(line);
			} catch (Throwable t) {
				if (!thrown) {
					System.err.println(" an error has occured while reading from the IOSurfaceRunner task for movie <" + filename + ">. We're going to keep going, but it can't be a good sign.");
					t.printStackTrace();
				}
				thrown = true;
			}
		}
	}

	public TwoPassElement getElement() {
		return element;
	}

	public TwoPassElement makeElement() {
		return new IOSurfaceElement();
	}

	private void parse(String line) {
		synchronized (this) {

			if (!line.startsWith("--"))
				return;
			if (line.startsWith("--DURATION")) {
				String[] parts = line.split("#");
				duration = Double.parseDouble(parts[1]);
			} else if (line.startsWith("--DIMENSIONS")) {
				String[] parts = line.split("#");
				width = Double.parseDouble(parts[1]);
				height = Double.parseDouble(parts[2]);
			} else if (line.startsWith("--ID")) {
				String[] parts = line.split("#");

				int idNow = Integer.parseInt(parts[1]);
				double currentTimeNow = Double.parseDouble(parts[2]);
				int slot = Integer.parseInt(parts[3]);

				// id.put(slot, idNow);
				// currentTime.put(slot, currentTimeNow);

				id = idNow;
				currentTime = currentTimeNow;
			}

		}
	}

	public double getDuration() {
		return duration;
	}

	public double getCurrentTime() {
		return currentTime;
	}

	public int getID() {
		return id;
	}

	public double getWidth() {
		return width;
	}

	public double getHeight() {
		return height;
	}

	class IOSurfaceElement extends TwoPassElement {

		public IOSurfaceElement() {
			super("", Base.StandardPass.preRender, Base.StandardPass.postRender);
		}

		@Override
		protected void post() {
			glBindTexture(GL_TEXTURE_RECTANGLE, 0);
			glDisable(GL_TEXTURE_RECTANGLE);
		}

		double lastTime = -1;

		@Override
		protected void pre() {
			glEnable(GL_TEXTURE_RECTANGLE);
			glBindTexture(GL_TEXTURE_RECTANGLE, textureId);

			synchronized (IOSurfaceRunner.this) {

				//System.out.println(" -- <bind to <" + textureId + ">");

				if (lastTime != id)
					new MiscNative().lookupAndBindIOSurfaceNow(id);
				else {
					lastTime = id;
				}
			}

		}

		int textureId;

		@Override
		protected void setup() {
			int[] textures = new int[1];
			glEnable(GL_TEXTURE_RECTANGLE);
			textures[0] = glGenTextures();
			glDisable(GL_TEXTURE_RECTANGLE);

			textureId = textures[0];
			BasicContextManager.putId(this, textureId);

		}

	}
}
