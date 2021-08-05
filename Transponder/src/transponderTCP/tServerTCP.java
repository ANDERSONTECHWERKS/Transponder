package transponderTCP;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

//Transponder-Server opens a server socket and transmits either the default payload 
//(If set), or keeps the channel open for on-demand transmissions from the 
//server-controller (TransponderTCP)

//TODO: Write IOstreams for inputStreams to accept clientSignOn, and consider 
// how the handshake for clientSignOn/clientSignOff should operate.

public class tServerTCP implements Runnable {

	private Socket remoteSocketTCP = null;

	private OutputStream outputStream = null;
	private InputStream inputStream = null;

	private BufferedInputStream serverBuffInputStream = null;
	private BufferedOutputStream serverBuffOutStream = null;

	private ObjectOutputStream objOutputStream = null;
	private ObjectInputStream objInputStream = null;

	private ServerMessage<?> currServMessage = null;

	private debugObj debugObject = null;

	private boolean stopFlag = false;
	private boolean debugFlag = false;

	private TransponderTCP parentTransponder = null;

	public tServerTCP(Socket serverSocket) {
		this.remoteSocketTCP = serverSocket;
	}

	public tServerTCP(Socket serverSocket, TransponderTCP parent) {
		this.remoteSocketTCP = serverSocket;
		this.parentTransponder = parent;
	}

	// isPayloadPresent returns a boolean TRUE if the currServMessage
	// field is populated with a Payload object, false if not.
	public boolean isServerMessagePresent() {

		if (this.currServMessage != null && this.currServMessage instanceof ServerMessage<?>) {
			return true;

		} else {

			// debug output for when debugFlag set to TRUE
			if (this.debugFlag == true) {
				System.out.println("tServer| ServerMessage not present or malformed!");
			}
			return false;
		}
	}

	// serviceStart() is intended to be put into a loop
	// listen for messages from client-side, process them according to the message
	// object.

	public synchronized void serviceStart() {

		try {

			Object input = this.objInputStream.readObject();

			// Begin writing service behaviors here.
			// Default behavior is to send a single payload
			// to a client

			if (input instanceof ClientMessage<?>) {

				ClientMessage<?> inpMessage = (ClientMessage<?>) input;

				if (this.debugFlag == true) {
					System.out.println("tServer| Received ClientMessage class!");
					System.out.println("tServer| ClientMessage received in serviceStart() method!");
					System.out.println("tServer| Message reads: \n" + inpMessage.toString());
				}

				synchronized (this.parentTransponder.getMasterCliMsg()) {
					// add message to the master list
					this.parentTransponder.getMasterCliMsg().put(inpMessage);

					// New message has been collected, setting parentTransponder.newClientMessage
					// flag to true
					this.parentTransponder.setNewClientMessageFlag(true);
				}
			}

			if (input instanceof ServerMessage<?>) {

				ServerMessage<?> inpMessage = (ServerMessage<?>) input;

				if (this.debugFlag == true) {
					System.out.println("tServer| Received ServerMessage class!");
					System.out.println("tServer| ServerMessage received in serviceStart() method!");
					System.out.println("tServer| Message reads: \n" + inpMessage.toString());
				}

				synchronized (this.parentTransponder.getMasterServMsg()) {
					// add this to the list of messages we recieved
					this.parentTransponder.getMasterServMsg().put(inpMessage);
					// New message has been collected, setting parentTransponder.newServerMessage
					// flag to true
					this.parentTransponder.setNewServerMessageFlag(true);
				}

			}

			if (input instanceof clientSignOn) {

				clientSignOn inpCliSignOn = (clientSignOn) input;

				if (this.debugFlag == true) {
					System.out.println("tServer| Received clientSignOn object!\n");
					System.out.println("tServer| inpClientSignOn object received in serviceStart() method!\n");
				}

				this.processSignOn(inpCliSignOn);

				// Initial Transmit: Send initial payload transmission. This syncs states.
				this.sendServerMessage(this.currServMessage);

			}

			// TODO: If we receive a clientSignOff object - gracefully close the connection!
			if (input instanceof clientSignOff) {
				clientSignOff inpCliSignOff = (clientSignOff) input;

				if (this.debugFlag == true) {
					System.out.println("tServer| Received clientSignOff class! Closing sockets! \n");
					System.out.println("tServer| clientSignOff object received in serviceStart() method!\n");
				}

				this.processSignOff(inpCliSignOff);

				// Once we receive clientSignOff object, stop this tServer.
				// We should still be listening at the TransponderTCP-level.
				this.stopFlag = true;
				this.parentTransponder.serverProcessSignOff(inpCliSignOff);
			}

		} catch (ClassNotFoundException e) {
			System.out.println("tServer| Class not found! Shutting down!");
			this.stop();
			e.printStackTrace();

		} catch (IOException e) {
			System.out.println("tServer| IO Exception! Shutting down!");
			this.stop();
			e.printStackTrace();

		}
	}

