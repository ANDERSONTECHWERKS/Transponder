package transponderTCP;

import java.util.Comparator;

public class MessageDateComparator implements Comparator<ClientMessage<?>>{

	@Override
	public int compare(ClientMessage<?> arg0, ClientMessage<?> arg1) {
		if(arg0 == arg1) {
			return 0;
		}
		
		if(arg0.timestamp.before(arg1.timestamp)) {
			return -1;
		}
		
		if(arg0.timestamp.after(arg1.timestamp)) {
			return 1;
		}
		
		return 0;
	}

}
