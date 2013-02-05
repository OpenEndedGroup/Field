/*
 * Created on Oct 11, 2003
 */
package field.core.network;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import field.launch.iUpdateable;
import field.math.BaseMath;
import field.namespace.generic.ReflectionTools;

/**
 * @author marc
 */
public class OSCInput implements iUpdateable {
    
	public static int max_packet_size = 20000;
	public static int num_buffers = 5;
	protected SimpleUDPListener listener;
    
	public OSCInput(int port) {
        
		;//System.out.println(" this is the new stuff ");
		
		try {
			listener = new SimpleUDPListener(max_packet_size, port) {
                
				@Override
				protected void handle(byte[] data) {
					
					decode(ByteBuffer.wrap(data));
				}
			};
		} catch (SocketException e) {
			e.printStackTrace();
		}
        
	}
    
	public void update() {
		if (listener!=null)
			listener.update();
	}
    
	protected void decode(ByteBuffer bb) {
		;//System.out.println(" inside decode :" + bb);
		if (bb.remaining() == 0)
			return;
        
		String dest = readString(bb);
		// ;//System.out.println(" string <"+dest+">");
		if (dest.startsWith("#")) {
			Long l = readLong(bb); // time-tag
			System.out.println(" time tag <" + l + ">");
			while (bb.remaining() > 0) {
				Integer length = readInt(bb);
				if (length.intValue() > 0) {
					// ;//System.out.println(" length <" +
					// length + ">");
					decode(bb);
					pad4(bb);
					// ;//System.out.println(" remaining <" +
					// bb.remaining() + ">");
				}
			}
		} else {
			String header = readString(bb);
			System.out.println(" header is <" + header + ">");
			List args = new LinkedList();
			for (int i = 1; i < header.length(); i++) {
				Object made = null;
				switch (header.charAt(i)) {
                    case 'i':
                        made = readInt(bb);
                        break;
                    case 's':
                        made = readString(bb);
                        break;
                    case 'S':
                        made = readString(bb);
                        break;
                    case 'f':
                        made = readFloat(bb);
                        break;
                    case 'd':
                        made = readDouble(bb);
                        break;
                    default:
                        ;//System.out.println(" :: warning :: <" + ((int) header.charAt(i)) + ">");
				}
				// ;//System.out.println(" made <"+made+">");
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
		;//System.out.println(" handle <" + dest + "> <" + h + "> <" + dispatchTable + "> : " + args);
		if (h != null)
			dispatchTo(h, dest, args);
		else if (dest.lastIndexOf("/") > 0) {
			String d2 = dest.substring(0, dest.lastIndexOf("/"));
			do {
				h = (Handler) dispatchTable.get(d2);
				// ;//System.out.println("   subhandle <"+d2+"> <"+h+">");
				if (h != null) {
					dispatchTo(h, dest, args);
					return;
				}
                
			} while (d2.lastIndexOf("/") > 1);
            
			if (defaultHandler != null)
                
				dispatchTo(defaultHandler, dest, args);
            
		} else if (defaultHandler != null)
			dispatchTo(defaultHandler, dest, args);
	}
    
	public void dispatchTo(Handler h, String dest, List args) {
		if (h instanceof DispatchableHandler) {
			((DispatchableHandler) h).handle(dest, args.toArray());
		} else {
			// call handle method through reflection
			Method m = ReflectionTools.findFirstMethodCalled(h.getClass(), "handle");
			try {
				m.invoke(h, args.toArray());
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}
    
	protected String readString(ByteBuffer b) {
		char read = '\0';
		int position = b.position();
		do {
			read = (char) b.get();
		} while (read != ';' && read != '\0' && b.remaining() > 0);
		byte[] bytes = new byte[b.position() - position - 1];
		b.position(position);
		b.get(bytes);
		if (b.remaining() > 0)
			b.get(); // the null term
		String ret = new String(bytes);
		pad4(b);
		return ret;
	}
    
	public Integer readInt(ByteBuffer b) {
		int i1 = BaseMath.intify(b.get());
		int i2 = BaseMath.intify(b.get());
		int i3 = BaseMath.intify(b.get());
		int i4 = BaseMath.intify(b.get());
		return new Integer((i1 << 24) | (i2 << 16) | (i3 << 8) | i4);
	}
    
	public Long readLong(ByteBuffer b) {
		long i1 = BaseMath.longify(readInt(b).intValue());
		long i2 = BaseMath.longify(readInt(b).intValue());
		return new Long((i1 << 32) | i2);
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
		if (b.remaining() == 0)
			return;
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
