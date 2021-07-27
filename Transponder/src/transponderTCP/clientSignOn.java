package transponderTCP;

import java.io.Serializable;
import java.net.InetAddress;

public class clientSignOn implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private InetAddress clientAddr = null;
	private InetAddress serverAddr = null;

	clientSignOn(InetAddress client, InetAddress server){
		this.clientAddr = client;
		this.serverAddr = server;
	}
}
