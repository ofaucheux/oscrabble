package oscrabble.client;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import oscrabble.controller.MicroServiceDictionary;
import oscrabble.controller.MicroServiceScrabbleServer;
import oscrabble.data.GameState;

import javax.swing.*;

import java.util.Arrays;

class PossibleMoveDisplayerTest
{
	@SneakyThrows
	@Test
	public void test()
	{
		final MicroServiceScrabbleServer scrabbleServer = MicroServiceScrabbleServer.getLocal();
		final GameState game = scrabbleServer.loadFixtures().iterator().next();

		final PossibleMoveDisplayer pmd = new PossibleMoveDisplayer(MicroServiceDictionary.getDefaultFrench());
		pmd.setServer(scrabbleServer);
		pmd.setGame(game.getGameId());

		pmd.updateList(game, Arrays.asList('c', 'A', 'R'));
		JOptionPane.showMessageDialog(null, pmd.mainPanel);
		while (pmd.mainPanel.isVisible())
		{
			Thread.sleep(100);
		}
	}
}