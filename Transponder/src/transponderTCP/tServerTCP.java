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
import java.util.concurrent.TimeUnit;

//Transponder-Server opens a server socket and transmits either the default payload 
//(If set), or keeps the channel open for on-demand transmissions from the 
//server-controller (TransponderTCP)

//TODO: Write IOstreams for inputStreams to accept clientSignOn, and consider 
// how the handshake for clientSignOn/clientSignOff should operate.

public class tServerTCP implements Runnable {

	private ServerSocket ServerSocketTCP = null;
	private SocketAddress localAddrTCP = null;
	private Socket remoteSocketTCP = null;
	private OutputStream outputStream = null;
	private InputStream inputStream = null;
	private BufferedInputStream serverBuffInputStream = null;
	private BufferedOutputStream serverBuffOutStream = null;
	private ObjectOutputStream objOutputStream = null;
	private ObjectInputStream objInputStream = null;
	private Payload outgoingPayload = null;
	private debugObj debugObject = null;
	private boolean stopFlag = false;
	private boolean debugFlag = false;
	private TransponderTCP parentTransponder = null;
	private int payloadHash;

	// tServer instance without parent reference.
	// ONLY USE FOR TROUBLESHOOTING/DEBUG!
	public tServerTCP(ServerSocket serverSocket, SocketAddress localAddr) {
		this.localAddrTCP = localAddr;
		this.ServerSocketTCP = serverSocket;
	}

	// Creates a tServer instance with a serverSocket
	// and a SocketAddress. Both are assumed and intended to be used
	// as a local ServerSocket with a local address.
	// Includes reference to parent transponderTCP object for callbacks!

	public tServerTCP(ServerSocket serverSocket, SocketAddress localAddr, TransponderTCP parent) {
		this.parentTransponder = parent;
		this.localAddrTCP = localAddr;
		this.ServerSocketTCP = serverSocket;
	}

	// Creates a tServer instance with the given serverSocket and clientSockets
	// already
	// instantiated
	// Includes reference to parent transponderTCP object for callbacks!

	public tServerTCP(ServerSocket serverSocket, Socket clientSocket, TransponderTCP parent) {
		this.parentTransponder = parent;
		this.ServerSocketTCP = serverSocket;
		this.localAddrTCP = this.ServerSocketTCP.getLocalSocketAddress();
		this.remoteSocketTCP = clientSocket;
	}

	// isPayloadPresent returns a boolean TRUE if the outgoingPayload
	// field is populated with a Payload object, false if not.
	public boolean isPayloadPresent() {
		if (this.outgoingPayload != null && this.outgoingPayload instanceof Payload) {
			return true;
		} else {
			// debug output for when debugFlag set to TRUE
			if (this.debugFlag == true) {
				System.out.println("tServer payload not present or malformed!");
			}
			return false;
		}
	}

	// serviceStart() is intended to be put into a loop
	// listen for messages from client-side, process them according to the message object.

