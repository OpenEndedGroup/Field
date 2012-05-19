package field.extras.max;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

/**
//This is a lot like UDPSender but for nio and byte buffers
//<p>
//if you can, reuse your buffer each tick and use a native one,
//the networking will probably be more native and good that way.
//<p>
//potential trouble: bind binds to a local address,
//so i think maybe you should let people specificy which local address,
//in case they are a laptop with 2.<p>-jg
 */

public class UDPNIOSender {
	protected DatagramChannel channel;
	protected InetSocketAddress ip;
	
	public UDPNIOSender(int port, String to){
		this.channel = makeChannel(-1);
		setup(channel, port, to);
	}

	
	public UDPNIOSender(int port, int local_port, String to){
		this.channel = makeChannel(local_port);
		setup(channel, port, to);
	}
 
	public UDPNIOSender(DatagramChannel channel, int port, String to){
		this.channel = channel;
		setup(channel, port, to);
	}
	
	protected DatagramChannel makeChannel(int local_port){
		System.out.println(" making channel on port "+local_port);
		
		DatagramSocket socket = null;
		DatagramChannel theChannel = null;
		try{
			theChannel = DatagramChannel.open();
			
			socket = theChannel.socket();
			socket.setReuseAddress(true);
		}catch(Exception ex){
			System.out.println("error creating channel:");
			ex.printStackTrace();
		}

		try{
			//	channel.connect(ip);
			if(local_port == -1){
				socket.bind(null);
			}else{
				socket.bind(new InetSocketAddress(local_port));
			}
		}catch(Exception ex){
			System.out.println("could not bind socket to local port("+local_port+")");
			ex.printStackTrace();
		}
//		System.out.println("local port is:"+socket.getLocalPort());
		return theChannel;
	}

	protected void setup(DatagramChannel channel, int port, String to){
		try{
			ip = new InetSocketAddress(to, port);
			if(to.endsWith("255")){
				channel.socket().setBroadcast(true);
			}
		}catch(Exception ex){
			System.out.println("error creating socket from address:"+to);
			ex.printStackTrace();
		}
		if(ip.isUnresolved()){
			System.out.println("Could not resolve host \""+to+"\"");
		}
		try{
			channel.configureBlocking(true);
		}catch(Exception ex){
			ex.printStackTrace();
		}
		try{
			System.out.println("Socket send buffer size:"+channel.socket().getSendBufferSize());
		}catch(Exception ex){
			System.out.println("error trying to print out the sendbuffersize of the socket.");
			ex.printStackTrace();
		}
	}
	

	public int getPort(){
		return ip.getPort();
	}
	public String getRemoteIP(){
		return ip.getAddress().getHostAddress();
	}

	/**
	//"data" should be ready to go; ie, already "flipped"
	//with position at start of data to send
	//and limit at end.
	*/
	public void send(ByteBuffer data){
		try{

			channel.socket().setSendBufferSize(20000);
			
//			System.out.println(" sending buffer <"+data.limit()+"> to <"+channel+"> <"+data+"> to <"+ip+">");
			channel.send(data, ip);
			
			
//			byte[] aa = new byte[Math.min(1460, data.limit())];
//			data.get(aa);
//			
//			channel.socket().send(new DatagramPacket(aa, aa.length, ip));


//			channel.send(data, ip);
		}catch(IOException ioex){
			System.out.println("io exception writing to buffer in UDPNIOSender!");
			ioex.printStackTrace();
		}
	}

	/**
	//get the channel in case you want to do something to it.  i dunno what.
	*/
	public DatagramChannel getChannel(){
		return channel;
	}

	public String toString(){
		return "<UDPNIOSender local:"+channel.socket().getLocalSocketAddress().toString()+" remote:"+ip.toString()+">";
	}


	public void close() {
		try {
			this.channel.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
