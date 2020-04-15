package oscrabble.json;

import oscrabble.configuration.Configuration;
import oscrabble.player.IPlayer;
import oscrabble.server.Server;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Representation of a remote player for the local server.
 */
public class PlayerStub implements IPlayer
{

	private final ArrayBlockingQueue<JsonMessage> incomingEventQueue = new ArrayBlockingQueue<>(128);

	private UUID playerKey;
	private Server server;

	@Override
	public Configuration getConfiguration()
	{
		throw new AssertionError("Not implemented"); // TODO
	}

	@Override
	public void setPlayerKey(final UUID key)
	{
		final SetPlayerKey message = new SetPlayerKey();
		message.setKey(key.toString());
		this.incomingEventQueue.add(message);
	}

	@Override
	public String getName()
	{
		throw new AssertionError("Not implemented"); // TODO
	}

	@Override
	public void setGame(final Server server)
	{
		throw new AssertionError("Not implemented"); // TODO
	}

	@Override
	public boolean isObserver()
	{
		throw new AssertionError("Not implemented"); // TODO
	}

	@Override
	public void editParameters()
	{
		throw new AssertionError("Not implemented"); // TODO
	}

	@Override
	public UUID getPlayerKey()
	{
		return this.playerKey;
	}

	@Override
	public Server.PlayerType getType()
	{
		throw new AssertionError("Not implemented"); // TODO
	}

	@Override
	public boolean hasEditableParameters()
	{
		throw new AssertionError("Not implemented"); // TODO
	}

	@Override
	public BlockingQueue<JsonMessage> getIncomingEventQueue()
	{
		return this.incomingEventQueue;
	}

	/**
	 * Fill the headers of the message and add it as message for the client.
	 *
	 * @param message the message
	 */
	private void addMessage(final JsonMessage message)
	{
		message.setFrom(this.server.getUUID().toString());
		message.setTo(this.playerKey.toString());
		message.setDate(new Date());
		this.incomingEventQueue.add(message);
	}

}
