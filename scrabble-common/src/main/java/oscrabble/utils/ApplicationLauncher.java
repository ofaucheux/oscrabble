package oscrabble.utils;

import lombok.SneakyThrows;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class for the start of external applications
 */
public class ApplicationLauncher {

	private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationLauncher.class);

	/**
	 * Start an application defined as a spring boot application in the current java machine.
	 */
	@SneakyThrows
	public static void startSpringBootApplicationInplace(final File springBootApplication) {

		// Class name to Class object mapping.
		final Map<String, Class<?>> classMap = new HashMap<>();

		final JarFile jarFile = new JarFile(springBootApplication);
		final Enumeration<JarEntry> jarEntryEnum = jarFile.entries();

		final URL[] urls = {new URL("jar:file:" + springBootApplication + "!/")};
		final URLClassLoader urlClassLoader = URLClassLoader.newInstance(urls);
		while (jarEntryEnum.hasMoreElements()) {
			final JarEntry jarEntry = jarEntryEnum.nextElement();

			final String jarEntryName = jarEntry.getName();
			if (jarEntryName.startsWith("org/springframework/boot") && jarEntryName.endsWith(".class")) {
				int endIndex = jarEntryName.lastIndexOf(".class");
				String className = jarEntryName.substring(0, endIndex).replace('/', '.');
				try {
					final Class<?> loadedClass = urlClassLoader.loadClass(className);
					classMap.put(loadedClass.getName(), loadedClass);
				} catch (final ClassNotFoundException ex) {
//					LOGGER.info(ex.toString());
				}
			}
		}

		jarFile.close();

		final Object jarFileArchive = classMap
				.get("org.springframework.boot.loader.archive.JarFileArchive")
				.getConstructor(File.class)
				.newInstance(springBootApplication);

		// Create JarLauncher object using JarLauncher(Archive) constructor.
		final Class<?> archiveClass = classMap.get("org.springframework.boot.loader.archive.Archive");
		final Constructor<?> jarLauncherConstructor = classMap
				.get("org.springframework.boot.loader.JarLauncher")
				.getDeclaredConstructor(archiveClass);
		jarLauncherConstructor.setAccessible(true);
		final Object jarLauncher = jarLauncherConstructor.newInstance(jarFileArchive);

		// Invoke JarLauncher#launch(String[]) method.
		final Method launchMethod = classMap
				.get("org.springframework.boot.loader.Launcher")
				.getDeclaredMethod("launch", String[].class);
		launchMethod.setAccessible(true);
		launchMethod.invoke(jarLauncher, new Object[]{new String[0]});
	}

	/**
	 * Search in a directory recursive a jar-application matching a name pattern and start it.
	 * If no searchDirectory is not given, search in the upper-directories of the current application.
	 *
	 * @param searchDirectory
	 * @param jarNamePattern
	 * @param inPlace
	 * @param args
	 * @return
	 */
	@SneakyThrows
	public static Process findAndStartJarApplication(File searchDirectory, Pattern jarNamePattern, boolean inPlace, String... args) {
		//
		// search the application jar file
		//

		final File currentJarFile = new File(
				ApplicationLauncher.class
						.getProtectionDomain()
						.getCodeSource()
						.getLocation()
						.toURI()
		);

		if (searchDirectory == null) {
			searchDirectory = currentJarFile;
			for (int i = 0; i < 6; i++) {
				searchDirectory = searchDirectory.getParentFile();
			}
		}

		final List<Path> matchingFiles = Files.find(
				searchDirectory.toPath(),
				999, (p, bfa) -> jarNamePattern.matcher(p.getFileName().getFileName().toString()).matches()
		).collect(Collectors.toList());

		if (matchingFiles.isEmpty()) {
			throw new IllegalArgumentException("No file found matching the pattern in " + searchDirectory.getAbsolutePath());
		} else if (matchingFiles.size() > 1) {
			throw new IllegalArgumentException("Several files found matching the pattern: " + matchingFiles);
		}
		final File jarFile = matchingFiles.get(0).toFile();

		//
		// Start the application
		//

		if (inPlace) {
			startSpringBootApplicationInplace(jarFile); // todo: assure it's a spring boot application
			return null;
		} else {
			return startJarApplication(jarFile, args);
		}
	}

	/**
	 * Start an application contained in a jar file.
	 */
	public static Process startJarApplication(final File jar, String ... args) throws IOException {
		Path javaExe = Paths.get(System.getProperty("java.home")).resolve("bin");
		if (SystemUtils.IS_OS_WINDOWS) {
			javaExe = javaExe.resolve("java.exe");
		} else if (SystemUtils.IS_OS_UNIX) {
			javaExe = javaExe.resolve("java");
		} else {
			throw new AssertionError("Unsupported OS: " + SystemUtils.OS_NAME);
		}

		final ArrayList<String> commands = new ArrayList<>();
		commands.add(javaExe.toFile().toString());
		commands.add("-jar");
		commands.add(jar.toString());
		commands.addAll(Arrays.asList(args));
		LOGGER.debug("Starting: " + commands);
		final Process process = new ProcessBuilder(commands).start();
		LOGGER.info(String.format(
				"Process started (pid) %d: %s",
				process.pid(),
				process.info().commandLine().orElse(commands.toString()))
		);
		return process;
	}
}
