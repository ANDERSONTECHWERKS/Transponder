package transponderTCP;

import java.util.ArrayList;

// debugObj is intended to be passed into separate parts
// of the multithreaded Transponder objects, so that 
// input and output objects can be collected and compared.

public class debugObj {
	Payload inputPayload;
	ArrayList<Payload> outputPayloadList = new ArrayList<Payload>();
	
	public void setInputPayload(Payload payload) {
		this.inputPayload = payload;
	}
	
	public void addOutputPayload(Payload payload) {
		this.outputPayloadList.add(payload);
	}
	
	public boolean evaluatePayloadEquivalance() {
		for(Payload currPayload : this.outputPayloadList) {
			System.out.println("inputPayload: \n" + this.inputPayload.toString());
			System.out.println("outputPayload: \n" + currPayload.toString());
			if(!currPayload.equals(inputPayload)) {
				return false;
			} 
		}
		return true;
	}
}
