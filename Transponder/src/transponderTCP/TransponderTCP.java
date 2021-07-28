package transponderTCP;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.HashSet;

public class TransponderTCP implements Runnable, Transponder{
	private int mode;
	private ControllerMenu localController = null;
	private HashSet<tClientTCP> tClientSet = new HashSet<tClientTCP>();
	private HashSet<Thread> clientThreads = new HashSet<Thread>();
	private HashSet<tServerTCP> tServerSet = new HashSet<tServerTCP>();
	private HashSet<Thread> serverThreads = new HashSet<Thread>();
	private tServerTCP tServerSingleton = null;
	private SocketAddress tServerSockAddr = null;
	private ServerSocket serverSocket = null;
	private Payload serverPayload = null;
	private debugObj debugObj = null;
	private boolean debugFlag = false;

	// BE ADVISED: Constructors without localController parameters are intended for debug ONLY!
	public TransponderTCP(int mode) {
		this.mode = mode;
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
				this.confMode1();
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
	
	public void setControllerMenu(ControllerMenu localMenu) {
		this.localController = localMenu;
	}

	public void updateServerPayload(Payload payload) {
		if (this.tServerSingleton != null) {
			this.tServerSingleton.setOutgoingPayload(payload);
		}
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
		
		//Standard re-initialization block, to clear out clients/servers between mode switches
		tClientSet = new HashSet<tClientTCP>();
		clientThreads = new HashSet<Thread>();
		tServerSet = new HashSet<tServerTCP>();
		serverThreads = new HashSet<Thread>();

		if (this.serverSocket == null) {
			throw new IllegalStateException("serverSocket not set!");
		}

		if (this.tServerSockAddr == null) {
			throw new IllegalStateException("tServerSockAddr not set!");
		}



		tServerTCP server = new tServerTCP(this.serverSocket,this.tServerSockAddr,this);
		server.setOutgoingPayload(serverPayload);
		
		//debug-specific actions when debugFlag set to TRUE
		if (this.debugFlag == true) {
			
			System.out.println("DebugFlag set to TRUE! Setting debugFlag on server instance!");
			server.setDebugFlag(true);
			
			System.out.println("transponderTCP| Listening for connection at: " + this.serverSocket.getInetAddress() +"\n");
			System.out.println("transponderTCP| Payload set to: \n" + serverPayload.toString());
		}


		Thread serverThread = new Thread(server);

		serverThread.setName("tServer " + server.getLocalAddr());
		this.tServerSet.add(server);
		this.serverThreads.add(serverThread);

		serverThread.start();

		if (this.debugFlag == true) {
			System.out.println("tServer Instance " + serverThread.getName() + " created!");
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
					System.out.println("debugObj not set for client " + currClient.getRemoteAddrString());
					System.out.println("Not using debugObj for debug purposes! Messages only!");
				}

				if (this.debugObj instanceof debugObj) {
					currClient.setDebugObj(debugObj);
				}
			}

			// Create a new client thread, change thread name, add to
			// clientThread hashSet
			Thread addedThread = new Thread(currClient);
			addedThread.setName("tClient| Connecting to IP address: " + currClient.getRemoteAddrString());
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

	public void setDebugFlag(boolean flag) {
		this.debugFlag = flag;
	}

	public String getStatus() {
		String result = "Status for current TransponderTCP: \n";

		if (this.mode == 0) {
			result += "Transponder stopped! \n";
		}

		if (this.mode == 1) {
			result += "Current Mode: Server \n";

			if (this.tClientSet.size() == 0) {
				result += "No clients connected!\n";
				result += "Server socket listening on:\n";
				result += "ServerSocket IP:Port" + this.serverSocket.getLocalSocketAddress().toString() + "\n";
			}

			for (tServerTCP currServer : this.tServerSet) {
				result += currServer.getStatus() + "\n";
			}
		}

		if (this.mode == 2) {
			result += "Current Mode: Client \n";

			for (tClientTCP currClient : this.tClientSet) {
				result += currClient.getStatus();
			}
		}

		if (this.tClientSet.size() > 0) {
			for (tClientTCP currClient : this.tClientSet) {
				result += currClient.getStatus();
			}
		}

		if (this.tServerSet.size() > 0) {
			for (tServerTCP currServer : this.tServerSet) {
				result += currServer.getStatus();
			}
		}
		return result;
	}
}
