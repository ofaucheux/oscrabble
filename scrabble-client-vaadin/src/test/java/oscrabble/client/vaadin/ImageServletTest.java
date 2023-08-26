package oscrabble.client.vaadin;

import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import oscrabble.data.Tile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

class ImageServletTest {

	@SneakyThrows
	@org.junit.jupiter.api.Test
	void getImage() {
		final String url = ImageServlet.urlForTile(true, Tile.builder().c('A').points(1).build(), true);
		final byte[] image = ImageServlet.getImage(Paths.get(url).getFileName().toString());
		saveInTempDirectory(image, "test_a.png");
	}

	public static void saveInTempDirectory(byte[] image, String filename) throws IOException {
		final File outputFile = new File(FileUtils.getTempDirectory(), filename);
		FileUtils.deleteQuietly(outputFile);
		FileUtils.writeByteArrayToFile(outputFile, image);
		System.out.println("Bild generiert in " + outputFile.getAbsolutePath());
	}
}