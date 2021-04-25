package oscrabble.utils;

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
		FileChannel fileChannel = null;
		try {
			fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.WRITE);
			try {
				fileChannel.close();
			} catch (IOException e) {
				throw new IOError(e);
			}
			return false;
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

	public static void writePid(File file) throws FileNotFoundException {
		final FileOutputStream fos = new FileOutputStream(file);
		final PrintStream ps = new PrintStream(fos);
		ps.println("Process id: " + ProcessHandle.current().pid());
		ps.println("Started at " + Instant.now().toString());
		// don't close or return the stream: it should not be close until the java process ends
	}
}