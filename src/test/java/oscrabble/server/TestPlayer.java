package oscrabble.server;

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
				final Move move = this.nextMoves.poll();
				if (move == null)
				{
					final String error = "no more predefined move anymore";
					LOGGER.error(error);
					this.server.quit(this, this.playerKey, error);
				}
				else
				{
					this.server.play(this, move);
				}
			}
			catch (ScrabbleException e)
			{
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
		System.out.println(msg);
	}

	@Override
	public void afterPlay(final int moveNr, final IPlayerInfo info, final IAction action, final int score)
	{
		System.out.println(info.getName() + " played " + action);
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