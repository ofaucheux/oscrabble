package oscrabble.server;

import org.apache.log4j.Logger;
import oscrabble.PlayTiles;
import oscrabble.ScrabbleException;
import oscrabble.player.AbstractPlayer;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Ein Test-Spieler, dessen Spielzügen durch {@code #addMove} vordefiniert sind.
 */
class TestPlayer extends AbstractPlayer
{
	public static final Logger LOGGER = Logger.getLogger(TestPlayer.class);
	private final Game server;
	private BlockingQueue<PlayTiles> nextPlayTiles;

	TestPlayer(final String name, final Game server)
	{
		super(name);
		this.server = server;
		this.nextPlayTiles = new ArrayBlockingQueue<>(1024);
	}

	UUID getKey()
	{
		return this.playerKey;
	}

	void addMove(final PlayTiles playTiles)
	{
		this.nextPlayTiles.add(playTiles);
	}

	@Override
	public void onPlayRequired(final Play play)
	{
		if (play.player == this)
		{
			LOGGER.info("Play required");

			try
			{
				Thread.sleep(100);
				final PlayTiles playTiles = this.nextPlayTiles.take();
				try
				{
					this.server.play(this.playerKey, play, playTiles);
				}
				catch (ScrabbleException.NotInTurn notInTurn)
				{
					final ArrayList<PlayTiles> remainings = new ArrayList<>();
					this.nextPlayTiles.drainTo(remainings);
					this.nextPlayTiles.add(playTiles);
					this.nextPlayTiles.addAll(remainings);
				}
			}
			catch (InterruptedException e)
			{
				final String error = "Interrupted";
				LOGGER.error(error);
				try
				{
					this.server.quit(this, this.playerKey, error);
				}
				catch (ScrabbleException ex)
				{
					throw new Error(ex);
				}
			}
			catch (ScrabbleException e)
			{
				LOGGER.error(e, e);
				throw new Error(e);
			}
		}
	}

	@Override
	public void onDictionaryChange()
	{}

	@Override
	public void onDispatchMessage(final String msg)
	{
		LOGGER.trace(msg);
	}

	@Override
	public void afterPlay(final Play play)
	{
		LOGGER.info(play.player.getName() + " played " + play.action + " - scored " + play.score);
	}

	@Override
	public void beforeGameStart()
	{
	}

	@Override
	public boolean isObserver()
	{
		return false;
	}
}