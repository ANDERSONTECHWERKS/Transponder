package transponderTCP;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import org.junit.Assert;
import org.junit.Test;

import junit.framework.TestCase;


public class Tests extends TestCase{


	
	@Test
	public void testMode1TransponderTCP() {
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

			assertTrue(dObj.evaluatePayloadEquivilance());
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
