package transponder;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.HashSet;

public class TransponderTCP implements Runnable, Transponder{
	
	private int mode;
	
	private ControllerMenu localController = null;
	
	private HashSet<tClientTCP> tClientSet = new HashSet<tClientTCP>();
	private HashSet<Thread> clientThreads = new HashSet<Thread>();
	private HashSet<tServerTCP> tServerSet = new HashSet<tServerTCP>();
	private HashSet<Thread> serverThreads = new HashSet<Thread>();
		
	private SocketAddress tServerSockAddr = null;
	
	private ServerSocket serverSocket = null;
	
	private Payload serverPayload = null;
	
	private debugObj debugObj = null;
	
	private boolean debugFlag = false;
	
	private boolean stopFlag = false;

	// BE ADVISED: Constructors without localController parameters are intended for debug ONLY!
	public TransponderTCP(int mode) {
		this.mode = mode;
	}

	// Constructor for Mode 1 (Server-DEBUG)
	public TransponderTCP(ServerSocket servSock) {
		this.mode = 1;

		this.serverSocket = servSock;
		
		if(servSock.isBound()) {
			this.tServerSockAddr = servSock.getLocalSocketAddress();
		} else {
			throw new IllegalStateException("servSock parameter must be bound when passing in!");
		}
	}
	
	// Constructor for Mode 1(Server-DEBUG)
	public TransponderTCP(int mode, ServerSocket localServerSocket, SocketAddress localSockAddr) {
		this.mode = mode;
		this.serverSocket = localServerSocket;
		
		// Check if the localServerSocket has connection details,
		// but is not bound.
		// This check is relevant when localServerSocket is passed in while unconnected,
		// This check will assign the localSockAddr details to the localServerSocket
		
		if(this.serverSocket.isBound() == false) {
			if(localSockAddr != null) {
				try {
					localServerSocket.bind(localSockAddr);
				} catch (IOException e) {
					System.out.println("TransponderTCP| serverSocket binding issue!");
					e.printStackTrace();
				}
			}
		}
		
		this.tServerSockAddr = localSockAddr;
	}
	


	// Constructor for Mode 2 (Client-DEBUG)
	public TransponderTCP(int mode, Socket clientSock, SocketAddress remoteAddr) {
		this.mode = mode;

		tClientTCP client = new tClientTCP(clientSock,this);

		client.setRemoteSocketAddress(remoteAddr);

		this.tClientSet.add(client);
	}
	
	// Constructor for Mode 1(Server-Actual)
	public TransponderTCP(int mode, ServerSocket localServerSocket, SocketAddress localSockAddr, ControllerMenu controller) {
		this.mode = mode;
		this.serverSocket = localServerSocket;
		this.localController = controller;
		
		// Check if the localServerSocket has connection details,
		// but is not bound.
		// This check is relevant when localServerSocket is passed in while unconnected,
		// This check will assign the localSockAddr details to the localServerSocket
		
		if(this.serverSocket.isBound() == false) {
			if(localSockAddr != null) {
				try {
					localServerSocket.bind(localSockAddr);
				} catch (IOException e) {
					System.out.println("TransponderTCP| serverSocket binding issue!");
					e.printStackTrace();
				}
			}
		}
		
		this.tServerSockAddr = localSockAddr;
	}

	// Constructor for Mode 2 (Client-Actual)
	public TransponderTCP(int mode, Socket clientSock, SocketAddress remoteAddr, ControllerMenu controller) {
		this.mode = mode;
		this.localController = controller;

		tClientTCP client = new tClientTCP(clientSock,this);

		client.setRemoteSocketAddress(remoteAddr);

		this.tClientSet.add(client);
	}
	
	// Run thread, configuring selected mode
	@Override
	public void run() {

		if (this.mode == 1) {
			if (this.serverSocket != null) {
				
				// listen-loop
				while(this.stopFlag == false) {
					this.confMode1();
				}
				
				// After we leave the listen-loop, close IO.
				// Assume we are done at this point.
				closeIO();
			}
		}

		if (this.mode == 2) {
			this.confMode2();
		}
	}

	// Close clients gracefully, stop threads (internally. Do NOT use Thread.stop()! Deprecated!)
	public void stop() {
		this.mode = 0;

		for (tClientTCP currClient : this.tClientSet) {
			currClient.stop();
		}

		for (tServerTCP currServer : this.tServerSet) {
			currServer.stop();
		}
	}

