package field.graphics.windowing;

import java.util.Date;

public class FrameRateThread {

	public FrameRateThread(Object canvas) {
		if (canvas instanceof FullScreenCanvasSWT)
			init((FullScreenCanvasSWT) canvas);
	}

	public FrameRateThread(final FullScreenCanvasSWT canvas) {
		init(canvas);
	}

	private void init(final FullScreenCanvasSWT canvas) {
		new Thread(new Runnable() {

			public void run() {
				while (true) {
					long in = System.currentTimeMillis();
					int frameIn = canvas.getFrameNumber();
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					int frameOut = canvas.getFrameNumber();
					long out = System.currentTimeMillis();
					System.err.println("fps:" + 1000 * (frameOut - frameIn) / (float) (out - in) + "       " + new Date());
				}
			}

		}).start();
	}
}
