package oscrabble.client;

import oscrabble.data.Player;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.UUID;

/**
 * Panel for the display of the actual score.
 */
class JScoreboard extends JPanel
{
	JScoreboard()
	{
		setPreferredSize(new Dimension(200, 0));
		setLayout(new GridBagLayout());
		setBorder(new TitledBorder("Score"));
	}

	void updateDisplay(final List<Player> players, final UUID playerOnTurn)
	{
		final double SMALL_WEIGHT = 0.1;
		final double BIG_WEIGHT = 10;

		final int LINE_HEIGHT = 26;
		final Dimension buttonDim = new Dimension(LINE_HEIGHT - 4, LINE_HEIGHT - 6);
		final GridBagConstraints c = new GridBagConstraints();
		removeAll();
		for (final Player player : players)
		{
			c.insets = new Insets(0, 0, 0, 0);
			c.gridy++;
			c.gridx = 0;
			c.weightx = SMALL_WEIGHT;
			final JLabel currentPlaying = new JLabel("► ");
			currentPlaying.setSize(buttonDim);
			add(currentPlaying, c);
			currentPlaying.setVisible(player.id.equals(playerOnTurn));

			c.gridx++;
			c.weightx = BIG_WEIGHT;
			c.anchor = GridBagConstraints.LINE_START;
			add(new JLabel(player.name), c);
			c.weightx = SMALL_WEIGHT;

			c.gridx++;
			c.anchor = GridBagConstraints.LINE_END;
			add(new JLabel(Integer.toString(player.score)), c);

			c.gridx++;
			final JButton parameterButton = new JButton();
			parameterButton.setPreferredSize(buttonDim);
			parameterButton.setFocusable(false);
			parameterButton.setAction(new AbstractAction("...")
			{
				@Override
				public void actionPerformed(final ActionEvent e)
				{
					// todo
					final SwingWorker<Void, Void> worker = new SwingWorker<>()
					{
						@Override
						protected Void doInBackground()
						{
//							JScoreboard.this.game.editParameters(playground.swingPlayers.getFirst().getPlayerKey(), player);
							return null;
						}
					};
					worker.execute();
				}
			});
			// todo
//			parameterButton.setVisible(player.hasEditableParameters());
			final JPanel p = new JPanel();
			p.setMinimumSize(new Dimension(LINE_HEIGHT, LINE_HEIGHT));
			p.add(parameterButton);
			add(p, c);

		}

		c.gridy++;
		c.gridx = 0;
		c.weighty = 5.0f;
		add(new JPanel(), c);

		final Insets i = getInsets();
		setPreferredSize(new Dimension(getPreferredSize().width, LINE_HEIGHT * players.size() + i.top + i.bottom));
		validate();
		repaint();
	}

}