	public void bindLocalServerSock(SocketAddress localEndpoint) {
		try {
			this.serverSocket.bind(localEndpoint);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setInitialServerPayload(Payload payload) {
		if (this.serverPayload == null) {
			this.serverPayload = payload;
		}
	}
	
	public void setServerOpts(String args[], ServerSocket sock) {
		// For the moment, args[] does nothing.
		// We will use this block to set options as we
		// develop and troubleshoot

		try {
			
			sock.setReuseAddress(true);
			
		} catch (SocketException e) {
			System.out.println("tServer| SocketException occured! Unable to setServerOpts()!");
			e.printStackTrace();
		}

	}
	
	public void setControllerMenu(ControllerMenu localMenu) {
		this.localController = localMenu;
	}

	public void setServerPayload(Payload payload) {
		this.serverPayload = payload;
	}

	// This method binds a specified tClient to the parameter endpoint
	public void clientBindRemoteSocks(tClientTCP client, SocketAddress remoteEndpoint) {
		
		for (tClientTCP curr : this.tClientSet) {
			
			if (curr.equals(client)) {
				curr.setRemoteSocketAddress(remoteEndpoint);
			}
		}
	}
	
	//Modes:
	// 0 - Stopped
	// 1 - Server
	// 2 - Client
	// 3 - Server, with receiving clients (relay mode? WIP.)

	public void setMode(int mode) {
		this.mode = mode;
	}

	// This method configures Mode 1
	// Mode 1 is server-only, no client
	public void confMode1() {

		// Set mode, in case of mode switch
		if (this.mode != 1) {
			this.mode = 1;
		}

		if (this.serverSocket == null) {
			throw new IllegalStateException("serverSocket not set!");
		}

		if (this.tServerSockAddr == null) {
			throw new IllegalStateException("tServerSockAddr not set!");
		}
		
		//debug-specific actions when debugFlag set to TRUE
		if (this.debugFlag == true) {
			
			System.out.println("TransponderTCP| DebugFlag set to TRUE! Setting debugFlag on server instance!");
			System.out.println("TransponderTCP| Listening for connection at: " + this.serverSocket.getInetAddress() +"\n");
			System.out.println("TransponderTCP| Payload set to: \n" + serverPayload.toString());
		}

		tServerTCP server = null;

		// Listen for connection on serverSocket
		try {
			
			server = new tServerTCP(this.serverSocket.accept());
			
		} catch (IOException e) {
			
			System.out.println("TransponderTCP| Failed to listen on socket!");
			e.printStackTrace();
			
		}

		if(this.debugFlag == true) {
			server.setDebugFlag(true);
			System.out.println("TransponderTCP| New client connected from:" + server.getRemoteAddr());

		}

		server.setOutgoingPayload(serverPayload);

		Thread serverThread = new Thread(server);

		serverThread.setName("tServer -" + server.getLocalAddr());

		this.tServerSet.add(server);

		this.serverThreads.add(serverThread);

		serverThread.start();

		if (this.debugFlag == true) {

			System.out.println("TransponderTCP| tServer Instance " + serverThread.getName() + " started!");

		}
	}

	// This method configures Mode 2
	// Mode 2 is client-only, no server
	public void confMode2() {

		// Set mode, in case of mode switch
		if (this.mode != 2) {
			this.mode = 2;
		}

		for (tClientTCP currClient : this.tClientSet) {

			// Debug flag condition actions
			if (this.debugFlag == true) {
				currClient.setDebugFlag(true);

				if (this.debugObj == null) {
					System.out.println("TransponderTCP| debugObj not set for client " + currClient.getRemoteAddrString());
					System.out.println("TransponderTCP| Not using debugObj for debug purposes! Messages only!");
				}

				if (this.debugObj instanceof debugObj) {
					currClient.setDebugObj(debugObj);
				}
			}

			// Create a new client thread, change thread name, add to
			// clientThread hashSet
			Thread addedThread = new Thread(currClient);
			
			addedThread.setName("tClient connection to: " + currClient.getRemoteAddrString());
			
			this.clientThreads.add(addedThread);
			
			addedThread.start();
		}
	}
	
	//TODO: Finish this method! Not sure if I want to start with re-initializing HashSets[tClientSet,clientThreads,tServerSet,serverThreads] or not...
	public void confMode3() {

		// Set mode, in case of mode switch
		if(this.mode != 3) {
			this.mode = 3;
		}
		
		//Standard re-initialization block, to clear out clients/servers between mode switches
		tClientSet = new HashSet<tClientTCP>();
		clientThreads = new HashSet<Thread>();
		tServerSet = new HashSet<tServerTCP>();
		serverThreads = new HashSet<Thread>();
		
	}

	// Assigns a debugObject.
	// debugObject needs to be set before running Transponder,
	// Or else an exception will be thrown.
	public void setDebugObject(debugObj debugObj) {
		this.debugObj = debugObj;
	}
	
	// Shuts down the IOstreams on the listening ServerSocket
	public void closeIO() {

		try {
			this.serverSocket.close();

		} catch (IOException e) {

			System.out.println("TransponderTCP| closeIO failed!");
			e.printStackTrace();

		}

	}

	public void setDebugFlag(boolean flag) {
		
		this.debugFlag = flag;
		
		if(this.tServerSet != null && this.tClientSet != null) {
			
			for(tServerTCP currServer : this.tServerSet) {
				currServer.setDebugFlag(flag);
			}
			
			for(tClientTCP currClient : this.tClientSet) {
				currClient.setDebugFlag(flag);
			}
		}
	}

	public String getStatus() {
		String result = "Status for current TransponderTCP: "+ this +"\n";

		if (this.mode == 0) {
			result += "Transponder stopped! \n";
		}

		if (this.mode == 1) {
			result += "TransponderTCP Mode: Server \n";

			if (this.tClientSet.size() == 0) {
				result += "No clients connected!\n";
				result += "Server socket listening on:\n";
				result +=  this.serverSocket.getLocalSocketAddress().toString() + "\n";
			} else {
	
				result += "Connected Clients:\n";
		
				int counter = 0;
				for(tServerTCP currServer : this.tServerSet) {
					counter++;
					result += counter + currServer.getRemoteAddr().toString() + "\n";
				}
			}

		}

		if (this.mode == 2) {
			result += "TransponderTCP Mode: Client \n";

			for (tClientTCP currClient : this.tClientSet) {
				result += currClient.getStatus() +"\n";
			}
		}
		return result;
	}
}
