package transponderTCP;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.SocketAddress;

// Transponder-Client 
// clientSocketLocal: Local socket
// clientSocketRemote: Remote socket
// socketLocalAddr: Local address for assignment to local socket
// socketRemoteAddr: Remote address for assignment to remote socket
// clientStream: Stream for incoming data
// objInpStream: Object Stream (used with clientStream) used to receive object
// incomingPayload: Recieving object for Payload-class payload, recieved via objInpStream
// stopFlag: boolean used to start / stop thread
public class tClient implements Runnable {
	private Socket clientSocketLocal = null;
	private SocketAddress socketLocalAddr = null;
	private SocketAddress socketRemoteAddr = null;
	private InputStream clientStream = null;
	private ObjectInputStream objInpStream = null;
	private Payload incomingPayload = null;
	private boolean stopFlag = false;
	private boolean debugFlag = false;
	private debugObj debugObject = null;
	
	tClient(Socket localSocket){
		this.clientSocketLocal = localSocket;
		
		if(localSocket.isBound()) {
			this.socketLocalAddr = localSocket.getLocalSocketAddress();
		}
		
		if(localSocket.isConnected()) {
			this.socketRemoteAddr = localSocket.getRemoteSocketAddress();
		}
	}
	
	tClient(Socket localSocket,SocketAddress localAddress, SocketAddress remoteEndpoint){
		if(localSocket.isBound()) {
			throw new IllegalStateException("localSocket is already bound!");
		}
		
		this.clientSocketLocal = localSocket;
		this.socketLocalAddr = localAddress;
		this.socketRemoteAddr = remoteEndpoint;
	}
	
	public void setRemoteSocketAddress(SocketAddress endpoint) {
		this.socketRemoteAddr = endpoint;
	}
	
	public void setLocalSocketAddress(SocketAddress localAddress) {
		this.socketLocalAddr = localAddress;
	}
	
	public void connectLocalTCP() {
		if(this.socketRemoteAddr == null) {
			throw new IllegalStateException("socketRemoteAddr not set!");
		}
		try {
			this.clientSocketLocal.connect(this.socketRemoteAddr);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void bindLocalTCP() {
		if(this.socketLocalAddr == null) {
			throw new IllegalArgumentException("tClient Local socket not set!");
		}
		if(this.clientSocketLocal.isBound()) {
			return;
		}
		
		try {
			this.clientSocketLocal.bind(socketLocalAddr);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void receiveTCP() {
		try {
			clientStream = this.clientSocketLocal.getInputStream();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		// TODO: Rewrite this run method, consider how you want this to behave.
		// TODO Auto-generated method stub
		while(this.stopFlag == false) {
			
			try {
				if(!this.clientSocketLocal.isBound()) {
					this.bindLocalTCP();
				}
				
				if(!this.clientSocketLocal.isConnected()) {
					this.connectLocalTCP();
				}

				this.clientStream = clientSocketLocal.getInputStream();
				this.objInpStream = new ObjectInputStream(this.clientStream);
				// TODO: Create type-check for this.incomingPayload
				this.incomingPayload = (Payload) objInpStream.readObject();
				
				// Debug check and method execution
				if(this.debugFlag == true) {
					this.debugPayloadIntegrity();
				}
				
				// Print the received payload
				// TODO: Decide and develop what to do with the received payload from here
				// Intended functionality is NOT to simply receive a payload and toString() it.
				System.out.println(this.incomingPayload);
			} catch (IOException | ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void stop() {
		//TODO: Create stop flag, assign it here
		this.stopFlag = true;
	}
	
	public String getRemoteAddrString() {
		if(this.socketRemoteAddr != null) {
			return this.socketRemoteAddr.toString();
		} else {
			throw new IllegalStateException("No clientSocketLocal set! Unable to return string!");
		}
	}
	
	// Returns the current payload
	// Throws an IllegalStateException if the IncomingPayload is currently null
	public Payload getPayload() {
		if(this.incomingPayload != null) {
			return this.incomingPayload;
		} else {
			throw new IllegalStateException("tClient incomingPayload is null!");
		}
	}
	
	public void setDebugFlag(Boolean flag) {
		this.debugFlag = flag;
	}
	
	public void setDebugObj(debugObj debug) {
		this.debugObject = debug;
	}
	
	public void debugPayloadIntegrity() {
		if(this.debugFlag == true) {
			if(this.incomingPayload == null) {
				System.out.println("tClient incomingPayload not set!");
			}
		}
		if(this.incomingPayload != null) {
			this.debugObject.addOutputPayload(incomingPayload);
		}
	}
}
