package oscrabble.utils;

import org.apache.commons.io.FileUtils;
import oscrabble.ScrabbleError;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class TempDirectory {

	/** Directory for all files created by the use of the application */
	public static final Path TEMP_DIRECTORY = FileUtils.getTempDirectory().toPath().resolve("scrabble");
	static {
		try {
			if (!TEMP_DIRECTORY.toFile().isDirectory()) {
				FileUtils.forceMkdir(TEMP_DIRECTORY.toFile());
			}
		} catch (IOException e) {
			throw new ScrabbleError("Cannot create the temp directory " + TEMP_DIRECTORY, e);
		}
	}

	public static File getFile(final String filename) {
		return TEMP_DIRECTORY.resolve(filename).toFile();
	}


}
