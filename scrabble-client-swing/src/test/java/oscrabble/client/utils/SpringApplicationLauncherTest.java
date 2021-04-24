package oscrabble.client.utils;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class SpringApplicationLauncherTest {
	@SneakyThrows
	@Test
	@Disabled // because dependant of an external file.
	void launchJar() {
		final File file = new File("C:/Programmierung/OScrabble/scrabble-server/build/libs/scrabble-server-1.0.18-SNAPSHOT.jar");
		assertTrue(file.isFile());
		SpringApplicationLauncher.startJar(file);
	}
}