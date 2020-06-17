package oscrabble.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.ScrabbleException;
import oscrabble.controller.Action;
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
import java.util.function.Consumer;

/**
 * JPanel and logic to let the system display all possible moves.
 */
public class PossibleMoveDisplayer
{

	private static final Logger LOGGER = LoggerFactory.getLogger(PossibleMoveDisplayer.class);

	private static final Strategy DO_NOT_DISPLAY_STRATEGIE = new Strategy("Hide")
	{
		@Override
		public void sort(final List<String> moves)
		{
			throw new AssertionError("Should not be called");
		}
	};

	protected final JPanel mainPanel;
	private final BruteForceMethod bfm;
	private final JList<Object> moveList;
	private final Set<AttributeChangeListener> attributeChangeListeners = new HashSet<>();
	/** The server to calculate the scores */
	private MicroServiceScrabbleServer server;
	/** the game */
	private UUID game;
	private List<Character> rack;

	private GameState state;
	private final JComboBox<Strategy> strategiesCb;

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

		final List<Strategy> orderStrategies = Arrays.asList(
				DO_NOT_DISPLAY_STRATEGIE,
				bestScore,
				new Strategy.BestSize()
		);

		this.mainPanel = new JPanel();
		this.mainPanel.setBorder(new TitledBorder(Application.MESSAGES.getString("possible.moves")));
		this.mainPanel.setSize(new Dimension(200, 500));
		this.mainPanel.setLayout(new BorderLayout());

		this.strategiesCb = new JComboBox<>();
		orderStrategies.forEach(os -> this.strategiesCb.addItem(os));
		this.strategiesCb.addActionListener(a -> refresh());
		this.mainPanel.add(this.strategiesCb, BorderLayout.NORTH);

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
		this.mainPanel.add(new JScrollPane(this.moveList));
	}

	/**
	 * Add a function to execute after the selection has changed.
	 * @param listSelectionListener
	 */
	void addSelectionListener(final Consumer<Action> listSelectionListener)
	{
		this.moveList.addListSelectionListener(l -> {
					final Score score = (Score) this.moveList.getSelectedValue();
					if (score == null)
					{
						return;
					}
					try
					{
						final Action action = Action.parse(null, score.getNotation());
						listSelectionListener.accept(action);
					}
					catch (ScrabbleException.NotParsableException e)
					{
						throw new AssertionError("Assertion error", e);
					}
				}
		);
	}

	public JList<Object> getMoveList()
	{
		return this.moveList;
	}

	/**
	 * Set the server this displayer is for. This information is transferred to the subcomponents too.
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
	 * Set the game this displayer is for. This information is transferred to the undercomponents too.
	 *
	 * @param game
	 */
	public void setGame(final UUID game)
	{
		this.game = game;
		invokeListeners("game", game);
	}

	public synchronized void setData(final GameState state, final List<Character> rack)
	{
		this.state = state;
		this.rack = rack;
	}

	public synchronized void refresh()
	{
		Strategy selectedOrderStrategy = ((Strategy) this.strategiesCb.getSelectedItem());

		if (selectedOrderStrategy == DO_NOT_DISPLAY_STRATEGIE)
		{
			this.moveList.setListData(new Object[0]);
			return;
		}

		if (!this.state.gameId.equals(this.game))
		{
			throw new IllegalArgumentException("GameId is " + this.state.gameId + ", expected was " + this.game);
		}

		try
		{
			this.bfm.setGrid(Grid.fromData(this.state.grid));
			final List<String> legalMoves = this.bfm.getLegalMoves(this.rack, selectedOrderStrategy);
			final Collection<Score> scores = this.server.getScores(this.state.getGameId(), legalMoves);
			this.moveList.setListData(new Vector<>(scores));
		}
		catch (ScrabbleException.CommunicationException e)
		{
			LOGGER.error("Cannot update list", e);
		}
	}

	public void setFont(final Font font)
	{
		this.moveList.setFont(font);
	}

	public String getSelectedAction()
	{
		final Score selected = (Score) this.moveList.getSelectedValue();
		return selected.getNotation();
	}

	public void reset()
	{
		this.strategiesCb.setSelectedIndex(0);
		refresh();
	}

	private interface AttributeChangeListener
	{
		void onChange(String fieldName, Object newValue);
	}
}
