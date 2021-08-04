package transponderTCP;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Date;

public class clientSignOn implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Date creationDate = new Date();

	private InetAddress clientAddr = null;
	private InetAddress serverAddr = null;

	clientSignOn(InetAddress client, InetAddress server){
		this.clientAddr = client;
		this.serverAddr = server;
	}
	
	public void setClientAddr(InetAddress clientAddr) {
		this.clientAddr = clientAddr;
	}
	
	public void setServerAddr(InetAddress serverAddr) {
		this.serverAddr = serverAddr;
	}
	
	public Date getCreationDate() {
		return this.creationDate;
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
	
	public InetAddress getClientAddr() {
		return this.clientAddr;
	}
	
	public InetAddress getServerAddr() {
		return this.serverAddr;
	}
}
