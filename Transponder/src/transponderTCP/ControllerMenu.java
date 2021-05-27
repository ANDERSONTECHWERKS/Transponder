package transponderTCP;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class ControllerMenu {
	private static ControllerMenu mainMenu = null;
	private int mode = 0;
	private TransponderTCP currTransponder = null;
	private Scanner inputScanner = null;
	private boolean debugFlag = false;
	private Thread transponderThread = null;
	
	public ControllerMenu() {
		if(this.inputScanner == null) {
			this.inputScanner = new Scanner(System.in);
		}
		this.mode = this.promptModeSetting(inputScanner);
		// Mode 1 is server-only
		if (this.mode == 1) {
			ServerSocket mode1ServSock = this.promptServerSocket(inputScanner);
			this.currTransponder = new TransponderTCP(1,mode1ServSock,mode1ServSock.getLocalSocketAddress());
		
			
			// After creating the transponder, 
			// prompt and initialize the payload.
			Payload initPayload = this.promptPayload(inputScanner);
			this.currTransponder.setInitialServerPayload(initPayload);
			
			// Debug prompt and set
			this.debugFlag = this.promptDebugFlag(inputScanner);
			if(this.debugFlag == true) {
				this.currTransponder.setDebugFlag(true);
			}
			
			this.currTransponder.run();

		}

		// Mode 2 is client-only
		if (this.mode == 2) {
			Socket mode2Sock = this.promptClientSocket(inputScanner);
			this.currTransponder = new TransponderTCP(2,mode2Sock, mode2Sock.getRemoteSocketAddress());
			
			// Debug prompt and set
			this.debugFlag = this.promptDebugFlag(inputScanner);
			if(this.debugFlag == true) {
				this.currTransponder.setDebugFlag(true);
			}
			this.currTransponder.run();

		}
	}

	public ControllerMenu(Scanner altScanner) {
		this.inputScanner = altScanner;
		this.mode = this.promptModeSetting(altScanner);
		// Mode 1 is server-only
		if (this.mode == 1) {
			// Create Server socket, use it in the constructor for currTransponder
			ServerSocket mode1ServSock = this.promptServerSocket(this.inputScanner);
			this.currTransponder = new TransponderTCP(1,mode1ServSock,mode1ServSock.getLocalSocketAddress());
			
			// After creating the transponder, 
			// prompt and initialize the payload.
			Payload initPayload = this.promptPayload(inputScanner);
			this.currTransponder.setInitialServerPayload(initPayload);
			
			// Debug prompt and set
			this.debugFlag = this.promptDebugFlag(inputScanner);
			if(this.debugFlag == true) {
				this.currTransponder.setDebugFlag(true);
			}
			
			// Create Transponder thread and start it
			Thread transponderThread = new Thread(this.currTransponder);
			this.transponderThread = transponderThread;
			transponderThread.start();
		}

		// Mode 2 is client-only
		if (this.mode == 2) {
			// Create client socket, use it in the constructor for currTransponder
			Socket mode2Sock = this.promptClientSocket(this.inputScanner);
			this.currTransponder = new TransponderTCP(2,mode2Sock, mode2Sock.getRemoteSocketAddress());

			// Debug prompt and set
			this.debugFlag = this.promptDebugFlag(inputScanner);

			if(this.debugFlag == true) {
				this.currTransponder.setDebugFlag(true);
			}

			// Create Transponder thread and start it
			Thread transponderThread = new Thread(this.currTransponder);
			this.transponderThread = transponderThread;
			transponderThread.start();
		}
	}

	public static void main(String[] args) {
		// Begin with greeting
		System.out.println(ControllerMenu.controllerGreeting());

		// Prompt for initial Transponder
		mainMenu = new ControllerMenu();
		mainMenu.controllerCMD(mainMenu.getScanner());
	}

	public static String controllerGreeting() {
		String greeting = "---TransponderTCP---\n";
		return greeting;
	}

	public void controllerCMD(Scanner userInput) {
		boolean stopFlag = false;

		while(stopFlag == false) {
			System.out.println("---COMMANDS---");
			String[] commandList = {"1. Status","2. Configure and Start Transponder",
					"3. Stop and Clear Transponder", "4. Exit"};

			// Iterate through options, printing each out.
			for(String currString : commandList) {
				System.out.println(currString);
			}

			int userChoice = userInput.nextInt();

			switch (userChoice) {

			case 1:
				// Condition where currTransponder has been cleared
				if(this.currTransponder == null) {
					System.out.println("Transponder Stopped!");
				} else {
					// If currTransponder is present: Get the status
					System.out.println(this.currTransponder.getStatus());
				}
				break;

			case 2:

				if(this.currTransponder == null) {
					
					// If we have cleared the currTransponder (or it never existed)
					// We reused the method from when we initially created the 
					// controllerMenu.
					// The fact that I had to copy/paste this means I should have 
					// designed this better...
					this.mode = this.promptModeSetting(inputScanner);
					// Mode 1 is server-only
					if (this.mode == 1) {
						ServerSocket mode1ServSock = this.promptServerSocket(inputScanner);
						this.currTransponder = new TransponderTCP(1,mode1ServSock,mode1ServSock.getLocalSocketAddress());
						
						// After creating the transponder, 
						// prompt and initialize the payload.
						Payload initPayload = this.promptPayload(inputScanner);
						this.currTransponder.setInitialServerPayload(initPayload);
						
						// Debug prompt and set
						this.debugFlag = this.promptDebugFlag(inputScanner);
						if(this.debugFlag == true) {
							this.currTransponder.setDebugFlag(true);
						}
					}

					// Mode 2 is client-only
					if (this.mode == 2) {
						Socket mode2Sock = this.promptClientSocket(inputScanner);
						this.currTransponder = new TransponderTCP(2,mode2Sock, mode2Sock.getRemoteSocketAddress());
						
						// Debug prompt and set
						this.debugFlag = this.promptDebugFlag(inputScanner);
						if(this.debugFlag == true) {
							this.currTransponder.setDebugFlag(true);
						}
					}
				} else if (this.currTransponder instanceof TransponderTCP) {
					System.out.println("Transponder already running!");
					System.out.println("Stop and Clear Transponder before"
							+ " attempting to start a new one.");
				}
				break;

			case 3:
				if(this.currTransponder == null) {
					System.out.println("Transponder not started! Nothing to stop!");
					System.out.println("Please Configure and Start Transponder!");
				} else if(this.currTransponder instanceof TransponderTCP) {
					this.currTransponder.stop();
					this.currTransponder = null;
				}
				break;

			case 4:
				mainMenu.closeScanner();
				stopFlag = true;
				break;
			}
		}
	}

	// reqServerAddrIPV4TCP:
	// Requests (from the user) the IPV4 ServerSocket information
	// required to instantiate the ServerSocket object.
	public ServerSocket promptServerSocket(Scanner keyboardInput) {
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
		System.out.println("Input connection backlog value on local server:\n");
		backlogInput = keyboardInput.nextInt();

		// Create InetAddress object using constructor
		// With string as the input
		try {
			serverAddress = InetAddress.getByName(serverInput);
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}

		// Create server socket by passing in
		// The InetAddress object,backlog int, socket int
		// Set socket options here
		// TODO: Future feature: Method that sets socket options
		try {
			serverSocket = new ServerSocket(portInput, backlogInput, serverAddress);
			serverSocket.setReuseAddress(true);

		} catch (IOException e) {
			e.printStackTrace();
		}

		return serverSocket;
	}
	
	public void setDebugFlag(Boolean bool) {
		if(this.currTransponder == null) {
			
		}
	}


	public int promptModeSetting(Scanner keyboardInput) {
		// Simply prompt and return mode setting
		// TODO: Set boundary integers for modes (1 through 2 only)

		int modeInput;

		System.out.println("Set Mode: \n");
		System.out.println("1 - Server\n2 - Client\n");
		modeInput = keyboardInput.nextInt();

		// While loop that handles invalid input
		while (modeInput > 2 || modeInput < 1) {
			System.out.println("Invalid input. Please choose from the following options: \n");
			System.out.println("1 - Server\n2 - Client\n");
			modeInput = keyboardInput.nextInt();
		}
		return modeInput;
	}

	// This method takes user input and creates a Socket intended to be used
	// for mode 2 (client operation) with a TransponderTCP object
	public Socket promptClientSocket(Scanner userInput) {
		// TODO: Create null-checkers
		// TODO: Create debugFlag boolean flag and related output

		Socket clientSocket = null;
		InetAddress clientRemoteAddr = null;
		InetAddress clientLocalAddr = null;
		int clientRemoteSocket;
		int clientLocalSocket;

		// Request the address on the local machine that 
		// we want to send data from, including the port number
		// Store as clientLocalAddr and clientLocalSocket.
		System.out.println("Input Local Client Address: \n");
		try {
			clientLocalAddr = InetAddress.getByName(userInput.next());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		System.out.println("Input Local Client Socket:\n");
		clientLocalSocket = userInput.nextInt();

		// Request the address for the remote machine that we want
		// to connect to, store as clientRemoteAddr.
		System.out.println("Input Remote Server Address:\n");
		try {
			clientRemoteAddr = InetAddress.getByName(userInput.next());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		System.out.println("Input Remote Server Socket:\n");
		clientRemoteSocket = userInput.nextInt();

		// Attempt creating a new Socket object using clientRemoteaddr,
		// clientRemoteSocket.
		try {
			clientSocket = new Socket(clientRemoteAddr, clientRemoteSocket, clientLocalAddr, clientLocalSocket);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return clientSocket;
	}
	
	public boolean promptDebugFlag(Scanner userInput) {
		System.out.println("Debug mode? \n");
		System.out.println("1 - ON\n2 - OFF");
		int response = userInput.nextInt();
		if(response == 1) {
			return true;
		}
		
		if(response == 2) {
			return false;
		}
		return false;
	}
	// prompyPayload will prompt develop a payload via user input
	// and return the Payload object
	// This will likely change as Transponder is developed further

	public Payload promptPayload(Scanner userInput) {

		if(this.currTransponder == null) {
			throw new IllegalStateException("Payload not set! reqPayload failed!");
		}

		System.out.println("Please enter the name of this Payload:");
		String payloadTitle = userInput.next();

		System.out.println("Please enter the serial number of this Payload:");
		int payloadNumber = userInput.nextInt();
		
		Payload payload = new Payload(payloadNumber,payloadTitle);
		
		return payload;
	}

	public Scanner getScanner() {
		if(this.inputScanner == null) {
			throw new IllegalStateException("Scanner not initialized!");
		} else {
			return this.inputScanner;
		}
	}

	public void closeScanner() {
		this.inputScanner.close();
	}
	

}
