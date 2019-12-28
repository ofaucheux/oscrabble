package oscrabble.client;

import oscrabble.ScrabbleException;
import oscrabble.Tile;
import oscrabble.player.AbstractPlayer;
import oscrabble.server.Game;
import oscrabble.server.Play;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.UUID;

public class SwingPlayer extends AbstractPlayer
{
	static Playground playground;
	JRack jRack;

	public SwingPlayer(final String name)
	{
		super(name);
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

	private void createUI()
	{
		final JDialog rackFrame = new JDialog(playground.gridFrame);
		rackFrame.setTitle(this.getName());

		rackFrame.setLayout(new BorderLayout());
		rackFrame.add(this.jRack);

		final JButton exchangeButton = new JButton((new ExchangeTilesAction()));
		exchangeButton.setToolTipText(exchangeButton.getText());
		exchangeButton.setHideActionText(true);
		final Dimension dim = new Dimension(30, 20);
		exchangeButton.setMaximumSize(dim);
		exchangeButton.setPreferredSize(dim);
		exchangeButton.setIcon(exchangeButton.getIcon());

		rackFrame.add(exchangeButton, BorderLayout.AFTER_LINE_ENDS);
		rackFrame.pack();
		rackFrame.setVisible(true);
		rackFrame.setLocation(
				playground.gridFrame.getX() + playground.gridFrame.getWidth(),
				playground.gridFrame.getY() + playground.gridFrame.getHeight() / 2
		);
		rackFrame.setFocusableWindowState(false);
		rackFrame.setFocusable(false);

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
						game.getRack(SwingPlayer.this, SwingPlayer.this.playerKey));

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
				playground.showMessage(null, e.getMessage());
			}
		}
	}

	@Override
	public void afterRollback()
	{
		playground.refreshUI(this);
	}


	/**
	 *
	 */
	private class ExchangeTilesAction extends AbstractAction
	{
		ExchangeTilesAction()
		{
			super(
					"Exchange tiles",
					new ImageIcon(Playground.class.getResource("exchangeTiles.png"))
			);
		}

		@Override
		public void actionPerformed(final ActionEvent e)
		{
			final JFrame frame = new JFrame("Exchange");
			frame.setLayout(new BorderLayout());

			final JPanel carpet = new JPanel();
			carpet.setBackground(Playground.SCRABBLE_GREEN);
			final Dimension carpetDimension = new Dimension(250, 250);
			carpet.setPreferredSize(carpetDimension);
			carpet.setSize(carpetDimension);
			frame.add(carpet, BorderLayout.NORTH);
			frame.add(new JButton(new AbstractAction("Exchange them!")
			{
				@Override
				public void actionPerformed(final ActionEvent e)
				{
//					exchange(); TODO
					frame.dispose();
				}
			}), BorderLayout.SOUTH);

			frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			frame.setVisible(true);
			frame.setLocationRelativeTo(SwingPlayer.this.jRack);
			frame.pack();
		}
	}

	@Override
	public void onDispatchMessage(final String msg)
	{
		playground.showMessage(this, msg);
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
	public void onPlayRequired(final Play play)
	{
		playground.onPlayRequired(this, play);
	}
}
