package oscrabble.player;

import oscrabble.server.IAction;
import oscrabble.server.IPlayerInfo;

import javax.swing.*;

public class ConsolePlayer extends AbstractPlayer
{
	ConsolePlayer()
	{
		super("Console");
	}

	@Override
	public void onPlayRequired(final AbstractPlayer currentPlayer)
	{
		if (currentPlayer == this)
		{
			final String move = JOptionPane.showInputDialog("New word?");
			final String verticalPattern = "(\\a\\d) +(\\w+)";
		}
	}

	@Override
	public void onDictionaryChange()
	{

	}

	@Override
	public void onDispatchMessage(final String msg)
	{

	}

	@Override
	public void afterPlay(final int moveNr, final IPlayerInfo player, final IAction action, final int score)
	{

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
