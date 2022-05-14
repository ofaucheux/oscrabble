package oscrabble.client.vaadin;

import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;

class ImageGeneratorTest {

	@SneakyThrows
	@Test
	void generateCellBox() {
		final byte[] bytes = new ImageGenerator().generateCellBox(
				1,
				3
		);
		FileUtils.writeByteArrayToFile(
				new File("C:\\temp\\testGenerateCellBox.png"),
				bytes
		);
	}
}