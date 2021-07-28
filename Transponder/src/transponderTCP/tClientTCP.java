package transponderTCP;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
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
	private BufferedInputStream clientBuffInputStream = null;
	private BufferedOutputStream clientBuffOutputStream = null;
	private ObjectInputStream objInpStream = null;
	private ObjectOutputStream objOutStream = null;

	private clientSignOff clientSOFF = null;
	private clientSignOn clientSON = null;
	private Payload incomingPayload = null;

	private boolean stopFlag = false;
	private boolean debugFlag = false;

	private debugObj debugObject = null;

	private TransponderTCP parentTransponder = null;

	// tClient instance without parent reference.
	// ONLY USE FOR TROUBLESHOOTING!

	tClient(Socket localSocket) {
		// Assign socket to field
		this.clientSocketLocal = localSocket;

		// Check if we have a localSocket that is already bound. Pull the
		// LocalSocketAddress from it.
		if (localSocket.isBound()) {
			// Output for debugFlag
			if (this.debugFlag == true) {
				System.out.println("tClient| localSocket already bound on creation!");
				System.out.println(
						"tClient| Setting socketLocalAddr to " + localSocket.getLocalSocketAddress().toString());
			}
			this.socketLocalAddr = localSocket.getLocalSocketAddress();
		}

		// Check if the localSocket is connected. If it is, pull the RemoteSocketAddress
		// from it.
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

	// Create tClient from a localSocket instance.
	// We are expecting that this socket is completely constructed (Local and remote
	// addresses assigned)
	// If this isn't happening: Make it happen at the ControllerMenu /
	// TransponderTCP level!
	// Includes reference to parent transponderTCP object for callbacks!
	tClient(Socket localSocket, TransponderTCP parent) {
		// Assign socket to field
		this.clientSocketLocal = localSocket;

		// Assigns reference to parentTransponder instance
		this.parentTransponder = parent;

		// Check if we have a localSocket that is already bound. Pull the
		// LocalSocketAddress from it.
		if (localSocket.isBound()) {
			// Output for debugFlag
			if (this.debugFlag == true) {
				System.out.println("tClient| localSocket already bound on creation!");
				System.out.println(
						"tClient| Setting socketLocalAddr to " + localSocket.getLocalSocketAddress().toString());
			}
			this.socketLocalAddr = localSocket.getLocalSocketAddress();
		}

		// Check if the localSocket is connected. If it is, pull the RemoteSocketAddress
		// from it.
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
	// Client is considered "Ready" when all streams have been instantiated

	public boolean isClientReady() {

		// Check if we have an existing clientSocket that we closed.
		// If it is closed, we will recreate and reassign the Socket using that very
		// ugly constructor below

		if (this.clientSocketLocal.isClosed()) {

			// Check if the socket exists, and is closed
			if (this.debugFlag == true) {
				System.out.println("clientSocketLocal not ready! Socket is closed!");
			}

			// Ugly constructor we are using to recreate the socket
			try {
				System.out.println("tClient| clientSocketLocal is closed! Attempting to recreate socket...");
				System.out.println("tClient| Using the following to recreate socket:");
				System.out.println("tClient| Server address:" + this.clientSocketLocal.getRemoteSocketAddress());
				System.out.println("tClient| Server port:" + this.clientSocketLocal.getPort());
				System.out.println("tClient| Client address:" + this.clientSocketLocal.getLocalAddress());
				System.out.println("tClient| Client port:" + this.clientSocketLocal.getLocalPort());

				this.clientSocketLocal = new Socket(this.clientSocketLocal.getInetAddress(),
						this.clientSocketLocal.getLocalPort(), this.clientSocketLocal.getLocalAddress(),
						this.clientSocketLocal.getLocalPort());

			} catch (UnknownHostException e) {
				System.out.println("tClient| Unknown host exception! Failed to create new socket!");
				e.printStackTrace();
				return false;
			} catch (IOException e) {
				System.out.println("tClient| IOException! Failed to create new socket!");
				e.printStackTrace();
				return false;
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
		if (this.clientBuffInputStream == null) {
			try {
				clientBuffInputStream = new BufferedInputStream(this.clientSocketLocal.getInputStream());

				// Output for debugFlag
				if (this.debugFlag == true) {
					if (this.clientBuffInputStream instanceof InputStream) {
						System.out.println("tClient| clientStream instantiated successfully!");
					}
				}

			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}

		// Check if objInpStream exists yet. If not - create it.
		if (this.objInpStream == null) {
			try {
				this.objInpStream = new ObjectInputStream(this.clientBuffInputStream);

				// Output for debugFlag, confirms that we have an instance of ObjectInputStream
				// successfully instantiated
				if (this.debugFlag == true) {
					if (this.objInpStream instanceof ObjectInputStream) {
						System.out.println("tClient| objInpStream instantiated successfully!");
					}
				}

			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}

		// This block used to check the first byte to see if the stream is at EOF.
		// ...Might be what's causing troubles. Commenting out for now.
//		// Ensures that we don't read on an EOF marker
//		// Blocks until a byte is readable
//		try {
//			if(this.objInpStream.read() == -1) {
//				return false;
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

		return true;
	}

	// receiveTCP attempts to obtain an inputStream from clientSocketLocal and
	// assign it to clientStream

	public void receivePayload() {

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
		} catch (java.net.SocketException e) {
			System.out.println("tClient| Connection reset! Stopping gracefully!");

			this.stopFlag = true;
			this.cleanupClientConnection();

		} catch (java.io.EOFException e) {
			// If the EOF is hit, because of the other side closing or some error, bring the
			// client down gracefully.
			// TODO: Think about what we want to do if we hit the EOF
			System.out.println("tClient| End of File! Stopping gracefully!");

			this.stopFlag = true;
			this.cleanupClientConnection();

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		// At this point: Use the received payload
		// TODO: Decide and develop what to do with the received payload from here
		// Intended functionality is NOT to simply receive a payload and toString() it.

		System.out.println("Payload recieved at time " + System.currentTimeMillis());
		System.out.println(this.incomingPayload.toString());

	}

	public void performSignOn() {
		// Assumes we have a connected and NOT-closed socket
		
		// Attempts writing the clientSON object
		if (!this.clientSocketLocal.isOutputShutdown()) {

			try {
				this.objOutStream.reset();
				this.objOutStream.writeObject(clientSON);
				this.objOutStream.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	// Transmits a clientSignOff object
	public void performSignOff() {
		// Assumes we have a connected and NOT-closed socket

		if (this.objOutStream != null && this.clientSOFF != null) {
			try {
				this.objOutStream.reset();
				this.objOutStream.writeObject(clientSOFF);
				this.objOutStream.flush();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	// Closes streams, and the clientSocketRemote
	// TODO: Throws a NPE here
	public void cleanupClientConnection() {

		this.incomingPayload = null;
		if (this.clientBuffInputStream != null && this.clientSocketLocal != null) {

			try {

				this.clientBuffInputStream.close();
				this.clientSocketLocal.close();

			} catch (IOException e) {

				e.printStackTrace();

			}
		}
	}

	public void createInputStreams() {
		//Create a new BufferedInputStream from the inputStream generated via Socket method
		try {
			this.clientBuffInputStream = new BufferedInputStream(this.clientSocketLocal.getInputStream());
			this.objInpStream = new ObjectInputStream(this.clientBuffInputStream);
		} catch(java.io.EOFException e) {
			System.out.println("tClient| EOF exception with ObjectInputStream!");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

	public void createOutputStreams() {
		//Create a new BufferedInputStream from the inputStream generated via Socket method
		try {
			this.clientBuffOutputStream = new BufferedOutputStream(this.clientSocketLocal.getOutputStream());
			this.objOutStream = new ObjectOutputStream(this.clientBuffOutputStream);
			this.objOutStream.flush();

		} catch (IOException e) {
			// TODO Auto-generated catch block
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
	
	public clientSignOn generateClientSignOn(InetAddress client, InetAddress server) {
		
		clientSignOn signOn = new clientSignOn(client,server);
		
		return signOn;
	}
	
	public clientSignOff generateClientSignOff(InetAddress client, InetAddress server) {
		clientSignOff signOff = new clientSignOff(client,server);
		
		return signOff;
	}

	// runs the thread
	@Override
	public void run() {

		// To begin, create the OutputStreams that we will use to send clientSignOn/clientSignOff objects
		this.createOutputStreams();
		
		// Create inputStreams and begin receiving
		this.createInputStreams();

		// Generate the clientSON and clientSOFF objects
		this.clientSON = this.generateClientSignOn(this.clientSocketLocal.getLocalAddress(), this.clientSocketLocal.getInetAddress());
		this.clientSOFF = this.generateClientSignOff(this.clientSocketLocal.getLocalAddress(), this.clientSocketLocal.getInetAddress());


		// Perform clientSignOn
		// When clientSignOn is transmitted, assume that payloads are being transmitted. 
		this.performSignOn();

		
		//pre-flight checks and begin recieving payload
		if (this.isClientReady() == true) {
			// Receive the TCP transmission
			while (this.stopFlag == false) {
				
				// Receive Payload after SignOn
				this.receivePayload();
			}
		} else {
			System.out.println("tClient| isClientReady returned false!");
		}
	}

	public void stop() {
		this.stopFlag = true;
		this.performSignOff();
		this.cleanupClientConnection();
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
		status += "tClient| Connection Status: \n";

		status += "---Local Address settings--- \n";
		if (this.socketLocalAddr == null) {
			status += "Local Address set to null!\n";
		} else {
			status += "Local Address set to:\n";
			status += "TCP: " + this.socketLocalAddr.toString();
			status += "Is local socket bound? : " + this.clientSocketLocal.isBound() + "\n";
			status += "Is local socket closed? : " + this.clientSocketLocal.isClosed() + "\n";
			status += "Is local socket connected? : " + this.clientSocketLocal.isConnected() + "\n";
		}

		status += "---Remote Address settings--- \n";
		if (this.socketRemoteAddr.toString() == null) {
			status += "No remote socket set! \n";
		} else {
			status += "Remote Address set to:\n";
			status += "TCP: " + this.socketRemoteAddr.toString() + "\n";

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
