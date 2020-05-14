package oscrabble.client;

import oscrabble.data.Player;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Panel for the display of the actual score.
 */
class JScoreboard extends JPanel
{
	private final Playground playground;

	/**
	 * Mapping player id -> corresponding line
	 */
	private final HashMap<UUID, ScorePanelLine> scoreLabels = new HashMap<>();

	JScoreboard(final Playground playground)
	{
		this.playground = playground;
		setPreferredSize(new Dimension(200, 0));
		setLayout(new GridBagLayout());
		setBorder(new TitledBorder("Score"));
	}
//
//	void refreshDisplay()
//	{
//		for (final IPlayerInfo playerInfo : this.game.getPlayers())
//		{
//			this.scoreLabels.get(playerInfo).score.setText(playerInfo.getScore() + " pts");
//		}
//	}


	void updateDisplay(final List<Player> players, final UUID playerOnTurn)
	{
		final double SMALL_WEIGHT = 0.1;
		final double BIG_WEIGHT = 10;

		final Dimension buttonDim = new Dimension(20, 20);
		final GridBagConstraints c = new GridBagConstraints();
		for (final Player player : players)
		{
			ScorePanelLine line = this.scoreLabels.get(player.id);
			if (line == null)
			{
				line = new ScorePanelLine();
				this.scoreLabels.put(player.id, line);

				c.insets = new Insets(0, 0, 0, 0);
				c.gridy++;
				c.gridx = 0;
				c.weightx = SMALL_WEIGHT;
				line.currentPlaying = new JLabel("►");
				line.currentPlaying.setPreferredSize(buttonDim);
				line.currentPlaying.setVisible(false);
				add(line.currentPlaying, c);

				c.gridx++;
				c.weightx = BIG_WEIGHT;
				c.anchor = GridBagConstraints.LINE_START;
				final String name = player.name;
				line.nameLabel = new JLabel(name);
				add(line.nameLabel, c);
				c.weightx = SMALL_WEIGHT;

				c.gridx++;
				c.anchor = GridBagConstraints.LINE_END;
				line.score = new JLabel();
				add(line.score, c);

				c.gridx++;
				line.parameterButton = new JButton();
				line.parameterButton.setPreferredSize(buttonDim);
				line.parameterButton.setFocusable(false);
				line.parameterButton.setAction(new AbstractAction("...")
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
	//			line.parameterButton.setVisible(player.hasEditableParameters());
				add(line.parameterButton, c);
			}

			line.nameLabel.setText(player.name);
			line.currentPlaying.setVisible(player.id.equals(playerOnTurn));
			line.score.setText(Integer.toString(player.score));
		}

		c.gridy++;
		c.gridx = 0;
		c.weighty = 5.0f;
		add(new JPanel(), c);

		setPreferredSize(new Dimension(200, 50 * players.size()));
		getParent().validate();
	}

	private static class ScorePanelLine
	{
		private JLabel nameLabel;
		private JLabel score;
		private JLabel currentPlaying;
		private JButton parameterButton;
	}
}
