package oscrabble.client;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import oscrabble.client.ui.JScoreboard;
import oscrabble.data.Player;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedList;
import java.util.Random;
import java.util.UUID;

@SuppressWarnings("HardCodedStringLiteral")
class JScoreboardTest {

	public static final Random RANDOM = new Random();

	@SneakyThrows
	@Test
	@Disabled// disable swing tests
	void testUpdate() {
		final LinkedList<Player> players = new LinkedList<>();
		final Player.PlayerBuilder builder = Player.builder();
		players.add(builder.name("Kevin").score(10).id(UUID.randomUUID()).build());
		players.add(builder.name("Jean").score(10).id(UUID.randomUUID()).build());
		final UUID stanley = UUID.randomUUID();
		players.add(builder.name("Stanley").score(150).id(stanley).build());
		players.add(builder.name("Grace").score(18).id(UUID.randomUUID()).build());

		final JScoreboard scb = new JScoreboard();

		final JButton button = new JButton("...");
		button.setPreferredSize(new Dimension(16, 16));
		scb.setAdditionalComponent(stanley, button);

		scb.updateDisplay(players, players.get(RANDOM.nextInt(players.size())).id);
		final JFrame f = new JFrame();
		f.add(scb);
		f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		f.setVisible(true);
		f.pack();
		while (f.isVisible()) {
			//noinspection BusyWait
			Thread.sleep(3000);
			scb.updateDisplay(players, players.get(RANDOM.nextInt(players.size())).id);
		}
	}
}