package transponder;

import java.io.Serializable;
import java.util.HashSet;

public class Payload implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 0L;
	
	private int messageNumber;
	private String messageName;
	
	public Payload(int messageNumber, String messageName) {
		this.messageNumber = messageNumber;
		this.messageName = messageName;
	}

	
	@Override
	public String toString() {
		String resultString = "Message#: " + messageNumber 
				+ "\n" +"Message Name: " +messageName + "\n";
		return resultString;
	}
	
	public String getMessageName() {
		return this.messageName;
	}
	
	public int getMessageNumber() {
		return this.messageNumber;
	}
	
	public int hashCode() {
		
		return this.messageNumber + this.messageName.hashCode();
		
	}
	
	
	@Override
	public boolean equals(Object o) {
		
		if(o == this) {
			return true;
		}
		
		if(!(o instanceof Payload)) {
			return false;
		}
		
		Payload p = (Payload)o;
		
		if(p.getMessageName().compareTo(this.getMessageName()) == 0
				&& p.getMessageNumber() == this.getMessageNumber()) {
			return true;
		}
		
		return false;
	}
}
