package oscrabble;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.TreeBag;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import oscrabble.action.PlayTiles;
import oscrabble.client.PlaygroundTest;
import oscrabble.dictionary.Language;
import oscrabble.dictionary.ScrabbleLetterInformation;

import java.awt.*;
import java.text.ParseException;

import static org.junit.jupiter.api.Assertions.*;

class GridTest
{
	@Test
	void getMetaInformation() throws ScrabbleException, ParseException, InterruptedException
	{
		final Grid grid = new Grid(new ScrabbleLetterInformation(Language.FRENCH));

		Grid.MoveMetaInformation metaInformation;
		PlayTiles playTiles;
		PlaygroundTest.showGrid(grid);

		// erster Test
		playTiles = (PlayTiles) PlayTiles.parseMove(grid, "A1 FINIT");
		metaInformation = grid.getMetaInformation(playTiles);
		assertEquals(27, metaInformation.score);
		Assertions.assertEquals(0, metaInformation.crosswords.size());
		assertBagContent(metaInformation.getRequiredLetters(), "FINIT");
		grid.put(playTiles);

		playTiles = (PlayTiles) PlayTiles.parseMove(grid,"1A FEMME");
		assertBagContent(
				grid.getMetaInformation(playTiles).getRequiredLetters(),
				"EMME"
		);
		grid.put(playTiles);

		// erster Test
		playTiles = (PlayTiles) PlayTiles.parseMove(grid, "J2 ELEPHANT");
		metaInformation = grid.getMetaInformation(playTiles);
		assertEquals(23, metaInformation.score);
		Assertions.assertEquals(0, metaInformation.crosswords.size());
		grid.put(playTiles);

		// Cross word
		playTiles = (PlayTiles) PlayTiles.parseMove(grid, "B5 ELU");
		metaInformation = grid.getMetaInformation(playTiles);
		Assertions.assertEquals(1, metaInformation.crosswords.size());
		assertEquals(7, metaInformation.score);
		assertBagContent(metaInformation.getRequiredLetters(), "ELU");
		grid.put(playTiles);

		// Cross word mit cross auf Bonus
		playTiles = (PlayTiles) PlayTiles.parseMove(grid, "C7 SUE");
		metaInformation = grid.getMetaInformation(playTiles);
		assertEquals(1, metaInformation.crosswords.size());
		assertEquals(8, metaInformation.score);
		grid.put(playTiles);

		// Blank
		playTiles = (PlayTiles) PlayTiles.parseMove(grid, "5J PhASME");
		metaInformation = grid.getMetaInformation(playTiles);
		assertEquals(16, metaInformation.score);
		grid.put(playTiles);

		// Remove
		Thread.sleep(500);
		grid.remove(metaInformation);
		assertFalse(grid.getSquare("5J").isEmpty());
		assertTrue(grid.getSquare("5K").isEmpty());
	}

	private void assertBagContent(final Bag<Character> bag, final String letters)
	{
		final TreeBag<Character> copy = new TreeBag<>();
		copy.addAll(bag);
		for (final char c : letters.toCharArray())
		{
			assertTrue(copy.contains(c), "Missing " + c + " in bag " + bag);
			copy.remove(c, 1);
		}
		assertTrue(copy.isEmpty(), "Bag assertContains too much characters: " + bag);
	}

	@AfterAll
	static void sleep() throws InterruptedException
	{
		for (final Frame frame : Frame.getFrames())
		{
			while (frame.isVisible())
			{
				Thread.sleep(100);
			}
		}
	}
}