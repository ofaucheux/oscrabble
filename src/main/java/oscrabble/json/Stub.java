package oscrabble.json;

import org.eclipse.jetty.client.HttpClient;
import oscrabble.json.messages.RequestMessage;
import oscrabble.json.messages.ResponseMessage;

import java.net.InetSocketAddress;

/**
 * Abstract Stub
 */
abstract class Stub
{
	private final org.eclipse.jetty.client.HttpClient outgoingClient = new HttpClient();

	private final InetSocketAddress destination;

	protected Stub(final InetSocketAddress destination)
	{
		this.destination = destination;
	}

	protected  <A extends ResponseMessage> A sendRequest(final RequestMessage<A> output)
	{
		return output.send(this.outgoingClient, this.destination);
	}

	protected JsonMessage send(final JsonMessage output)
	{
		return output.send(this.outgoingClient, this.destination);
	}

}
