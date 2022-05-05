package oscrabble.client.utils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class NameUtilsTest {
	@Test
	public void getFrenchFirstNames() {
		final ArrayList<String> names = NameUtils.getFrenchFirstNames();
		assertTrue(names.contains("Olivier"));
	}
}
