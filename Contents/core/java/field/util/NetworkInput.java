package field.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashMap;

import field.math.abstraction.iFloatProvider;
import field.math.linalg.VectorN;

/**
 * 
 * takes multicast networked ip messages for double providers and dispatches
 * 
 * them. based on work by jesse
 * 
 * 
 * 
 * @author marc
 * 
 * @created September 23, 2001
 * 
 */
public class NetworkInput implements Runnable {
	public static boolean debugArrival = false;

	public static int MAX_PACKET_SIZE = 4500;

	public static String DEFAULT_GROUP = NetworkOutput.DEFAULT_GROUP;

	public static int DEFAULT_PORT = NetworkOutput.DEFAULT_PORT;

	public final static int THREAD_SLEEP_TIME = 0;

	protected MulticastSocket inputSocket = null;

	protected DatagramPacket dp;

	HashMap dispatchTable = new HashMap();

	AnyHandler defaultHandler = null;

	public interface AnyHandler {
	}

	public interface Handler extends AnyHandler {
		public void handle(String name, float f);
	}

	public interface VecHandler extends AnyHandler {
		public void handle(String name, VectorN v);
	}

	public interface ObjectHandler<T> extends AnyHandler {
		public void handle(String name, T o);
	}

	public NetworkInput() {
		this(DEFAULT_GROUP, DEFAULT_PORT);
	}

	public NetworkInput(String group, int port) {
		try {
			inputSocket = new MulticastSocket(port);
			inputSocket.setReceiveBufferSize(MAX_PACKET_SIZE * 5);
		} catch (Exception ex) {
			System.out.println("could not make a socket on port:" + port + " ?\n" + ex);
		}
		InetAddress multicastGroup = null;
		try {
			multicastGroup = InetAddress.getByName(group);
		} catch (Exception ex) {
			System.out.println("could not resolve address:" + group + " ?\n" + ex);
		}
		try {
			inputSocket.joinGroup(multicastGroup);
		} catch (Exception ex) {
			System.out.println("could not join group:" + group + " ?\n" + ex);
		}
		byte[] buffer = new byte[MAX_PACKET_SIZE];
		dp = new DatagramPacket(buffer, buffer.length);
		(new Thread(this)).start();
	}

	public Handler register(Handler h, String name) {
		Handler ha = (Handler) dispatchTable.put(name, h);
		return ha;
	}

	public VecHandler register(VecHandler h, String name) {
		VecHandler ha = (VecHandler) dispatchTable.put(name, h);
		return ha;
	}

	public ObjectHandler register(ObjectHandler h, String name) {
		ObjectHandler ha = (ObjectHandler) dispatchTable.put(name, h);
		return ha;
	}

	public AnyHandler registerDefaultHandler(AnyHandler h) {
		AnyHandler old = defaultHandler;
		defaultHandler = h;
		return old;
	}

