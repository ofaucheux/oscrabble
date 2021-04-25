package oscrabble.utils;

import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

/**
 * Lock files to inform about an application state
 */
public class PidFiles {
	final static public File PID_FILE_DICTIONARY = new File(FileUtils.getTempDirectory(), "dictionary.pid");
	final static public File PID_FILE_SERVER = new File(FileUtils.getTempDirectory(), "server.pid");

	static boolean isLocked(final File file) {
		if (!file.exists()) {
			return false;
		}

		FileChannel fileChannel;
		try {
			fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.WRITE);
			final boolean locked = fileChannel.tryLock() == null;
			fileChannel.close();
			return locked;
		} catch (IOException e) {
			return true;
		}
	}

	public static boolean isDictionaryRunning() {
		return isLocked(PID_FILE_DICTIONARY);
	}

	public static boolean isServerRunning() {
		return isLocked(PID_FILE_SERVER);
	}

	@SneakyThrows
	public static void writePid(File file) throws FileNotFoundException {
		final FileOutputStream fos = new FileOutputStream(file);
		fos.getChannel().lock();
		final PrintStream ps = new PrintStream(fos);
		ps.println("Process id: " + ProcessHandle.current().pid());
		ps.println("Started at " + Instant.now().toString());
		ps.flush();
		// don't close or return the stream: it should not be close until the java process ends
	}
}