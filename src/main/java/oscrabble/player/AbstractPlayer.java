package oscrabble.player;

import org.apache.log4j.Logger;
import oscrabble.server.Game;
import oscrabble.server.IAction;
import oscrabble.server.IPlayerInfo;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public abstract class AbstractPlayer
{
	public static final Logger LOGGER = Logger.getLogger(AbstractPlayer.class);
	private String name;

	protected UUID playerKey;
	protected Game game;

	/**
	 * Queue to receive events from server
	 */
	private BlockingQueue<Game.ScrabbleEvent> incomingEvents = new ArrayBlockingQueue<>(16);

	protected AbstractPlayer(final String name)
	{
		this.name = name;
	}

	public void setGame(final Game game)
	{
		this.game = game;

		new Thread(() -> {
			try
			{
				while (true)
				{
					final Game.ScrabbleEvent event = this.incomingEvents.take();
					event.accept(AbstractPlayer.this);
				}
			}
			catch (InterruptedException e)
			{
				LOGGER.error(e, e);
			}
		}).start();
	}

	public final String getName()
	{
		return this.name;
	}

	public Queue<Game.ScrabbleEvent> getIncomingEventQueue()
	{
		return this.incomingEvents;
	}

	public void setPlayerKey(final UUID playerKey)
	{
		this.playerKey = playerKey;
	}

	/**
	 * Sent to all players to indicate who now has to play.
	 */
	public abstract void onPlayRequired(AbstractPlayer currentPlayer);

	public abstract void onDictionaryChange();

	public abstract void onDispatchMessage(String msg);

	/**
	 * Send to all players after a player has played
	 */
	public abstract void afterPlay(IPlayerInfo info, IAction action, int score);

	public void beforeGameStart()
	{
	}

	public void afterGameEnd()
	{
	}

	public abstract boolean isObserver();

	@Override
	public String toString()
	{
		return getName();
	}

	/**
	 * @return {@code true} wenn manche Parameters editierbar sind.
	 */
	public boolean hasEditableParameters()
	{
		return false;
	}

	/**
	 * Gibt die Möglichkeit, die Parameters zu ändern. Es kann z.B. durch die Anzeige eines JPanels erfolgen.
	 */
	public void editParameters()
	{
		throw new AssertionError("Default implementation has no editable parameter");
	}

}
