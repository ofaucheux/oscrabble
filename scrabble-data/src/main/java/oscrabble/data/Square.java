package oscrabble.data;

import lombok.Data;

import java.util.UUID;

@Data
public class Square
{
	public Character tile;
	public int value; //  TODO to be filled
	public boolean joker; // TODO to be filled

	/**
	 * the game play which set the tile on the square, if any
	 */
	public UUID settingPlay; // TODO: to be filled

	/**
	 * Coordinate of the square
	 */
	public String coordinate;

	/**
	 * Bonus of the square // TODO: fill it
	 */
	public String bonus;
}
