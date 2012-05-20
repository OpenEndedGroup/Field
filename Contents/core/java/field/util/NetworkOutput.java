package field.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import field.launch.SystemProperties;
import field.math.linalg.VectorN;


public class NetworkOutput {
	public static String DEFAULT_GROUP = SystemProperties.getProperty("NetworkDoubleProvider.group.output", "239.45.89.15");
	public static int DEFAULT_PORT = SystemProperties.getIntProperty("NetworkDoubleProvider.port.output", 1230);
	public static String DEFAULT_INTERFACE = SystemProperties.getProperty("NetworkDoubleProvider.interface.output", "en0");

	public final static byte TIME_TO_LIVE = 1;

	protected DatagramPacket dp;

	protected InetAddress multicastGroup = null;

	protected MulticastSocket outputSocket = null;

	protected int port = 0;

	protected DatagramPacket stringPacket;

	public NetworkOutput() {
		this(DEFAULT_GROUP, DEFAULT_PORT, DEFAULT_INTERFACE);
	}

	public NetworkOutput(String group, int port, String interfac) {
		this.port = port;
		try {
			outputSocket = new MulticastSocket(port);
			outputSocket.setSendBufferSize(NetworkInput.MAX_PACKET_SIZE * 2);

		} catch (Exception ex) {
			;//System.out.println("unable to make a multicast socket on port:" + port + " ?\n");
		}
		try {
			multicastGroup = InetAddress.getByName(group);
		} catch (Exception ex) {
			;//System.out.println("Unable to resolve address:" + group + " ?\n" + ex);
		}
		dp = new DatagramPacket(new byte[NetworkInput.MAX_PACKET_SIZE], NetworkInput.MAX_PACKET_SIZE, multicastGroup, port);
	}

	public void send(String name, float value) {
		byte[] buffer = dp.getData();
		encode(buffer, 0, 1);
		encode(buffer, 4, value);
		encodeString(buffer, 8, name);
		try {
			int ttl = outputSocket.getTimeToLive();
			outputSocket.setTimeToLive(TIME_TO_LIVE);
			outputSocket.send(dp);
			outputSocket.setTimeToLive(ttl);
		} catch (Exception ex) {
			;//System.out.println("Error sending packet!: " + ex);
		}
	}

	public void send(String name, Object value) {
		byte[] buffer = dp.getData();
		encode(buffer, 0, -1);
		encode(buffer, 4, new Object[]{name, value});
		try {
			int ttl = outputSocket.getTimeToLive();
			outputSocket.setTimeToLive(TIME_TO_LIVE);
			outputSocket.send(dp);
			outputSocket.setTimeToLive(ttl);
		} catch (Exception ex) {
			;//System.out.println("Error sending packet!: " + ex);
		}
	}


	public void send(String name, VectorN vec) {
		byte[] buffer = dp.getData();
		assert vec.dim() * 4 + 4 < buffer.length : vec.dim() + " " + buffer.length;
		encode(buffer, 0, vec.dim());
		for (int i = 0; i < vec.dim(); i++) {
			encode(buffer, 4 + 4 * i, (float) vec.get(i));
		}
		encodeString(buffer, 4 + 4 * vec.dim(), name);
		try {
			int ttl = outputSocket.getTimeToLive();
			outputSocket.setTimeToLive(TIME_TO_LIVE);
			outputSocket.send(dp);
			outputSocket.setTimeToLive(ttl);
		} catch (Exception ex) {
			;//System.out.println("Error sending packet!: " + ex);
		}
	}

	public static int encode(byte[] to, int at, float me) {
		return encode(to, at, Float.floatToRawIntBits(me));
	}

	public static int encode(byte[] to, int at, int me) {
		to[at] = (byte) (me >> 24);
		to[at + 1] = (byte) ((me >> 16) & 255);
		to[at + 2] = (byte) ((me >> 8) & 255);
		to[at + 3] = (byte) (me & 255);
		return at + 4;
	}

	public static int encodeString(byte[] to, int at, String me) {
		to[at++] = (byte) (me.length());
		int conv;
		for (int i = 0; i < me.length(); i++) {
			conv = (int) (me.charAt(i));
			to[at++] = (byte) ((conv >> 8) & 255);
			to[at++] = (byte) (conv & 255);
		}
		return at;
	}

	private static int encode(byte[] buffer, int i, Object objects) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(objects);
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

//		if (buffer.length - i <bos.size())
//		{
			System.arraycopy(bos.toByteArray(), 0, buffer, i, bos.size());
			return i+bos.size();
//		}
//		return i;
	}
	
	
	
}
