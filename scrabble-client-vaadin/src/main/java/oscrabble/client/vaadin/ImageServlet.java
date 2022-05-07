package oscrabble.client.vaadin;

import lombok.SneakyThrows;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import oscrabble.data.Tile;
import oscrabble.data.objects.Grid;

import javax.servlet.http.HttpServlet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@RequestMapping("image")
public class ImageServlet extends HttpServlet {

	private static final String IMAGE_MAPPING = "image";
	private static final ImageGenerator imageGenerator = new ImageGenerator();

	@SneakyThrows
	@RequestMapping(value = "/{name}", produces = MediaType.IMAGE_PNG_VALUE)
	@ResponseBody
	public static byte[] getImage(@PathVariable String name) {
		final Matcher tileNameMatcher = Pattern.compile("(.*)_(.*)_(.*)_(.*).png").matcher(name);
		if (ImageGenerator.GRID_FILENAME.equals(name)) {
			return imageGenerator.generateGridImage();
		}

		if (tileNameMatcher.matches()) {
			final Tile tile = Tile.builder()
					.c(tileNameMatcher.group(1).charAt(0))
					.points(Integer.parseInt(tileNameMatcher.group(2)))
					.isJoker(Boolean.parseBoolean(tileNameMatcher.group(3)))
					.build();
			final boolean flash = Boolean.parseBoolean(tileNameMatcher.group(4));
			return imageGenerator.generateTileImage(tile, flash);
		}

		try {
			return imageGenerator.generateDirectionArrowImage(Grid.Direction.valueOf(name));
		} catch (IllegalArgumentException e) {
			// ok
		}

		throw new IllegalArgumentException("Cannot find page \"" + name + "\"");
	}

	public static String urlForTile(final boolean withPath, final Tile tile, final boolean flash) {
		final StringBuilder sb = new StringBuilder();
		if (withPath) {
			sb.append(IMAGE_MAPPING).append('/');
		}
		sb.append(tile.c)
				.append("_")
				.append(tile.points)
				.append("_")
				.append(tile.isJoker)
				.append("_")
				.append(flash)
				.append(".png");
		return sb.toString();
	}

	public static String urlForGrid() {
		return String.format("%s/%s", IMAGE_MAPPING, ImageGenerator.GRID_FILENAME);
	}

	public static String urlForStartWordArrow(Grid.Direction direction) {
		return String.format("%s/%s_%s.png", IMAGE_MAPPING, ImageGenerator.ARROW_FILENAME, direction.name());
	}
}