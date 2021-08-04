package transponderTCP;

import java.util.Date;

import transponderTCP.*;

public class ChatMessage extends ClientMessage<String>{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Date timestamp = null;
	
	public ChatMessage(String message) {
		super.setPayload(message);
		this.timestamp = new Date();
	}
	
	@Override
	public String toString() {
		String result = "";
		
		result += "Message: " + super.getPayload().toString() +"\n";
		result += "Timestamp: " + super.getTimestamp() + "\n";
		return result;
	}
	
	public Date getTimestamp() {
		return this.timestamp;
	}

	@Override
	public int compareTo(ClientMessage<String> o) {
		
		if(this.timestamp.before(o.getTimestamp())) {
			return -1;
		}
		
		if(this.timestamp.after(o.getTimestamp())) {
			return 1;
		} 
		
		// Return 0 if both messages were sent at the same time
		return 0;
	}

	
}
