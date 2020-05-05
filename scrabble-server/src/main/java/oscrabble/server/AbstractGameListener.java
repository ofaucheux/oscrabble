package oscrabble.server;

import oscrabble.controller.Action;

import java.util.Queue;

/**
 * Abstract listener
 */
public class AbstractGameListener implements GameListener
{
	@Override
	public void onPlayRequired(final PlayerInformation player)
	{
	}

	@Override
	public void onDispatchMessage(final String msg)
	{
	}

	@Override
	public void afterRollback()
	{
	}

	@Override
	public void afterPlay(final Action action)
	{
	}

	@Override
	public void beforeGameStart()
	{
	}

	@Override
	public void afterGameEnd()
	{
	}

	@Override
	public Queue<ScrabbleEvent> getIncomingEventQueue()
	{
		return null;
	}

	@Override
	public void onGameStateChanged()
	{
	}

	@Override
	public void afterRejectedAction(final PlayerInformation player, final Action action)
	{
	}
}
