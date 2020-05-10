package oscrabble.json.messages.requests;

import oscrabble.json.JsonMessage;
import oscrabble.json.messages.reponses.VoidResponse;

import java.util.concurrent.TimeUnit;

/**
 * Send from the client to retrieve a message the server has to deliver.
 */
public class PoolMessage extends JsonMessage
{
	/**
	 * Time out before a {@link VoidResponse} is returned
	 */
	private int timeout;

	/**
	 * Unit for timeout
	 */
	private TimeUnit timeoutUnit;

	public int getTimeout()
	{
		return timeout;
	}

	public void setTimeout(final int timeout)
	{
		this.timeout = timeout;
	}

	public TimeUnit getTimeoutUnit()
	{
		return timeoutUnit;
	}

	public void setTimeoutUnit(final TimeUnit timeoutUnit)
	{
		this.timeoutUnit = timeoutUnit;
	}
}