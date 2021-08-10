package transponderTCP;

import java.util.Comparator;

public class MessageDateComparatorSM implements Comparator<ServerMessage<?>>{

	@Override
	public int compare(ServerMessage<?> arg0, ServerMessage<?> arg1) {
		if(arg0 == arg1) {
			return 0;
		}
		
		if(arg0.getTimestamp().before(arg1.getTimestamp())) {
			return -1;
		}
		
		if(arg0.getTimestamp().after(arg1.getTimestamp())) {
			return 1;
		}
		
		return 0;
	}

}
