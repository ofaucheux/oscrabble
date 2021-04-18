package oscrabble.client;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import oscrabble.client.ui.JScoreboard;
import oscrabble.client.ui.LevelSlider;
import oscrabble.data.Player;

import javax.swing.*;
import java.util.LinkedList;
import java.util.Random;
import java.util.UUID;

class JScoreboardTest {

	public static final Random RANDOM = new Random();

	@SneakyThrows
	@Test
//	@Disabled
	void testUpdate() {
		final LinkedList<Player> players = new LinkedList<>();
		final Player.PlayerBuilder builder = Player.builder();
		players.add(builder.name("Kevin").score(10).id(UUID.randomUUID()).build());
		players.add(builder.name("Jean").score(10).id(UUID.randomUUID()).build());
		final UUID standley = UUID.randomUUID();
		players.add(builder.name("Standley").score(150).id(standley).build());
		players.add(builder.name("Grace").score(18).id(UUID.randomUUID()).build());

		final JScoreboard scb = new JScoreboard();
		scb.setAdditionalComponent(standley, new LevelSlider());
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