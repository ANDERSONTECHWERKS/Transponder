package transponderTCP;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.stream.Stream;
import transponderTCP.MessageDateComparator;

public class TransponderTCP implements Runnable {

	private int mode;

	private ControllerMenu localController = null;

	private HashSet<tClientTCP> tClientSet = new HashSet<tClientTCP>();
	private HashSet<Thread> clientThreads = new HashSet<Thread>();

	private HashSet<tServerTCP> tServerSet = new HashSet<tServerTCP>();
	private HashSet<Thread> serverThreads = new HashSet<Thread>();

	private HashSet<clientSignOn> csonSet = new HashSet<clientSignOn>();
	private HashSet<clientSignOff> csoffSet = new HashSet<clientSignOff>();

	private PriorityBlockingQueue<ClientMessage<?>> clientMessages = new PriorityBlockingQueue<ClientMessage<?>>();
	private PriorityBlockingQueue<ServerMessage<?>> serverMessages = new PriorityBlockingQueue<ServerMessage<?>>();

	private SocketAddress tServerSockAddr = null;

	private ServerSocket serverSocket = null;

	private ServerMessage<?> servMessage = null;

	private debugObj debugObj = null;

	private boolean debugFlag = false;
	private boolean stopFlag = false;

	public TransponderTCP(int mode) {
		this.mode = mode;
	}

	// Constructor for Mode 1(Server-DEBUG)
	public TransponderTCP(ServerSocket servSocket) {
		this.mode = 1;
		this.serverSocket = servSocket;

		// Check if the localServerSocket has connection details,
		// but is not bound.
		// This check is relevant when localServerSocket is passed in while unconnected,
		// This check will assign the localSockAddr details to the localServerSocket

		if (this.serverSocket.isBound() == false) {
			throw new IllegalStateException("transponderTCP| ServerSocket was not bound when passed in!");
		}

		this.tServerSockAddr = servSocket.getLocalSocketAddress();
	}

	public TransponderTCP(Socket clientSock) {
		this.mode = 2;

		tClientTCP client = new tClientTCP(clientSock, this, this.clientMessages);

		if (clientSock.isBound() == false) {
			throw new IllegalStateException("transponderTCP| clientSock was not bound when passed in!");
		}

		this.tClientSet.add(client);
	}

	// Constructor for Mode 1(With controllerMenu)
	// This constructor assumes a bound serverSocket.

	public TransponderTCP(ServerSocket localServerSocket, ControllerMenu controller) {
		this.mode = 1;
		this.serverSocket = localServerSocket;
		this.localController = controller;

		// Check if the localServerSocket has connection details,
		// but is not bound.
		// This check is relevant when localServerSocket is passed in while unconnected,
		// This check will assign the localSockAddr details to the localServerSocket

		if (this.serverSocket.isBound() == false) {

			throw new IllegalStateException("TransponderTCP| ServerSocket passed into constructor is not bound!");
		}
		this.tServerSockAddr = localServerSocket.getLocalSocketAddress();
	}

