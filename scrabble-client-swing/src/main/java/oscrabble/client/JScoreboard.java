package oscrabble.client;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashMap;

/**
 * Panel for the display of the actual score.
 */
class JScoreboard extends JPanel
{
	private final Playground playground;
	private final IGame game;
	private final HashMap<IPlayerInfo, ScorePanelLine> scoreLabels = new HashMap<>();

	JScoreboard(final Playground playground, final IGame game)
	{
		this.playground = playground;
		this.game = game;
		setPreferredSize(new Dimension(200, 0));
		setLayout(new GridBagLayout());
		setBorder(new TitledBorder("Score"));
	}

	void refreshDisplay()
	{
		for (final IPlayerInfo playerInfo : this.game.getPlayers())
		{
			this.scoreLabels.get(playerInfo).score.setText(playerInfo.getScore() + " pts");
		}
	}

	void prepareBoard()
	{
		final double SMALL_WEIGHT = 0.1;
		final double BIG_WEIGHT = 10;

		final Dimension buttonDim = new Dimension(20, 20);
		final List<IPlayerInfo> players = this.game.getPlayers();
		final GridBagConstraints c = new GridBagConstraints();
		for (final IPlayerInfo player : players)
		{
			final ScorePanelLine line = new ScorePanelLine();
			this.scoreLabels.put(player, line);

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
			final String name = player.getName();
			add(new JLabel(name), c);
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
					final SwingWorker<Void, Void> worker = new SwingWorker<>()
					{
						@Override
						protected Void doInBackground()
						{
							JScoreboard.this.game.editParameters(playground.swingPlayers.getFirst().getPlayerKey(), player);
							return null;
						}
					};
					worker.execute();
				}
			});
			line.parameterButton.setVisible(player.hasEditableParameters());
			add(line.parameterButton, c);

		}

		c.gridy++;
		c.gridx = 0;
		c.weighty = 5.0f;
		add(new JPanel(), c);

		setPreferredSize(new Dimension(200, 50 * players.size()));
		getParent().validate();
	}

	private class ScorePanelLine
	{
		private JLabel score;
		private JLabel currentPlaying;
		private JButton parameterButton;
	}
}
