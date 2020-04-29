//package oscrabble.player;
//
//import oscrabble.configuration.Configuration;
//import oscrabble.server.Game;
//import oscrabble.server.Play;
//
//import javax.swing.*;
//
//public class ConsolePlayer extends AbstractPlayer
//{
//	ConsolePlayer()
//	{
//		super(null, "Console");
//	}
//
//	@Override
//	public void onPlayRequired(final Play play)
//	{
//		if (play.player == this)
//		{
//			final String move = JOptionPane.showInputDialog("New word?");
//			final String verticalPattern = "(\\a\\d) +(\\w+)";
//		}
//	}
//
//	@Override
//	public void onDispatchMessage(final String msg)
//	{
//
//	}
//
//	@Override
//	public void afterPlay(final Play play)
//	{
//
//	}
//
//	@Override
//	public Configuration getConfiguration()
//	{
//		return null;
//	}
//
//	@Override
//	public void beforeGameStart()
//	{
//
//	}
//
//	@Override
//	public boolean isObserver()
//	{
//		return false;
//	}
//
//	@Override
//	public Game.PlayerType getType()
//	{
//		return null;// TODO
//	}
//}
