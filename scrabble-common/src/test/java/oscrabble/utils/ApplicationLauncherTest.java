package oscrabble.utils;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationLauncherTest {

	@SneakyThrows
	@Test
	@Disabled
		// because dependant of an external file.
	void startSpringBootApplicationInplace() {
		final File file = new File("C:/Programmierung/OScrabble/scrabble-server/build/libs/scrabble-server-1.0.18-SNAPSHOT.jar");
		assertTrue(file.isFile());
		ApplicationLauncher.startSpringBootApplicationInplace(file);
	}

	@Test
	void startApplicationByJarName() throws InterruptedException {
		final Process process = ApplicationLauncher.findAndStartJarApplication(
				Collections.singleton(new File("C:\\utils").toPath()),
				Pattern.compile("sqlt.*\\.jar"),
				false
		);
		Thread.sleep(1000);
		assertTrue(process.isAlive());
		process.destroy();
		assertFalse(process.isAlive());
	}

	@Test
	void startJarApplication() throws IOException, InterruptedException {
		final Process process = ApplicationLauncher.startJarApplication(new File("C:\\utils\\sqltool.jar"));
		Thread.sleep(1000);
		assertTrue(process.isAlive());
		process.destroy();
		assertFalse(process.isAlive());
	}

	@Test
	void testApplicationJarNameComparator() {
		final List<String> list = Arrays.asList(
				"scrabble-1.0.20-SNAPSHOT.jar",
				"scrabble-1.0.20.jar",
				"scrabble-1.0.21-SNAPSHOT.jar"
		);
		Collections.shuffle(list);
		list.sort(new ApplicationLauncher.ApplicationJarNameComparator());
		final Iterator<String> it = list.iterator();
		assertEquals("scrabble-1.0.20-SNAPSHOT.jar", it.next());
		assertEquals("scrabble-1.0.20.jar", it.next());
		assertEquals("scrabble-1.0.21-SNAPSHOT.jar", it.next());
	}
}