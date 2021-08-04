package transponderTCP;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Date;

public abstract class ServerMessage<S> implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private  Date creationDate = new Date();

	private InetAddress clientAddr = null;
	private InetAddress serverAddr = null;

	String message = "";
	S payload = null;
	
	public void setClientAddr(InetAddress clientAddr) {
		this.clientAddr = clientAddr;
	}
	
	public void setServerAddr(InetAddress serverAddr) {
		this.serverAddr = serverAddr;
	}
	
	public InetAddress getClientAddr() {
		return this.clientAddr;
	}
	
	public InetAddress getServerAddr() {
		return this.serverAddr;
	}
	
	public Date getCreationDate() {
		return this.creationDate;
	}

	public void setPayload(S o) {
		this.payload = o;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public String getMessage() {
		return this.message;
	}
	
	public Object getPayload() {
		return payload;
	}
	
}
