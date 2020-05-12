package oscrabble.client;

import oscrabble.ScrabbleException;
import oscrabble.client.configuration.Configuration;
import oscrabble.player.AbstractPlayer;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.UUID;

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
	private JRack jRack;
	JDialog rackFrame;

	public SwingPlayer(final String name)
	{
		this.name = name;
		if (playground == null)
		{
			playground = new Playground();
		}
		playground.addPlayer(this);
		this.jRack = new JRack();
	}

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

	private void createUI()
	{
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

	public void updateRack()
	{
		this.jRack.update(null);
	}

	private class JRack extends JPanel
	{
		static final int RACK_SIZE = 7;
		final JRackCell[] cells = new JRackCell[7];

		private JRack()
		{
			this.setLayout(new GridLayout(1,7));
			for (int i = 0; i < RACK_SIZE; i++)
			{
				this.cells[i] = new JRackCell();
				add(this.cells[i]);
			}
		}

//		void update()
//		{
//			try
//			{
//				final ArrayList<Tile> tiles = new ArrayList<>(
//						SwingPlayer.this.game.getRack(SwingPlayer.this, SwingPlayer.this.playerKey));
//
//				for (int i = 0; i < RACK_SIZE; i++)
//				{
//					this.cells[i].setTile(
//							i >= tiles.size() ? null : tiles.get(i)
//					);
//				}
//				this.repaint();
//			}
//			catch (ScrabbleException e)
//			{
//				playground.showMessage(e.getMessage());
//			}
//		}
	}

	public void afterRollback()
	{
		playground.refreshUI(this);
	}


	public void onDispatchMessage(final String msg)
	{
		playground.showMessage(msg);
	}

	public void afterPlay()
	{
		playground.afterPlay(this, null);
		this.jRack.update(null);
	}

	public void beforeGameStart()
	{
		playground.beforeGameStart();
		this.jRack.update(null);
		createUI();
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
