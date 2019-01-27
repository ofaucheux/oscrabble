package oscrabble.player;

import oscrabble.server.IAction;
import oscrabble.server.IPlayerInfo;

import java.util.UUID;

public abstract class AbstractPlayer
{
	private String name;

	protected UUID playerKey;

	protected AbstractPlayer(final String name)
	{
		this.name = name;
	}

	public final String getName()
	{
		return this.name;
	}

	public void setPlayerKey(final UUID playerKey)
	{
		this.playerKey = playerKey;
	}

	public abstract void onPlayRequired();
	public abstract void onDictionaryChange();
	public abstract void onDispatchMessage(String msg);
	public abstract void afterPlay(IPlayerInfo info, IAction action, int score);
	public abstract void beforeGameStart();

	public abstract boolean isObserver();

	@Override
	public String toString()
	{
		return getName();
	}
}
