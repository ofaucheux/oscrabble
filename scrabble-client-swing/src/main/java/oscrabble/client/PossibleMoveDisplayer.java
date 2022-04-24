package oscrabble.client;

import org.springframework.lang.Nullable;
import oscrabble.ScrabbleException;
import oscrabble.client.utils.I18N;
import oscrabble.controller.Action;
import oscrabble.data.IDictionary;
import oscrabble.data.Score;
import oscrabble.player.ai.Strategy;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.function.Consumer;

/**
 * JPanel and logic to let the system display all possible moves.
 */
public class PossibleMoveDisplayer extends AbstractPossibleMoveDisplayer {

	protected final JPanel mainPanel;
	private final JList<Object> moveList;

	private final JComboBox<Strategy> strategiesCb;

	public PossibleMoveDisplayer(final IDictionary dictionary) {
		super(dictionary);
		final LinkedHashMap<Strategy, String> orderStrategies = this.getStrategyList();

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

	@Override
	protected Strategy getSelectedStrategy() {
		return (Strategy) this.strategiesCb.getSelectedItem();
	}

	@Override
	protected void setListData(final Collection<Score> scores) {
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
}