	public void serviceStart() {

		try {
			
			Object input = this.objInputStream.readObject();

			// Run object through checkMesageIntegrity AKA: SecurityManager
			if(this.checkMessageIntegrity(input)) {
	
				// Begin writing service behaviors here.
				// Default behavior is to send a single payload
				// to a client 
				
				// Typically wouldn't see a Payload returned to the server, but we'll write this just to handle
				// the case since it's...maybe possible...idk.
				if(input instanceof Payload) {

					Payload inpPayload = (Payload)input;

					if (this.debugFlag == true) {
						System.out.println("tServer| Received Payload class!");
						System.out.println("tServer| Payload received in serviceStart() method!");

					}
				}

				// TODO: If we receive another cSignOn object after the initial connection...
				// Do...something? Think about this.

				if(input instanceof clientSignOn) {
					clientSignOn inpCliSignOn = (clientSignOn) input;

					if (this.debugFlag == true) {
						System.out.println("tServer| Received clientSignOn object!\n");
						System.out.println("tServer| inpClientSignOn object received in serviceStart() method!\n");

					}
				}
				
				// TODO: If we receive a clientSignOff object - gracefully close the connection!
				if(input instanceof clientSignOff) {
					clientSignOff inpCliSignOff = (clientSignOff) input;
					
					if (this.debugFlag == true) {
						System.out.println("tServer| Received clientSignOff class! Closing sockets! \n");
						System.out.println("tServer| clientSignOff object received in serviceStart() method!\n");

					}
					
					this.stopFlag = true;
				}
			}
			
		} catch (ClassNotFoundException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void processSignOn() {
		
		if (this.objInputStream != null) {
			try {

				Object signOnObject = this.objInputStream.readObject();

				if (signOnObject instanceof clientSignOn) {

					// debug output for when debugFlag set to TRUE
					if (this.debugFlag == true) {
						System.out
								.println("tServer| signOnObject received!\nclientSignOn Object ID:" + signOnObject.toString() + "\n");
					}
				}

			} catch (ClassNotFoundException | IOException e) {
				System.out.println("tServer| signOnObject error!\n");
				e.printStackTrace();
			}
		}
	}

	// setOutgoingPayload assigns an outgoing payload object to this
	// tServer's outgoingPayload field.
	public void setOutgoingPayload(Payload payload) {
		// debug output for when debugFlag set to TRUE
		if (this.debugFlag == true) {
			System.out.println("tServer setting outgoingPayload to" + this.outgoingPayload.toString() + "\n");
		}

		this.outgoingPayload = payload;
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
				this.objOutputStream.flush();

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

	// transmitPayload checks for the presence of an outgoingPayload,
	// then creates an outputStream, as well as an associated object output stream
	// (objOutputStream) and writes the object to the output
	public void transmitPayload(Payload payload) {

		if (this.outgoingPayload == null) {
			throw new IllegalArgumentException("tServer| Payload not set!");
		}

		try {

			// Keep in this block until we know where we want it to go
			// TL;DR: If the debugFlag is true, we will output any/all relevant
			// socket and stream information before we transmit
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
			}

			// write the object, allow the address to be reused, and close the streams
			if (this.remoteSocketTCP.isConnected() == true && !this.remoteSocketTCP.isClosed()) {

				// TODO: EXPERIMENTAL: Using "if" check to make a single transmission

				if (this.stopFlag == false) {

					// debug output for when debugFlag set to TRUE
					if (this.debugFlag == true) {
						System.out.println("tServer| Writing Object: \n" + this.outgoingPayload.toString());
					}

					// Transmit the object via ObjOutputStream!
					this.objOutputStream.reset();
					this.objOutputStream.writeObject(payload);
					this.objOutputStream.flush();
				}
			}

		} catch (SocketException e) {

			if (this.debugFlag == true) {
				System.out.println("tServer| Socket Exception occurred!");
//				System.out.println("Setting stopFlag to TRUE!");
				this.stopFlag = true;
			}

			e.printStackTrace();

		} catch (IOException e) {

			System.out.println("tServer| IO Exception occurred!");
			System.out.println("Setting stopFlag to TRUE!");
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
			this.ServerSocketTCP.setReuseAddress(true);

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
		
		if(o instanceof Payload) {
			Payload inpPayload = (Payload)o;
			return true;
		}
		
		if(o instanceof clientSignOn) {
			clientSignOn inpCliSignOn = (clientSignOn) o;
			return true;
		}
		
		if(o instanceof clientSignOff) {
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

			this.ServerSocketTCP.close();

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

	// run method for thread execution
	@Override
	public void run() {
		
		// Debugging operations
		if (this.debugFlag == true) {
			if (this.debugObject != null && this.debugObject instanceof debugObj) {
				this.debugPayloadIntegrity();
				System.out.println("tServer| debugPayloadIntegrity ran...");
				System.out.println("tServer| Transmitting Payload...");
			}
		}

		// Here, we wait for a Socket connection
		try {
			this.remoteSocketTCP = this.ServerSocketTCP.accept();

			if (this.debugFlag == true) {
				System.out.println(
						"tServer| Client connected from: " + this.remoteSocketTCP.getRemoteSocketAddress().toString());
			}

		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		// Set the server options for the newly-created socket
		// setServerOpts is where we will pass misc. options in the future!
		this.setServerOpts(null);

		// Handshake_1: Create InputStreams
		this.createInputStreams();

		// Handshake_2: Create Output streams
		this.createOutputStreams();

		// Handshake_3: Listen for clientSignOn object
		this.processSignOn();

		// Initial Transmit: Send initial payload transmission
		this.transmitPayload(this.outgoingPayload);

		// Listen and begin service

		// While stopFlag = false, we are going to listen for further messages and handle them 
		// accordingly.

		while(stopFlag == false) {
			serviceStart();
		}
		
		// After we close from the listening loop - close our IO and finish the run method.
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
			if (this.outgoingPayload == null) {
				System.out.println("tServer| outgoingPayload not set!");
			}
		}

		if (this.outgoingPayload != null) {
			this.debugObject.setInputPayload(outgoingPayload);
		}

	}

	public String getLocalAddr() {
		
		if (this.localAddrTCP == null) {
			return "tServer| localAddrTCP not set!";
		}
		
		return this.localAddrTCP.toString();
	}

	public int getPayloadHash() {
		
		if (this.outgoingPayload != null) {
			return this.outgoingPayload.hashCode();
		} else {
			throw new IllegalStateException("tServer| outgoingPayload is null!");
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
		
		if (this.localAddrTCP == null) {
			status += "Local Address set to null!\n";
		} else {
			status += "Local Address set to:\n";
			status += "TCP: " + this.localAddrTCP.toString() + "\n";
			status += "Is socket bound? : " + this.ServerSocketTCP.isBound() + "\n";
			status += "Is socket closed? : " + this.ServerSocketTCP.isClosed() + "\n";
		}

		status += "---Remote Address settings--- \n";

		if (this.remoteSocketTCP == null) {
			status += "No remote socket set! \n";
		} else {
			status += "Remote Address set to:\n";
			status += "TCP: " + this.remoteSocketTCP.getRemoteSocketAddress().toString();
			status += "Is remote socket bound? : " + this.remoteSocketTCP.isBound() + "\n";
			status += "Is remote socket closed? : " + this.remoteSocketTCP.isClosed() + "\n";
			status += "Is remote socket connected? : " + this.remoteSocketTCP.isConnected() + "\n";
		}

		status += "---Payload status--- \n";

		if (this.isPayloadPresent() == false) {
			status += "Payload is not present!\n";
		} else {
			status += "Payload loaded. Current payload:\n";
			status += this.outgoingPayload.toString();
		}

		return status;
	}
	
}
