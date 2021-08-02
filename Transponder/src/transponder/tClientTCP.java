package transponder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

// Transponder-Client 
// clientSocket: Local socket
// clientSocketRemote: Remote socket
// socketLocalAddr: Local address for assignment to local socket
// socketRemoteAddr: Remote address for assignment to remote socket
// clientStream: Stream for incoming data
// objInpStream: Object Stream (used with clientStream) used to receive object
// incomingPayload: Recieving object for Payload-class payload, recieved via objInpStream
// stopFlag: boolean used to start / stop thread
public class tClientTCP implements Runnable {

	private Socket clientSocket = null;
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
	// DEBUG ONLY!
	
	tClientTCP(){
		// Nothing set...
	}

	// tClient instance without parent reference.
	// DEBUG ONLY!
	tClientTCP(Socket localSocket) {
		// Assign socket to field
		this.clientSocket = localSocket;

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
	// Includes reference to parent transponder object for callbacks!
	tClientTCP(Socket localSocket, TransponderTCP parent) {
		// Assign socket to field
		this.clientSocket = localSocket;

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

			this.clientSocket.connect(this.socketRemoteAddr);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// bindLocalTCP attempts to bind clientSocket to the address specified by
	// socketLocalAddr
	public void bindLocalTCP() {
		if (this.socketLocalAddr == null) {
			throw new IllegalArgumentException("tClient| Local socket not set!");
		}
		if (this.clientSocket.isBound()) {
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
			this.clientSocket.bind(socketLocalAddr);
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

		if (this.clientSocket.isClosed()) {

			// Check if the socket exists, and is closed
			if (this.debugFlag == true) {
				System.out.println("tClient| clientSocket not ready! Socket is closed!");
			}

			// Ugly constructor we are using to recreate the socket
			try {
				System.out.println("tClient| clientSocket is closed! Attempting to recreate socket...");
				System.out.println("tClient| Using the following to recreate socket:");
				System.out.println("tClient| Server address:" + this.clientSocket.getRemoteSocketAddress());
				System.out.println("tClient| Server port:" + this.clientSocket.getPort());
				System.out.println("tClient| Client address:" + this.clientSocket.getLocalAddress());
				System.out.println("tClient| Client port:" + this.clientSocket.getLocalPort());

				this.clientSocket = new Socket(this.clientSocket.getInetAddress(),
						this.clientSocket.getLocalPort(), this.clientSocket.getLocalAddress(),
						this.clientSocket.getLocalPort());

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

		if (!this.clientSocket.isBound()) {
			// Output for debugFlag
			if (this.debugFlag == true) {
				System.out.println("tClient| clientSocket is NOT BOUND! Running bindLocalTCP!");
			}
			this.bindLocalTCP();
		}

		if (!this.clientSocket.isConnected()) {
			// Output for debugFlag
			if (this.debugFlag == true) {
				System.out.println("tClient| clientSocket is NOT CONNECTED! Running connectLocalTCP!");
			}
			this.connectLocalTCP();
		}

		// Output for debugFlag
		if (this.debugFlag == true) {
			System.out.println(
					"tClient| clientSocket local address: " + this.clientSocket.getLocalAddress().toString()
							+ " Port: " + this.clientSocket.getLocalPort() + "\n") ;
			System.out.println("tClient| clientSocket remote address: "
					+ this.clientSocket.getRemoteSocketAddress().toString() + " Port: "
					+ this.clientSocket.getPort() + "\n");
		}

		// Check if clientStream exists yet. If not - create it.
		if (this.clientBuffInputStream == null) {
			try {
				clientBuffInputStream = new BufferedInputStream(this.clientSocket.getInputStream());

				// Output for debugFlag
				if (this.debugFlag == true) {
					if (this.clientBuffInputStream instanceof InputStream) {
						System.out.println("tClient| clientStream instantiated successfully! \n");
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
						System.out.println("tClient| objInpStream instantiated successfully! \n");
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

	// receiveTCP attempts to obtain an inputStream from clientSocket and
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

				// Debug output
				if (this.debugFlag == true) {

					System.out.println("tClient| Payload recieved! Payload toString() reads: \n");
					System.out.println("- - - - - - - - - -");
					System.out.println(this.incomingPayload.toString());
					System.out.println("- - - - - - - - - -");

					if (this.debugObject != null) {

						this.debugObject.addOutputPayload(incomingPayload);
					}
				}
			}

		} catch (SocketException e) {

			System.out.println("tClient| Connection reset! Stopping gracefully! \n");

			this.stopFlag = true;

			this.closeIO();

		} catch (EOFException e) {
			// If the EOF is hit, because of the other side closing or some error, bring the
			// client down gracefully.
			// TODO: Think about what we want to do if we hit the EOF
			
			System.out.println("tClient| End of File! Stopping gracefully! \n");

			this.stopFlag = true;
			this.closeIO();
			
			e.printStackTrace();


		} catch (IOException e) {
			
			System.out.println("tClient| IOException! Stopping gracefully! \n");

			this.stopFlag = true;
			this.closeIO();
			
			e.printStackTrace();

			
		} catch (ClassNotFoundException e) {
			
			System.out.println("tClient| ClassNotFound Exception! Stopping gracefully! \n");

			this.stopFlag = true;
			this.closeIO();
			
			e.printStackTrace();
		}

		// At this point: Use the received payload
		// TODO: Decide and develop what to do with the received payload from here
		// Intended functionality is NOT to simply receive a payload and toString() it.

		if(debugFlag == true && this.incomingPayload != null) {
			System.out.println("Payload recieved at time " + System.currentTimeMillis() + "\n");
			System.out.println(this.incomingPayload.toString() + "\n");
		}


	}

	public void performSignOn() {
		// Assumes we have a connected and NOT-closed socket

		// Attempts writing the clientSON object
		if (!this.clientSocket.isOutputShutdown()) {

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
	
	// pre-run preflights
	public void preflight_run() {
		// Condition where we have our local address and remote address
		if(this.socketLocalAddr != null && this.socketRemoteAddr != null) {
			
			// Extended condition where our current socket is null
			if(this.clientSocket == null) {
				
				this.clientSocket = new Socket();
				try {
					this.clientSocket.bind(socketLocalAddr);
					this.clientSocket.setReuseAddress(true);
					this.clientSocket.connect(socketRemoteAddr);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	// Closes streams, and the clientSocketRemote
	public void closeIO() {

		this.incomingPayload = null;
		if (this.clientBuffInputStream != null && this.clientSocket != null) {

			try {
				this.clientSocket.shutdownInput();
				this.clientSocket.shutdownOutput();
				
				this.objInpStream.close();
				this.objOutStream.close();
				
				this.clientBuffOutputStream.close();
				this.clientBuffInputStream.close();
				
				this.clientSocket.close();
			} catch (IOException e) {

				e.printStackTrace();

			} catch (NullPointerException e) {
				
				e.printStackTrace();
			}
		}
	}

	public void createInputStreams() {
		// Create a new BufferedInputStream from the inputStream generated via Socket
		// method
		try {
			
			this.clientBuffInputStream = new BufferedInputStream(this.clientSocket.getInputStream());
			this.objInpStream = new ObjectInputStream(this.clientBuffInputStream);
			
		} catch (java.io.EOFException e) {
			
			System.out.println("tClient| EOF exception with ObjectInputStream!\n");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void createOutputStreams() {

		// Create a new BufferedInputStream from the inputStream generated via Socket
		// method
		try {
	
			this.clientBuffOutputStream = new BufferedOutputStream(this.clientSocket.getOutputStream());
			this.objOutStream = new ObjectOutputStream(this.clientBuffOutputStream);
			this.objOutStream.flush();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void checkConnectionResetKeepAlive() {

		if (this.clientSocket.isConnected()) {
			
			try {
				
				this.clientSocket.setKeepAlive(true);
				
			} catch (SocketException e) {
				e.printStackTrace();
			}
			
		} else {
			try {
				
				this.clientSocket.setKeepAlive(false);
				
			} catch (SocketException e) {
				e.printStackTrace();
			}
		}
	}

	public clientSignOn generateClientSignOn(InetAddress client, InetAddress server) {

		clientSignOn signOn = new clientSignOn(client, server);

		return signOn;
	}

	public clientSignOff generateClientSignOff(InetAddress client, InetAddress server) {
		
		clientSignOff signOff = new clientSignOff(client, server);

		return signOff;
	}

	// runs the thread
	@Override
	public void run() {
		// Preflight
		this.preflight_run();
		
		// To begin, create the OutputStreams that we will use to send
		// clientSignOn/clientSignOff objects
		this.createOutputStreams();

		// Create inputStreams and begin receiving
		this.createInputStreams();

		// Generate the clientSON and clientSOFF objects
		this.clientSON = this.generateClientSignOn(this.clientSocket.getLocalAddress(),
				this.clientSocket.getInetAddress());
		
		this.clientSOFF = this.generateClientSignOff(this.clientSocket.getLocalAddress(),
				this.clientSocket.getInetAddress());

		// Perform clientSignOn
		// When clientSignOn is transmitted, assume that payloads are being transmitted.
		this.performSignOn();

		// pre-flight checks with isClientReady() and begin recieving payload
		if (this.isClientReady() == true) {
			
			// Receive the TCP transmission

				// Receive Payload after SignOn
				this.receivePayload();
				
		} else {
			
			System.out.println("tClient| isClientReady returned false! NO-OP!");
			
		}
		
		// Finally, send clientSignOff object
		this.performSignOff();
		this.closeIO();
	}

	public void stop() {
		
		this.stopFlag = true;
		this.performSignOff();
		this.closeIO();
	}

	public String getLocalAddrString() {
		
		if (this.socketLocalAddr != null) {
			
			return this.socketLocalAddr.toString();
			
		} else {
			
			throw new IllegalStateException("tClient| No socketLocalAddr set! Unable to return string! \n");
		}
	}
	
	public String getRemoteAddrString() {
		
		if (this.socketRemoteAddr != null) {
			
			return this.socketRemoteAddr.toString();
			
		} else {
			
			throw new IllegalStateException("tClient| No clientSocket set! Unable to return string! \n");
		}
	}

	// Returns the current payload
	// Throws an IllegalStateException if the IncomingPayload is currently null
	public Payload getPayload() {
		
		if (this.incomingPayload != null) {
			return this.incomingPayload;
		} else {
			
			throw new IllegalStateException("tClient| incomingPayload is null! \n");
		}
	}

	public void setDebugFlag(Boolean flag) {
		this.debugFlag = flag;
	}

	public void setDebugObj(debugObj debug) {
		this.debugObject = debug;
	}
	

	public String getStatus() {
		
		String status = "";
		
		status += "tClient| Connection Status: \n";

		status += "---Local Address settings--- \n";
		
		if (this.socketLocalAddr == null) {
			status += "Local Address set to null!\n";
			
		} else {
			status += "Local Address set to:\n";
			status += "TCP: " + this.socketLocalAddr.toString() + "\n";
			status += "Is local socket bound? : " + this.clientSocket.isBound() + "\n";
			status += "Is local socket closed? : " + this.clientSocket.isClosed() + "\n";
			status += "Is local socket connected? : " + this.clientSocket.isConnected() + "\n";
		}

		status += "---Remote Address settings--- \n";
		
		if (this.socketRemoteAddr == null) {
			status += "No remote socket set! \n";
		}
		
		if (this.socketRemoteAddr != null) {
			status += "Remote Address set to:\n";
			status += "IP/TCP: " + this.socketRemoteAddr.toString() + "\n";
		}

		status += "Payload status: \n";
		
		if (this.incomingPayload == null) {
			status += "tClient| incomingPayload Currently null!\n";
		} 
		
		if (this.incomingPayload instanceof Payload) {
			status += "Payload received. Current payload:\n";
			status += this.incomingPayload.toString() + "\n";
		}

		return status;
	}
}
