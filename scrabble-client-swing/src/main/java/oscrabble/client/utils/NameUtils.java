package oscrabble.client.utils;

import lombok.SneakyThrows;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class NameUtils {
	@SneakyThrows
	public static ArrayList<String> getFrenchFirstNames() {
		final ArrayList<String> names = new ArrayList<>();
		final BufferedReader is = new BufferedReader(new InputStreamReader(
				NameUtils.class.getResourceAsStream("frenchFirstNames.txt"),
				StandardCharsets.UTF_8
		));
		String line;
		while ((line = is.readLine()) != null) {
			names.add(line);
		}
		return names;
	}
}
