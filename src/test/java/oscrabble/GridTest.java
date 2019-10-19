package oscrabble;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.TreeBag;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import oscrabble.client.SwingClientTest;
import oscrabble.dictionary.Dictionary;

import java.awt.*;
import java.text.ParseException;

import static org.junit.jupiter.api.Assertions.*;

class GridTest
{
	@Test
	void getMetaInformation() throws ScrabbleException, ParseException
	{
		final Grid grid = new Grid(Dictionary.getDictionary(Dictionary.Language.FRENCH));

		Grid.MoveMetaInformation metaInformation;
		Move move;
		SwingClientTest.showGrid(grid);

		// erster Test
		move = Move.parseMove(grid, "A1 FINIT", false);
		metaInformation = grid.getMetaInformation(move);
		assertEquals(27, metaInformation.score);
		Assertions.assertEquals(0, metaInformation.crosswords.size());
		assertBagContent(metaInformation.getRequiredLetters(), "FINIT");
		grid.put(move);

		move = Move.parseMove(grid,"1A FEMME", false);
		assertBagContent(
				grid.getMetaInformation(move).getRequiredLetters(),
				"EMME"
		);
		grid.put(move);

		// erster Test
		move = Move.parseMove(grid, "J2 ELEPHANT", false);
		metaInformation = grid.getMetaInformation(move);
		assertEquals(23, metaInformation.score);
		Assertions.assertEquals(0, metaInformation.crosswords.size());
		grid.put(move);

		// Cross word
		move = Move.parseMove(grid, "B5 ELU", false);
		metaInformation = grid.getMetaInformation(move);
		Assertions.assertEquals(1, metaInformation.crosswords.size());
		assertEquals(7, metaInformation.score);
		assertBagContent(metaInformation.getRequiredLetters(), "ELU");
		grid.put(move);

		// Cross word mit cross auf Bonus
		move = Move.parseMove(grid, "C7 SUE", false);
		metaInformation = grid.getMetaInformation(move);
		assertEquals(1, metaInformation.crosswords.size());
		assertEquals(8, metaInformation.score);
		grid.put(move);

		// Blank
		move = Move.parseMove(grid, "5J PhASME", false);
		metaInformation = grid.getMetaInformation(move);
		assertEquals(16, metaInformation.score);
		grid.put(move);
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