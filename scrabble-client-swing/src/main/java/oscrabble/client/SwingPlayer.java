package oscrabble.client;

import oscrabble.data.Player;
import oscrabble.data.Tile;
import oscrabble.player.AbstractPlayer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Locale;
import java.util.ResourceBundle;

public class SwingPlayer extends AbstractPlayer
{
	/**
	 * Resource Bundle
	 */
	public final static ResourceBundle MESSAGES = ResourceBundle.getBundle("Messages", new Locale("fr_FR"));

	static Playground playground;

	/**
	 * Rack to display the tiles of this player.
	 */
	private final JRack jRack;
	protected final JDialog rackFrame;

	// TODO?
//	public void setGame(final Game game)
//	{
//		super.setGame(game);
//		playground.setGame(game);
//	}

//	@Override
//	todo
//	public Configuration getConfiguration()
//	{
//		return null;
//	}

	public SwingPlayer(final String name)
	{
		super(name);
		this.jRack = new JRack();

		this.rackFrame = new JDialog(playground.gridFrame);
		this.rackFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.rackFrame.setTitle(this.name);

		this.rackFrame.setLayout(new BorderLayout());
		this.rackFrame.add(this.jRack);

		final JButton moreActionMenu = new JButton();
		final JPopupMenu popupMenu = new JPopupMenu();
		this.rackFrame.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(final WindowEvent e)
			{
				popupMenu.setVisible(false);
			}
		});
		moreActionMenu.setAction(new AbstractAction("...")
		{
			@Override
			public void actionPerformed(final ActionEvent actionEvent)
			{
				popupMenu.setVisible(!popupMenu.isVisible());
				final Point p = moreActionMenu.getLocationOnScreen().getLocation();
				p.translate(moreActionMenu.getWidth(), 0);
				popupMenu.setLocation(p);
				popupMenu.validate();
			}
		});
		moreActionMenu.addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentHidden(final ComponentEvent e)
			{
				popupMenu.setVisible(false);
			}
		});
		this.rackFrame.add(moreActionMenu, BorderLayout.AFTER_LINE_ENDS);

		// todo
//		popupMenu.add(new JMenuItem(new AbstractAction(MESSAGES.getString("exchange.tiles"))
//		{
//			@Override
//			public void actionPerformed(final ActionEvent actionEvent)
//			{
//				final int remaining = SwingPlayer.this.game.getNumberTilesInBag();
//				final int minimum = SwingPlayer.this.game.getRequiredTilesInBagForExchange();
//				if (remaining < minimum)
//				{
//					playground.showMessage(MessageFormat.format(
//							MESSAGES.getString("exchange.of.tiles.not.authorized.because.number.of.tiles.in.bag.0.smaller.as.1"),
//							remaining,
//							minimum)
//					);
//				}
//				else
//				{
//					playground.showMessage(MESSAGES.getString("to.exchange.tiles.enter.letters.to.exchange.p.ex.abc"));
//				}
//			}
//		}));
//		popupMenu.add(new JMenuItem(new AbstractAction(MESSAGES.getString("pass.the.turn"))
//		{
//			@Override
//			public void actionPerformed(final ActionEvent actionEvent)
//			{
//				playground.showMessage(MESSAGES.getString("to.pass.the.turn.enter"));
//			}
//		}));

		this.rackFrame.pack();
		this.rackFrame.setVisible(true);
		this.rackFrame.setFocusableWindowState(false);
		this.rackFrame.setFocusable(false);

		playground.afterUiCreated(this);
	}

	public void update(final Player inputData)
	{
		// TODO when name change
		final ArrayList<Tile> tiles = inputData.rack.tiles;
		for (int i = 0; i < tiles.size(); i++)
		{
			this.jRack.cells[i].setTile(new JTile(tiles.get(i)));
		}
		for (int i = tiles.size(); i < this.jRack.cells.length; i++)
		{
			this.jRack.cells[i] = null;
		}
	}
	public void afterRollback()
	{
		playground.refreshUI(this);
	}


	public void afterPlay()
	{
		playground.afterPlay(this, null);
		this.jRack.update(null);
	}

	public void beforeGameStart()
	{
		this.jRack.update(null);
	}

	public boolean isObserver()
	{
		return false;
	}


	public void onPlayRequired()
	{
		// todo: wahrscheinlich nicht.
//		if (playground.getNumberSwingPlayers() > 1)
//		{
//			this.jRack.setBorder(play.player == this
//					? new LineBorder(Color.green.darker(), 6)
//					: null);
//			SwingUtilities.invokeLater(() -> this.rackFrame.pack());
//		}

		playground.onPlayRequired(this);
	}
}
