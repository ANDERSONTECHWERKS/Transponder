package transponder;

import java.net.ServerSocket;
import java.net.SocketAddress;
import java.util.HashSet;

// Transponder interface. Standardizes common fields between TransponderTCP and TransponderUDP.
// Essentially, just a marker interface right now.
public interface Transponder {

	void setInitialServerPayload(Payload initPayload);

	void setDebugFlag(boolean b);
}
