package field.extras.max;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * @author marc
 */
public class OSCInput {

	public static int max_packet_size = 20000;
	public static int num_buffers = 5;
	protected UDPNIOListener listener;

	public OSCInput(int port) {
		listener = new UDPNIOListener(port, max_packet_size, num_buffers) {
			protected void processBuffer(ByteBuffer bb) {
				bb.rewind();
				decode(bb);
			}
		};
	}
	public void update() {
		listener.update();
	}

	protected void decode(ByteBuffer bb) {
		if (bb.remaining()==0) return;
				String dest = readString(bb);
		if (dest.startsWith("#")) {
			Long l = readLong(bb); // time-tag
			//System.out.println(" time tag <" + l + ">");
			while (bb.remaining() > 0) {
				Integer length = readInt(bb);
				if (length.intValue() > 0) {
					//System.out.println(" length <" + length + ">");
					decode(bb);
					pad4(bb);
					//System.out.println(" remaining <" + bb.remaining() + ">");
				}
			}
		} else { 
			String header = readString(bb);
			List args = new LinkedList();
			for (int i = 1; i < header.length(); i++) {
				Object made = null;
				switch (header.charAt(i)) {
					case 'i' :
						made = readInt(bb);
						break;
					case 's' :
						made = readString(bb);
						break;
					case 'S' :
						made = readString(bb);
						break;
					case 'f' :
						made = readFloat(bb);
						break;
					default :
						System.out.println(" :: warning :: <" + ((int)header.charAt(i)) + ">");
				}
				args.add(made);
			}

			dispatch(dest, args);
		}
	}

	public interface Handler {
	}

	public interface DispatchableHandler extends Handler {
		public void handle(String s, Object[] args);
	}

	/**
	 * returns old handler
	 */
	public Handler setDefaultHandler(Handler h) {
		Handler old = defaultHandler;
		defaultHandler = h;
		return old;
	}

	public Handler registerHandler(String dest, Handler h) {
		Handler old = (Handler) dispatchTable.get(dest);
		dispatchTable.put(dest, h);
		return old;
	}

	HashMap dispatchTable = new HashMap();
	Handler defaultHandler = null;

	protected void dispatch(String dest, List args) {
		Handler h = (Handler) dispatchTable.get(dest);
		if (h != null)
			dispatchTo(h, dest, args);
		else if (defaultHandler != null)
			dispatchTo(defaultHandler, dest, args);
	}

	public void dispatchTo(Handler h, String dest, List args) {
		if (h instanceof DispatchableHandler) {
			((DispatchableHandler) h).handle(dest, args.toArray());
		} 
//		else {
//			// call handle method through reflection
//			Method m = ReflectionTools.findFirstMethodCalled(h.getClass(), "handle");
//			try {
//				m.invoke(h, args.toArray());
//			} catch (IllegalArgumentException e) {
//				e.printStackTrace();
//			} catch (IllegalAccessException e) {
//				e.printStackTrace();
//			} catch (InvocationTargetException e) {
//				e.printStackTrace();
//			}
//		}
	}

	protected String readString(ByteBuffer b) {
		char read = '\0';
		int position = b.position();
		do {
			read = (char) b.get();
		} while (read != ';' && read!='\0');
		byte[] bytes = new byte[b.position() - position-1];
		b.position(position);
		b.get(bytes);
		b.get(); // the null term
		String ret = new String(bytes);
		pad4(b);
		return ret;
	}

	public static int intify(byte argh) {
		int i = (argh < 0 ? ((int) argh) + 256 : (int) argh);
		return i;
	}

	public Integer readInt(ByteBuffer b) {
		int i1 = intify(b.get());
		int i2 = intify(b.get());
		int i3 = intify(b.get());
		int i4 = intify(b.get());
		return new Integer((i1 << 24) | (i2 << 16) | (i3 << 8) | i4);
	}

	public Long readLong(ByteBuffer b) {
		long i1 = longify(readInt(b).intValue());
		long i2 = longify(readInt(b).intValue());
		return new Long((i1 << 32) | i2);
	}
	
	public static long longify(int argh) {
		long i = (argh < 0 ? ((long) argh) + (long) 4294967296L : (long) argh);
		return i;
	}


	public Double readDouble(ByteBuffer b) {
		long l = readLong(b).longValue();
		return new Double(Double.longBitsToDouble(l));
	}
	public Float readFloat(ByteBuffer b) {
		int l = readInt(b).intValue();
		return new Float(Float.intBitsToFloat(l));
	}

	protected void pad4(ByteBuffer b) {
		int i = (4 - (b.position() % 4)) % 4;
		for (int n = 0; n < i; n++) {
			byte read = b.get();
			assert read == 0 : read + "!=" + 0;
		}
	}

	public void close() {
		listener.close();
	}

}
