package oscrabble.data;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class Square
{
	/**
	 * Coordinate of the square
	 */
	public String coordinate;

	/**
	 * Bonus of the square // TODO: fill it
	 */
	public int letterBonus;

	/**
	 * Bonus of the square // TODO: fill it
	 */
	public int wordBonus;

	/**
	 * Tile set on the square, if any.
	 */
	public Tile tile;

	/**
	 * the game play which set the tile on the square, if any
	 */
	public UUID settingPlay; // TODO: to be filled
}
