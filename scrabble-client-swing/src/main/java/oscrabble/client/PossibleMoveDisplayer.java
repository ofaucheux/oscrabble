package oscrabble.client;

import org.springframework.lang.Nullable;
import oscrabble.ScrabbleException;
import oscrabble.client.utils.I18N;
import oscrabble.controller.Action;
import oscrabble.controller.ScrabbleServerInterface;
import oscrabble.data.GameState;
import oscrabble.data.IDictionary;
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
public class PossibleMoveDisplayer {

	private static final Strategy DO_NOT_DISPLAY_STRATEGIE = new Strategy() {
		@Override
		public TreeMap<Integer, List<String>> sort(final Set<String> moves) {
			throw new AssertionError("Should not be called");
		}
	};

	protected final JPanel mainPanel;
	private final BruteForceMethod bfm;
	private final JList<Object> moveList;
	private final Set<AttributeChangeListener> attributeChangeListeners = new HashSet<>();
	/**
	 * The server to calculate the scores
	 */
	private ScrabbleServerInterface server;
	/**
	 * the game
	 */
	private UUID game;
	private List<Character> rack;

	private GameState state;
	private final JComboBox<Strategy> strategiesCb;

	public PossibleMoveDisplayer(final IDictionary dictionary) {
		this.bfm = new BruteForceMethod(dictionary);

		final Strategy.BestScore bestScore = new Strategy.BestScore(null, null);
		this.attributeChangeListeners.add((fieldName, newValue) -> {
			switch (fieldName) {
				case "game": //NON-NLS
					bestScore.setGame((UUID) newValue);
					break;
				case "server":
					bestScore.setServer(((ScrabbleServerInterface) newValue));
					break;
			}
		});

		final LinkedHashMap<Strategy, String> orderStrategies = new LinkedHashMap<>();
		orderStrategies.put(DO_NOT_DISPLAY_STRATEGIE, I18N.get("nothing"));
		orderStrategies.put(bestScore, I18N.get("best.scores"));
		orderStrategies.put(new Strategy.BestSize(), I18N.get("best.sizes"));

		this.mainPanel = new JPanel();
		this.mainPanel.setBorder(new TitledBorder(I18N.get("possible.moves")));
		this.mainPanel.setSize(new Dimension(200, 500));
		this.mainPanel.setLayout(new BorderLayout());

		this.strategiesCb = new JComboBox<>();
		this.strategiesCb.setRenderer(new DefaultListCellRenderer() {
			@SuppressWarnings("SuspiciousMethodCalls")
			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
				assert orderStrategies.containsKey(value);
				return new JLabel(orderStrategies.get(value));
			}
		});
		orderStrategies.keySet().forEach(os -> this.strategiesCb.addItem(os));
		this.strategiesCb.addActionListener(a -> refresh());
		this.mainPanel.add(this.strategiesCb, BorderLayout.NORTH);

		this.moveList = new JList<>();
		this.moveList.setCellRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
				final Component label = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (value instanceof Score) {
					final Score sc = (Score) value;
					this.setText(sc.getNotation() + "  " + sc.getScore() + I18N.get("pts"));
				}
				return label;
			}
		});
		this.mainPanel.add(new JScrollPane(this.moveList));
	}

	/**
	 * Add a function to execute after the selection has changed.
	 *
	 * @param listSelectionListener
	 */
	void addSelectionListener(final Consumer<Action> listSelectionListener) {
		this.moveList.addListSelectionListener(l -> {
					final Score score = (Score) this.moveList.getSelectedValue();
					if (score == null) {
						return;
					}
					try {
						final Action action = Action.parse(null, score.getNotation());
						listSelectionListener.accept(action);
					} catch (ScrabbleException.NotParsableException e) {
						throw new AssertionError("Assertion error", e);
					}
				}
		);
	}

	public JList<Object> getMoveList() {
		return this.moveList;
	}

	/**
	 * Set the server this displayer is for. This information is transferred to the subcomponents too.
	 *
	 * @param server
	 */
	void setServer(final ScrabbleServerInterface server) {
		this.server = server;
		invokeListeners("server", server);
	}

	private void invokeListeners(final String fieldName, final Object newValue) {
		this.attributeChangeListeners.forEach(l -> l.onChange(fieldName, newValue));
	}

	/**
	 * Set the game this displayer is for. This information is transferred to the under-components too.
	 *
	 * @param game
	 */
	public void setGame(final UUID game) {
		this.game = game;
		invokeListeners("game", game); //NON-NLS
	}

	public synchronized void setData(final GameState state, final List<Character> rack) {
		this.state = state;
		this.rack = rack;
	}

	public synchronized void refresh() {
		Strategy selectedOrderStrategy = ((Strategy) this.strategiesCb.getSelectedItem());

		if (selectedOrderStrategy == DO_NOT_DISPLAY_STRATEGIE) {
			this.moveList.setListData(new Object[0]);
			return;
		}

		if (!this.state.gameId.equals(this.game)) {
			throw new IllegalArgumentException("GameId is " + this.state.gameId + ", expected was " + this.game);
		}

		final Collection<Score> scores;
		if (selectedOrderStrategy == null) {
			scores = Collections.emptyList();
		} else {
			this.bfm.setGrid(Grid.fromData(this.state.grid));
			final ArrayList<String> words = new ArrayList<>();
			for (final List<String> subWords : selectedOrderStrategy.sort(this.bfm.getLegalMoves(this.rack)).values()) {
				words.addAll(0, subWords);
			}
			try {
				scores = this.server.getScores(this.game, words);
			} catch (ScrabbleException e) {
				throw new Error(e);
			}
		}
		this.moveList.setListData(new Vector<>(scores));
	}

	public void setFont(final Font font) {
		this.moveList.setFont(font);
	}

	/**
	 * @return selected action in scrabble notation.
	 */
	@Nullable
	public String getSelectedAction() {
		final Score selected = (Score) this.moveList.getSelectedValue();
		return selected == null ? null : selected.getNotation();
	}

	public void reset() {
		this.strategiesCb.setSelectedIndex(0);
		refresh();
	}

	private interface AttributeChangeListener {
		void onChange(String fieldName, Object newValue);
	}
}
