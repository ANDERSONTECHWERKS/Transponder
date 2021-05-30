package transponderTCP;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

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
	private BufferedInputStream clientStream = null;
	private ObjectInputStream objInpStream = null;
	private Payload incomingPayload = null;
	private boolean stopFlag = false;
	private boolean debugFlag = false;
	private debugObj debugObject = null;

	// Create tClient from a localSocket instance.
	// We are expecting that this socket is completely constructed (Local and remote addresses assigned)
	// If this isn't happening: Make it happen at the ControllerMenu / TransponderTCP level!
	tClient(Socket localSocket) {
		// Assign socket to field
		this.clientSocketLocal = localSocket;

		// Check if we have a localSocket that is already bound. Pull the LocalSocketAddress from it.
		if (localSocket.isBound()) {
			// Output for debugFlag
			if (this.debugFlag == true) {
				System.out.println("tClient| localSocket already bound on creation!");
				System.out.println(
						"tClient| Setting socketLocalAddr to " + localSocket.getLocalSocketAddress().toString());
			}
			this.socketLocalAddr = localSocket.getLocalSocketAddress();
		}

		// Check if the localSocket is connected. If it is, pull the RemoteSocketAddress from it.
		if (localSocket.isConnected()) {
			// Output for debugFlag
			if (this.debugFlag == true) {
				System.out.println("tClient| localSocket already connected on creation!");
				System.out.println(
						"tClient| Setting socketRemoteAddr to " + localSocket.getRemoteSocketAddress().toString());
			}
			this.socketRemoteAddr = localSocket.getRemoteSocketAddress();
		}
	}

	public void setRemoteSocketAddress(SocketAddress remoteAddr) {
		// Output for debugFlag
		if (this.debugFlag == true) {
			System.out.println("tClient| setting socketRemoteAddr to " + remoteAddr.toString());
		}

		this.socketRemoteAddr = remoteAddr;
	}

	public void setLocalSocketAddress(SocketAddress localAddress) {
		// Output for debugFlag
		if (this.debugFlag == true) {
			System.out.println("tClient| setting socketLocalAddr to " + localAddress.toString());
		}
		this.socketLocalAddr = localAddress;
	}

	public void connectLocalTCP() {

		if (this.socketRemoteAddr == null) {
			throw new IllegalStateException("tClient| socketRemoteAddr not set!");
		}

		try {
			// Output for debugFlag
			if (this.debugFlag == true) {
				System.out.println("tClient| attempting connectLocalTCP to: ");
				System.out.println(this.socketRemoteAddr.toString());
			}

			this.clientSocketLocal.connect(this.socketRemoteAddr);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// bindLocalTCP attempts to bind clientSocketLocal to the address specified by
	// socketLocalAddr
	public void bindLocalTCP() {
		if (this.socketLocalAddr == null) {
			throw new IllegalArgumentException("tClient| Local socket not set!");
		}
		if (this.clientSocketLocal.isBound()) {
			// Output for debugFlag
			if (this.debugFlag == true) {
				System.out.println("tClient| bindLocalTCP: Already bound! Returning!");
			}
			return;
		}

		try {
			// Output for debugFlag
			if (this.debugFlag == true) {
				System.out.println("tClient| bindLocalTCP attempting to bind to address: \n");
				System.out.println(this.socketLocalAddr.toString());
			}
			this.clientSocketLocal.bind(socketLocalAddr);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	// Pre-flight checks for transmission of Payload
	public boolean isClientReady() {

		// Check if we have an existing clientSocket that we closed.
		// If it is closed, we will recreate and reassign the Socket using that very ugly constructor below

		if (this.clientSocketLocal.isClosed()) {
			
			// Check if the socket exists, and is closed
			if (this.debugFlag == true) {
				System.out.println("clientSocketLocal not ready! Socket is closed!");
			}
			
			// Ugly constructor we are using to recreate the socket
			try {
				
				this.clientSocketLocal = new Socket(this.clientSocketLocal.getInetAddress().toString(),this.clientSocketLocal.getLocalPort()
						, this.clientSocketLocal.getLocalAddress(),this.clientSocketLocal.getLocalPort());
				
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if (!this.clientSocketLocal.isBound()) {
			// Output for debugFlag
			if (this.debugFlag == true) {
				System.out.println("tClient| clientSocketLocal is NOT BOUND! Running bindLocalTCP!");
			}
			this.bindLocalTCP();
		}

		if (!this.clientSocketLocal.isConnected()) {
			// Output for debugFlag
			if (this.debugFlag == true) {
				System.out.println("tClient| clientSocketLocal is NOT CONNECTED! Running connectLocalTCP!");
			}
			this.connectLocalTCP();
		}
		
		// Output for debugFlag
		if (this.debugFlag == true) {
			System.out.println(
					"tClient| clientSocketLocal local address: " + this.clientSocketLocal.getLocalAddress().toString()
							+ " Port: " + this.clientSocketLocal.getLocalPort());
			System.out.println("tClient| clientSocketLocal remote address: "
					+ this.clientSocketLocal.getRemoteSocketAddress().toString() + " Port: "
					+ this.clientSocketLocal.getPort());
		}

		// Check if clientStream exists yet. If not - create it.
		if (this.clientStream == null) {
			try {
				clientStream = new BufferedInputStream(this.clientSocketLocal.getInputStream());
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}

		// Output for debugFlag
		if (this.debugFlag == true) {
			if (this.clientStream instanceof InputStream) {
				System.out.println("tClient| clientStream instantiated successfully!");
			}
		}

		// Check if objInpStream exists yet. If not - create it.
		if (this.objInpStream == null) {
			try {
				this.objInpStream = new ObjectInputStream(this.clientStream);
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}

		// Output for debugFlag
		if (this.debugFlag == true) {
			if (this.objInpStream instanceof ObjectInputStream) {
				System.out.println("tClient| objInpStream instantiated successfully!");
			}
		}
		return true;
	}

	// receiveTCP attempts to obtain an inputStream from clientSocketLocal and
	// assign it to clientStream
	public synchronized void receiveTCP() {

			// TODO: Create type-check for this.incomingPayload
			// This is likely a huge security issue to just *blatantly accept* objects and
			// cast them
			// as Payloads.

			try {
				Object temp = objInpStream.readObject();
				
				if (temp instanceof Payload) {

					this.incomingPayload = (Payload) temp;

					// Debug check and method execution
					if (this.debugFlag == true) {
						System.out.println("tClient| Payload recieved! Payload toString() reads: \n");
						System.out.println("- - - - - - - - - -");
						System.out.println(this.incomingPayload.toString());
						System.out.println("- - - - - - - - - -");

						if (this.debugObject != null) {
							this.debugPayloadIntegrity();
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}

			// At this point: Use the received payload
			// TODO: Decide and develop what to do with the received payload from here
			// Intended functionality is NOT to simply receive a payload and toString() it.

			System.out.println("Payload recieved at time " + System.currentTimeMillis());

			this.cleanupClientConnection();

	}

	// Closes streams, and the clientSocketRemote
	public void cleanupClientConnection() {
		
		this.incomingPayload = null;
		try {
			this.clientStream.close();
			this.clientSocketLocal.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void checkConnectionResetKeepAlive() {
		if (this.clientSocketLocal.isConnected()) {
			try {
				this.clientSocketLocal.setKeepAlive(true);
			} catch (SocketException e) {
				e.printStackTrace();
			}
		} else {
			try {
				this.clientSocketLocal.setKeepAlive(false);
			} catch (SocketException e) {
				e.printStackTrace();
			}
		}
	}

	// runs the thread
	@Override
	public void run() {
		// TODO: Consider whether or not we need this run block within a do(while)-loop
		while(this.stopFlag == false) {
			if(this.isClientReady() == true) {
				// Receive the TCP transmission
				this.receiveTCP();
			}
		}
	}

	public void stop() {
		this.stopFlag = true;
	}

	public String getRemoteAddrString() {
		if (this.socketRemoteAddr != null) {
			return this.socketRemoteAddr.toString();
		} else {
			throw new IllegalStateException("tClient| No clientSocketLocal set! Unable to return string!");
		}
	}

	// Returns the current payload
	// Throws an IllegalStateException if the IncomingPayload is currently null
	public Payload getPayload() {
		if (this.incomingPayload != null) {
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
		if (this.debugFlag == true) {
			if (this.incomingPayload == null) {
				System.out.println("tClient| incomingPayload not set!");
			}
		}
		if (this.incomingPayload != null) {
			this.debugObject.addOutputPayload(incomingPayload);
		}
	}

	public String getStatus() {
		String status = "";
		status += "Connection Status: \n";

		status += "Local Address settings: \n";
		if (this.socketLocalAddr == null) {
			status += "Local Address set to null!\n";
		} else {
			status += "Local Address set to:\n";
			status += "TCP: " + this.socketLocalAddr.toString();
		}

		status += "Remote Address settings: \n";
		if (this.socketRemoteAddr.toString() == null) {
			status += "No remote socket set! \n";
		} else {
			status += "Remote Address set to:\n";
			status += "TCP: " + this.socketRemoteAddr.toString();
		}

		status += "Payload status: \n";
		if (this.incomingPayload == null) {
			status += "Payload not received! Currently: null \n";
		} else if (this.incomingPayload instanceof Payload) {
			status += "Payload received. Current payload:\n";
			status += this.incomingPayload.toString();
		}
		return status;
	}
}
