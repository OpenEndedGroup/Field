package field.core.network;

import java.nio.ByteBuffer;

/**
 * @author marc
 */
public class OSCOutput {
	public interface OSCElement {
		public char getType();

		public void write(OSCOutput into);
	}

	static public class OscInteger implements OSCElement {
		private int i;

		public OscInteger(int i) {
			this.i = i;
		}

		public char getType() {
			return 'i';
		}

		public void write(OSCOutput into) {
			into.writeInt(i);
		}
	}

	static public class OscFloat implements OSCElement {
		private float i;

		public OscFloat(float i) {
			this.i = i;
		}

		public char getType() {
			return 'f';
		}

		public void write(OSCOutput into) {
			into.writeFloat(i);
		}
	}

	static public class OscString implements OSCElement {
		private String i;

		public OscString(String i) {
			this.i = i;
		}

		public char getType() {
			return 's';
		}

		public void write(OSCOutput into) {
			into.writeString(i);
		}
	}

	ByteBuffer buffer;
	UDPNIOSender sender;

	public OSCOutput(int maxPacketSize, UDPNIOSender sender) {
		buffer = ByteBuffer.allocateDirect(maxPacketSize);
		this.sender = sender;
	}

	public OSCOutput encode(String destination, OSCElement[] elements) {
		buffer.clear();
		writeString(destination);
		String header = ",";
		for (int i = 0; i < elements.length; i++)
			header += elements[i].getType();
		writeString(header);
		for (int i = 0; i < elements.length; i++)
			elements[i].write(this);
		return this;
	}

	public OSCOutput send(String name, float f) {
		return encode(name, new OSCElement[] { new OscFloat(f) }).send();
	}

	public OSCOutput send(String name, int f) {
		return encode(name, new OSCElement[] { new OscInteger(f) }).send();
	}

	public OSCOutput send() {
		buffer.limit(buffer.position());
		buffer.rewind();
		sender.send(buffer);
		return this;
	}

	public void writeString(String s) {
		byte[] b = s.getBytes();
		buffer.put(b);
		buffer.put((byte) 0);
		pad4();
	}

	public void pad4() {
		int i = (4 - (buffer.position() % 4)) % 4;
		for (int n = 0; n < i; n++)
			buffer.put((byte) 0);
	}

	public void writeInt(int i) {
		buffer.put((byte) ((i >> 24) & 255)).put((byte) ((i >> 16) & 255)).put((byte) ((i >> 8) & 255)).put((byte) ((i >> 0) & 255));
	}

	public void writeLong(long i) {
		buffer.put((byte) ((i >> 56) & 255)).put((byte) ((i >> 48) & 255)).put((byte) ((i >> 40) & 255)).put((byte) ((i >> 32) & 255)).put((byte) ((i >> 24) & 255)).put((byte) ((i >> 16) & 255)).put((byte) ((i >> 8) & 255)).put((byte) ((i >> 0) & 255));
	}

	public void writeFloat(float d) {
		writeInt(Float.floatToIntBits(d));
	}

	public void simpleSend(String name, Object... things) {
		OSCElement[] e = new OSCElement[things.length];
		int x = 0;
		for (Object o : things) {
			if (o instanceof Integer)
				e[x] = new OscInteger(((Number) o).intValue());
			else if (o instanceof Float || o instanceof Number)
				e[x] = new OscFloat(((Number) o).floatValue());
			else if (o instanceof String)
				e[x] = new OscString((String) o);
			else
				throw new IllegalArgumentException(" couldn't transform <" + o + "> of class <" + (o == null ? null : o.getClass()) + ">");
			x++;
		}

		encode(name, e).send();
	}

	public void close() {
		sender.close();
	}

}
