package oscrabble.client;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import oscrabble.client.utils.SwingUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;

class JTileTest {

	@BeforeEach
	public void setUpHeadlessMode() {
		System.setProperty("java.awt.headless", "true");
	}

	@Test
	@Disabled
	public void test() throws InterruptedException, IOException {
		final JGrid grid = new JGrid();
		grid.setLayout(new GridLayout(2, 2));

		JTile stone;
		stone = new JTile('A', 1, false);
		grid.add(stone);
		stone = new JTile('Y', 10, false);
		grid.add(stone);
		stone = new JTile('c', 0, true);
		grid.add(stone);

		final byte[] png = SwingUtils.getImage(grid, new Dimension(600, 600)).getRight();
		FileUtils.writeByteArrayToFile(
				new File("C:/temp/2022-04-22/" + System.currentTimeMillis() + ".png"),
				png
		);
	}
}