package transponderTCP;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.HashSet;

public class TransponderTCP{
	int mode;
	private HashSet<tClient> tClientSet = new HashSet<tClient>();
	private HashSet<Thread> clientThreads = new HashSet<Thread>();
	private tServer tServerSingleton = null;
	private SocketAddress tServerSockAddr = null;
	private ServerSocket serverSocket = null;
	private Thread serverThread = null;
	private Payload serverPayload = null;
	private debugObj debugObj = null;
	private boolean debugFlag = false;
	
	public TransponderTCP(int mode) {
		this.mode = mode;
	}
	
	public TransponderTCP(int mode, Socket clientSock, SocketAddress remoteAddr) {
		this.mode = mode;
		tClient client = new tClient(clientSock);
		client.setRemoteSocketAddress(remoteAddr);
		this.tClientSet.add(client);
	}
	
	public TransponderTCP(int mode, ServerSocket localServerSocket, SocketAddress localSockAddr) {
		this.mode = mode;
		this.serverSocket = localServerSocket;
		this.tServerSockAddr = localSockAddr;
	}
	public TransponderTCP(int mode, ServerSocket localServerSocket, Socket localSock, SocketAddress localAddress, SocketAddress remoteAddress) {
		this.mode = mode;
		tClient client = new tClient(localSock, localAddress,remoteAddress);
		this.tClientSet.add(client);
		this.serverSocket = localServerSocket;
	}
	
	
	public void run() {
		if(this.mode == 1) {
			if(this.serverSocket != null) {
				this.confMode1();
			}
		}
		
		if(this.mode == 2) {
			this.confMode2();
			
		}
		
		if(this.mode == 3) {
			//TODO: Figure out what you want Mode 3 to do. Ad-hoc? Master/slave servers?
		}
	}
	
	public void stop() {
		for(tClient currClient : this.tClientSet) {
			currClient.stop();
		}
		
		this.tServerSingleton.stop();
	}
	
	public void bindLocalServerSock(SocketAddress localEndpoint) {
		try {
			this.serverSocket.bind(localEndpoint);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void setInitialServerPayload(Payload payload) {
		if(this.serverPayload == null) {
			this.serverPayload = payload;
		}
	}
	
	public void updateServerPayload(Payload payload) {
		if(this.tServerSingleton != null) {
			this.tServerSingleton.setOutgoingPayload(payload);
		}
	}
	
	//This method binds a specified tClient to the parameter endpoint
	public void clientBindRemoteSocks(tClient client,SocketAddress remoteEndpoint) {
		for(tClient curr : this.tClientSet) {
			if(curr.equals(client)) {
				curr.setRemoteSocketAddress(remoteEndpoint);
			}
		}
	}
	

	public void setMode(int mode) {
		this.mode = mode;
	}
	
	// This method configures Mode 1
	// Mode 1 is server-only, no client
	public void confMode1(){
		//Set mode, in case of mode switch
		if(this.mode != 1) {
			this.mode = 1;
		}
		
		if(this.serverSocket == null) {
			throw new IllegalStateException("serverSocket not set!");
		}
		
		if(this.tServerSockAddr == null) {
			throw new IllegalStateException("tServerSockAddr not set!");
		}

		
		//Assign ServerSocket, create tServer object, create and start thread.
		this.tServerSingleton = new tServer(this.serverSocket,this.tServerSockAddr);
		this.tServerSingleton.setOutgoingPayload(serverPayload);
		
		// After creating tServer object, check if debugFlag is TRUE.
		// If TRUE: Then assign 
		if(this.debugFlag == true) {
			if(this.debugObj == null) {
				throw new IllegalStateException("TransponderTCP debugObj not set!");
			}	
			this.tServerSingleton.setDebugFlag(true);
			this.tServerSingleton.setDebugObj(this.debugObj);
		}
		
		this.serverThread = new Thread(tServerSingleton);
		this.serverThread.start();
	}
	
	// This method configures Mode 2
	// Mode 2 is client-only, no server
	public void confMode2() {
		//Set mode, in case of mode switch
		if(this.mode != 2) {
			this.mode = 2;
		}	
		
		for(tClient currClient : this.tClientSet) {
			if(this.debugFlag == true) {
				if(this.debugObj == null) {
					throw new IllegalStateException("TransponderTCP debugObj not set!");
				}	
				
				currClient.setDebugFlag(true);
				currClient.setDebugObj(debugObj);
			}
			
			Thread addedThread = new Thread(currClient);
			addedThread.setName("tClient for remote " + currClient.getRemoteAddrString());
			this.clientThreads.add(addedThread);
			addedThread.start();
		}
	}
	
	// This method configures Mode 3
	// Mode 3 is server AND client
	// Ideally for future-used with some peering function
	public void confMode3(ServerSocket inputServerSock, Socket inputSock) {
		//Set mode, in case of mode switch
		if(this.mode != 3) {
			this.mode = 3;
		}
		
		this.serverSocket = inputServerSock;
		tClient client = new tClient(inputSock);
		this.tClientSet.add(client);		
	}
	
	// Assigns a debugObject.
	// debugObject needs to be set before running Transponder,
	// Or else an exception will be thrown.
	public void setDebugObject(debugObj debugObj) {
		this.debugObj = debugObj;
	}
	
	public void setDebugFlag(boolean flag) {
		this.debugFlag = flag;
	}
	

}

