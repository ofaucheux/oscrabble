package oscrabble.client.vaadin;

import ch.reto_hoehener.japng.Apng;
import ch.reto_hoehener.japng.ApngFactory;
import lombok.SneakyThrows;
import oscrabble.client.JGrid;
import oscrabble.client.JTile;
import oscrabble.client.ui.StartWordArrow;
import oscrabble.client.utils.SwingUtils;
import oscrabble.data.Tile;
import oscrabble.data.objects.Grid;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.CRC32;

public class ImageGenerator {
	static final String GRID_FILENAME = "grid";
	static final String ARROW_FILENAME = "arrow";
	private static final ConcurrentHashMap<Integer, byte[]> imageCache = new ConcurrentHashMap<>();
	private static final Random RANDOM = new Random();
	public byte[] generateDirectionArrowImage(final Grid.Direction direction) {
		return StartWordArrow.getPNG(direction);
	}

	@SneakyThrows
	public byte[] generateTileImage(final Tile tile, final boolean flash) {
		final String key = String.format("%s-%s-%s-%s", tile.c, tile.points, tile.isJoker, flash);
		byte[] image = imageCache.get(key.hashCode());
		if (image == null) {
			final byte[] tileImage = SwingUtils.getImage( new JTile(tile), ScrabbleView.CELL_DIMENSION);
			if (flash) {
				// create the flashing image as a APNG one
				final byte[] empty = SwingUtils.getImage(null, ScrabbleView.CELL_DIMENSION);
				Apng apng = ApngFactory.createApng();
				final int delay = 500;
				for (int i = 0; i < 3; i++) {
					apng.addFrame(new ByteArrayInputStream(tileImage), delay);
					apng.addFrame(new ByteArrayInputStream(empty), delay);
				}

				// Firefox (and Edge, perhaps other ones too) won't flash the tile if the same tile already have been displayed
				// once (it's the case if the same letter already have been played at a previous turn). I suppose the browsers
			    // synchronize the display of the same image, what make problems in our fall because the first flashing occurrence
				// (from a previous turn) is not flashing anymore.
				// As solution: we use a random algorithm, so the image is not 100 % the same. The user won't remark anything,
				// but the tiles will flash.
				final int lastFrameDuration = 32767 /* maximal value the apng accepts */ - RANDOM.nextInt(1000);
				apng.addFrame(new ByteArrayInputStream(tileImage), lastFrameDuration);

				//noinspection SpellCheckingInspection
				final ByteArrayOutputStream baos = new ByteArrayOutputStream();
				apng.assemble(baos);
				image = baos.toByteArray();
			} else {
				image = tileImage;
				imageCache.put(key.hashCode(), image);
			}
		}
		return image;
	}

	@SneakyThrows
	public byte[] generateGridImage() {
		return JGrid.getPNG();
	}
}
