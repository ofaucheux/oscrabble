package oscrabble.client.ui;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import oscrabble.player.ai.AIPlayer;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class AIPlayerConfigPanelTest {

	/**
	 * test disabled because requires user interaction.
	 */
	@Disabled
	@Test
	void displayPanel() {

		System.out.println("TEST");
		final AIPlayer player = AIPlayer.createTestMocker();

		final ArrayList<AIPlayerConfigPanel> panels = new ArrayList<>();
		panels.add(new AIPlayerConfigPanel(player));
		panels.add(new AIPlayerConfigPanel(player));

		Executors.newScheduledThreadPool(8).scheduleAtFixedRate(
				() -> panels.forEach(p -> p.refreshContent()),
				0,
				2,
				TimeUnit.SECONDS
		);

		final JPanel demoPanel = new JPanel(new GridLayout(0, 2));
		panels.forEach(p -> demoPanel.add(p));
		JOptionPane.showConfirmDialog(null, demoPanel);
		System.out.println(player);
	}
}