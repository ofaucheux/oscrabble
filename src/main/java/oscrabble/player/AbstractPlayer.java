package oscrabble.player;

import org.apache.log4j.Logger;
import oscrabble.client.SwingPlayer;
import oscrabble.server.Game;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public abstract class AbstractPlayer implements Game.GameListener
{
	public static final Logger LOGGER = Logger.getLogger(AbstractPlayer.class);
	private String name;

	protected UUID playerKey;
	protected static Game game;

	private SwingPlayer firstClient;

	/**
	 * Queue to receive events from server
	 */
	private BlockingQueue<Game.ScrabbleEvent> incomingEvents = new ArrayBlockingQueue<>(16);
	private boolean destroyEDT;

	/** Event dispatching thread */
	private Thread edt;

	protected AbstractPlayer(final String name)
	{
		this.name = name;
	}

	public void setGame(final Game game)
	{
		AbstractPlayer.game = game;

		this.edt = new Thread(() -> {
			while (!this.destroyEDT)
			{
				try
				{
					final Game.ScrabbleEvent event = this.incomingEvents.poll(100, TimeUnit.MILLISECONDS);
					if (event != null)
					{
						event.accept(AbstractPlayer.this);
					}
				}
				catch (InterruptedException e)
				{
					LOGGER.error(e, e);
				}
			}
		});
		this.edt.setName(this.name + " EDT");
		this.destroyEDT = false;
		this.edt.start();
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

	@Override
	public void afterRollback()
	{
		// TODO
	}

	public void beforeGameStart()
	{
	}

	public void afterGameEnd()
	{
		LOGGER.info("Destroy EDT of " + this.getName());
		this.destroyEDT = true;
		this.edt.interrupt();
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
