package oscrabble.client;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationTest {
	@Test
	public void getFrenchFirstNames() {
		final ArrayList<String> names = Application.getFrenchFirstNames();
		assertTrue(names.contains("Olivier"));
	}
}