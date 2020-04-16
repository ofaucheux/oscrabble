package oscrabble.json;

import oscrabble.configuration.Configuration;
import oscrabble.json.messages.requests.GetName;
import oscrabble.player.IPlayer;
import oscrabble.server.Server;

import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Representation of a remote player for the local server.
 */
public class PlayerStub extends Stub implements IPlayer
{

	private final ArrayBlockingQueue<Server.ScrabbleEvent> incomingEventQueue = new ArrayBlockingQueue<>(128);

	private UUID playerKey;

	public PlayerStub(final InetSocketAddress remotePlayer)
	{
		super(remotePlayer);

		final Thread daemon = new Thread(() -> {
			// TODO read the event queue and send it as a JsonMessage
		});
		daemon.setDaemon(true);
		daemon.start();
	}

	@Override

	public Configuration getConfiguration()
	{
		throw new AssertionError("Not implemented"); // TODO
	}

	@Override
	public void setPlayerKey(final UUID key)
	{
		this.playerKey = key;
	}

	@Override
	public String getName()
	{
		return sendRequest(new GetName()).getName();
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
	public void destroy() { }

	@Override
	public BlockingQueue<Server.ScrabbleEvent> getIncomingEventQueue()
	{
		return this.incomingEventQueue;
	}

}
