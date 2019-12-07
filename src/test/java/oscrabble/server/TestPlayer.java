package oscrabble.server;

import org.apache.log4j.Logger;
import oscrabble.PlayTiles;
import oscrabble.ScrabbleException;
import oscrabble.player.AbstractPlayer;

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
	public void onPlayRequired(final AbstractPlayer player)
	{
		if (player == this)
		{
			try
			{
				final PlayTiles playTiles = this.nextPlayTiles.take();
				this.server.play(this, playTiles);
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
		}
	}

	@Override
	public void onDictionaryChange()
	{}

	@Override
	public void onDispatchMessage(final String msg)
	{
		LOGGER.info(msg);
	}

	@Override
	public void afterPlay(final int playNr, final IPlayerInfo info, final IPlay action, final int score)
	{
		LOGGER.info(info.getName() + " played " + action + " - scored " + score);
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