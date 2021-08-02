package transponder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Scanner;

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
			testTranspServer = new TransponderTCP(1,servSock,serverSockAddr);
			testTranspServer.setInitialServerPayload(testPayload);
			testTranspServer.setDebugFlag(true);
			testTranspServer.setDebugObject(dObj);
			testTranspServer.run();
			
			clientSock.bind(clientSockAddr);
			testTranspClient = new TransponderTCP(2,clientSock,serverSockAddr);
			testTranspClient.setDebugFlag(true);
			testTranspClient.setDebugObject(dObj);
			testTranspClient.run();

			assertTrue(dObj.evaluatePayloadEquivalanceMulti());
			System.out.println("dObj evaluatePayloadEquivalence result: " + dObj.evaluatePayloadEquivalanceMulti());
			
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

		tServerTCP testServer = null;
		
		try {
			testServer = new tServerTCP(serverSock.accept());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		testServer.setOutgoingPayload(testPayload);
		
		Thread serverThread = new Thread(testServer);
		
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
		
		Payload testPayload = testClient.getPayload();
		
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

		tServerTCP testServer = null;
		
		testServer = new tServerTCP();
		
		testServer.setOutgoingPayload(testPayload);
		testServer.setDebugFlag(true);
		testServer.setDebugObj(debugger);
		
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
		
		testServer.buildSocket(localSock.toString(), localSock.getLocalPort(),serverAddr.toString(), serverAddr.getPort());

		Thread serverThread = new Thread(testServer);
		Thread clientThread = new Thread(testClient);

		serverThread.start();
		clientThread.start();

		boolean payloadsMatch = debugger.evaluatePayloadEquivalanceMulti();

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
	public void testTransponderServer() {
		
		InetSocketAddress serverAddr = new InetSocketAddress(InetAddress.getLoopbackAddress(),6969);
		ServerSocket serverSock = null;
		
		// Try instantiating serverSock
		try {
			 serverSock = new ServerSocket(6969, 1, serverAddr.getAddress());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		TransponderTCP currTransponder = new TransponderTCP(1, serverSock, serverSock.getLocalSocketAddress());

		Payload testPayload = new Payload(5,"RED");


		currTransponder.setInitialServerPayload(testPayload);


		// Create Transponder thread assign to transponderThread field
		Thread transponderThread = new Thread(currTransponder);
		currTransponder.setDebugFlag(true);

		transponderThread.start();
	}
	
	@Test
	public void testTransponderClient() {
		Socket mode2Sock = null;
		
		try {
			mode2Sock = new Socket(Inet4Address.getLoopbackAddress(),6969, Inet4Address.getLoopbackAddress(),7000);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		TransponderTCP currTransponder = new TransponderTCP(2, mode2Sock, mode2Sock.getRemoteSocketAddress());
		currTransponder.setDebugFlag(true);

		// Create Transponder thread assign to transponderThread field
		Thread transponderThread = new Thread(currTransponder);

		transponderThread.start();
	}
	
	@Test
	public void testTransponderClientServer() {
		this.testTransponderServer();
		this.testTransponderClient();
	}
	
	@Test
	public void testMultipleClients() {
		InetSocketAddress serverAddr = new InetSocketAddress(InetAddress.getLoopbackAddress(),6969);
		InetSocketAddress clientAddr = new InetSocketAddress(InetAddress.getLoopbackAddress(),7000);

		HashSet<tClientTCP> clientSet = new HashSet<tClientTCP>();
		HashSet<Thread> clientThreadSet = new HashSet<Thread>();

		for(int i = 0; i < 10; i++) {
			
			int incPort = clientAddr.getPort() + i;
			
			InetSocketAddress clientAddrInc = new InetSocketAddress(InetAddress.getLoopbackAddress(),incPort);

			tClientTCP newClient = new tClientTCP();
			
			newClient.setLocalSocketAddress(clientAddrInc);
			newClient.setRemoteSocketAddress(serverAddr);
			
			newClient.generateClientSignOn(clientAddrInc.getAddress(), serverAddr.getAddress());
			newClient.generateClientSignOff(clientAddrInc.getAddress(), serverAddr.getAddress());
			newClient.setDebugFlag(true);
			
			clientSet.add(newClient);
			
		}
		
		for(tClientTCP currClient : clientSet) {
			Thread newThread = new Thread(currClient);
			clientThreadSet.add(newThread);
		}
		
		Payload testPayload = new Payload(7,"SIGMA");
		tServerTCP testServ = new tServerTCP();
		
		testServ.setLocalAddr(serverAddr);
		testServ.setOutgoingPayload(testPayload);
		testServ.setDebugFlag(true);
		
		
		Thread servThread = new Thread(testServ);
		
		servThread.start();
		
		for(Thread currThread : clientThreadSet) {
			currThread.start();
		}
	}
}
