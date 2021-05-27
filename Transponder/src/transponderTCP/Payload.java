package transponderTCP;

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
}
