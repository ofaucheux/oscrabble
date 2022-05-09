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
		FileUtils.writeByteArrayToFile(new File("C:/temp/test_a.png"), image);
	}
}