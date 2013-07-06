package field.graphics.core;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import field.graphics.core.Base.StandardPass;
import field.graphics.core.BasicFrameBuffers.iDisplayable;
import field.graphics.windowing.FullScreenCanvasSWT;
import field.launch.iUpdateable;

public class LayeredCrossfader {

	private int width;
	private int height;
	private int layers;
	private LayeredFrameBuffer a;
	private LayeredFrameBuffer b;

	List<iDisplayable> withA = new ArrayList<iDisplayable>();
	List<iDisplayable> withB = new ArrayList<iDisplayable>();
	
	int cadence = 4;
	int tick = 0;

	float alpha;

	boolean updateA = false;
	boolean updateB = false;
	boolean clear = false;

	public LayeredCrossfader(FullScreenCanvasSWT canvas, final LayeredFrameBuffer buffer, int width, int height, int layers) {
		this.width = width;
		this.height = height;
		this.layers = layers;

		buffer.setClearColor(null);

		a = new LayeredFrameBuffer(width, height, layers, true);
		b = new LayeredFrameBuffer(width, height, layers, true);

		canvas.getPostQueue().addUpdateable(new iUpdateable() {

			@Override
			public void update() {
				if (updateA) {
					System.out.println(" update A");
					for(iDisplayable d : withA)
						d.display();
					a.display();
					updateA = false;
					clear = true;
				}
				if (updateB) {
					System.out.println(" update B");
					for(iDisplayable d : withB)
						d.display();
					b.display();
					updateB = false;
					clear = true;
				}
			}
		});
		
		// attach a program and quad that copies buffer to a and b
		
		buffer.getSceneList().add(StandardPass.preTransform).register("__layered_cf__" + this, new iUpdateable() {

			@Override
			public void update() {
				int d = GL11.glGetError();

				tick++;
				if (tick % cadence == 0) {
					if (tick % (cadence * 2) == 0) {
						updateA = true;
					} else {
						updateB = true;
					}
				}

				if (clear) {
					System.out.println(" clear ");
					GL11.glClearColor(0, 0, 0, 0);
					GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
					clear = false;
				}

				int t = tick % (cadence * 2);
				if (t > cadence)
					t = 2 * cadence - t;

				alpha = t / (float) cadence;

			}
		});
	}

	public LayeredFrameBuffer getA() {
		return a;
	}

	public LayeredFrameBuffer getB() {
		return b;
	}

	public float getAlpha() {
		return alpha;
	}
}
