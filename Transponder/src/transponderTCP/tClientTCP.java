package transponderTCP;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
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
import java.util.Date;
import java.util.concurrent.PriorityBlockingQueue;

// Transponder-Client 
// clientSocket: Local socket
// clientSocketRemote: Remote socket
// socketLocalAddr: Local address for assignment to local socket
// socketRemoteAddr: Remote address for assignment to remote socket
// clientStream: Stream for incoming data
// objInpStream: Object Stream (used with clientStream) used to receive object
// lastServMessage: Recieving object for Payload-class payload, recieved via objInpStream
// stopFlag: boolean used to start / stop thread
public class tClientTCP implements Runnable {

	private Socket clientSocket = null;

	private SocketAddress socketLocalAddr = null;
	private SocketAddress socketRemoteAddr = null;

	private OutputStream outputStream = null;
	private InputStream inputStream = null;
	
	private BufferedInputStream clientBuffInputStream = null;
	private BufferedOutputStream clientBuffOutputStream = null;

	private ObjectInputStream objInpStream = null;
	private ObjectOutputStream objOutStream = null;

	private clientSignOff clientSOFF = null;
	private clientSignOn clientSON = null;

	private ServerMessage<?> lastServMessage = null;
	private ClientMessage<?> lastClientMessage = null;

	private boolean stopFlag = false;
	private boolean debugFlag = false;

	private debugObj debugObject = null;

