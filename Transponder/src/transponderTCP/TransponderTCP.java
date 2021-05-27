package transponderTCP;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.HashSet;

public class TransponderTCP{
	int mode;
	private HashSet<tClient> tClientSet = new HashSet<tClient>();
	private HashSet<Thread> clientThreads = new HashSet<Thread>();
	private HashSet<tServer> tServerSet = new HashSet<tServer>();
	private HashSet<Thread> serverThreads = new HashSet<Thread>();
	private tServer tServerSingleton = null;
	private SocketAddress tServerSockAddr = null;
	private ServerSocket serverSocket = null;
	private Payload serverPayload = null;
	private debugObj debugObj = null;
	private boolean stopFlag = false;
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
		tClient client = new tClient(localSock);
		client.setLocalSocketAddress(localAddress);
		client.setRemoteSocketAddress(remoteAddress);
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
	}

	public void stop() {
		this.stopFlag = true;
		this.mode = 0;

		for(tClient currClient : this.tClientSet) {
			currClient.stop();
		}

		for(tServer currServer : this.tServerSet) {
			currServer.stop();
		}

		// If we are in Mode 1 (Server):
		// Close the serverSocket
		// This happens in each tServer instance as well,
		// But close it everywhere just to be sure.

		if(this.mode == 1) {
			try {
				this.serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
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
		
		if(this.debugFlag == true) {
			System.out.println("Listening for connection at: "
					 + this.serverSocket.getInetAddress());
		}
		
		// Listen for incoming connections, then create tServer objects
		// and initialize payload, then...
		// as the connections come in, starting each thread after creation.
		// Also: Put them into the respective tServerSet, tServerThreadSet.
		// Then run().
		while(stopFlag == false) {
			try {
				Socket listener = this.serverSocket.accept();

				if(this.debugFlag == true) {
					System.out.println("Client connected from: " + 
				listener.getInetAddress().toString());
				}

				tServer server = new tServer(this.serverSocket,listener);
				server.setOutgoingPayload(serverPayload);

				if(this.debugFlag == true) {
					System.out.println("Creating tServer instance and starting thread");
					server.setDebugFlag(true);
				}
				
				Thread serverThread = new Thread(server);
				
				serverThread.setName("tServer "+ server.getLocalAddr());
				this.tServerSet.add(server);
				this.serverThreads.add(serverThread);

				serverThread.run();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	// This method configures Mode 2
	// Mode 2 is client-only, no server
	public void confMode2() {
		//Set mode, in case of mode switch
		if(this.mode != 2) {
			this.mode = 2;
		}	

		for(tClient currClient : this.tClientSet) {
			
			// Debug flag condition actions
			if(this.debugFlag == true) {
				currClient.setDebugFlag(true);

				if(this.debugObj == null) {
					System.out.println("debugObj not set for client " + currClient.getRemoteAddrString());
					System.out.println("Not using debugObj for debug purposes! Messages only!");
				}	
				
				if(this.debugObj instanceof debugObj) {
					currClient.setDebugObj(debugObj);
				}
			}
			
			// Create a new client thread, change thread name, add to 
			// clientThread hashSet
			Thread addedThread = new Thread(currClient);
			addedThread.setName("tClient "+ currClient.getRemoteAddrString());
			this.clientThreads.add(addedThread);
			addedThread.start();
		}
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
		String result = "Status for current TCP Transponder: \n";
		if(this.mode == 0) {
			result += "Transponder stopped!";
		}

		if(this.mode == 1) {
			result += "Current Mode: Server \n";

			if(this.tServerSet.size() == 0) {
				result += "No clients connected!\n";
				result += "Server socket listening on:\n";
				result += "ServerSocket IP:Port" + this.serverSocket.getLocalSocketAddress().toString() +"\n";
			}

			for(tServer currServer : this.tServerSet) {
				result += currServer.getStatus() + "\n";
			}
		}

		if(this.mode == 2) {
			result += "Current Mode: Client \n";

			for(tClient currClient : this.tClientSet) {
				result += currClient.getStatus();
			}
		}

		if(this.tClientSet.size() > 0) {
			for(tClient currClient : this.tClientSet) {
				result += currClient.getStatus();
			}
		}

		if(this.tServerSet.size() > 0) {
			for(tServer currServer : this.tServerSet) {
				result += currServer.getStatus();
			}
		}
		return result;
	}
}

