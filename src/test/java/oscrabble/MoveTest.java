package oscrabble;

import org.junit.jupiter.api.Test;

import java.text.ParseException;

import static org.junit.jupiter.api.Assertions.*;

class MoveTest
{
	final Grid grid = new Grid(null);

	@Test
	void parseMove() throws ParseException
	{

		final String a13_chien = "A13 CHIEN";
		final Move chien = Move.parseMove(grid, a13_chien, true);
		assertEquals(a13_chien, chien.getNotation());
		assertEquals(1, chien.startSquare.getX());
		assertEquals(13, chien.startSquare.getY());
		assertEquals("CHIEN", chien.word);
		assertEquals(Move.Direction.VERTICAL, chien.getDirection());

		final String _12F = "12F";
		final Move chat = Move.parseMove(grid, _12F, true);
		assertEquals(_12F, chat.getNotation());
		assertEquals(6, chat.startSquare.getX());
		assertEquals(12, chat.startSquare.getY());
		assertEquals("", chat.word);
		assertEquals(Move.Direction.HORIZONTAL, chat.getDirection());

		try
		{
			Move.parseMove(grid, _12F);
			fail();
		}
		catch (ParseException ex)
		{
			// OK
		}

		try
		{
			Move.parseMove(grid, "AB13", true);
			fail("should have failed");
		}
		catch (ParseException e)
		{
			// ist gewünscht
		}

		try
		{
			Move.parseMove(grid, "AB13 Porc-Epique", true);
			fail("should have failed");
		}
		catch (ParseException e)
		{
			// ist gewünscht
		}


	}

	@Test
	void getInverseDirection() throws ParseException
	{
		final Move chien = Move.parseMove(grid, "A13 CHIEN", true);
		final Move chat = chien.getInvertedDirectionCopy();
		assertEquals(chien.startSquare.getX(), chat.startSquare.getX());
		assertEquals(chien.startSquare.getY(), chat.startSquare.getY());
		assertNotEquals(chien.getDirection(), chat.getDirection());
		assertEquals(chien.word, chat.word);

	}
}