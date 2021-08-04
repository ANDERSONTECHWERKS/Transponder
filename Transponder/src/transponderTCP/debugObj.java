package transponderTCP;

import java.util.ArrayList;
import java.util.HashMap;

// debugObj is intended to be passed into separate parts
// of the multithreaded Transponder objects, so that 
// input and output objects can be collected and compared.

public class debugObj {
	ServerMessage<?> inputServMessage;
	ServerMessage<?> recievedServerMessage;
	
	HashMap<ServerMessage<?>,Integer> recievedServerMessagesMap = new HashMap<ServerMessage<?>,Integer>();
	
	debugObj(){
		this.inputServMessage = null;
	}
	
	debugObj(ServerMessage<?> servMessage){
		this.inputServMessage = servMessage;
	}
	
	public void setInpServerMessage(ServerMessage<?> servMessage) {
		this.inputServMessage = servMessage;
	}
	
	public void setRecievedServMessage(ServerMessage<?> serverMessage) {
		
		// If the recievedServerMessagesMap already contains this payload - increment it.
		if(this.recievedServerMessagesMap.containsKey(serverMessage)) {
			
			this.recievedServerMessagesMap.replace(serverMessage, this.recievedServerMessagesMap.get(serverMessage) +1);
		}
		
		// If the recievedServerMessagesMap does not contain this payload - add it.
		if(!this.recievedServerMessagesMap.containsKey(serverMessage)) {
			this.recievedServerMessagesMap.put(serverMessage, 1);
		}
		
	}
	
	public boolean evaluateMessageEquivalanceMulti() {
		
		for(ServerMessage<?> currServMessage : this.recievedServerMessagesMap.keySet()) {

			int counter = 0;
			
			System.out.println("debugObj eval| inputServMessage: \n" + this.inputServMessage.toString());
			System.out.println("debugObj eval| recievedServerMessage number:" + counter + "\n" +currServMessage.toString() +"\n" +
			"Payload count:" + this.recievedServerMessagesMap.get(currServMessage) + "\n");

			// TODO: Legitimate problem - how do we test equivalence between recievedServerMessage and serverMessage?
			// Think about this. We'll use .equals on their toString() equivalents for now.
			// Going to write an issue about this and carry on
			
			if(!currServMessage.toString().equals(inputServMessage.toString())) {
				return false;
			} 
		}
		return true;
	}

}
