package transponderTCP;

import java.util.ArrayList;

// debugObj is intended to be passed into separate parts
// of the multithreaded Transponder objects, so that 
// input and output objects can be collected and compared.

public class debugObj {
	Payload inputPayload;
	Payload outputPayloadSingle;
	ArrayList<Payload> outputPayloadList = new ArrayList<Payload>();
	debugObj(){
		this.inputPayload = null;
	}
	
	debugObj(Payload payload){
		this.inputPayload = payload;
	}
	
	public void setInputPayload(Payload payload) {
		this.inputPayload = payload;
	}
	
	public void setOutputPayloadSingle(Payload payload) {
		this.outputPayloadSingle = payload;
	}
	
	public void addOutputPayload(Payload payload) {
		this.outputPayloadList.add(payload);
	}
	
	public boolean evaluatePayloadEquivalanceMulti() {
		for(Payload currPayload : this.outputPayloadList) {
			System.out.println("debugObj| inputPayload: \n" + this.inputPayload.toString());
			System.out.println("debugObj| outputPayload: \n" + currPayload.toString());
			
			if(!currPayload.equals(inputPayload)) {
				return false;
			} 
		}
		return true;
	}
	
	public boolean evaluatePayloadEquivalanceSingle() {
		if(this.inputPayload.equals(outputPayloadSingle));

		return true;
	}
}
