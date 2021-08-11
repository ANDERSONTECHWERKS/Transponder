package transponderTCP;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Scanner;

public class ControllerMenu {

	private static ControllerMenu mainMenu = null;
	private int mode = 0;
	private TransponderTCP currTransponder = null;
	private Scanner inputScanner = null;
	private boolean debugFlag = false;
	private boolean stopFlag = false;
	private Thread transponderThread = null;

	public ControllerMenu() {

		if (this.inputScanner == null) {

			this.inputScanner = new Scanner(System.in);

		}

		this.mode = this.promptModeSetting(inputScanner);

		// Mode 1 is server-only
		if (this.mode == 1) {

			ServerSocket mode1ServSock = this.promptServerSocket(inputScanner);

			this.currTransponder = new TransponderTCP(mode1ServSock, this);

			// After creating the transponder,
			// prompt and initialize the payload.
			ServerMessage<?> initServMessage = this.promptServerMessage(inputScanner);

			this.currTransponder.setInitServerMessage(initServMessage);

			// Debug prompt and set
			this.debugFlag = this.promptDebugFlag(inputScanner);

			if (this.debugFlag == true) {
				this.currTransponder.setDebugFlag(true);
			}

			// Create Transponder thread assign to transponderThread field
			Thread transponderThread = new Thread(this.currTransponder);

			this.transponderThread = transponderThread;

			// Set this object to be the static mainMenu
			mainMenu = this;
			mainMenu.controllerCMD(mainMenu.getScanner());

		}

		// Mode 2 is client-only
		if (this.mode == 2) {

			Socket mode2Sock = this.promptClientSocket(inputScanner);

			this.currTransponder = new TransponderTCP(mode2Sock);

			// Debug prompt and set
			this.debugFlag = this.promptDebugFlag(inputScanner);

			if (this.debugFlag == true) {

				this.currTransponder.setDebugFlag(true);

			}

			// Create Transponder thread assign to transponderThread field
			Thread transponderThread = new Thread(this.currTransponder);

			this.transponderThread = transponderThread;

			// Set this object to be the static mainMenu
			mainMenu = this;
			mainMenu.controllerCMD(mainMenu.getScanner());

		}
	}

	// This constructor intended for testing purposes. May not be updated regularly!
	public ControllerMenu(Scanner altScanner) {

		this.inputScanner = altScanner;
		this.mode = this.promptModeSetting(altScanner);

		// Mode 1 is server-only
		if (this.mode == 1) {

			// Create Server socket, use it in the constructor for currTransponder
			ServerSocket mode1ServSock = this.promptServerSocket(this.inputScanner);

			this.currTransponder = new TransponderTCP(mode1ServSock, this);

			// After creating the transponder,
			// prompt and initialize the payload.
			ServerMessage<?> initServMessage = this.promptServerMessage(inputScanner);

			this.currTransponder.setInitServerMessage(initServMessage);

			// Debug prompt and set
			this.debugFlag = this.promptDebugFlag(inputScanner);

			if (this.debugFlag == true) {
				this.currTransponder.setDebugFlag(true);
			}

			// Create Transponder thread assign to transponderThread field
			Thread transponderThread = new Thread(this.currTransponder);
			this.transponderThread = transponderThread;
		}

		// Mode 2 is client-only
		if (this.mode == 2) {
			// Create client socket, use it in the constructor for currTransponder
			Socket mode2Sock = this.promptClientSocket(this.inputScanner);
			this.currTransponder = new TransponderTCP(mode2Sock);

			// Debug prompt and set
			this.debugFlag = this.promptDebugFlag(inputScanner);

			if (this.debugFlag == true) {
				this.currTransponder.setDebugFlag(true);
			}

			// Create Transponder thread assign to transponderThread field
			Thread transponderThread = new Thread(this.currTransponder);
			this.transponderThread = transponderThread;
		}
	}

	public static void main(String[] args) {
		// Begin with greeting
		System.out.println(ControllerMenu.controllerGreeting());

		// Create ControllerMenu instance, which also prompts for mode/debug/related
		// fields
		mainMenu = new ControllerMenu();
		mainMenu.controllerCMD(mainMenu.getScanner());
	}

	public static String controllerGreeting() {
		String greeting = "---TransponderTCP---\n";
		return greeting;
	}

