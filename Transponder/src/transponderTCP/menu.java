package transponderTCP;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class menu {
	int mode = 0;
	TransponderTCP currTransponder = null;
	Scanner inputScanner = new Scanner(System.in);

	
	public menu() {
		this.mode = this.reqModeSetting(inputScanner);
		// Mode 1 is server-only
		if (this.mode == 1) {
			ServerSocket mode1ServSock = this.reqServerAddrIPV4TCP(inputScanner);
			this.currTransponder = new TransponderTCP(1,mode1ServSock,mode1ServSock.getLocalSocketAddress());
		}

		// Mode 2 is client-only
		if (this.mode == 2) {
			Socket mode2Sock = this.reqClientAddrIPV4TCP(inputScanner);
			this.currTransponder = new TransponderTCP(2,mode2Sock, mode2Sock.getRemoteSocketAddress());
		}

		// Mode 3 is intended to be a server that connects to other clients.
		// Requires further development to make servers communicate in client/slave modes.
		//TODO: Mode 3 is completely unfinished. Only creates a transponderTCP that doesn't have any clients.
		if (this.mode == 3) {
			System.out.println("Mode 3 coming soon!");
//			ServerSocket mode3ServSock = this.reqServerAddrIPV4TCP(inputScanner);
//			Socket mode2ClientSock = this.reqClientAddrIPV4TCP(inputScanner);
//			this.currTransponder = new TransponderTCP(3,mode3ServSock,mode3ServSock.getLocalSocketAddress());
		}
	}

	public static void main(String[] args) {
		menu mainMenu = new menu();
		mainMenu.closeScanner();
	}

	public ServerSocket reqServerAddrIPV4TCP(Scanner keyboardInput) {
		// TODO: Create checks for appropriate inputs
		// TODO: Create null-checks
		// Create a scanner for use in gathering input
		ServerSocket serverSocket = null;
		InetAddress serverAddress = null;
		int portInput;
		int backlogInput;
		// Prompt user for server address and socket,
		// Create relevant variables for each

		System.out.println("Input Local Server Address:\n");
		String serverInput = keyboardInput.next();
		System.out.println("Input Local Server Socket:\n");
		portInput = keyboardInput.nextInt();
		System.out.println("Input MAXIMUM number of pending connections on local server:\n");
		backlogInput = keyboardInput.nextInt();

		// Create InetAddress object using constructor
		// With string as the input
		try {
			serverAddress = InetAddress.getByName(serverInput);
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// Create server socket by passing in
		// The InetAddress object,backlog int, socket int
		try {
			serverSocket = new ServerSocket(portInput, backlogInput, serverAddress);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return serverSocket;
	}

	public int reqModeSetting(Scanner keyboardInput) {
		// Simply prompt and return mode setting
		// TODO: Set boundary integers for modes (1 through 3 only)

		int modeInput;

		System.out.println("Set Mode: ");
		System.out.println("1 - Server\n2 - Client\n3 - Server/Client\n");
		modeInput = keyboardInput.nextInt();

		// While loop that handles invalid input
		while (modeInput > 3 || modeInput < 1) {
			System.out.println("Invalid input. Please choose from the following options: \n");
			System.out.println("1 - Server\n2 - Client\n3 - Server/Client\n");
			modeInput = keyboardInput.nextInt();
		}

		return modeInput;
	}

	public Socket reqClientAddrIPV4TCP(Scanner keyboardInput) {
		// TODO: Create null-checkers

		Socket clientSocket = null;
		InetAddress clientRemoteAddr = null;
		int clientRemoteSocket;

		System.out.println("Input Remote Server Address:\n");
		// Try creating a InetAddress object via user input
		try {
			clientRemoteAddr = InetAddress.getByName(keyboardInput.next());
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("Input Remote Server Socket:\n");
		clientRemoteSocket = keyboardInput.nextInt();
		keyboardInput.close();

		// Attempt creating a new Socket object using clientRemoteaddr,
		// clientRemoteSocket.
		try {
			clientSocket = new Socket(clientRemoteAddr, clientRemoteSocket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return clientSocket;
	}

	public void closeScanner() {
		this.inputScanner.close();
	}
}
