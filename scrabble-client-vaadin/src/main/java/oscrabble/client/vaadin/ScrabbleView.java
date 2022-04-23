package oscrabble.client.vaadin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import oscrabble.ScrabbleException;
import oscrabble.client.AbstractPossibleMoveDisplayer;
import oscrabble.client.JGrid;
import oscrabble.client.utils.SwingUtils;
import oscrabble.data.*;
import oscrabble.dictionary.Dictionary;
import oscrabble.dictionary.Language;
import oscrabble.player.ai.Strategy;
import oscrabble.server.Game;
import oscrabble.server.Server;

import java.util.*;
import java.util.List;

@Route(value = "scrabble")
@PageTitle("Scrabble | By Olivier")
public class ScrabbleView extends HorizontalLayout {

	private static final TextRenderer<Action> ACTION_RENDERER = new TextRenderer<>(a -> a.getScore() + " pts");
	private static final TextRenderer<Score> SCORE_RENDERER = new TextRenderer<>(score -> score.getNotation() + " " + score.getScore() + " pts");

	private final Game game;
	private final Server server;

	public ScrabbleView() throws ScrabbleException, InterruptedException {

		List<Player> players = new ArrayList<>();
		for (String n : Arrays.asList("Eleonore", "Kevin", "Charlotte")) {
			players.add(Player.builder().name(n).id(UUID.randomUUID()).build());
		}

		this.server = new Server();
		this.game = new Game(this.server, Dictionary.getDictionary(Language.FRENCH), 2);
		for (Player p : players) {
			this.game.addPlayer(p);
		}
		this.game.startGame();
		this.game.play(
				Action.builder()
						.notation("H8 GATE")
						.player(this.game.getPlayerOnTurnUUID())
						.build()
		);

		getElement().getStyle().set(
				"background-color",
				"#eeeeee"
		); // fixme: not a good thing to change the style here

		//
		// Center column
		//

		final VerticalLayout centerColumn = new VerticalLayout();
		centerColumn.setAlignItems(Alignment.STRETCH);

		final GridComponent grid = new GridComponent();
		centerColumn.add(grid);
		centerColumn.add(new TextField());

		add(centerColumn);
		centerColumn.setWidth(grid.getWidth());

		//
		// Right column
		//

		final VerticalLayout rightColumn = new VerticalLayout();

		final GameState gameState = this.game.getGameState();
		addTitledComponent(rightColumn, "scores", new PlayerComponent(
				gameState.players,
				gameState.playerOnTurn
		));
		addTitledComponent(rightColumn, "possible moves", new PossibleMovesDisplayer(this.game.getDictionary()).createComponent());
		addTitledComponent(rightColumn, "history", new HistoryComponent(gameState.playedActions));

		add(rightColumn);
		rightColumn.setPadding(false);
		rightColumn.setHeight(centerColumn.getHeight());
	}

	private String createGridHTML() {
		final JGrid jGrid = new JGrid();
		jGrid.setGrid(this.game.getGrid());
		final byte[] image = SwingUtils.getImage(jGrid, null);
		final String encoded = Base64.getEncoder().encodeToString(image);
		return String.format(
				"<img style='display:block' id='base64image' src='data:image/png;base64,%s' />",
				encoded
		);
	}

	private void addTitledComponent(final HasComponents parent, final String title, final Component child) {
		final Element fs = new Element("fieldset");
		Element legend = new Element("legend").setText(title);
		fs.appendChild(legend);
		final Div div = new Div(child);
		div.setWidth("220px");
		fs.appendChild(div.getElement());
		parent.getElement().appendChild(fs);
	}

	class GridComponent extends Div {
		GridComponent() {
			getElement().setProperty("innerHTML", createGridHTML());
		}
	}

	private static void setThemeVariants(Grid<?> grid) {
		grid.addThemeVariants(GridVariant.LUMO_COMPACT);
		grid.addThemeVariants(GridVariant.LUMO_NO_ROW_BORDERS);
	}

	static class PlayerComponent extends Grid<Player> {
		PlayerComponent(Collection<Player> players, final UUID onTurnUUID) {
			final ItemLabelGenerator<Player> onTurn =
					player -> player.getId().equals(onTurnUUID)
							? "\u27A4"
							: "";

			addColumn(new TextRenderer<>(onTurn));
			addColumn(Player::getName);
			addColumn(Player::getScore);

			setSelectionMode(SelectionMode.NONE);
			setHeight("150px");
			final Iterator<Column<Player>> it = getColumns().iterator();
			it.next().setWidth("5px");
			it.next().setAutoWidth(true);
			it.next().setWidth("50px");
			setThemeVariants(this);

			setItems(players);
		}
	}

	private class PossibleMovesDisplayer extends AbstractPossibleMoveDisplayer {
		final Grid<Score> grid;
		private final Select<Strategy> strategyComboBox;
		private Strategy selectedStrategy = null;

		public PossibleMovesDisplayer(IDictionary dictionary) throws ScrabbleException {
			super(dictionary);

			this.grid = new Grid<>();
			this.grid.setHeight("150px");
			this.grid.setItems(Score.builder().score(12).notation("A 34").build());
			this.grid.addColumn(SCORE_RENDERER);
			setThemeVariants(this.grid);

			final Bag rack = ScrabbleView.this.server.getRack(ScrabbleView.this.game.getId(), ScrabbleView.this.game.getPlayerOnTurnUUID());
			final LinkedHashMap<Strategy, String> strategies = getStrategyList();
			refresh(
					ScrabbleView.this.server,
					ScrabbleView.this.game.getGameState(),
					rack.getChars()
			);
			this.strategyComboBox = new Select<>();
			final ItemLabelGenerator<Strategy> labelGenerator = item -> strategies.get(item);
			this.strategyComboBox.setRenderer(new TextRenderer<>(labelGenerator));
			this.strategyComboBox.setItems(strategies.keySet());
			this.strategyComboBox.addValueChangeListener(a -> {
				this.selectedStrategy = a.getValue();
				refresh();
			});
			setListData(List.of(
					Score.builder().score(100).notation("A14").build()
			));
		}

		public Component createComponent() {
			return new VerticalLayout(this.strategyComboBox, this.grid);
		}

		@Override
		protected Strategy getSelectedStrategy() {
			return this.selectedStrategy;
		}

		@Override
		protected void setListData(final Collection<Score> scores) {
			this.grid.setItems(scores);
		}
	}

	private static class HistoryComponent extends Grid<Action> {
		public HistoryComponent(final List<Action> history) {
			addColumn(Action::getNotation);
			addColumn(ACTION_RENDERER);
			setHeight("150px");

			setItems(history);
		}
	}
}