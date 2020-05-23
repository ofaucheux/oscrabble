package oscrabble.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.ScrabbleException;
import oscrabble.controller.MicroServiceDictionary;
import oscrabble.controller.MicroServiceScrabbleServer;
import oscrabble.data.GameState;
import oscrabble.data.Score;
import oscrabble.data.objects.Grid;
import oscrabble.player.ai.BruteForceMethod;
import oscrabble.player.ai.Strategy;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;
import java.util.*;

public class PossibleMoveDisplayer
{

	public static final Logger LOGGER = LoggerFactory.getLogger(PossibleMoveDisplayer.class);
	protected final JPanel mainPanel;
	private final BruteForceMethod bfm;
	//	private final JButton showPossibilitiesButton;
	private final JList<Object> moveList;
	private final ButtonGroup orderButGroup;

	/** Available order strategies */
	private List<Strategy> orderStrategies;

	/** Selected order strategy */
	private Strategy selectedOrderStrategy;

	/** The server to calculate the scores */
	private MicroServiceScrabbleServer server;

	/** the game */
	private UUID game;

	private final Set<AttributeChangeListener> attributeChangeListeners = new HashSet<>();

	public PossibleMoveDisplayer(final MicroServiceDictionary dictionary)
	{
		this.bfm = new BruteForceMethod(dictionary);

		final Strategy.BestScore bestScore = new Strategy.BestScore(null, null);
		this.attributeChangeListeners.add((fieldName, newValue) -> {
			switch (fieldName)
			{
				case "game":
					bestScore.setGame((UUID) newValue);
					break;
				case "server":
					bestScore.setServer(((MicroServiceScrabbleServer) newValue));
					break;
			}
		});
		this.orderStrategies = Arrays.asList(bestScore);
		this.selectedOrderStrategy = this.orderStrategies.get(0);

		this.mainPanel = new JPanel();
		this.mainPanel.setBorder(new TitledBorder(Application.MESSAGES.getString("possible.moves")));
		this.mainPanel.setSize(new Dimension(200, 300));
		this.mainPanel.setLayout(new BorderLayout());
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
		this.mainPanel.add(this.moveList);
	}

	/**
	 * Set the server this displayer is for. This information is transferred to the undercomponents too.
	 * @param server
	 */
	void setServer(final MicroServiceScrabbleServer server)
	{
		this.server = server;
		invokeListeners("server", server);
	}

	private void invokeListeners(final String fieldName, final Object newValue)
	{
		this.attributeChangeListeners.forEach(l -> l.onChange(fieldName, newValue));
	}

	/**
	 * Set the game this displayer is for. This information is transfered to the undercomponents too.
	 *
	 * @param game
	 */
	public void setGame(final UUID game)
	{
		this.game = game;
		invokeListeners("game", game);
	}

	public synchronized void updateList(final GameState state, final List<Character> characters)
	{
		if (!state.gameId.equals(this.game))
		{
			throw new IllegalArgumentException("GameId is " + state.gameId + ", expected was " + this.game);
		}

		try
		{
			if (true /*this.showPossibilitiesButton.isSelected()*/)
			{
				// todo: show the values
				this.bfm.setGrid(Grid.fromData(state.grid));

				final List<String> legalMoves = this.bfm.getLegalMoves(characters, this.selectedOrderStrategy);

				final Collection<Score> scores = this.server.getScores(state.getGameId(), legalMoves);
				this.moveList.setListData(new Vector<>(scores));
			}
			else
			{
				// todo: hide the values
			}
//			this.mainPanel.setSize(this.mainPanel.getPreferredSize());
		}
		catch (ScrabbleException.CommunicationException e)
		{
			LOGGER.error("Cannot update list", e);
		}
	}

	private interface AttributeChangeListener
	{
		void onChange(String fieldName, Object newValue);
	}
}
