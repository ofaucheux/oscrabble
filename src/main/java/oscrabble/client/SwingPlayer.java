package oscrabble.client;

import oscrabble.ScrabbleException;
import oscrabble.Tile;
import oscrabble.configuration.Configuration;
import oscrabble.player.AbstractPlayer;
import oscrabble.server.Game;
import oscrabble.server.Play;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.UUID;

public class SwingPlayer extends AbstractPlayer
{
	static Playground playground;
	private JRack jRack;
	JDialog rackFrame;

	public SwingPlayer(final String name)
	{
		super(null, name);
		if (playground == null)
		{
			playground = new Playground();
		}
		playground.addPlayer(this);
		this.jRack = new JRack();
	}

	UUID getPlayerKey()
	{
		return this.playerKey;
	}

	@Override
	public void setGame(final Game game)
	{
		super.setGame(game);
		playground.setGame(game);
	}

	@Override
	public Configuration getConfiguration()
	{
		return null;
	}

	private void createUI()
	{
		this.rackFrame = new JDialog(playground.gridFrame);
		this.rackFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.rackFrame.setTitle(this.getName());

		this.rackFrame.setLayout(new BorderLayout());
		this.rackFrame.add(this.jRack);

		final JMenu moreActionMenu = new JMenu("...");
		this.rackFrame.add(moreActionMenu);
		moreActionMenu.add(new AbstractAction(Game.MESSAGES.getString("exchange.tiles...."))
		{
			@Override
			public void actionPerformed(final ActionEvent actionEvent)
			{
				final int remaining = SwingPlayer.this.game.getNumberTilesInBag();
				final int minimum = SwingPlayer.this.game.getRequiredTilesInBagForExchange();
				if (remaining < minimum)
				{
					playground.showMessage(MessageFormat.format(
							Game.MESSAGES.getString("exchange.of.tiles.not.authorized.because.number.of.tiles.in.bag.(1).smaller.as(2)"),
							remaining,
							minimum)
					);
				}
				else
				{
					playground.showMessage(Game.MESSAGES.getString("to.exchange.tiles.enter.-.letters.to.exchange.p.ex.-ABC"));
				}
			}
		});
		moreActionMenu.add(new AbstractAction(Game.MESSAGES.getString("pass.the.turn"))
		{
			@Override
			public void actionPerformed(final ActionEvent actionEvent)
			{
				playground.showMessage(Game.MESSAGES.getString("to.pass.the.turn.enter.-"));
			}
		});

		this.rackFrame.pack();
		this.rackFrame.setVisible(true);
		this.rackFrame.setFocusableWindowState(false);
		this.rackFrame.setFocusable(false);

		playground.afterUiCreated(this);
	}

	public void updateRack()
	{
		this.jRack.update();
	}

	private class JRack extends JPanel
	{
		static final int RACK_SIZE = 7;
		final Playground.RackCell[] cells = new Playground.RackCell[7];

		private JRack()
		{
			this.setLayout(new GridLayout(1,7));
			for (int i = 0; i < RACK_SIZE; i++)
			{
				this.cells[i] = new Playground.RackCell();
				add(this.cells[i]);
			}
		}

		void update()
		{
			try
			{
				final ArrayList<Tile> tiles = new ArrayList<>(
						SwingPlayer.this.game.getRack(SwingPlayer.this, SwingPlayer.this.playerKey));

				for (int i = 0; i < RACK_SIZE; i++)
				{
					this.cells[i].setTile(
							i >= tiles.size() ? null : tiles.get(i)
					);
				}
				this.repaint();
			}
			catch (ScrabbleException e)
			{
				playground.showMessage(e.getMessage());
			}
		}
	}

	@Override
	public void afterRollback()
	{
		playground.refreshUI(this);
	}


	@Override
	public void onDispatchMessage(final String msg)
	{
		playground.showMessage(msg);
	}

	@Override
	public void afterPlay(final Play played)
	{
		playground.afterPlay(this, played);
		this.jRack.update();
	}

	@Override
	public void beforeGameStart()
	{
		playground.beforeGameStart();
		this.jRack.update();
		createUI();
	}

	@Override
	public boolean isObserver()
	{
		return false;
	}

	@Override
	public Game.PlayerType getType()
	{
		return Game.PlayerType.SWING;
	}


	@Override
	public void onPlayRequired(final Play play)
	{
		if (playground.getNumberSwingPlayers() > 1)
		{
			this.jRack.setBorder(play.player == this
					? new LineBorder(Color.green.darker(), 6)
					: null);
			SwingUtilities.invokeLater(() -> this.rackFrame.pack());
		}

		playground.onPlayRequired(this, play);
	}
}
