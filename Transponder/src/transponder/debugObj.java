package transponder;

import java.util.ArrayList;
import java.util.HashMap;

// debugObj is intended to be passed into separate parts
// of the multithreaded Transponder objects, so that 
// input and output objects can be collected and compared.

public class debugObj {
	Payload inputPayload;
	Payload outputPayloadSingle;
	HashMap<Payload,Integer> outputPayloadMap = new HashMap<Payload,Integer>();
	
	debugObj(){
		this.inputPayload = null;
	}
	
	debugObj(Payload payload){
		this.inputPayload = payload;
	}
	
	public void setInputPayload(Payload payload) {
		this.inputPayload = payload;
	}
	
	public void addOutputPayload(Payload payload) {
		
		// If the outputPayloadMap already contains this payload - increment it.
		if(this.outputPayloadMap.containsKey(payload)) {
			
			this.outputPayloadMap.replace(payload, this.outputPayloadMap.get(payload) +1);
		}
		
		// If the outputPayloadMap does not contain this payload - add it.
		if(!this.outputPayloadMap.containsKey(payload)) {
			this.outputPayloadMap.put(payload, 1);
		}
		
	}
	
	public boolean evaluatePayloadEquivalanceMulti() {
		for(Payload currPayload : this.outputPayloadMap.keySet()) {
			int counter = 0;
			
			System.out.println("debugObj eval| inputPayload: \n" + this.inputPayload.toString());
			System.out.println("debugObj eval| outputPayload number:" + counter + "\n" +currPayload.toString() +"\n" +
			"Payload count:" + this.outputPayloadMap.get(currPayload) + "\n");

			if(!currPayload.equals(inputPayload)) {
				return false;
			} 
		}
		return true;
	}

}
