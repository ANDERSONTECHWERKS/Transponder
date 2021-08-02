package transponder;

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
	
	@Override
	public String toString() {
		String result = "";
		result += "clientSignOn Object:" + this.hashCode() + "\n";
		result += "Client InetAddress:" + clientAddr.toString() + "\n";
		result += "Server InetAddress:" + serverAddr.toString() + "\n";
		return result;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o == this) {
			return true;
		}
		
		if(!(o instanceof clientSignOn)) {
			return false;
		}
		
		clientSignOn castCSO = (clientSignOn)o;
		
		if(castCSO.clientAddr.equals(this.clientAddr) &&
				castCSO.serverAddr.equals(this.serverAddr)) {
			return true;
		}
		
		return false;
	}
}