	public void run() {
		try {
			byte[] buffer = dp.getData();
			while (true) {
				// gotta reset the length, because length of a datagram is both length of most recent
				// packet received, and the cut off for recieving new data- so it will just keep shrinking
				// on its own.\/
				dp.setLength(MAX_PACKET_SIZE);
				try {
					inputSocket.receive(dp);
				} catch (Exception ex) {
					System.out.println("failed while trying to receive packet!" + ex);
				}
				int dim = decodeInt(buffer, 0);
				if (dim == 1) {
					// get the flow
					float value = decode(buffer, 4);
					// get the name
					String name = decodeString(buffer, 8);
					if (debugArrival)
						System.err.println(" net <" + name + "> : = <" + value + ">");
					set(name, value);
				} else if (dim == -1) {
					Object[] value = decodeObject(buffer, 4);
					if (debugArrival)
						System.err.println(" net <" + value[0] + "> : x = <" + value[1] + "> <" + buffer.length + ">");
					set((String) value[0], value[1]);

				} else {
					VectorN v = new VectorN(dim);
					for (int i = 0; i < dim; i++) {
						float val = decode(buffer, 4 + 4 * i);
						v.set(i, val);
					}
					String name = decodeString(buffer, 4 + 4 * dim);
					if (debugArrival)
						System.err.println(" net <" + name + "> := <" + v + ">");
					set(name, v);
				}
				try {
					Thread.sleep(THREAD_SLEEP_TIME);
				} catch (InterruptedException ex) {
				}
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
			System.err.println("\n\n\n\n\n\n spurious exit ?? \n\n\n\\n\n\n");
//			System.exit(1);
		}
	}

	public void set(String name, float value) {
		// find it
		Object o = dispatchTable.get(name);
		if (o == null)
			o = defaultHandler;
		if (o instanceof Handler) {
			Handler handler = (Handler) o;
			handler.handle(name, value);
		} else if (o instanceof VecHandler) {
			VecHandler handler = (VecHandler) o;
			VectorN v = new VectorN(1);
			v.set(0, value);
			handler.handle(name, v);
		} else if (o instanceof ObjectHandler) {
			try {
				((ObjectHandler) o).handle(name, value);
			} catch (Throwable t) {
				System.err.println(" exception thrown in handler <" + name + ">");
				t.printStackTrace();
			}
		} else {
			// System.out.println("networkdoubleprovider: warning: couldn't find handler called <" + name + ">");
		}
	}

	public void set(String name, VectorN value) {
		// find it
		Object o = dispatchTable.get(name);
		if (o == null)
			o = defaultHandler;
		if (o instanceof Handler) {
			Handler handler = (Handler) o;
			handler.handle(name, (float) value.get(0));
		} else if (o instanceof VecHandler) {
			VecHandler handler = (VecHandler) o;
			handler.handle(name, value);
		} else if (o instanceof ObjectHandler) {
			((ObjectHandler) o).handle(name, value);
		} else {
			// System.out.println("networkdoubleprovider: warning: couldn't find handler called <" + name + ">");
		}
	}

	public void set(String name, Object value) {
		// find it
		Object o = dispatchTable.get(name);
		if (o == null)
			o = defaultHandler;
		if (o instanceof ObjectHandler) {
			try {
				((ObjectHandler) o).handle(name, value);
			} catch (Throwable t) {
				System.err.println(" exception thrown in handler <" + name + ">");
				t.printStackTrace();
			}
		} else {
			// System.out.println("networkdoubleprovider: warning: couldn't find handler called <" + name + ">");
		}
	}

	public static float decode(byte[] from, int at) {
		return Float.intBitsToFloat(decodeInt(from, at));
	}

	public static int decodeInt(byte[] from, int at) {
		int i = (from[at] << 24) & (255 << 24);
		i += (from[at + 1] << 16) & (255 << 16);
		i += (from[at + 2] << 8) & (255 << 8);
		i += (from[at + 3]) & 255;
		return i;
	}

	public static String decodeString(byte[] from, int at) {
		char[] result;
		int string_length = from[at++];
		result = new char[string_length];
		for (int i = 0; i < result.length; i++) {
			result[i] = (char) ((from[at++] << 8) & (255 << 8));
			result[i] += (from[at++]) & 255;
		}
		return new String(result);
	}

	private Object[] decodeObject(byte[] buffer, int at) {
		ByteArrayInputStream bis = new ByteArrayInputStream(buffer, at, buffer.length - at);
		try {
			ObjectInputStream ois = new ObjectInputStream(bis);
			Object[] o = (Object[]) ois.readObject();
			return o;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	public class RecieveFloat implements iFloatProvider {
		public float value = 0;

		public RecieveFloat(String name, float def) {
			this.value = def;
			register(new Handler() {
				public void handle(String name, float f) {
					value = f;
				}
			}, name);
		}

		public float evaluate() {
			return value;
		}
	}
}