	public void processSignOn(clientSignOn csOn) {

		// debug output for when debugFlag set to TRUE
		if (this.debugFlag == true) {
			System.out.println("tServer| clientSignOn received!\nclientSignOn Object ID:" + csOn.toString() + "\n");
		}

		// Hand the clientSignOn object to the parent transponder
		this.parentTransponder.serverProcessSignOn(csOn);

	}

	public void processSignOff(clientSignOff csOff) {

		// debug output for when debugFlag set to TRUE
		if (this.debugFlag == true) {
			System.out.println("tServer| clientSignOff received!\nclientSignOff Object ID:" + csOff.toString() + "\n");
		}

		// Hand the clientSignOn object to the parent transponder
		this.parentTransponder.serverProcessSignOff(csOff);

	}

	// setOutgoingPayload assigns an outgoing payload object to this
	// tServer's currServMessage field.
	public void setServerMessage(ServerMessage<?> servMessage) {

		// debug output for when debugFlag set to TRUE
		if (this.debugFlag == true) {
			System.out.println("tServer setting currServMessage to" + this.currServMessage.toString() + "\n");
		}

		this.currServMessage = servMessage;
	}

	public void setParentTransponder(TransponderTCP parent) {
		this.parentTransponder = parent;
	}

	public void createInputStreams() {
		// Create inputStreams
		if (this.objInputStream == null) {

			try {

				this.inputStream = this.remoteSocketTCP.getInputStream();
				this.serverBuffInputStream = new BufferedInputStream(inputStream);
				this.objInputStream = new ObjectInputStream(serverBuffInputStream);

				if (this.debugFlag == true) {
					System.out.println("tServer| createInputStreams successful!");
				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("tServer| inputStream creation failed!");
				e.printStackTrace();
			}
		}

	}

	public void createOutputStreams() {

		// The order of the inputStream / outputStream creation matters between client
		// and server!
		// Idea: Establish connection, make client send a 'clientSignOn' object,
		// then the server opens the output stream and transmits the payload until a
		// clientSignOff has been received.

		// If we currently have no outputStream, create one by calling getOutputStream()
		// on the remote socket
		if (this.objOutputStream == null) {

			try {

				this.outputStream = this.remoteSocketTCP.getOutputStream();
				this.serverBuffOutStream = new BufferedOutputStream(this.outputStream);
				this.objOutputStream = new ObjectOutputStream(this.serverBuffOutStream);

				// debug output
				if (this.debugFlag == true) {
					System.out.println("tServer| outputStreams created successfully!");

				}

			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("tServer| outputStreams creation failed!");
				e.printStackTrace();
			}

		}

	}

	// transmitPayload checks for the presence of an currServMessage,
	// then creates an outputStream, as well as an associated object output stream
	// (objOutputStream) and writes the object to the output
	public void sendServerMessage(ServerMessage<?> servMessage) {

		if (servMessage == null) {
			throw new IllegalArgumentException("tServer| servMessage is null!");
		}

		try {

			// write the object, allow the address to be reused, and close the streams
			if (this.remoteSocketTCP.isConnected() == true && !this.remoteSocketTCP.isClosed()) {

				if (this.stopFlag == false) {

					// debug output for when debugFlag set to TRUE
					if (this.debugFlag == true) {
						System.out.println("tServer| Writing Object: \n" + servMessage.toString());
					}

					this.preflight();
					
					// Transmit the object via ObjOutputStream!
					this.objOutputStream.writeObject(servMessage);
					this.objOutputStream.flush();
					this.serverBuffOutStream.flush();
				}
			}

		} catch (SocketException e) {

			if (this.debugFlag == true) {
				System.out.println("tServer| Socket Exception occurred!");
				this.stopFlag = true;
			}

			e.printStackTrace();

		} catch (IOException e) {

			System.out.println("tServer| IO Exception occurred!");
			e.printStackTrace();

		}
	}

	public void sendClientMessage(ClientMessage<?> message) {
		try {

			if (this.remoteSocketTCP.isConnected() == true && !this.remoteSocketTCP.isClosed()) {

				if (this.stopFlag == false) {

					// debug output for when debugFlag set to TRUE
					if (this.debugFlag == true) {
						System.out.println("tServer| Writing Object: \n" + message.toString());
					}

					// Transmit the object via ObjOutputStream!
					this.objOutputStream.writeObject(message);
					this.objOutputStream.flush();
					this.serverBuffOutStream.flush();
				}
			}

		} catch (SocketException e) {
			System.out.println("tServer| Socket Exception occurred!");
			e.printStackTrace();
			
		} catch (IOException e) {
			System.out.println("tServer| IO Exception occurred!");
			e.printStackTrace();
		}
	}

	public void setServerOpts(String args[]) {
		// For the moment, args[] does nothing.
		// We will use this block to set options as we
		// develop and troubleshoot

		try {
			this.remoteSocketTCP.setSoLinger(true, 0);
			this.remoteSocketTCP.setKeepAlive(false);
			this.remoteSocketTCP.setReuseAddress(true);

		} catch (SocketException e) {
			System.out.println("tServer| SocketException occured! Unable to setServerOpts()!");
			e.printStackTrace();
		}

	}

	// Homebrew object filter, for the moment.
	// Checks if the input object is an instance of anything we care about
	// TODO: Future - Perhaps use this space for hashchecking?
	// TODO: Implement Security Manager stuff here

	public boolean checkMessageIntegrity(Object o) {

		if (o instanceof ServerMessage<?>) {
			ServerMessage<?> inpPayload = (ServerMessage<?>) o;
			return true;
		}

		if (o instanceof ClientMessage<?>) {
			ClientMessage<?> inpPayload = (ClientMessage<?>) o;
			return true;
		}

		if (o instanceof ChatMessage) {
			ChatMessage inpPayload = (ChatMessage) o;
			return true;
		}

		if (o instanceof clientSignOn) {
			clientSignOn inpCliSignOn = (clientSignOn) o;
			return true;
		}

		if (o instanceof clientSignOff) {
			clientSignOff inpCliSignOff = (clientSignOff) o;
			return true;
		}

		return false;
	}

	// Shuts down the IOstreams on the socket to the remote client
	public void closeIO() {

		try {
			this.remoteSocketTCP.shutdownOutput();
			this.remoteSocketTCP.shutdownInput();

			this.objOutputStream.close();
			this.objInputStream.close();

			this.serverBuffOutStream.close();
			this.serverBuffInputStream.close();

			this.remoteSocketTCP.close();

		} catch (IOException e) {
			System.out.println("tServer| closeIO failed!");
			e.printStackTrace();
		}

	}

	public void preflight() {
		// Keep in this block until we know where we want it to go
		// TL;DR: If the debugFlag is true, we will output any/all relevant
		// socket and stream information before we transmit
		
		if(this.remoteSocketTCP != null) {
			if(!this.remoteSocketTCP.isBound()) {
				throw new IllegalStateException("tServer| Remote socket is unbound!");
			}
			
			if(this.remoteSocketTCP.isClosed()) {
				throw new IllegalStateException("tServer| Remote socket is closed!");
			}
			
			if(this.remoteSocketTCP.isConnected()) {
				if(this.objInputStream == null && this.objOutputStream == null) {
					
					if(this.objOutputStream == null && this.objInputStream == null && this.serverBuffOutStream == null
							&& this.serverBuffOutStream == null &&
							this.inputStream == null && this.outputStream == null) {
						
						this.createOutputStreams();
						this.createInputStreams();
					}
				}
			}
		}
		
		if (this.debugFlag == true) {
			System.out.println("tServer| ServerSocket and Stream Status:");

			if (this.objOutputStream == null) {
				System.out.println("tServer| objOutputStream is null!");

			} else {
				System.out.println("tServer| ObjOutputStream ID: " + this.objOutputStream.toString());
			}

			if (this.outputStream == null) {
				System.out.println("tServer| outputStream is null!");

			} else {
				System.out.println("tServer| outputStream ID:" + this.outputStream.toString());
			}

			if (this.remoteSocketTCP == null) {
				System.out.println("tServer| remoteSocketTCP is null!");

			} else {
				System.out.println("tServer| remoteSocketTCP ID:" + this.remoteSocketTCP.toString());
			}

			if (this.debugObject != null && this.debugObject instanceof debugObj) {
				this.debugPayloadIntegrity();
				System.out.println("tServer| debugPayloadIntegrity ran...");
				System.out.println("tServer| Transmitting Payload...");
			}
			
			

			System.out.println("tServer| Serving Client connected from: "
					+ this.remoteSocketTCP.getRemoteSocketAddress().toString());
		}
	}

	// run method for thread execution
	@Override
	public void run() {

		// Set the server options for the newly-created socket
		// setServerOpts is where we will pass misc. options in the future!
		// this.setServerOpts(null);

		// pre-flights, includes IOStream creation
		this.preflight();

		// Listen and begin service

		// While stopFlag = false, we are going to listen for further messages and
		// handle them
		// accordingly.

		while (stopFlag == false) {
			serviceStart();
		}

		// After we close from the listening loop - close our IO and finish the run
		// method.
		this.closeIO();
	}

	public void stop() {

		// debug output for when debugFlag set to TRUE
		if (this.debugFlag == true) {
			System.out.println("tServer| Stop method called!");
		}

		this.stopFlag = true;

		this.closeIO();
	}

	public void setDebugFlag(Boolean flag) {
		this.debugFlag = flag;
	}

	public void setDebugObj(debugObj debug) {
		this.debugObject = debug;
	}

	public void debugPayloadIntegrity() {
		System.out.println("tServer| Running debugPayloadIntegrity!");
		System.out.println("tServer| Will report payload to debugObj!");

		if (this.debugFlag == true) {
			if (this.currServMessage == null) {
				System.out.println("tServer| currServMessage not set!");
			}
		}

		if (this.currServMessage != null) {
			this.debugObject.setInpServerMessage(currServMessage);
		}

	}

	public String getLocalAddr() {

		if (this.remoteSocketTCP == null) {
			throw new IllegalStateException("tServer| remoteSocketTCP not set! Cannot provide address!");
		}

		return this.remoteSocketTCP.getLocalAddress().toString();
	}

	public int getLocalPort() {

		if (this.remoteSocketTCP == null) {
			throw new IllegalStateException("tServer| remoteSocketTCP not set! Cannot provide port!");
		}

		return this.remoteSocketTCP.getPort();
	}

	public int getServMessageHash() {

		if (this.currServMessage != null) {
			return this.currServMessage.hashCode();
		} else {
			throw new IllegalStateException("tServer| currServMessage is null!");
		}
	}

	public String getRemoteAddr() {

		if (this.remoteSocketTCP == null) {
			return "remoteSocketTCP not set!";
		}
		return this.remoteSocketTCP.getRemoteSocketAddress().toString();
	}

	public String getStatus() {
		String status = "";

		status += "tServer| Connection Status: \n";

		status += "---Local Address settings--- \n";

		if (this.remoteSocketTCP == null) {
			status += "tServer| remoteSocketTCP set to null!\n";
		} else {
			status += "tServer| Local Address set to:\n";
			status += "tServer| TCP: " + this.remoteSocketTCP.getLocalSocketAddress().toString() + "\n";
			status += "tServer| Is socket bound? : " + this.remoteSocketTCP.isBound() + "\n";
			status += "tServer| Is socket closed? : " + this.remoteSocketTCP.isClosed() + "\n";
			status += "Remote Address set to:\n";
			status += "TCP: " + this.remoteSocketTCP.getRemoteSocketAddress().toString();
			status += "Is remote socket connected? : " + this.remoteSocketTCP.isConnected() + "\n";
		}

		status += "---ServerMessage status--- \n";

		if (this.isServerMessagePresent() == false) {
			status += "ServerMessage is not present!\n";
		} else {
			status += "ServerMessage loaded. Current ServerMessage:\n";
			status += this.currServMessage.toString();
		}

		return status;
	}

	public ServerMessage<?> getCurrServerMessage() {
		return this.currServMessage;
	}
}
