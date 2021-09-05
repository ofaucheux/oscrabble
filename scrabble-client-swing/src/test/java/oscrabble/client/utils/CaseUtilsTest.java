package oscrabble.client.utils;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("HardCodedStringLiteral")
class CaseUtilsTest {
	@Test
	void toSnakeCase() {
		final HashMap<String, String> data = new HashMap<>();
		data.put("myFunction", "MY_FUNCTION");
		data.put("FirstIsUppercase", "FIRST_IS_UPPERCASE");
		data.put("severalWithNumber100And200", "SEVERAL_WITH_NUMBER_100_AND_200");
		data.put("testA20", "TEST_A20");
		data.put("withUmlautÄndernAndHäuser", "WITH_UMLAUT_ÄNDERN_AND_HÄUSER");

		for (final String input : data.keySet()) {
			assertEquals(data.get(input), CaseUtils.toSnakeCase(input));
		}
	}
}