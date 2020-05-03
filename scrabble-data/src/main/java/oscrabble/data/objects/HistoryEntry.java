package oscrabble.data.objects;

import oscrabble.controller.Action;

import java.util.HashMap;
import java.util.Set;

/**
 * Description of a played move.
 */
public class HistoryEntry
{
	private final boolean errorOccurred;

	/**
	 * Points gained by this play for each player
	 */
	private final HashMap<Player, Integer> scores = new HashMap<>();

	private final Set<Character> drawn;  // to be used for rewind
	/**
	 * Information about the move at time of the action.
	 */
	private final ScoreCalculator.MoveMetaInformation metaInformation;
	private final Game.Player player;
	private Action play;

	HistoryEntry(final Game.Player player, final Action play, final boolean errorOccurred, final int score, final Set<Character> drawn, final ScoreCalculator.MoveMetaInformation metaInformation)
	{
		this.player = player;
		this.play = play;
		this.errorOccurred = errorOccurred;
		this.scores.put(player, score);
		this.drawn = drawn;
		this.metaInformation = metaInformation;
	}

	public String formatAsString()
	{
		//noinspection StringBufferReplaceableByString
		final StringBuilder sb = new StringBuilder(this.player.name);
		sb.append(" - ").append(this.errorOccurred ? "*" : "").append(this.play.notation);
		sb.append(" ").append(this.scores.get(this.player)).append(" pts");
		return sb.toString();
	}

	/**
	 * @return ob der Spielzug ein neu gelegtes Wort war
	 */
	public final boolean isPlayTileAction()
	{
		return this.play instanceof Action.PlayTiles;
	}

//		/**
//		 * @return die Buchstaben des letzten Spielzugs
//		 * @throws Error wenn der Spielzug nicht passt.
//		 */
//		public final PlayTiles getPlayTiles()
//		{
//			return ((PlayTiles) this.play.action);
//		}
}
