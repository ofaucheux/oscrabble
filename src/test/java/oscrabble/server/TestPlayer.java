package oscrabble.server;

import oscrabble.Move;
import oscrabble.player.AbstractPlayer;

class TestPlayer extends AbstractPlayer
{
	private final ScrabbleServer server;
	private Move nextMove;

	public TestPlayer(final String name, final ScrabbleServer server)
	{
		super(name);
		this.server = server;
	}

	public void setNextMove(final Move move)
	{
		if (this.nextMove != null)
		{
			throw new IllegalStateException("Next move already set");
		}

		this.nextMove = move;
	}

	@Override
	public void onPlayRequired()
	{
		while (this.nextMove == null)
		{
			try
			{
				Thread.sleep(100);
			}
			catch (InterruptedException e)
			{
				throw new Error(e);
			}
		}
		try
		{
			this.server.play(this, this.nextMove);
		}
		finally
		{
			this.nextMove = null;
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
	public void afterPlay(final IPlayerInfo info, final IAction action, final int score)
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