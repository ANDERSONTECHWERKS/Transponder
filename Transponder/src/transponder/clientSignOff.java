package transponder;

import java.io.Serializable;
import java.net.InetAddress;

public class clientSignOff implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private InetAddress clientAddr = null;
	private InetAddress serverAddr = null;
	
	clientSignOff(InetAddress client, InetAddress server){
		this.clientAddr = client;
		this.serverAddr = server;
	}
}
