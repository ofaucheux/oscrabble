package oscrabble.controller;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("HardCodedStringLiteral")
class SetRefusedWordActionTest {

	@Test
	void getNotation() {
		String notation = "[\"JEAN\",\"INTERNET\",\"METHODE\"]";
		final SetRefusedWordAction action = new SetRefusedWordAction(notation);
		final HashSet<Object> words = new HashSet<>(Arrays.asList("METHODE", "JEAN", "INTERNET"));
		assertEquals(
				words,
				action.getRefusedWords()
		);
		assertEquals(
				"REFUSE:[\"INTERNET\",\"JEAN\",\"METHODE\"]",
				action.getNotation()
		);
	}
}