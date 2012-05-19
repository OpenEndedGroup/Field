package field.graphics.core;

import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glFinish;
import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL11.glReadPixels;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_BINDING;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.ParameterBlock;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import javax.media.jai.JAI;

import org.lwjgl.opengl.GL11;

import com.sun.media.jai.codec.JPEGEncodeParam;

import field.bytecode.protect.dispatch.Cont;
import field.bytecode.protect.dispatch.Cont.ReturnCode;
import field.bytecode.protect.dispatch.Cont.aRun;
import field.graphics.windowing.FullScreenCanvasSWT;
import field.math.BaseMath;

public class SaverOnscreen {

	private final int numWorkers;

	private final int width;

	private final int height;

	private final ExecutorService pool;
				
	private final String prefix;

	public SaverOnscreen(int width, int height, int numWorkers, String prefix) {
		this.width = width;
		this.height = height;
		this.numWorkers = numWorkers;
		this.prefix = prefix;

		pool = Executors.newCachedThreadPool();
	}

	List<FutureTask<ByteBuffer>> workers = new ArrayList<FutureTask<ByteBuffer>>();

	int frameNumber = 0;

	boolean on = false;
	boolean drip = false;

	private String lastFilename;

	public void setOn(boolean on) {
		this.on = on;
		drip = false;
	}
	
	public void start()
	{
		setOn(true);
	}
	
	public void stop()
	{
		setOn(false);
	}

	public void drip()
	{
		on = true;
		drip = true;
	}
	
	private void update() {
		if (!on) return;

		ByteBuffer storage = null;

		if (workers.size() < numWorkers) {

			storage = newStorage();
		} else {
			System.out.println("state opf workers: ");
			for(FutureTask t : workers)
				System.out.println(t.isDone());

			FutureTask<ByteBuffer> w = workers.remove(0);
			try {
				storage = w.get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}

		getImage(storage);

		lastFilename = prefix + pad(frameNumber++) + ".jpg";
		FutureTask<ByteBuffer> task = new FutureTask<ByteBuffer>(makeWorker(storage, lastFilename));
		pool.execute(task);
		workers.add(task);
		
		if (drip)
			on = false;
	}

	private String pad(int i) {
		String s = i + "";
		while (s.length() < 5)
			s = "0" + s;
		return s;
	}

	private void getImage(ByteBuffer storage) {

		int[] a = { 0 };
		assert glGetError() == 0;

		// I recall that there exists a way to do async readpixels, in that case we would begin a read here and complete the previous one, copying the previous one into this storage buffer

		
//		glFinish();
		storage.rewind();
		a[0] = glGetInteger(GL_FRAMEBUFFER_BINDING);
		glBindFramebuffer(GL_FRAMEBUFFER, 0);
//		glFinish();
//		GL11.glReadBuffer(GL11.GL_BACK);
		glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, storage);
//		glFinish();
		glBindFramebuffer(GL_FRAMEBUFFER, a[0]);
		storage.rewind();

		assert glGetError() == 0;

	}

	private Callable<ByteBuffer> makeWorker(final ByteBuffer storage, final String filename) {
		return new Callable<ByteBuffer>() {
			public ByteBuffer call() throws Exception {

				BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

				WritableRaster tile = bi.getWritableTile(0, 0);
				DataBuffer buffer = tile.getDataBuffer();

				IntBuffer storagei = storage.asIntBuffer();
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						int r = BaseMath.intify(storage.get());
						int g = BaseMath.intify(storage.get());
						int b = BaseMath.intify(storage.get());
						int a = BaseMath.intify(storage.get());

						int d = (g << 8) | (r << 16) | (b);// | (a << 24);
						buffer.setElem((height - 1 - y) * width + x, d);
					}
				}

				FileOutputStream fos;
				//RenderedOp op = JAI.create("filestore", bi, filename, "JPEG");

				JPEGEncodeParam encodeParam = new JPEGEncodeParam();
				encodeParam.setQuality(100.00f);
				ParameterBlock pb = new ParameterBlock();
				pb.addSource(bi);
				pb.add(new FileOutputStream(filename));
				pb.add("jpeg");
				pb.add(encodeParam);
				JAI.create("encode", pb);


				System.out.println(" thread <"+Thread.currentThread()+"> saved <"+filename+">");

				return storage;
			}
		};
	}

	private ByteBuffer newStorage() {
		return ByteBuffer.allocateDirect(width * height * 4);
	}

	public void join(FullScreenCanvasSWT canvas) {
		aRun arun = new Cont.aRun() {
			@Override
			public ReturnCode head(Object calledOn, Object[] args) {
				update();
				return super.head(calledOn, args);
			}
		};
		Cont.linkWith(canvas, canvas.method_beforeFlush, arun);

	}

}
	