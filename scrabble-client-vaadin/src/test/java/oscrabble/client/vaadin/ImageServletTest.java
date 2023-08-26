package oscrabble.client.vaadin;

import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import oscrabble.data.Tile;

import java.io.File;
import java.nio.file.Paths;

class ImageServletTest {

	@SneakyThrows
	@org.junit.jupiter.api.Test
	void getImage() {
		final String url = ImageServlet.urlForTile(true, Tile.builder().c('A').points(1).build(), true);
		final byte[] image = ImageServlet.getImage(Paths.get(url).getFileName().toString());
		final File outputFile = new File(FileUtils.getTempDirectory(), "test_a.png");
		FileUtils.deleteQuietly(outputFile);
		FileUtils.writeByteArrayToFile(outputFile, image);
		System.out.println("Bild generiert in " + outputFile.getAbsolutePath());
	}
}