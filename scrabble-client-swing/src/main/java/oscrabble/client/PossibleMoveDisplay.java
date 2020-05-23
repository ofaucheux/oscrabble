package oscrabble.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.ScrabbleException;
import oscrabble.controller.MicroServiceDictionary;
import oscrabble.controller.MicroServiceScrabbleServer;
import oscrabble.data.GameState;
import oscrabble.data.Score;
import oscrabble.data.Tile;
import oscrabble.data.objects.Grid;
import oscrabble.player.ai.BruteForceMethod;
import oscrabble.player.ai.Strategy;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;
import java.util.*;

public class PossibleMoveDisplay
{

	public static final Logger LOGGER = LoggerFactory.getLogger(PossibleMoveDisplay.class);
	protected final JPanel mainPanel;
	private final BruteForceMethod bfm;
//	private final JButton showPossibilitiesButton;
	private final JList<Object> moveList;
	private final ButtonGroup orderButGroup;
	private Strategy strategy;

	public PossibleMoveDisplay(final MicroServiceDictionary dictionary)
	{
		this.mainPanel = new JPanel();
		this.mainPanel.setBorder(new TitledBorder(Application.MESSAGES.getString("possible.moves")));
		this.mainPanel.setSize(new Dimension(200, 300));
		this.mainPanel.setLayout(new BorderLayout());
		bfm = new BruteForceMethod(dictionary);
//		showPossibilitiesButton = new JButton(new PossibleMoveDisplayer(this, bruteForceMethod));
//		showPossibilitiesButton.setFocusable(false);

		this.orderButGroup = new ButtonGroup();
		this.moveList = new JList<>();
		this.moveList.setCellRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus)
			{
				final Component label = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (value instanceof Score)
				{
					final Score sc = (Score) value;
					this.setText(sc.getNotation() + "  " + sc.getScore() + " pts");
				}
				return label;
			}
		});
	}


	public void updateList(final MicroServiceScrabbleServer server, final GameState state, final ArrayList<Tile> playerTiles)
	{
		try
		{
			if (true /*this.showPossibilitiesButton.isSelected()*/)
			{
				// todo: show the values
				bfm.setGrid(Grid.fromData(state.grid));

				final ArrayList<Character> characters = new ArrayList<>();
				playerTiles.forEach(t -> characters.add(t.c));
				final List<String> legalMoves = this.bfm.getLegalMoves(characters, this.strategy);

				final Collection<Score> scores = server.getScores(state.getGameId(), legalMoves);
				this.moveList.setListData(new Vector<>(scores));
			}
			else
			{
				// todo: hide the values
			}
		}
		catch (ScrabbleException.CommunicationException e)
		{
			LOGGER.error("Cannot update list", e);
		}
	}
}
