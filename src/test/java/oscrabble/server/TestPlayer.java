package oscrabble.server;

import org.apache.log4j.Logger;
import oscrabble.Move;
import oscrabble.ScrabbleException;
import oscrabble.player.AbstractPlayer;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Ein Test-Spieler, dessen Spielzügen durch {@code #addMove} vordefiniert sind.
 */
class TestPlayer extends AbstractPlayer
{
	public static final Logger LOGGER = Logger.getLogger(TestPlayer.class);
	private final Game server;
	private BlockingQueue<Move> nextMoves;

	TestPlayer(final String name, final Game server)
	{
		super(name);
		this.server = server;
		this.nextMoves = new ArrayBlockingQueue<>(1024);
	}

	void addMove(final Move move)
	{
		this.nextMoves.add(move);
	}

	@Override
	public void onPlayRequired(final AbstractPlayer player)
	{
		if (player == this)
		{
			try
			{
				final Move move = this.nextMoves.take();
				this.server.play(this, move);
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
	public void afterPlay(final int moveNr, final IPlayerInfo info, final IAction action, final int score)
	{
		LOGGER.info(info.getName() + " played " + action);
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