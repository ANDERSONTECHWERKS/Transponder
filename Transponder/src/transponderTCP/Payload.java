package transponderTCP;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;

public class Payload extends ServerMessage<String> implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 0L;

	private int messageNumber;
	private String messageName;
	private Date timestamp = new Date();

	public Payload(int messageNumber, String messageName) {
		this.messageNumber = messageNumber;
		this.messageName = messageName;
	}

	public Date getTimestamp() {
		return this.timestamp;
	}

	@Override
	public String toString() {
		String resultString = "Message#: " + messageNumber + "\n" + "Message Name: " + messageName + "\n";
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

		if (o == this) {
			return true;
		}

		if (!(o instanceof Payload)) {
			return false;
		}

		Payload p = (Payload) o;

		if (p.getMessageName().compareTo(this.getMessageName()) == 0
				&& p.getMessageNumber() == this.getMessageNumber()) {
			return true;
		}

		return false;
	}

	@Override
	public int compareTo(ServerMessage<String> o) {

		if (this.timestamp.before(o.getTimestamp())) {
			return -1;
		}

		if (this.timestamp.after(o.getTimestamp())) {
			return 1;
		}

		// Return 0 if both messages were sent at the same time
		return 0;
	}

}
