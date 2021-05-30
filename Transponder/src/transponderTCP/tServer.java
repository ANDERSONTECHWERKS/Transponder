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
import java.util.concurrent.TimeUnit;

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

	// Creates a tServer instance with the given serverSocket and clientSockets already
	// instantiated
	public tServer(ServerSocket serverSocket, Socket clientSocket) {
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

	// setOutgoingPayload assigns an outgoing payload object to this
	// tServer's outgoingPayload field.
	public void setOutgoingPayload(Payload payload) {
		// debug output for when debugFlag set to TRUE
		if (this.debugFlag == true) {
			System.out.println("tServer setting outgoingPayload to" + this.outgoingPayload.toString());
		}

		this.outgoingPayload = payload;
	}
	
	public void createStreams() {
		
		// If we currently have no outputStream, create one by calling getOutputStream() 
		// on the remote socket
		if (this.outputStream == null) {
			try {
				this.outputStream = this.remoteSocketTCP.getOutputStream();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		// If we currently have no ObjectOutputStream, create one by using outputStream as the
		// argument for the ObjectOutputStream constructor

		if (this.objOutputStream == null) {
			try {
				this.objOutputStream = new ObjectOutputStream(this.outputStream);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	// transmitPayload checks for the presence of an outgoingPayload,
	// then creates an outputStream, as well as an associated object output stream
	// (objOutputStream) and writes the object to the output
	public void transmitPayload(Payload payload) {
		
		if (this.outgoingPayload == null) {
			throw new IllegalArgumentException("tServer payload not set!");
		}
		try {
			
			// debug output for when debugFlag set to TRUE
			if (this.debugFlag == true) {
				System.out.println("tServer| objOutputStream created. Writing payload to objOutputStream.");
				System.out.println("tServer| Writing Object: \n" + this.outgoingPayload.toString());
			}
			
			// debug output (again)
			// Keep in this block until we know where we want it to go
			// TL;DR: If the debugFlag is true, we will output any/all relevant 
			// socket and stream information before we transmit
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

			this.setServerOpts(null);
			
			// write the object, allow the address to be reused, and close the streams
			if(this.remoteSocketTCP.isConnected() == true && !this.remoteSocketTCP.isClosed()){
				this.objOutputStream.writeObject(payload);

			}
			
			this.cleanupServerConnection();
			

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
			this.ServerSocketTCP.setReuseAddress(true);
		} catch (SocketException e) {
			System.out.println("tServer| SocketException occured! Unable to setServerOpts()!");
			e.printStackTrace();
		}

	}

	public void cleanupServerConnection() {
		
		try {
			this.objOutputStream.close();
			this.outputStream.close();
			this.remoteSocketTCP.close();
		} catch (IOException e) {
			System.out.println("tServer| cleanupServerConnection failed!");
			e.printStackTrace();
		}

	}

	// run method for thread execution
	@Override
	public void run() {
			// Debug flag check
			if (this.debugFlag == true) {
				if (this.debugObject != null) {
					this.debugPayloadIntegrity();
					System.out.println("tServer debugPayloadIntegrity ran...");
					System.out.println("tServer Transmitting Payload...");
				}
			}

			
			// Here, we wait for a Socket connection
			try {
				this.remoteSocketTCP = this.ServerSocketTCP.accept();
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			
			// Create streams, assuming we have been passed a valid ServerSocket object
			this.createStreams();
			
			
			// Assuming we now have a socket connection and relevant streams
			// 
			try {
				while(stopFlag == false) {
					if (this.debugFlag == true) {
						System.out.println("Client connected from: " + this.remoteSocketTCP.getRemoteSocketAddress().toString());
					}
					
					// We're just using this to see if our issues are timing, or something else
					TimeUnit.SECONDS.sleep(5);

					this.transmitPayload(this.outgoingPayload);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
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
