package transponderTCP;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.Date;

public class clientSignOff implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Date creationDate = new Date();
	
	private InetAddress clientAddr = null;
	private InetAddress serverAddr = null;
	
	clientSignOff(InetAddress client, InetAddress server){
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
	
	public InetAddress getClientAddr() {
		return this.clientAddr;
	}
	
	public InetAddress getServerAddr() {
		return this.serverAddr;
	}
}
