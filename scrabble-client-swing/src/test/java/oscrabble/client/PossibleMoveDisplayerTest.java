package oscrabble.client;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import oscrabble.controller.ScrabbleServerInterface;
import oscrabble.data.GameState;
import oscrabble.dictionary.Dictionary;
import oscrabble.dictionary.Language;
import oscrabble.server.Game;
import oscrabble.server.Server;

import javax.swing.*;

import java.util.Arrays;

class PossibleMoveDisplayerTest {
	@SneakyThrows
	@Test
	@Disabled("because with GUI interaction")
	public void test() {
		final ScrabbleServerInterface scrabbleServer = new Server();
		final GameState game = Game.loadFixtures().iterator().next();

		final PossibleMoveDisplayer pmd = new PossibleMoveDisplayer(Dictionary.getDictionary(Language.FRENCH));
		pmd.setFont(Playground.MONOSPACED);
		pmd.setServer(scrabbleServer);
		pmd.setGame(game.getGameId());

		pmd.setData(game, Arrays.asList('c', 'A', 'R'));
		pmd.refresh();
		JOptionPane.showMessageDialog(null, pmd.mainPanel);
		while (pmd.mainPanel.isVisible()) {
			Thread.sleep(100);
		}
	}
}