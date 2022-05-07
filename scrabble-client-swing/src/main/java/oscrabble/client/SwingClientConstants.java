package oscrabble.client;

import oscrabble.ScrabbleConstants;
import oscrabble.data.objects.Grid;

import java.awt.*;

public interface SwingClientConstants {
	int CELL_SIZE = 40;
	Dimension CELL_DIMENSION = new Dimension(CELL_SIZE, CELL_SIZE);
	Dimension GRID_DIMENSION = new Dimension(
			CELL_SIZE * Grid.GRID_SIZE_PLUS_2,
			CELL_SIZE * Grid.GRID_SIZE_PLUS_2
	);
}
