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
	
	// Create tClient from a localSocket instance. 
	tClient(Socket localSocket){
		this.clientSocketLocal = localSocket;
		
		if(localSocket.isBound()) {
			// Output for debugFlag
			if(this.debugFlag == true) {
				System.out.println("tClient| localSocket already bound on creation!");
				System.out.println("tClient| Setting socketLocalAddr to " + localSocket.getLocalSocketAddress().toString());
			}
			this.socketLocalAddr = localSocket.getLocalSocketAddress();
		}
		
		if(localSocket.isConnected()) {
			// Output for debugFlag
			if(this.debugFlag == true) {
				System.out.println("tClient| localSocket already connected on creation!");
				System.out.println("tClient| Setting socketRemoteAddr to " + localSocket.getRemoteSocketAddress().toString());
			}
			this.socketRemoteAddr = localSocket.getRemoteSocketAddress();
		}
	}

	
	public void setRemoteSocketAddress(SocketAddress remoteAddr) {
		// Output for debugFlag
		if(this.debugFlag == true) {
			System.out.println("tClient| setting socketRemoteAddr to " + remoteAddr.toString());
		}
		
		this.socketRemoteAddr = remoteAddr;
	}
	
	public void setLocalSocketAddress(SocketAddress localAddress) {
		// Output for debugFlag
		if(this.debugFlag == true) {
			System.out.println("tClient| setting socketLocalAddr to " + localAddress.toString());
		}
		this.socketLocalAddr = localAddress;
	}
	
	public void connectLocalTCP() {
		if(this.socketRemoteAddr == null) {
			throw new IllegalStateException("tClient| socketRemoteAddr not set!");
		}
		try {
			// Output for debugFlag
			if(this.debugFlag == true) {
				System.out.println("tClient| attempting connectLocalTCP to: ");
				System.out.println(this.socketRemoteAddr.toString());
			}
			this.clientSocketLocal.connect(this.socketRemoteAddr);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// bindLocalTCP attempts to bind clientSocketLocal to the address specified by socketLocalAddr
	public void bindLocalTCP() {
		if(this.socketLocalAddr == null) {
			throw new IllegalArgumentException("tClient| Local socket not set!");
		}
		if(this.clientSocketLocal.isBound()) {
			// Output for debugFlag
			if(this.debugFlag == true) {
				System.out.println("tClient| bindLocalTCP: Already bound! Returning!");
			}
			return;
		}
		
		try {
			// Output for debugFlag
			if(this.debugFlag == true) {
				System.out.println("tClient| bindLocalTCP attempting to bind to address: \n");
				System.out.println(this.socketLocalAddr.toString());
			}
			this.clientSocketLocal.bind(socketLocalAddr);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	// receiveTCP attempts to obtain an inputStream from clientSocketLocal and assign it to clientStream
	public void receiveTCP() {
		try {
			// Output for debugFlag
			if(this.debugFlag == true) {
				System.out.println("tClient| attempting receiveTCP from clientSocketLocal and instantiate clientStream object...");
				System.out.println("tClient| clientSocketLocal local address: " + this.clientSocketLocal.getLocalAddress().toString() +
						"Port: " + this.clientSocketLocal.getLocalPort());
				System.out.println("tClient| clientSocketLocal remote address: " + this.clientSocketLocal.getRemoteSocketAddress().toString() +
						"Port: " + this.clientSocketLocal.getPort());
			}
			
			clientStream = this.clientSocketLocal.getInputStream();

			// Output for debugFlag
			if(this.debugFlag == true) {
				if(this.clientStream instanceof InputStream) {
					System.out.println("tClient| clientStream instantiated successfully!");
				}
			}
			
			this.objInpStream = new ObjectInputStream(this.clientStream);
			
			// Output for debugFlag
			if(this.debugFlag == true) {
				if(this.objInpStream instanceof ObjectInputStream) {
					System.out.println("tClient| objInpStream instantiated successfully!");
				}
			}
			
			// TODO: Create type-check for this.incomingPayload
			// This is likely a huge security issue to just *blatantly accept* objects and cast them
			// as Payloads. 
			this.incomingPayload = (Payload) objInpStream.readObject();
			
			// Debug check and method execution
			if(this.debugFlag == true) {
				System.out.println("tClient| Payload recieved! Payload toString() reads: \n");
				System.out.println("- - - - - - - - - -");
				System.out.println(this.incomingPayload.toString());
				System.out.println("- - - - - - - - - -");

				this.debugPayloadIntegrity();
			}
			
			// Print the received payload
			// TODO: Decide and develop what to do with the received payload from here
			// Intended functionality is NOT to simply receive a payload and toString() it.

			System.out.println(this.incomingPayload);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// runs the thread
	// 
	@Override
	public void run() {
		// TODO: Rewrite this run method, consider how you want this to behave.
		// TODO Auto-generated method stub
		while(this.stopFlag == false) {
			
			if(!this.clientSocketLocal.isBound()) {
				// Output for debugFlag
				if(this.debugFlag == true) {
					System.out.println("tClient| clientSocketLocal is NOT BOUND! Running bindLocalTCP!");
				}
				this.bindLocalTCP();
			}
			
			if(!this.clientSocketLocal.isConnected()) {
				// Output for debugFlag
				if(this.debugFlag == true) {
					System.out.println("tClient| clientSocketLocal is NOT CONNECTED! Running connectLocalTCP!");
				}
				this.connectLocalTCP();
			}
			
			this.receiveTCP();
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
			throw new IllegalStateException("tClient| No clientSocketLocal set! Unable to return string!");
		}
	}
	
	// Returns the current payload
	// Throws an IllegalStateException if the IncomingPayload is currently null
	public Payload getPayload() {
		if(this.incomingPayload != null) {
			return this.incomingPayload;
		} else {
			throw new IllegalStateException("tClient| incomingPayload is null!");
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
				System.out.println("tClient| incomingPayload not set!");
			}
		}
		if(this.incomingPayload != null) {
			this.debugObject.addOutputPayload(incomingPayload);
		}
	}
}