	// Constructor for Mode 2 (With controllerMenu)
	public TransponderTCP(Socket clientSock, ControllerMenu controller) {

		this.mode = 2;
		this.localController = controller;

		tClientTCP client = new tClientTCP(clientSock, this, this.clientMessages);

		client.setRemoteSocketAddress(clientSock.getRemoteSocketAddress());

		this.tClientSet.add(client);
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

	// Run thread, configuring selected mode
	@Override
	public void run() {

		if (this.mode == 1) {
			if (this.serverSocket != null) {

				// listen-loop
				while (this.stopFlag == false) {
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

	// Close clients gracefully, stop threads (internally. Do NOT use Thread.stop()!
	// Deprecated!)
	public void stopAll() {
		this.mode = 0;

		for (tClientTCP currClient : this.tClientSet) {
			currClient.stop();
		}

		for (tServerTCP currServer : this.tServerSet) {
			currServer.stop();
		}
	}

	public void stopClients() {
		this.mode = 0;

		for (tClientTCP currClient : this.tClientSet) {
			currClient.stop();
		}
	}

	public void stopServers() {
		this.mode = 0;

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

	// Directs each server instance to send the directed message NOW!
	public void serverSendMessage(ServerMessage<?> servMessage) {
		this.servMessage = servMessage;
		for (tServerTCP currServ : this.tServerSet) {
			currServ.setServerMessage(servMessage);
			currServ.transmitServerMessage(servMessage);
		}
	}

	public void setServerMessage(ServerMessage<?> servMessage) {
		this.servMessage = servMessage;
	}

	public void setControllerMenu(ControllerMenu localMenu) {
		this.localController = localMenu;
	}

	public void updateServerInstances(ServerMessage<?> servMessage) {
		for (tServerTCP currServ : this.tServerSet) {
			currServ.setServerMessage(servMessage);
		}
	}

	// This method binds a specified tClient to the parameter endpoint
	public void clientBindRemoteSocks(tClientTCP client, SocketAddress remoteEndpoint) {

		for (tClientTCP currClient : this.tClientSet) {

			if (currClient.equals(client)) {
				currClient.setRemoteSocketAddress(remoteEndpoint);
			}

		}
	}

	// Modes:
	// 0 - Stopped
	// 1 - Server
	// 2 - Client
	// 3 - Server, with receiving clients (relay mode? WIP.)

	public void setMode(int mode) {
		this.mode = mode;
	}

	// This method configures Mode 1
	// Mode 1 is server-only, no client

	private void confMode1() {
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

		tServerTCP server = null;

		// Try to create tServer instance every time we accept a connection
		try {
			server = new tServerTCP(this.serverSocket.accept(), this);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		server.setServerMessage(this.servMessage);

		// debug-specific actions when debugFlag set to TRUE
		if (this.debugFlag == true) {

			System.out.println("DebugFlag set to TRUE! Setting debugFlag on server instance!");
			server.setDebugFlag(true);

			System.out.println(
					"transponderTCP| Listening for connection at: " + this.serverSocket.getInetAddress() + "\n");
			System.out.println("transponderTCP| ServerMessage set to: \n" + this.servMessage.toString());
		}

		Thread serverThread = new Thread(server);

		serverThread.setName("tServer| " + server.getLocalAddr());

		this.tServerSet.add(server);
		this.serverThreads.add(serverThread);

		serverThread.start();

		if (this.debugFlag == true) {
			System.out.println("tServer Instance " + serverThread.getName() + " created!");
		}
	}

	// This method configures Mode 2
	// Mode 2 is client-only, no server
	private void confMode2() {

		// Set mode, in case of mode switch
		if (this.mode != 2) {
			this.mode = 2;
		}

		for (tClientTCP currClient : this.tClientSet) {

			// Debug flag condition actions
			if (this.debugFlag == true) {
				currClient.setDebugFlag(true);

				if (this.debugObj == null) {
					System.out.println("debugObj not set for client " + currClient.toString());
					System.out.println("Not using debugObj for debug purposes! Messages only!");
				}

				if (this.debugObj instanceof debugObj) {
					currClient.setDebugObj(debugObj);
				}
			}

			// Create a new client thread, change thread name, add to
			// clientThread hashSet
			Thread addedThread = new Thread(currClient);

			addedThread.setName("tClient| Connected to: " + currClient.getRemoteAddrString());

			this.clientThreads.add(addedThread);

			addedThread.start();
		}
	}

	// TODO: Future - relay mode?
	private void confMode3() {

		// Set mode, in case of mode switch
		if (this.mode != 3) {
			this.mode = 3;
		}

		// Standard re-initialization block, to clear out clients/servers between mode
		// switches
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

		if (this.tServerSet != null && this.tClientSet != null) {

			for (tServerTCP currServer : this.tServerSet) {
				currServer.setDebugFlag(flag);
			}

			for (tClientTCP currClient : this.tClientSet) {
				currClient.setDebugFlag(flag);
			}
		}
	}

	public String getStatus() {
		String result = "Status for current TransponderTCP: " + this + "\n";

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
			result += "Printing status for each client associated with this TransponderTCP: +\n";

			for (tClientTCP currClient : this.tClientSet) {
				result += currClient.getStatus();
			}
		}

		return result;
	}

	//

	public Stream<ClientMessage<?>> getClientStream() {
		// Creates a master messageList, fills it with all the available messages in
		// each
		// client's messageQueue, and sorts it by date. After that: Converts to a
		// stream.

		ArrayList<ClientMessage<?>> messageList = new ArrayList<ClientMessage<?>>();

		for (tClientTCP currClient : this.tClientSet) {

			PriorityBlockingQueue<ClientMessage<?>> cliMessages = currClient.getMessageQueue();

			for (ClientMessage<?> currMessage : cliMessages) {
				messageList.add(currMessage);
			}
		}
		MessageDateComparator dateComparator = new MessageDateComparator();

		messageList.sort(dateComparator);

		return messageList.stream();
	}

	public void serverProcessSignOn(clientSignOn cson) {
		System.out.println("TransponderTCP| new signon from:" + cson.getClientAddr().toString());
		this.csonSet.add(cson);
	}

	public void serverProcessSignOff(clientSignOff csoff) {
		System.out.println("TransponderTCP| new signoff from:" + csoff.getClientAddr().toString());
		this.csoffSet.add(csoff);
	}
	
	// Retrieves an ArrayList of ClientMessages logged with the server, sorted by timestamp.

	public ArrayList<ClientMessage<?>> serverRetrieveMessages() {

		ArrayList<ClientMessage<?>> messageList = new ArrayList<ClientMessage<?>>();

		for (tServerTCP currServer : this.tServerSet) {

			PriorityBlockingQueue<ClientMessage<?>> cliMessages = currServer.getMessageQueue();

			for (ClientMessage<?> currMessage : cliMessages) {
				messageList.add(currMessage);
			}
		}
		MessageDateComparator dateComparator = new MessageDateComparator();

		messageList.sort(dateComparator);

		return messageList;
	}

	public void clientPerformSignOn() {
		for (tClientTCP currClient : this.tClientSet) {
			currClient.clientPerformSignOn();
		}
	}

	public void clientPerformSignOff() {
		for (tClientTCP currClient : this.tClientSet) {
			currClient.clientPerformSignOff();
		}

	}

	public void clientSendMessage(ClientMessage<?> message) {
		// Sends the specified message to all clients.
		// TODO: Think about how we can specify clients to send messages to in the
		// future.
		for (tClientTCP currClient : this.tClientSet) {
			currClient.clientSendMessage(message);
		}
	}

	public ClientMessage<?> clientGetLastMessage() throws InterruptedException {
		return this.clientMessages.take();
	}

}
