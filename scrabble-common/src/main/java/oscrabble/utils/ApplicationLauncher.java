package oscrabble.utils;

import lombok.SneakyThrows;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Utility class for the start of external applications
 */
public class ApplicationLauncher {

	/**
	 * Start an application defined as a spring boot application in the current java machine.
	 */
	@SneakyThrows
	public static void startJar(final File springBootApplication) {

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

}
