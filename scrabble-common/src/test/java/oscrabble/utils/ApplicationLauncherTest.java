package oscrabble.utils;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
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
				new File("C:\\utils"),
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
}