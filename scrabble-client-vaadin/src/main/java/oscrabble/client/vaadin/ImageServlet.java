package oscrabble.client.vaadin;

import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import oscrabble.client.JGrid;
import oscrabble.client.JTile;
import oscrabble.client.ui.StartWordArrow;
import oscrabble.client.utils.SwingUtils;
import oscrabble.data.Tile;
import oscrabble.data.objects.Grid;

import javax.servlet.http.HttpServlet;
import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@RequestMapping("image")
public class ImageServlet extends HttpServlet {

	private static final String IMAGE_MAPPING = "image";
	private static final String GRID_FILENAME = "grid";
	private static final String ARROW_FILENAME = "arrow";

	private final static File TEMP_DIRECTORY;
	static {
		TEMP_DIRECTORY = FileUtils.getTempDirectory();
	}

	@SneakyThrows
	@RequestMapping(value = "/{name}", produces = MediaType.IMAGE_PNG_VALUE)
	@ResponseBody
	public byte[] getImage(@PathVariable String name) {
		final File pngFile = new File(TEMP_DIRECTORY, name);
		if (!pngFile.exists()) {
			final Matcher arrowNameMatcher = Pattern.compile(ARROW_FILENAME + "_(.*)\\.png").matcher(name);
			final byte[] bytes;
			if (GRID_FILENAME.equals(name)) {
				bytes = JGrid.getPNG();
			} else if (arrowNameMatcher.matches()) {
				final Grid.Direction direction = Grid.Direction.valueOf(arrowNameMatcher.group(1));
				bytes = StartWordArrow.getPNG(direction);
			} else {
				final Tile tile = tileFromFilename(name);
				bytes = SwingUtils.getImage(new JTile(tile), ScrabbleView.CELL_DIMENSION);
			}
			FileUtils.writeByteArrayToFile(pngFile, bytes);
			pngFile.deleteOnExit();
		}
		return FileUtils.readFileToByteArray(pngFile);
	}

	public static String urlForTile(final Tile tile) {
		return String.format("%s/%s_%s_%s.png", IMAGE_MAPPING, tile.c, tile.points, tile.isJoker);
	}

	public static String urlForGrid() {
		return String.format("%s/%s", IMAGE_MAPPING, GRID_FILENAME);
	}

	public static String urlForStartWordArrow(Grid.Direction direction) {
		return String.format("%s/%s_%s.png", IMAGE_MAPPING, ARROW_FILENAME, direction.name());
	}

	public static Tile tileFromFilename(final String filename) {
		final Matcher matcher = Pattern.compile("(.*)_(.*)_(.*).png").matcher(filename);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Cannot parse filename " + filename);
		}
		return Tile.builder()
				.c(matcher.group(1).charAt(0))
				.points(Integer.parseInt(matcher.group(2)))
				.isJoker(Boolean.parseBoolean(matcher.group(3)))
				.build();
	}
}