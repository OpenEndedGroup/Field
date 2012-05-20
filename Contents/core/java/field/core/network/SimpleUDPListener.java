package field.core.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

import field.launch.iUpdateable;
import field.util.TaskQueue;

public class SimpleUDPListener implements iUpdateable {

	private DatagramSocket socket;
	private Thread t;

	int maxSize = 1536;

	TaskQueue queue = new TaskQueue();

	public SimpleUDPListener(int maxSize, int port) throws SocketException {
		this.maxSize = maxSize;
		socket = new DatagramSocket(port);

		t = new Thread(new Runnable() {

			@Override
			public void run() {

				byte[] buffer = new byte[SimpleUDPListener.this.maxSize];
				final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

				try {
					while (true) {
						;//System.out.println(" listening for data ");
						socket.receive(packet);
						byte[] data = packet.getData();
						final byte[] b2 = new byte[data.length];
						System.arraycopy(data, 0, b2, 0, b2.length);
						;//System.out.println(" got data");
						queue.new Task() {
							protected void run() {
								handle(b2);
							};
						};
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		t.start();
	}

	protected void handle(byte[] data) {
	}

	public void close() {
		socket.close();
	}

	@Override
	public void update() {
		queue.update();
	}
}
