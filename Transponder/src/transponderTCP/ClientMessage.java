package transponderTCP;

import java.io.Serializable;
import java.util.Date;

public abstract class ClientMessage<C> implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String message = "";
	C payload = null;
	Date timestamp = new Date();
	
	public void setPayload(C object) {
		this.payload = object;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
		
	public Object getPayload() {
		return payload;
	}
	
}