	private TransponderTCP parentTransponder = null;

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
				System.out.println(
						"tClient| Setting socketRemoteAddr to " + localSocket.getRemoteSocketAddress().toString());
			}

			this.socketLocalAddr = localSocket.getLocalSocketAddress();
			this.socketRemoteAddr = localSocket.getRemoteSocketAddress();
		}

		// Check if the localSocket is connected. If it is, pull the RemoteSocketAddress
		// from it.
		if (localSocket.isConnected()) {

			// Output for debugFlag
			if (this.debugFlag == true) {

				System.out.println("tClient| localSocket already connected on creation!");
				System.out.println(
						"tClient| localSocket connected to:" + localSocket.getRemoteSocketAddress().toString());
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
	tClientTCP(Socket localSocket, TransponderTCP parent) {

		// Assign socket to field
		this.clientSocket = localSocket;

		// Assigns reference to parentTransponder instance
		this.parentTransponder = parent;

		// Check if we have a localSocket that is already bound. Pull the
		// LocalSocketAddress from it.

		if (localSocket.isBound() && localSocket.isConnected()) {
			// Output for debugFlag
			if (this.debugFlag == true) {
				System.out.println("tClient| localSocket already bound on creation!");
				System.out.println(
						"tClient| Setting socketLocalAddr to " + localSocket.getLocalSocketAddress().toString());
			}
			this.socketLocalAddr = localSocket.getLocalSocketAddress();
			this.socketRemoteAddr = localSocket.getRemoteSocketAddress();
		}

		// Check if the localSocket is connected. If it is, pull the RemoteSocketAddress
		// from it.
		if (localSocket.isBound() && !localSocket.isConnected()) {
			// Output for debugFlag
			if (this.debugFlag == true) {
				System.out.println("tClient| localSocket bound, but not connected on tClient creation!");
				System.out.println(
						"tClient| Setting socketLocalAddr to " + localSocket.getLocalSocketAddress().toString());
			}
			this.socketLocalAddr = localSocket.getLocalSocketAddress();

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
			System.out.println("tClient| Exception while connecting local TCP! ");
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

	private boolean isClientReady() {

		// Check if we have an existing clientSocket that we closed.
		// If it is closed, we will recreate and reassign the Socket using that very
		// ugly constructor below
		if (this.clientSocket == null) {
			throw new IllegalStateException("tClient| clientSocket does not exist! isClientReady failed!");
		}

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

				this.clientSocket = new Socket(this.clientSocket.getInetAddress(), this.clientSocket.getLocalPort(),
						this.clientSocket.getLocalAddress(), this.clientSocket.getLocalPort());

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
			System.out.println("tClient| clientSocket local address: " + this.clientSocket.getLocalAddress().toString()
					+ " Port: " + this.clientSocket.getLocalPort() + "\n");
			System.out.println(
					"tClient| clientSocket remote address: " + this.clientSocket.getRemoteSocketAddress().toString()
							+ " Port: " + this.clientSocket.getPort() + "\n");
		}

		// Check if clientStream exists yet. If not - create it.
		if (this.clientBuffOutputStream == null) {
			try {
				clientBuffOutputStream = new BufferedOutputStream(this.clientSocket.getOutputStream());

				// Output for debugFlag
				if (this.debugFlag == true) {
					if (this.clientBuffOutputStream instanceof OutputStream) {
						System.out.println("tClient| clientBuffOutputStream instantiated successfully! \n");
					}
				}

			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
		}

		// Check if clientStream exists yet. If not - create it.
		if (this.clientBuffInputStream == null) {
			try {
				clientBuffInputStream = new BufferedInputStream(this.clientSocket.getInputStream());

				// Output for debugFlag
				if (this.debugFlag == true) {
					if (this.clientBuffInputStream instanceof InputStream) {
						System.out.println("tClient| clientBuffInputStream instantiated successfully! \n");
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

		return true;
	}

	public void receiveMessages() {

		// TODO: Create type-check for this.incomingPayload
		// This is likely a huge security issue to just *blatantly accept* objects and
		// cast them
		// as Payloads.

		try {

			Object inputObj = objInpStream.readObject();

			if (inputObj instanceof ClientMessage<?>) {
				
				ClientMessage<?> inpCliMess = (ClientMessage<?>) inputObj;
				
				// If we recieve a clientMessage - throw it into the clientMessages queue
				
				this.lastClientMessage = inpCliMess;

				// Debug output
				if (this.debugFlag == true) {

					System.out.println("tClient| ClientMessage recieved! ClientMessage toString() reads: \n");
					System.out.println("- - - - - - - - - -");
					System.out.println(inputObj.toString());
					System.out.println("- - - - - - - - - -");

					if (this.debugObject != null) {
						// TODO: Figure out what to do with ClientMessages in debugObj
					}
				}

				// Add received message to master list
				this.parentTransponder.addCliMessageToMaster(inpCliMess);
				
				// New message has been collected, setting parentTransponder.newClientMessage
				// flag to true
				this.parentTransponder.setNewClientMessageFlag(true);
				

			}

			if (inputObj instanceof ServerMessage<?>) {

				this.lastServMessage = (ServerMessage<?>) inputObj;

				// Debug output
				if (this.debugFlag == true) {

					System.out.println("tClient| ServerMessage recieved! ServerMessage toString() reads: \n");
					System.out.println("- - - - - - - - - -");
					System.out.println(this.lastServMessage.toString());
					System.out.println("- - - - - - - - - -");

					if (this.debugObject != null) {
						this.debugObject.setRecievedServMessage(lastServMessage);
					}

				}

				// Add received message to master list
				this.parentTransponder.addServMessageToMaster(lastServMessage);
				
				// New message has been collected, setting parentTransponder.newClientMessage
				// flag to true
				this.parentTransponder.setNewServerMessageFlag(true);
			}

		} catch (SocketException e) {

			System.out.println("tClient| Connection reset! Stopping gracefully! \n");

			this.stopFlag = true;

			this.closeIO();

		} catch (EOFException e) {
			// If the EOF is hit, because of the other side closing or some error, bring the
			// client down gracefully.
			// TODO: Think about what we want to do if we hit the EOF

			System.out.println("tClient| End of File! Stopping client gracefully! \n");

			this.stopFlag = true;
			this.closeIO();

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

		if (debugFlag == true && this.lastServMessage != null) {
			System.out.println("Last Server Message recieved at" + System.currentTimeMillis() + "\n ServerMessage reads: \n");
			System.out.println(this.lastServMessage.toString() + "\n");
		}

	}

	public boolean clientSendMessage(ClientMessage<?> message) {

		this.preflight();

		try {
			synchronized (this.objOutStream) {
				this.objOutStream.writeObject(message);
				this.objOutStream.flush();
				this.clientBuffOutputStream.flush();
				this.clientSocket.getOutputStream().flush();
			}
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

	}

	public synchronized void clientPerformSignOn() {
		// Assumes we have a connected and NOT-closed socket

		// Attempts writing the clientSON object

		if(this.clientSON instanceof clientSignOn) {
			
			if(this.debugFlag == true) {
				System.out.println("tClientTCP| Attempting to write clientSignOn object" + this.clientSON.toString());
			}
			if(this.clientSON == null) {
				if(this.debugFlag == true) {
					System.out.println("tClient| clientSignOn object not instantiated! Generating...");
				}
				
				if(this.clientSocket == null) {
					throw new IllegalStateException("tClient| clientSignOn object is null, as well as"
							+ " the clientSocket! Fatal.");
				}
				
				if(this.clientSocket != null) {
					this.generateClientSignOn(this.clientSocket.getLocalAddress(),
							this.clientSocket.getInetAddress());
				}
			}
			
			try {
				synchronized (this.objOutStream) {

					this.objOutStream.writeObject(clientSON);
					this.objOutStream.flush();
					this.clientBuffOutputStream.flush();
					this.clientSocket.getOutputStream().flush();
				}

			} catch (IOException e) {
				System.out.println("tClient| IOException!");
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		

	}

	// Transmits a clientSignOff object
	public synchronized void clientPerformSignOff() {
		// Assumes we have a connected and NOT-closed socket

		if (this.objOutStream != null && this.clientSOFF != null) {

			try {

				this.objOutStream.writeObject(clientSOFF);
				this.objOutStream.flush();
				this.clientBuffOutputStream.flush();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	// Closes streams, and the clientSocketRemote
	public void closeIO() {

		this.lastServMessage = null;
		if (this.clientBuffInputStream != null && this.clientSocket != null) {

			try {
				this.objInpStream.close();
				this.objOutStream.close();
				
				this.clientBuffOutputStream.close();
				this.clientBuffInputStream.close();
				
				this.clientSocket.shutdownInput();
				this.clientSocket.shutdownOutput();

				this.clientSocket.close();
			} catch (IOException e) {

				System.out.println("tClient| IOException occurred during closeIO. Not fatal!");

			}
		}
	}

	public void createInputStreams() {
		// Create a new BufferedInputStream from the inputStream generated via Socket
		// method
		try {
			this.inputStream = this.clientSocket.getInputStream();
			this.clientBuffInputStream = new BufferedInputStream(this.inputStream);
			this.objInpStream = new ObjectInputStream(this.clientBuffInputStream);

		} catch (EOFException e) {

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
			this.outputStream = this.clientSocket.getOutputStream();
			this.clientBuffOutputStream = new BufferedOutputStream(this.outputStream);
			this.objOutStream = new ObjectOutputStream(this.clientBuffOutputStream);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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

	// pre-run preflights
	public void preflight() {
		
		// Generate clientSignOn and clientSignOff objects if they do not exist
		if(this.clientSON == null && this.clientSOFF == null && this.clientSocket != null) {
			// Generate the clientSON and clientSOFF objects
			this.clientSON = this.generateClientSignOn(this.clientSocket.getLocalAddress(),
					this.clientSocket.getInetAddress());

			this.clientSOFF = this.generateClientSignOff(this.clientSocket.getLocalAddress(),
					this.clientSocket.getInetAddress());
		}
		
		// Condition where we have our local address and remote address
		if (this.socketLocalAddr != null && this.socketRemoteAddr != null) {

			// Extended condition where our current socket is null
			if (this.clientSocket == null) {

				this.clientSocket = new Socket();
				try {
					this.clientSocket.bind(socketLocalAddr);
					this.clientSocket.setReuseAddress(true);
					this.clientSocket.connect(socketRemoteAddr);
					
				} catch (IOException e) {
					System.out.println("tClient| Failed preflights!");
					e.printStackTrace();
				}
			}

			if (this.clientSocket != null) {

				if (this.clientSocket.isBound() && this.clientSocket.isConnected()) {
					if (this.objOutStream == null && this.objInpStream == null &&
							this.clientBuffOutputStream == null && this.clientBuffInputStream == null) {
						this.createOutputStreams();
						this.createInputStreams();
					}
				}

				if (!this.clientSocket.isBound() && !this.clientSocket.isConnected()) {
					try {
						this.clientSocket.bind(socketLocalAddr);
						this.clientSocket.setReuseAddress(true);
						this.clientSocket.connect(socketRemoteAddr);
					} catch (IOException e) {
						System.out.println("tClient| Failed preflights!");
						e.printStackTrace();
					}

					if (this.objOutStream == null && this.objInpStream == null &&
							this.clientBuffOutputStream == null && this.clientBuffInputStream == null) {
						this.createOutputStreams();
						this.createInputStreams();
					}
				}
			}
		}

		if (this.clientSocket.isBound() && !this.clientSocket.isConnected()) {
			// If we have a valid socket that just needs connected, try connecting!
			try {
				this.clientSocket.connect(socketRemoteAddr);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if (this.objOutStream == null && this.objInpStream == null &&
					this.clientBuffOutputStream == null && this.clientBuffInputStream == null) {
				this.createOutputStreams();
				this.createInputStreams();
			}
			
		}

	}

	// runs the thread
	@Override
	public void run() {

		// preflights
		this.preflight();

		// Perform clientSignOn
		// When clientSignOn is transmitted, assume that payloads are being transmitted.
		this.clientPerformSignOn();

		// Receive the TCP transmission

		while (this.stopFlag == false) {
			// Receive Payload after SignOn
			this.receiveMessages();
		}

	}

	public void stop() {

		this.stopFlag = true;
		this.clientPerformSignOff();
		this.closeIO();
	}

	public String getRemoteAddrString() {

		if (this.socketRemoteAddr != null) {

			return this.socketRemoteAddr.toString();

		} else {

			throw new IllegalStateException("tClient| No clientSocket set! Unable to return string! \n");
		}
	}

	public String getLocalAddrString() {
		if (this.socketRemoteAddr != null) {

			return this.socketLocalAddr.toString();

		} else {

			throw new IllegalStateException("tClient| No clientSocket set! Unable to return string! \n");
		}
	}

	// Returns the current payload
	// Throws an IllegalStateException if the IncomingPayload is currently null
	public ServerMessage<?> getLastServerMessage() {

		if (this.lastServMessage != null) {
			return this.lastServMessage;
		} else {

			throw new IllegalStateException("tClient| lastServMessage is null! \n");
		}
	}

	public void setParentTransponder(TransponderTCP parent) {
		this.parentTransponder = parent;
	}

	public void setDebugFlag(Boolean flag) {
		this.debugFlag = flag;
	}

	public void setDebugObj(debugObj debug) {
		this.debugObject = debug;
	}
	
	public void setCSonObject(clientSignOn csOn) {
		this.clientSON = csOn;
	}
	
	public void setCSoffObject(clientSignOff csOff) {
		this.clientSOFF = csOff;
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

		if (this.lastServMessage == null) {
			status += "ServerMessage not received! Currently: null \n";
		}

		if (this.lastClientMessage == null) {
			status += "ClientMessage not received! Currently: null \n";
		}

		if (this.lastServMessage instanceof Payload) {
			status += "Payload received. Last payload:\n";
			status += this.lastServMessage.toString() + "\n";
		}

		if (this.lastServMessage instanceof ServerMessage<?>) {
			status += "ServerMessage received. Last ServerMessage:\n";
			status += this.lastServMessage.toString() + "\n";
		}
		if (this.lastClientMessage instanceof ClientMessage<?>) {
			status += "ClientMessage received. Last ClientMessage:\n";
			status += this.lastClientMessage.toString() + "\n";
		}

		return status;
	}
}