	// controllerCMD contains the actual application engine via while loop
	public void controllerCMD(Scanner userInput) {
		
		ClientMessage<?> clientMess = null;
		ServerMessage<?> serverMess = null;

		// TODO: Fix menu object IAW their related menu items
		while (stopFlag == false) {
			
			

			System.out.println("---COMMANDS---");

			ArrayList<String> commandList = new ArrayList<String>();
			
			commandList.add("1. Transponder Status");
			commandList.add("2. Configure Transponder");
			commandList.add("3. Start Transponder");
			commandList.add("4. Stop Transponder");
			commandList.add("5. Exit");
			
			// Additional transponder options for an already-running transponder 
			// in mode 1 or 2
			
			// Server-mode additional options
			if(this.mode == 1) {
				commandList.add("6. Print client(s) information");
				commandList.add("7. Set ClientMessage Content");
				commandList.add("8. Set ServerMessage Content");
				commandList.add("9. Send ClientMessage to all clients");
				commandList.add("10. Send ServerMessage to all clients");
			}
			
			// Client-mode additional options
			if(this.mode == 2) {
				commandList.add("6. Print server information");
				commandList.add("7. Set ClientMessage Content");
				commandList.add("8. Set ServerMessage Content");
				commandList.add("9. Send ClientMessage to all clients");
				commandList.add("10. Send ServerMessage to all clients");
				commandList.add("11. Send ClientMessage to all servers");

			}

			// Iterate through options, printing each out.
			for (String currString : commandList) {
				System.out.println(currString);
			}

			int userChoice = userInput.nextInt();

			switch (userChoice) {

			case 1:
				// case 1: Transponder Status

				// Condition where currTransponder has been cleared
				if (this.currTransponder == null) {

					System.out.println("Transponder Stopped!");

				} else {

					System.out.println("---THREADSTATUS---");

					// If transponderThread is null, consider that stopped
					if (this.transponderThread == null) {
						System.out.println("Transponder Stopped!");
					}

					if (this.transponderThread.getState().equals(Thread.State.RUNNABLE)) {
						System.out.println("Transponder Thread is running!");
						
					} else if (this.transponderThread.getState().equals(Thread.State.BLOCKED)) {
						System.out.println("Transponder Thread is blocking!");
						
					} else {
						System.out.println("Transponder Thread state:" + this.transponderThread.getState().toString());
					}

					// If currTransponder is present: Get the status
					System.out.println(this.currTransponder.getStatus());
				}

				break;

			case 2:
				// case 2: Configure Transponder

				if (this.currTransponder == null) {

					// If we have cleared the currTransponder (or it never existed)
					// We reused the method from when we initially created the
					// controllerMenu.
					// The fact that I had to copy/paste this means I should have
					// designed this better...

					this.mode = this.promptModeSetting(inputScanner);

					// Mode 1 is server-only
					if (this.mode == 1) {

						ServerSocket mode1ServSock = this.promptServerSocket(inputScanner);

						this.currTransponder = new TransponderTCP(mode1ServSock, this);

						// After creating the transponder,
						// prompt and initialize the payload.
						ServerMessage<?> initServerMessage = this.promptServerMessage(inputScanner);

						this.currTransponder.setInitServerMessage(initServerMessage);

						// Debug prompt and set
						this.debugFlag = this.promptDebugFlag(inputScanner);

						// Set debug TRUE, if applicable
						if (this.debugFlag == true) {

							this.currTransponder.setDebugFlag(true);

						}

						// Set debug FALSE, if applicable
						if (this.debugFlag == false) {

							this.currTransponder.setDebugFlag(false);

						}
					}

					// Mode 2 is client-only
					if (this.mode == 2) {

						Socket mode2Sock = this.promptClientSocket(inputScanner);

						this.currTransponder = new TransponderTCP(mode2Sock,this);

						// Debug prompt
						this.debugFlag = this.promptDebugFlag(inputScanner);

						// Set debug TRUE, if applicable
						if (this.debugFlag == true) {

							this.currTransponder.setDebugFlag(true);

						}

						// Set debug FALSE, if applicable
						if (this.debugFlag == false) {

							this.currTransponder.setDebugFlag(false);

						}
					}

				} else if (this.currTransponder instanceof TransponderTCP) {
					System.out.println("Transponder already running!");
					System.out.println("Stop and Clear Transponder before attempting to start a new one.");
				}
				break;

			case 3:
				// case 3: Start Transponder

				// Condition where currTransponder does not exist
				if (this.currTransponder == null) {
					System.out.println("Transponder not instantiated! Please configure transponder before running!");
				}

				// If we have an instance of TransponderTCP and the state is anything *except*
				// NEW, do nothing and alert user
				if (this.currTransponder instanceof TransponderTCP
						&& !(this.transponderThread.getState() == Thread.State.NEW)) {

					System.out.println("Transponder not in a runnable state! Current thread state is: "
							+ this.transponderThread.getState().toString()
							+ " \n Please Stop and clear transponder before trying to start a new instance!");
				}

				// If we have an instance of TransponderTCP and the state is NEW - start the
				// thread!
				if (this.currTransponder instanceof TransponderTCP
						&& this.transponderThread.getState() == Thread.State.NEW) {
					System.out.println("Transponder starting!");

					// set stopFlag false, since we check for a new transponderThread instance.
					// Start thread.
					this.stopFlag = false;
					this.transponderThread.start();

					// debug output if debugFlag set to TRUE
					if (this.debugFlag == true) {
						System.out.println("ControllerMenu| Thread information:\n Thread state: "
								+ this.transponderThread.getState().toString());
					}

				}

				break;

			case 4:
				// case 4: Stop Transponder

				if (this.currTransponder == null) {
					System.out.println("Transponder not started! Nothing to stop!");
					System.out.println("Please Configure and Start Transponder!");

				} else if (this.currTransponder instanceof TransponderTCP) {
					this.currTransponder.stopAll();
					this.currTransponder = null;
				}
				
				break;

			case 5:
				// case 5: Exit (gracefully)

				mainMenu.closeScanner();
				stopFlag = true;

				// Check if the current transponder has been cleared. If not - stop it, then
				// clear.
				// I know clearing it seems ridiculous right now, as this is the end of the
				// program at
				// the time of writing - but I'm just gonna get into good
				// data/connection-oriented habits right now.

				if (this.currTransponder != null) {
					this.currTransponder.stopAll();
				}
				break;
				
			case 6:
				// case 6: print information about underlying client/server status
				if(this.mode == 0) {
					System.out.println("This option not relevant for mode 0");
				}
				
				if(this.mode == 1) {
					System.out.println(this.currTransponder.getServerStatus());
				}
				
				if(this.mode == 2) {
					System.out.println(this.currTransponder.getClientStatus());
				}

				break;
				
			case 7:
				// case 7: Set ClientMessage Content
				Scanner userInput2 = new Scanner(System.in);

				if(this.mode == 0) {
					System.out.println("This option not relevant for mode 0");
				}
				
				
				if(this.mode == 1) {
					System.out.println("Enter text to be used in the ClientMessage (ChatMessage):");
					String cm = userInput2.nextLine();
					clientMess = new ChatMessage(cm);
					System.out.println("Created ChatMessage:\n" + cm.toString());
				}
				
				if(this.mode == 2) {
					System.out.println("Enter text to be used in the ClientMessage (ChatMessage):");
					String cm = userInput2.nextLine();
					clientMess = new ChatMessage(cm);
					System.out.println("Created ChatMessage:\n" + cm.toString());
				}
				break;
				
			case 8:
				Scanner userInput3 = new Scanner(System.in);
				
				//"8. Set ServerMessage Content"
				if(this.mode == 0) {
					System.out.println("This option not relevant for mode 0");
				}
				
				if(this.mode == 1) {
					System.out.println("Enter text to be used in the ServerMessage (Payload):");
					String sm = userInput3.nextLine();
					serverMess = new Payload(1,sm);
					System.out.println("Created Payload:\n" + sm.toString());
				}
				
				if(this.mode == 2) {
					System.out.println("This option not relevant for mode 2");
				}
				break;
			case 9:
				//9. Send ClientMessage to all clients
				if(this.mode == 0) {
					System.out.println("This option not relevant for mode 0");
				}
				
				if(this.mode == 1) {
					if(clientMess == null) {
						System.out.println("ClientMessage was never set! Returning to menu.");
						break;
					} else {
						if(this.debugFlag == true) {
							System.out.println("Sending ClientMessage to all clients!");
						}
						this.currTransponder.sendClientMessageToAll(clientMess);
					}
				}
				
				if(this.mode == 2) {
					if(clientMess == null) {
						System.out.println("ClientMessage was never set! Returning to menu.");
						break;
					} else {
						if(this.debugFlag == true) {
							System.out.println("Sending ClientMessage to all servers!");
						}
						this.currTransponder.sendClientMessageToAll(clientMess);
					}
				}
				break;
			
			case 10:
				// 10. Send ServerMessage to all clients
				
				if(this.mode == 0) {
					System.out.println("This option not relevant for mode 0");
				}
				
				if(this.mode == 1) {
					
					if(serverMess == null) {
						System.out.println("ServerMessage was never set! Returning to menu.");
						break;
						
					} else {
						
						if(this.debugFlag == true) {
							System.out.println("Sending ServerMessage to all servers!");
						}
						this.currTransponder.sendServerMessageToAll(serverMess);
					}
				}
				
				if(this.mode == 2) {
					System.out.println("This option not relevant for mode 0");
					break;
				}
				
				break;
				
			case 11:
				this.currTransponder.sendClientMessageToAll(clientMess);
				break;

			default:
				System.out.println("Please choose a valid option!");
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

		int portInput;
		int backlogInput;
		
		// Prompt user for server address and socket,
		// Create relevant variables for each

		System.out.println("Server Address:\n");
		String serverInput = keyboardInput.next();
		
		System.out.println("Server Socket:\n");
		portInput = keyboardInput.nextInt();
		
		System.out.println("Connection backlog value on local server:\n");
		backlogInput = keyboardInput.nextInt();

		// Create server socket by passing in
		// The InetAddress object,backlog int, socket int
		// Set socket options here
		// TODO: Future feature: Method that sets socket options
		try {
			InetSocketAddress serverAddr = new InetSocketAddress(InetAddress.getByName(serverInput),portInput);
			
			serverSocket = new ServerSocket();
			serverSocket.setReuseAddress(true);
			serverSocket.bind(serverAddr,backlogInput);
			
		} catch (IOException e) {
			
			e.printStackTrace();
		}

		return serverSocket;
	}

	public void setDebugFlag(Boolean bool) {
		if (this.currTransponder == null) {

		}
	}

	public int promptModeSetting(Scanner keyboardInput) {
		// Simply prompt and return mode setting

		int modeInput;

		System.out.println("Choose Transponder Mode: \n");
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
		// TODO: Create null-checks
		// TODO: Create debugFlag boolean flag and related output

		Socket clientSocket = null;

		InetAddress clientRemoteAddr = null;
		InetAddress clientLocalAddr = null;

		int clientRemoteSocket;
		int clientLocalPort;

		// Request the address on the local machine that
		// we want to send data from, including the port number
		// Store as clientLocalAddr and clientLocalSocket.

		System.out.println("Enter Client IP Address: \n");

		try {
			clientLocalAddr = InetAddress.getByName(userInput.next());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		System.out.println("Enter Client Port:\n");

		clientLocalPort = userInput.nextInt();

		// Request the address for the remote machine that we want
		// to connect to, store as clientRemoteAddr.
		System.out.println("Enter Remote Server IP Address:\n");
		
		try {
			clientRemoteAddr = InetAddress.getByName(userInput.next());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		System.out.println("Enter Remote Server Port:\n");
		clientRemoteSocket = userInput.nextInt();

		// Attempt creating a new Socket object using clientRemoteaddr,
		// clientRemoteSocket.

		try {
			
			clientSocket = new Socket(clientRemoteAddr, clientRemoteSocket, clientLocalAddr, clientLocalPort);
			
		} catch (IOException e) {
			System.out.println("ControllerMenu| Socket creation failed!");
			e.printStackTrace();
		}

		return clientSocket;
	}

	public boolean promptDebugFlag(Scanner userInput) {

		System.out.println("Debug mode? \n");
		System.out.println("1 - ON\n2 - OFF");

		int response = userInput.nextInt();
		if (response == 1) {
			return true;
		}

		if (response == 2) {
			return false;
		}
		return false;
	}

	// prompyPayload will prompt develop a payload via user input
	// and return the Payload object
	// This will likely change as Transponder is developed further

	public ServerMessage<?> promptServerMessage(Scanner userInput) {

		if (this.currTransponder == null) {
			throw new IllegalStateException("ServerMessage not set! currTransponder is null!");
		}

		System.out.println("Please enter the name of this Payload:");
		
		String payloadTitle = userInput.next();

		System.out.println("Please enter the serial number of this Payload:");
		int payloadNumber = userInput.nextInt();

		ServerMessage<?> payload = new Payload(payloadNumber, payloadTitle);

		return payload;
	}

	public Scanner getScanner() {
		if (!(this.inputScanner instanceof Scanner)) {
			throw new IllegalStateException("inputScanner not an instance of Scanner!");
		} else {
			return this.inputScanner;
		}
	}

	public void closeScanner() {
		this.inputScanner.close();
	}

}
