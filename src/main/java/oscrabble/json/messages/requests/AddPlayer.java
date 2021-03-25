package oscrabble.json.messages.requests;

import oscrabble.json.messages.RequestMessage;
import oscrabble.json.messages.reponses.AddPlayerResponse;

public class AddPlayer extends RequestMessage<AddPlayerResponse> {
	private String name;

	private String inetSocketAddress;

	public String getName() {
		return this.name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getInetSocketAddress() {
		return this.inetSocketAddress;
	}

	public void setInetSocketAddress(final String inetSocketAddress) {
		this.inetSocketAddress = inetSocketAddress;
	}
}
