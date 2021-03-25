package oscrabble.json.messages;

import org.eclipse.jetty.client.HttpClient;
import oscrabble.json.IOScrabbleError;
import oscrabble.json.JsonMessage;

import java.net.InetSocketAddress;

public abstract class RequestMessage<A extends ResponseMessage> extends JsonMessage {
	@Override
	public A send(final HttpClient sender, final InetSocketAddress destination) throws IOScrabbleError {
		//noinspection unchecked
		return (A) super.send(sender, destination);
	}
}
