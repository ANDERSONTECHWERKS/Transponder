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
	HashSet<Serializable> serializableSet;
	
	public Payload(int messageNumber, String messageName) {
		this.messageNumber = messageNumber;
		this.messageName = messageName;
	}

	
	@Override
	public String toString() {
		String resultString = "Message#: " + messageNumber 
				+ "\n" +"Message Name: " +messageName + "\n";
		
		if(serializableSet.size() > 0) {
			resultString += "Message Contents: \n";
			
			for(Serializable currSerial:this.serializableSet) {
				resultString += currSerial.toString();
			}
		}
		return resultString;
	}
	
	// This method adds a serializable object to the serializableSet hashSet.
	public void addSerializable(Serializable addedObject) {
		this.serializableSet.add(addedObject);
	}
	
	// This method removes a serializable object to the serializableSet hashSet.
	public void removeSerializable(Serializable removedObject) {
		this.serializableSet.remove(removedObject);
	}
}
