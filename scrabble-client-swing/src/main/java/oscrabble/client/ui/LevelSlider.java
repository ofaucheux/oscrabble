package oscrabble.client.ui;

import oscrabble.player.ai.AIPlayer;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Hashtable;

/**
 * Slider to choose an AI level
 */
public class LevelSlider extends JSlider {
	public LevelSlider() {
		super(0, AIPlayer.Level.values().length - 1);
		setMajorTickSpacing(1);
		setMinorTickSpacing(1);
		setPaintLabels(true);
		setSnapToTicks(true);
		final Hashtable<Integer, JLabel> labels = new Hashtable<>();

		final HashMap<AIPlayer.Level, Color> colors = new HashMap<>();
		colors.put(AIPlayer.Level.VERY_HARD, Color.red);
		colors.put(AIPlayer.Level.HARD, Color.orange);
		colors.put(AIPlayer.Level.MIDDLE, Color.yellow);
		colors.put(AIPlayer.Level.SIMPLE, Color.green.darker());
		colors.put(AIPlayer.Level.VERY_SIMPLE, Color.green);
		colors.forEach(
				(l, c) -> {
					final JLabel label = new JLabel("•");
					label.setForeground(c);
					labels.put(l.ordinal(), label);
				}) ;

		setLabelTable(labels);
	}

	/**
	 * @return the selected value
	 */
	public AIPlayer.Level getSelectedLevel() {
		return AIPlayer.Level.values()[getValue()];
	}
}
