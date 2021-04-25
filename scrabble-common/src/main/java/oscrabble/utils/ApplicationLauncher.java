package oscrabble.utils;

import lombok.SneakyThrows;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for the start of external applications
 */
public class ApplicationLauncher {

	private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationLauncher.class);

	/** flag: start inside the application, or as separated process */
	private boolean inPlace;

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
	 * Search in a jar-application matching a name pattern and start it. If several ones are found, they are sorting by their
	 * name and the last one is started - meaning: if the build number is part of the name, the last version is selected.
	 *
	 * @param searchDirectories directories to search in
	 * @param jarNamePattern name pattern
	 * @param args application arguments
	 * @return
	 */
	@SneakyThrows
	public Process findAndStartJarApplication(Collection<Path> searchDirectories, Pattern jarNamePattern, String... args) {
		if (searchDirectories == null || searchDirectories.isEmpty()) {
			throw new IllegalArgumentException("Search directory cannot be null or empty");
		}

		//
		// search the application jar file
		//

		final TreeMap<String, Path> matchingFiles = new TreeMap<>();

		for (final Path directory : searchDirectories) {
			//noinspection ConstantConditions
			final File file = directory.toFile();
			if (file.isDirectory()) {
				final String[] list = file.list(new RegexFileFilter(jarNamePattern));
				for (final String jarName : list) {
					matchingFiles.put(jarName, directory.resolve(jarName));
				}
			}
		}

		if (matchingFiles.isEmpty()) {
			throw new IllegalArgumentException("No file found matching the pattern " + jarNamePattern.pattern() + " in " + searchDirectories);
		}

		final File jarFile = matchingFiles.lastEntry().getValue().toFile();

		//
		// Start the application
		//

		if (this.inPlace) {
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

	/**
	 * Assure order
	 * <nl>
	 *     <li>scrabble-1.0.20-SNAPSHOT.jar</li>
	 *     <li>scrabble-1.0.20.jar</li>
	 *     <li>scrabble-1.0.21-SNAPSHOT.jar</li>
	 */
	public static class ApplicationJarNameComparator implements Comparator<String> {

		private static final Pattern SNAPSHOT_NAME_PATTERN = Pattern.compile("(.*)-SNAPSHOT.jar");

		@Override
		public int compare(final String o1, final String o2) {
			if (isSnapShotOf(o1, o2)) {
				return -1;
			}
			if (isSnapShotOf(o2, o1)) {
				return 1;
			}
			return o1.compareTo(o2);
		}

		/**
		 *
		 * @param o1
		 * @param o2
		 * @return if o1 is the snapshot name of o2
		 */
		private boolean isSnapShotOf(final String o1, final String o2) {
			final Matcher m1 = SNAPSHOT_NAME_PATTERN.matcher(o1);
			return m1.matches() && o2.equals(o1 + "jar");
		}
	}

}
