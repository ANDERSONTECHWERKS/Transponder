package transponderTCP;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.PriorityBlockingQueue;

import org.junit.Assert;
import org.junit.Test;

import junit.framework.TestCase;


public class Tests extends TestCase{


	// This test checks if we can create two TransponderTCP objects and have them communicate
	@Test
	public void testMode1andMode2TransponderTCPLocal() {
		ServerSocket servSock;
		TransponderTCP testTranspServer;
		InetSocketAddress serverSockAddr = new InetSocketAddress("127.0.0.1",6969);
		InetSocketAddress clientSockAddr = new InetSocketAddress("127.0.0.1",7000);
		Socket clientSock = new Socket();
		TransponderTCP testTranspClient;
		Payload testPayload = new Payload(69,"Test");
		debugObj dObj = new debugObj();
		
		try {
			servSock = new ServerSocket();
			servSock.bind(serverSockAddr);
			
			testTranspServer = new TransponderTCP(servSock);
			testTranspServer.setInitServerMessage(testPayload);
			testTranspServer.setDebugFlag(true);
			testTranspServer.setDebugObject(dObj);
			Thread transpServThread = new Thread(testTranspServer);
			transpServThread.start();
			
			clientSock.bind(clientSockAddr);
			clientSock.connect(serverSockAddr);
			
			testTranspClient = new TransponderTCP(clientSock);
			testTranspClient.setDebugFlag(true);
			testTranspClient.setDebugObject(dObj);
			Thread transpClientThread = new Thread(testTranspClient);
			transpClientThread.start();

			assertTrue(dObj.evaluateMessageEquivalanceMulti());
			
			System.out.println("dObj evaluatePayloadEquivalence result: " + dObj.evaluateMessageEquivalanceMulti());
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// This test checks that our menu creates the appropriate Sockets and ServerSockets used as input
	// for our Transponder objects.
	@Test
	public void testMenuObjects() {
		// This test may be confusing. 
		// Basically: What we are doing is feeding inputStreams into the scanner
		// to simulate user input.
		// The first input simulates a user selecting mode 1 and creating a
		// serverSocket.
		// The second input is used to call a method on that object and return a client
		// socket object. 
		// ...Not the most standardized way to test methods, but we'll work on it.
		
		String createMode1Input = new String("1" + System.lineSeparator() + "127.0.0.1"
				+ System.lineSeparator() + "6969" + System.lineSeparator()  
				+ System.lineSeparator() + "1");
		String createClientSocketInput = new String("127.0.0.1"
				+ System.lineSeparator() + "7000" + System.lineSeparator() + "127.0.0.1"
				+ System.lineSeparator() + "6969");
		
		// Note: We recreate the scanner and inputStream when testing different
		// methods
		// These objects are, ultimately, setting up a menu object that 
		// creates a currTransponder object in mode 1 (server) as well as 
		// creating a Socket object using the method within the testMenu 
		// object that is setup to connect to the currTransponder object 
		// contained within our testMenu.
		// End-goal here: Test that our Socket creation methods are 
		// producing the objects with the states that we want.
		InputStream input = new ByteArrayInputStream(createMode1Input.getBytes());
		Scanner testScanner = new Scanner(input);
		ControllerMenu testMenu = new ControllerMenu(testScanner);

		
		input = new ByteArrayInputStream(createClientSocketInput.getBytes());
		testScanner = new Scanner(input);
		
		// Creating this socket tests the output of 
		// ControllerMenu.reqClientAddrIPV4TCP
		Socket testSocket = testMenu.promptClientSocket(testScanner);
		
		
		// Then we gather the fields from the created testSocket
		String testSockLocalAddr = testSocket.getLocalAddress().toString();
		String testSockRemoteAddr = testSocket.getInetAddress().toString();
		int testSockLocalPort = testSocket.getLocalPort();
		int testSockRemotePort = testSocket.getPort();
		
		assertEquals(testSockLocalAddr.compareTo("/127.0.0.1"),0);
		assertEquals(testSockRemoteAddr.compareTo("/127.0.0.1"),0);
		assertTrue(testSockLocalPort == 7000);
		assertTrue(testSockRemotePort == 6969);

		
	}
	@Test
	public void testServer() {
		InetSocketAddress serverAddr = null;
		ServerSocket serverSock = null;
		Payload testPayload = new Payload(5,"RED");

		serverAddr = new InetSocketAddress(InetAddress.getLoopbackAddress(),6969);
		
		// Try instantiating serverSock
		try {
			serverSock = new ServerSocket(6969, 1, serverAddr.getAddress());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		TransponderTCP testTransp = new TransponderTCP(serverSock);
		
		testTransp.setInitServerMessage(testPayload);
		
		Thread serverThread = new Thread(testTransp);
		
		serverThread.start();
	}
	
	@Test
	public void testClient() {
		Socket localSock = null;
		
		try {
			System.out.println("--testClient JUNIT Test output--\ntestClient| Server address:" + Inet4Address.getLoopbackAddress()+ ":6969  \n"
					+ "testClient| Client Address:" + Inet4Address.getLoopbackAddress() + ":7000");
			
			localSock = new Socket(Inet4Address.getLoopbackAddress(),6969, Inet4Address.getLoopbackAddress(),7000);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		tClientTCP testClient = new tClientTCP(localSock);
		
		testClient.setDebugFlag(true);
		
		Thread clientThread = new Thread(testClient);

		clientThread.start();
		
		ServerMessage<?> testPayload = testClient.getLastServerMessage();
		
		if(testPayload instanceof Payload) {
			System.out.println("testClient: Payload received!\n" + testPayload.toString() +"\n");
		}
	}
	
	//testServerAndClient is currently broken. Gotta think of a clever way to do this...
	public void testServerAndClient() {

		InetSocketAddress serverAddr = null;
		ServerSocket serverSock = null;
		Payload testPayload = new Payload(5,"RED");
		debugObj debugger = new debugObj(testPayload);

		serverAddr = new InetSocketAddress(Inet4Address.getLoopbackAddress(),6969);
		
		// Try instantiating serverSock
		try {
			serverSock = new ServerSocket(6969, 1, serverAddr.getAddress());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		TransponderTCP testTranspTCP = new TransponderTCP(serverSock);

		testTranspTCP.setDebugFlag(true);
		testTranspTCP.setInitServerMessage(testPayload);
		testTranspTCP.setDebugObject(debugger);

		Thread transpThread = new Thread(testTranspTCP);
		
		transpThread.start();		
		
		
		Socket localSock = null;
		
		try {
			System.out.println("--testClient--\nServer address:" + Inet4Address.getLoopbackAddress()+ ":6969  \n"
					+ "Client Address:" + Inet4Address.getLoopbackAddress() + ":7000");
			
			localSock = new Socket(Inet4Address.getLoopbackAddress(),6969, Inet4Address.getLoopbackAddress(),7000);
			
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		tClientTCP testClient = new tClientTCP(localSock);
		
		testClient.setDebugFlag(true);
		testClient.setDebugObj(debugger);
		
		Thread clientThread = new Thread(testClient);
		
		clientThread.start();

		boolean payloadsMatch = debugger.evaluateMessageEquivalanceMulti();

		System.out.println("testClientAndServer| debugObj payloads match? :" + payloadsMatch);
		
		assertTrue(payloadsMatch);
	}
	
	@Test
	public void testServerSimInput() {
		String createMode1Input = new String("1" + System.lineSeparator() + "127.0.0.1"
				+ System.lineSeparator() + "6969" + System.lineSeparator()  
				+ System.lineSeparator() + "1" + System.lineSeparator() + "Test"
				+ System.lineSeparator() + "69" + System.lineSeparator() +
				"1");
		InputStream input = new ByteArrayInputStream(createMode1Input.getBytes());
		Scanner testScanner = new Scanner(input);
		ControllerMenu testMenu = new ControllerMenu(testScanner);

		
	}
	
	@Test
	public void testClientSimInput() {
		String createMode2Input = new String("2" + System.lineSeparator() + "127.0.0.1"
				+ System.lineSeparator() + "7000" + System.lineSeparator()  
				+ System.lineSeparator() + "127.0.0.1" + System.lineSeparator() + "6969"
				+ System.lineSeparator() + "1");
		InputStream input = new ByteArrayInputStream(createMode2Input.getBytes());
		Scanner testScanner = new Scanner(input);
		ControllerMenu testMenu = new ControllerMenu(testScanner);
	}
	
	
	@Test
	public void testMultipleClients() {
		InetSocketAddress serverAddr = new InetSocketAddress(InetAddress.getLoopbackAddress(),6969);
		InetSocketAddress clientAddr = new InetSocketAddress(InetAddress.getLoopbackAddress(),7000);
		ServerSocket servSock = null;
		
		try {
			servSock = new ServerSocket();
			servSock.bind(serverAddr);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		TransponderTCP testServer = new TransponderTCP(servSock);

		Payload testPayload = new Payload(8,"REIN");

		testServer.setDebugFlag(true);
		testServer.setInitServerMessage(testPayload);
		
		Thread transpThread = new Thread(testServer);
		transpThread.start();
		
		TransponderTCP testClient = new TransponderTCP(2);
		testClient.setDebugFlag(true);

		for(int i = 0; i < 10; i++) {
			
			int incPort = clientAddr.getPort() + i;
			
			InetSocketAddress clientAddrInc = new InetSocketAddress(InetAddress.getLoopbackAddress(),incPort);

			Socket clientSock = null;
			
			clientSock = new Socket();

			try {

				clientSock.bind(clientAddrInc);
				clientSock.connect(serverAddr);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// add testClient tClientTCP's via clientSock constructor
			testClient.addClient(clientSock);
		}
		Thread clientThread = new Thread(testClient);
		clientThread.start();
		
		System.out.println("*---testClient status---*\n" + testClient.getStatus());

	}
	
	@Test
	public void testSingleTranspCliServ() {
		InetSocketAddress serverAddr = new InetSocketAddress(InetAddress.getLoopbackAddress(),6969);
		InetSocketAddress clientAddr = new InetSocketAddress(InetAddress.getLoopbackAddress(),7000);
		ServerSocket servSock = null;
		Socket clientSock = new Socket();

		
		ClientMessage<String> testMessage1 = new ChatMessage("Test1");
		
		ClientMessage<String> testMessage2 = new ChatMessage("Test2");
		
		ClientMessage<String> testMessage3 = new ChatMessage("Test3");
		
		ClientMessage<String> testMessage4 = new ChatMessage("Test4");
		

		ServerMessage<?> testPayload = new Payload(7,"SIGMA");
		ServerMessage<?> testPayload2 = new Payload(8,"Large Fries");
		
		// setup server socket, then client socket
		try {
			servSock = new ServerSocket();
			servSock.bind(serverAddr);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			clientSock.setReuseAddress(true);
			clientSock.bind(clientAddr);
			clientSock.connect(serverAddr);
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		// initialize server transponder
		TransponderTCP testTranspServ = new TransponderTCP(1,true);

		testTranspServ.setDebugFlag(true);
		testTranspServ.setInitServerMessage(testPayload);
		testTranspServ.setServerSocket(servSock);
		
		Thread transpServThread = new Thread(testTranspServ);
		
		transpServThread.start();
		
		
		// initialize client transponder
		TransponderTCP testTranspCli = new TransponderTCP(2, true);
		
		testTranspCli.addClient(clientSock);
		
		Thread transpCliThread = new Thread(testTranspCli);
		
		transpCliThread.start();

		
		testTranspServ.sendClientMessageToAll(testMessage1);
		testTranspServ.sendClientMessageToAll(testMessage2);
		
		testTranspCli.sendClientMessageToAll(testMessage3);
		testTranspCli.sendClientMessageToAll(testMessage4);
		
		testTranspServ.sendServerMessageToAll(testPayload2);
		
		System.out.println("Client Master ClientMessage list contains the following: \n" + testTranspCli.getMasterCliMsg().toString()+ "\n");
		System.out.println("Client Master ServerMessage list contains the following: \n" + testTranspCli.getMasterServMsg().toString()+ "\n");
		System.out.println("Server Master ClientMessage list contains the following: \n " + testTranspServ.getMasterCliMsg().toString()+ "\n");
		System.out.println("Server Master ServerMessage list contains the following: \n" + testTranspServ.getMasterServMsg().toString()+ "\n");
		
	}
	
	@Test
	public void testMultipleTranspCliServ() {
		
		InetSocketAddress serverAddr = new InetSocketAddress(InetAddress.getLoopbackAddress(),6969);
		InetSocketAddress clientAddr = new InetSocketAddress(InetAddress.getLoopbackAddress(),7000);
		
		ServerSocket servSock = null;
		
		ClientMessage<String> testMessage1 = new ChatMessage("Test1");
		
		ClientMessage<String> testMessage2 = new ChatMessage("Test2");
		
		ClientMessage<String> testMessage3 = new ChatMessage("Test3");
		
		ClientMessage<String> testMessage4 = new ChatMessage("Test4");
		
		// setup client and server sockets
		try {
			servSock = new ServerSocket();
			servSock.bind(serverAddr);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

		
		
		TransponderTCP testTranspServ = new TransponderTCP(servSock);

		Payload testPayload = new Payload(7,"SIGMA");
		Payload testPayload2 = new Payload(8,"Large Fries");
		
		testTranspServ.setInitServerMessage(testPayload);
		
		Thread transpThread = new Thread(testTranspServ);
		
		transpThread.start();
		
		TransponderTCP testTranspCli = new TransponderTCP(2);

		for(int i = 0; i < 10; i++) {
			
			int incPort = clientAddr.getPort() + i;
			
			InetSocketAddress clientAddrInc = new InetSocketAddress(InetAddress.getLoopbackAddress(),incPort);

			Socket clientSock = new Socket();

			try {
				clientSock.setReuseAddress(true);
				clientSock.bind(clientAddrInc);
				clientSock.connect(serverAddr);
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			testTranspCli.addClient(clientSock);

		}
		
		testTranspServ.setDebugFlag(true);
		testTranspCli.setDebugFlag(true);

		Thread transpClientThread = new Thread(testTranspCli);
		transpClientThread.start();
		
		testTranspCli.sendClientMessageToAll(testMessage1);
		testTranspCli.sendClientMessageToAll(testMessage2);
		testTranspCli.sendClientMessageToAll(testMessage3);
		
		testTranspServ.sendClientMessageToAll(testMessage4);

		
		testTranspServ.sendServerMessageToAll(testPayload2);

		Comparator<ClientMessage<?>> dateCompCM = new MessageDateComparatorCM();
		Comparator<ServerMessage<?>> dateCompSM = new MessageDateComparatorSM();

		
		System.out.println("Test Server recieved the following ClientMessages:\n" + testTranspServ.getReceivedCMsOrdered(dateCompCM));
		System.out.println("Test Client recieve the following ServerMessages:\n" + testTranspCli.getReceivedSMsOrdered(dateCompSM));
		System.out.println("Test Client recieve the following ClientMessages:\n" + testTranspCli.getReceivedCMsOrdered(dateCompCM));


	}
	
}
