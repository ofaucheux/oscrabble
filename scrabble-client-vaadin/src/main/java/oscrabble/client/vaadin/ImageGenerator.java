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
import java.util.concurrent.ConcurrentHashMap;

public class ImageGenerator {
	static final String GRID_FILENAME = "grid";
	static final String ARROW_FILENAME = "arrow";

	private static final ConcurrentHashMap<Integer, byte[]> imageCache = new ConcurrentHashMap<>();

	public byte[] generateDirectionArrowImage(final Grid.Direction direction) {
		return StartWordArrow.getPNG(direction);
	}

	@SneakyThrows
	public byte[] generateTileImage(final Tile tile, final boolean flash) {
		final int key = tile.hashCode() + (flash ? 1024 : 0);
		byte[] value = imageCache.get(key);
		if (value == null) {
			final byte[] tileImage = SwingUtils.getImage(new JTile(tile), ScrabbleView.CELL_DIMENSION);
			if (flash) {
				// create the flashing image as a APNG one
				final byte[] empty = SwingUtils.getImage(null, ScrabbleView.CELL_DIMENSION);
				Apng apng = ApngFactory.createApng();
				final int delay = 500;
				for (int i = 0; i < 3; i++) {
					apng.addFrame(new ByteArrayInputStream(tileImage), delay);
					apng.addFrame(new ByteArrayInputStream(empty), delay);
				}
				apng.addFrame(new ByteArrayInputStream(tileImage), 32767 /* maximal value the apng accepts */);

				//noinspection SpellCheckingInspection
				final ByteArrayOutputStream baos = new ByteArrayOutputStream();
				apng.assemble(baos);
				value = baos.toByteArray();
			} else {
				value = tileImage;
			}
			imageCache.put(key, value);
		}
		return value;
	}


	@SneakyThrows
	public byte[] generateGridImage() {
		return JGrid.getPNG();
	}
}
