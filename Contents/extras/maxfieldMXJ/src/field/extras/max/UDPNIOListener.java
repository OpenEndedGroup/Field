package field.extras.max;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;

/**
 * //basic nio udp receiver. subclass and implement "processBuffer" to do something with the buffers. //
 * <p>
 * //some danger: packets received immediately after creation will show up in first update, // potentially a lot later. //
 * <p>
 * //this class should do threadsafe caching of recieved packets for you. //as many as numBuffers packets can be received between updates without dropping one. //then processBuffer is called on each of the received packets in order during the update. //
 * <p>
 * //processBuffer will be called at most numBuffer times per update.
 * <p>
 * -jg
 */

public class UDPNIOListener implements Runnable {
	protected DatagramChannel channel;
	protected ByteBuffer[] buffers;
	protected int numBuffers;
	protected boolean keepReceiving = true;
	protected boolean flushData = false;
	protected Object lock = new Object();
	protected InetSocketAddress address;
	protected SocketAddress[] froms;

	protected static class SynchronizedData {
		public int receivingInto;
		public int lastProcessed;
	}

	protected SynchronizedData syncData = new SynchronizedData();

	/**
	 * //numBuffers must be 3 or greater.
	 */
	public UDPNIOListener(int port, int maxPacketSize, int numBuffers) {
		this.channel = makeChannel(port); 
		setupBuffersAndGo(maxPacketSize, numBuffers, true, false);
	}

	public UDPNIOListener(DatagramChannel channel, int maxPacketSize, int numBuffers) {
		this.channel = channel;
		setupBuffersAndGo(maxPacketSize, numBuffers, true, false);
	}

	/**
	 * //useNativeBuffers only if you'll be using this listener for a while. //set flushdata to allow new data to overwrite old; //this improves latency for streams that are so fast we can't read every packet.
	 */

	public UDPNIOListener(int port, int maxPacketSize, int numBuffers, boolean useNativeBuffers, boolean flushData) {
		this.channel = makeChannel(port);
		setupBuffersAndGo(maxPacketSize, numBuffers, useNativeBuffers, flushData);
	}

	public UDPNIOListener(DatagramChannel channel, int maxPacketSize, int numBuffers, boolean useNativeBuffers, boolean flushData) {
		this.channel = channel;
		setupBuffersAndGo(maxPacketSize, numBuffers, useNativeBuffers, flushData);
	}

	protected DatagramChannel makeChannel(int port) {
		DatagramChannel channel = null;
		try {
			channel = DatagramChannel.open();
		} catch (Exception ex) {
			;//;//System.out.println("Error creating channel for UDPNIOListener");
			ex.printStackTrace();
		}
		DatagramSocket socket = null;
		try {
			socket = channel.socket();
			socket.setReuseAddress(true); //this should probably be an option
		} catch (Exception ex) {
			;//;//System.out.println("error getting socket for channel.");
			ex.printStackTrace();
		}
		try {
			address = new InetSocketAddress(port);
		} catch (Exception ex) {
			;//;//System.out.println("Error creating socketAddress for port:" + port);
			ex.printStackTrace();
		}
		try {
			channel.configureBlocking(true);
		} catch (Exception ex) {
			;//;//System.out.println("error setting channel to blocking mode.");
			ex.printStackTrace();
		}
		try {
			channel.socket().bind(address);
			//channel.connect(address);
		} catch (Exception ex) {
			;//;//System.out.println("unable to connect channel in UDPNIOListener to address:" + address);
			ex.printStackTrace();
			
		}

		try {
			;//;//System.out.println("Socket receive buffer size:" + socket.getReceiveBufferSize());
		} catch (Exception ex) {
			;//;//System.out.println("error trying to print out the receiveBufferSize of the socket.");
			ex.printStackTrace();
		}
		return channel;
	}

