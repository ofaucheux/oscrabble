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
		final PossibleMoveDisplayer pmd = new PossibleMoveDisplayer(MicroServiceDictionary.getDefaultFrench());
		final MicroServiceScrabbleServer scrabbleServer = MicroServiceScrabbleServer.getLocal();

		final GameState game = scrabbleServer.loadFixtures().iterator().next();
		pmd.updateList(scrabbleServer, game, Arrays.asList('c', 'A', 'R'));
		JOptionPane.showMessageDialog(null, pmd.mainPanel);
		while (pmd.mainPanel.isVisible())
		{
			Thread.sleep(100);
		}
	}
}