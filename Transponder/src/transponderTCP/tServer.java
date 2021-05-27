package transponderTCP;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
// Transponder-Server opens a server socket and transmits either the default payload 
// (If set), or keeps the channel open for on-demand transmissions from the 
// server-controller (Yet to be written)
// TODO: Write server controller
import java.net.SocketException;

public class tServer implements Runnable {

	private ServerSocket ServerSocketTCP = null;
	private SocketAddress localAddrTCP = null;
	private Socket remoteSocketTCP = null;
	private OutputStream outputStream = null;
	private ObjectOutputStream objOutputStream = null;
	private Payload outgoingPayload = null;
	private debugObj debugObject = null;
	private boolean stopFlag = false;
	private boolean debugFlag = false;

	// Creates a tServer instance with a serverSocket
	// and a SocketAddress. Both are assumed and intended to be used
	// as a local ServerSocket with a local address.

	public tServer(ServerSocket serverSocket, SocketAddress localAddr) {
		this.localAddrTCP = localAddr;
		this.ServerSocketTCP = serverSocket;
	}

	public tServer(ServerSocket serverSocket, Socket clientSocket) {
		this.ServerSocketTCP = serverSocket;
		this.localAddrTCP = this.ServerSocketTCP.getLocalSocketAddress();
		this.remoteSocketTCP = clientSocket;
	}

	// Deprecated: We listen() in the TransponderTCP class now.
	// listen() checks for pre-binding and checks/binds the
	// local TCP address (localAddrTCP), assuming localAddrTCP is set.
	// listen() then accepts TCP connections on the assigned port
	// and assigns the connection to a socket (remoteSocketTCP)
//	public void listen() {
//		try {
//			if (!this.ServerSocketTCP.isBound()) {
//
//				// Debug output for when debugFlag is set to TRUE
//				if (this.debugFlag == true) {
//					System.out.println("tServer not bound! Binding to:\n " + localAddrTCP.toString());
//				}
//				this.ServerSocketTCP.bind(localAddrTCP);
//			}
//			this.remoteSocketTCP = ServerSocketTCP.accept();
//
//			// Debug output for when debugFlag is set to TRUE
//			if (this.debugFlag == true) {
//				System.out.println("tServer accepted connection from:\n"
//						+ this.remoteSocketTCP.getRemoteSocketAddress().toString());
//				System.out.println("At local address: \n" + this.remoteSocketTCP.getLocalSocketAddress().toString());
//				System.out.println("tServer transmitting the following payload:\n");
//				System.out.println("- - - - - - - - - -");
//				System.out.println(this.outgoingPayload.toString());
//				System.out.println("- - - - - - - - - -");
//			}
//
//			this.transmitPayload(outgoingPayload);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//	}

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

	// setOutgoingPayload assigns an outgoing payload object to this
	// tServer's outgoingPayload field.
	public void setOutgoingPayload(Payload payload) {
		// debug output for when debugFlag set to TRUE
		if (this.debugFlag == true) {
			System.out.println("tServer setting outgoingPayload to" + this.outgoingPayload.toString());
		}

		this.outgoingPayload = payload;
	}

	// transmitPayload checks for the presence of an outgoingPayload,
	// then creates an outputStream, as well as an associated object output stream
	// (objOutputStream) and writes the object to the output
	public void transmitPayload(Payload payload) {
		

		
		if (this.outgoingPayload == null) {
			throw new IllegalArgumentException("tServer payload not set!");
		}
		try {
			if (this.outputStream == null) {
				this.outputStream = this.remoteSocketTCP.getOutputStream();
				
			}
			if (this.objOutputStream == null) {
				this.objOutputStream = new ObjectOutputStream(this.outputStream);
			}
			
			
			// debug output for when debugFlag set to TRUE
			if (this.debugFlag == true) {
				System.out.println("tServer| objOutputStream created. Writing payload to objOutputStream.");
				System.out.println("tServer| Writing Object: \n" + this.outgoingPayload.toString());
			}
			
			if(this.debugFlag == true) {
				System.out.println("tServer| ServerSocket and Stream Status:");

				if(this.objOutputStream == null) {
					System.out.println("objOutputStream is null!");
				} else {
					System.out.println(this.objOutputStream.toString());
				}

				if(this.outputStream == null) {
					System.out.println("outputStream is null!");
				} else {
					System.out.println(this.outputStream.toString());					
				}
				
				if(this.remoteSocketTCP == null) {
					System.out.println("remoteSocketTCP is null!");
				} else {
					System.out.println(this.remoteSocketTCP.toString());					
				}
			}

			if(this.remoteSocketTCP.isConnected() == true){
				this.objOutputStream.writeObject(payload);
				this.objOutputStream.flush();
			}
			

		} catch (SocketException e) {
			if (this.debugFlag == true) {
				System.out.println("tServer| Socket Exception occurred!");
				System.out.println("Setting stopFlag to TRUE!");
				this.stopFlag = true;
			}
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}

	// run method for thread execution
	@Override
	public void run() {
		while (this.stopFlag == false) {
			// Debug flag check
			if (this.debugFlag == true) {
				if (this.debugObject != null) {
					this.debugPayloadIntegrity();
					System.out.println("tServer debugPayloadIntegrity ran...");
					System.out.println("tServer Transmitting Payload...");
				}
			}
			this.transmitPayload(this.outgoingPayload);
		}
	}


	public void stop() {

		// debug output for when debugFlag set to TRUE
		if (this.debugFlag == true) {
			System.out.println("tServer stop method called!");
		}

		this.stopFlag = true;
	}

	public void setDebugFlag(Boolean flag) {
		this.debugFlag = flag;
	}

	public void setDebugObj(debugObj debug) {
		this.debugObject = debug;
	}

	public void debugPayloadIntegrity() {
		System.out.println("tServer running debugPayloadIntegrity.");
		System.out.println("tServer will report it's payload to debugObj.");
		if (this.debugFlag == true) {
			if (this.outgoingPayload == null) {
				System.out.println("tServer outgoingPayload not set!");
			}
		}

		if (this.outgoingPayload != null) {
			this.debugObject.setInputPayload(outgoingPayload);
		}

	}

	public String getLocalAddr() {
		if (this.localAddrTCP == null) {
			return "localAddrTCP not set!";
		}
		return this.localAddrTCP.toString();
	}

	public String getRemoteAddr() {
		if (this.remoteSocketTCP == null) {
			return "remoteSocketTCP not set!";
		}
		return this.remoteSocketTCP.getRemoteSocketAddress().toString();
	}

	public String getStatus() {
		String status = "";
		status += "Connection Status: \n";

		status += "Local Address settings: \n";
		if (this.localAddrTCP == null) {
			status += "Local Address set to null!\n";
		} else {
			status += "Local Address set to:\n";
			status += "TCP: " + this.localAddrTCP.toString();
		}

		status += "Remote Address settings: \n";
		if (this.remoteSocketTCP == null) {
			status += "No remote socket set! \n";
		} else {
			status += "Remote Address set to:\n";
			status += "TCP: " + this.remoteSocketTCP.getRemoteSocketAddress().toString();
		}

		status += "Payload status: \n";
		if (this.isPayloadPresent() == false) {
			status += "Payload is not present!\n";
		} else {
			status += "Payload loaded. Current payload:\n";
			status += this.outgoingPayload.toString();
		}
		return status;
	}
}