	protected void setupBuffersAndGo(int maxPacketSize, int numBuffers, boolean useNativeBuffers, boolean flushData) {
		this.flushData = flushData;
		this.numBuffers = numBuffers;

		if (numBuffers <= 3) {
			throw new IllegalArgumentException("numBuffers must be 3 or greater.  got " + numBuffers);
		}
		buffers = new ByteBuffer[numBuffers];
		froms = new SocketAddress[numBuffers];

		for (int i = 0; i < numBuffers; i++) {
			if (useNativeBuffers) {
				buffers[i] = ByteBuffer.allocateDirect(maxPacketSize).order(ByteOrder.nativeOrder());
			} else {
				buffers[i] = ByteBuffer.allocate(maxPacketSize);
			}
		}
		syncData.receivingInto = 0;
		syncData.lastProcessed = numBuffers - 1; //last one "before" 0, using mod arithmatic.

		 (new Thread(this)).start();
	}

	public void run() {
		DatagramSocket sock = channel.socket();
		while (!sock.isClosed()) {
			try {
				buffers[syncData.receivingInto].clear();
				froms[syncData.receivingInto] = channel.receive(buffers[syncData.receivingInto]);
			} catch (java.nio.channels.AsynchronousCloseException asex) {
				;//;//System.out.println("UDPNIOListener socket threw an 'AsynchronousCloseException' exception; probably someone closed it.");
				//sockEX.printStackTrace();
			} catch (Exception ex) {
				;//;//System.out.println("error receiving packet in UDPNIOListener receive thread");
				ex.printStackTrace();
				continue;
			}
			while (syncData.receivingInto == syncData.lastProcessed) {
				synchronized (syncData) {
					if (syncData.receivingInto == syncData.lastProcessed) {
						try {
							syncData.wait();
						} catch (Exception ex) {
							;//;//System.out.println("wait interrupted, huh?");
							ex.printStackTrace();
						}
					}
				}
			}
			synchronized (syncData) {
				if (!flushData || (syncData.receivingInto + 1) % numBuffers != syncData.lastProcessed) {
					//only do this if we're not flushing data, so its ok to stall,
					//OR if we are flushing data and doing this won't make us stall.
					syncData.receivingInto = (syncData.receivingInto + 1) % numBuffers;
				}
			}
		}
		;//;//System.out.println("UDPNIOListener receive thread exited.");
	}
	/**
	 * //probably don't override this, it handles the buffer. check out "processBuffer".
	 */
	protected int buffercurrentlybeingprocessedbysubclass = -1;
	public void update() {
		int ri = syncData.receivingInto;

		//I could see an argument for updating lastProcessed and calling notify for each
		//iteration here to get though a huge backlog of packets in the native buffer
		//of the socket... but i dunno, this method could execute for an arbitrary length of time
		//in that case, if the packets were coming fast enough...
		for (int i = (syncData.lastProcessed + 1) % numBuffers; i != ri; i = (i + 1) % numBuffers) {
			buffers[i].flip();
			try {
				buffercurrentlybeingprocessedbysubclass = i;
				processBuffer(buffers[i]);
				buffercurrentlybeingprocessedbysubclass = -1;
			} catch (Exception ex) {
				;//;//System.out.println("Exception processing buffer, skipping.");
				ex.printStackTrace();
			}
			buffers[i].clear();
		}
		synchronized (syncData) {
			syncData.lastProcessed = (ri + numBuffers - 1) % numBuffers;
			syncData.notify();
		}
	}

	public SocketAddress whereDidThisBufferComeFrom() {
		if(buffercurrentlybeingprocessedbysubclass == -1){
			;//;//System.out.println("error, you cannot call \"whereDidThisBufferComeFrom\" unless you are calling it from PacketHandler.handle(..)");
		}		
		return froms[buffercurrentlybeingprocessedbysubclass];
	}

	/**
	 * //override this method to actually do something.
	 * <p>// bb is only valid for the duration of this method; it is cleared immediately afterwards
	 * <p>
	 * //this method is called from within the main update loop, so go nuts; no need to worry about //missing packets by doing something slow here, or thread safety with main update loop objects.
	 */
	protected void processBuffer(ByteBuffer bb) {
	}

	/**
	 * //stops the receive thread(even if its blocked in "receive") //by calling socket.close().
	 */
	public void stopReceiveThread() {
		channel.socket().close();
	}

	public void close() {
		stopReceiveThread();
	}
}
