package oscrabble.client.vaadin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.Autocapitalize;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinSession;
import elemental.json.JsonObject;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.ScrabbleException;
import oscrabble.client.AbstractPossibleMoveDisplayer;
import oscrabble.client.Application;
import oscrabble.client.utils.I18N;
import oscrabble.controller.Action.PlayTiles;
import oscrabble.data.*;
import oscrabble.data.objects.Coordinate;
import oscrabble.data.objects.Square;
import oscrabble.exception.IllegalCoordinate;
import oscrabble.player.ai.Strategy;
import oscrabble.server.Game;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

@Route(value = "scrabble")
@PageTitle("Scrabble | By Olivier")
@CssImport("./scrabble.css")
public class ScrabbleView extends VerticalLayout implements BeforeEnterObserver {
	private static final int CELL_SIZE = 36;
	public static final Dimension CELL_DIMENSION = new Dimension(CELL_SIZE, CELL_SIZE);
	public static final Dimension GRID_DIMENSION = new Dimension(17 * CELL_SIZE, 17 * CELL_SIZE);
	private static final int CELL_BORDER = 1;
	private final static ImageGenerator imageFactory = new ImageGenerator();

	private static final Logger LOGGER = LoggerFactory.getLogger(ScrabbleView.class);

	private static final TextRenderer<Action> ACTION_RENDERER = new TextRenderer<>(a -> a.getScore() + " pts");
	private static final TextRenderer<Score> SCORE_RENDERER = new TextRenderer<>(score -> score.getNotation() + " " + score.getScore() + " pts");

	private TextField inputTextField;
	private GridComponent grid;
	private PlayerComponent playerComponent;
	private HistoryComponent historyComponent;
	private RackComponent rackComponent;
	private PossibleMovesDisplayer possibleMovesDisplayer;
	private UI ui;
	private boolean hasInputTextFieldChanged;
	private Context ctx;

	/**
	 * Create the components of this view.
	 */
	private void build() {
		this.removeAll();

		final Style style = getElement().getStyle();
		style.set(
				"background-color",
				"#eeeeee"
		); // fixme: not a good thing to change the style here
		style.set("font-family", "Tahoma");
		style.set("font-weight", "bold");
		style.set("font-size", "12px");

		//
		// Menu
		//

		MenuBar menuBar = new MenuBar();
		MenuItem item = menuBar.addItem(I18N.get("menu.game"));
		SubMenu subMenu = item.getSubMenu();
		subMenu.addItem(I18N.get("menu.entry.new.game"), e -> {
			try {
				startNewGame();
				update();
			} catch (ScrabbleException ex) {
				LOGGER.error(ex.toString(), ex);
			}
		});
		add(menuBar);

		final HorizontalLayout mainPanel = new HorizontalLayout();
		this.add(mainPanel);

		//
		// Center column
		//

		final VerticalLayout centerColumn = new VerticalLayout();
		centerColumn.setAlignItems(Alignment.STRETCH);

		this.inputTextField = new TextField();
		this.inputTextField.setAutocapitalize(Autocapitalize.CHARACTERS);
		this.grid = new GridComponent();
		centerColumn.add(this.grid);
		addTitledComponent(centerColumn, I18N.get("your.move"), this.inputTextField);
		this.inputTextField.setValueChangeMode(ValueChangeMode.EAGER);
		this.inputTextField.addValueChangeListener(
				ev -> {
					this.hasInputTextFieldChanged = true;
					this.grid.actualize();
				}
		);
		this.inputTextField.addKeyPressListener(
				Key.ENTER,
				ev -> play()
		);
		mainPanel.add(centerColumn);
		centerColumn.setWidth(this.grid.getWidth());

		//
		// Right column
		//

		final VerticalLayout rightColumn = new VerticalLayout();

		this.playerComponent = new PlayerComponent();
		addTitledComponent(rightColumn, I18N.get("border.title.score"), this.playerComponent);
		this.rackComponent = new RackComponent();
		rightColumn.add(this.rackComponent);
		this.possibleMovesDisplayer = new PossibleMovesDisplayer(this.ctx.game.getDictionary());
		addTitledComponent(rightColumn, I18N.get("possible.moves"), this.possibleMovesDisplayer.createComponent());
		this.historyComponent = new HistoryComponent();
		addTitledComponent(rightColumn, I18N.get("moves"), this.historyComponent);

		rightColumn.add(new VersionLabel());

		mainPanel.add(rightColumn);
		rightColumn.setPadding(false);
		rightColumn.setHeight(centerColumn.getHeight());

		//
		// start a thread for watching the game
		//

		final BlockingQueue<Long> listener = new LinkedBlockingQueue<>();
		this.ctx.game.addListener(listener);
		final Thread th = new Thread(() -> {
			while (true) // todo: quit if client disconnects
				try {
					listener.take();
					update();
				} catch (InterruptedException | ScrabbleException ex) {
					LOGGER.error("Error occurred", ex);
				}
		});
		th.setDaemon(true);
		th.start();

		this.inputTextField.focus();

	}

	@Override
	public void beforeEnter(final BeforeEnterEvent event) {
		final Map<String, List<String>> parameters = event.getLocation().getQueryParameters().getParameters();
		final List<String> gameList = parameters.get("game");
		// assert the url contains a game UUID, otherwise redirect to a random one.
		if (gameList == null || gameList.isEmpty()) {
			this.ctx = startNewGame();
		} else {
			this.ctx = Context.get(UUID.fromString(gameList.get(0)));
		}

		build();
	}

	private Context startNewGame() {
		final UUID gameId;
		gameId = UUID.randomUUID();
		UI.getCurrent().navigate(
				"scrabble",
				new QueryParameters(
						Map.of("game", List.of(gameId.toString()))
				));
		return Context.get(gameId);
	}

	@Override
	protected void onAttach(final AttachEvent attachEvent) {
		LOGGER.info("Attaching UI: " + attachEvent);
		this.ui = attachEvent.getUI();
		try {
			update();
		} catch (ScrabbleException e) {
			LOGGER.error(e.getLocalizedMessage(), e);
		}
	}

	private void play() {
		final oscrabble.data.Action action = oscrabble.data.Action.builder()
				.player(this.ctx.humanPlayer)
				.turnId(UUID.randomUUID()) //TODO: the game should give the id
				.notation(this.inputTextField.getValue().toUpperCase())
				.build();
		try {
			final PlayActionResponse response = this.ctx.server.play(this.ctx.game.getId(), action);
			LOGGER.info(response.message);
			if (response.success) {
				this.inputTextField.clear();
				this.inputTextField.setHelperText("");
				this.hasInputTextFieldChanged = false;
			} else {
				this.inputTextField.setHelperText(response.message);
			}
		} catch (ScrabbleException | InterruptedException e) {
			LOGGER.error(e.toString(), e);
			this.inputTextField.setHelperText(e.toString());
		}

		this.grid.actualize();
	}

	private String createGridHTML() {
		final Game game = this.ctx.game;
		final oscrabble.data.objects.Grid grid = game.getGrid();

		// prepare action
		PlayTiles preparedAction;
		try {
			final String notation = this.inputTextField.getValue().toUpperCase();
			final oscrabble.controller.Action action = PlayTiles.parse(this.ctx.humanPlayer, notation);
			preparedAction = action instanceof PlayTiles ? ((PlayTiles) action) : null;
		} catch (ScrabbleException.NotParsableException e) {
			// ok
			preparedAction = null;
		}

		final StringBuilder html = new StringBuilder();

		// grid background
		html.append(createHtmlImgCode(GRID_DIMENSION, ImageServlet.urlForGrid(), ""));

		// letters
		final List<Action> playedActions = game.getGameState().getPlayedActions();
		final UUID lastTurnId = playedActions.isEmpty() ? null : playedActions.get(playedActions.size() - 1).turnId;
		for (final Square square : grid.getAllSquares()) {
			if (square.tile != null) {
				html.append("\n");
				final boolean flash = !this.hasInputTextFieldChanged && square.tile.turn == lastTurnId;
				html.append(
						getHtmlPositionedImgCode(
								square.getX(),
								square.getY(),
								1,
								1,
								this.imageFactory.generateTileImage(square.tile, flash)
						));
			}
		}

		if (preparedAction != null) {
			// compute currently played tiles list
			final boolean isPreparedMoveHorizontal = preparedAction.getDirection() == oscrabble.data.objects.Grid.Direction.HORIZONTAL;
			final ArrayList<Triple<Integer, Integer, Tile>> preparedTiles = new ArrayList<>();
			for (int i = 0; i < preparedAction.word.length(); i++) {
				final char c = preparedAction.word.charAt(i);
				final int x = preparedAction.startSquare.x + (isPreparedMoveHorizontal ? i : 0);
				final int y = preparedAction.startSquare.y + (isPreparedMoveHorizontal ? 0 : i);
				final Square actual = grid.get(x, y);
				if (actual.isBorder || (actual.tile != null && actual.tile.getC() != c)) {
					// word cannot be played
					preparedTiles.clear();
					break;
				}
				final int points = game.getDictionary().getScrabbleRules().getPoints(c);
				preparedTiles.add(Triple.of(
						x,
						y,
						Tile.builder().c(c).points(points).build()
				));
			}

			// display currently played tiles
			for (final Triple<Integer, Integer, Tile> t : preparedTiles) {
				html.append("\n");
				html.append(
						getHtmlPositionedImgCode(
								t.getLeft(),
								t.getMiddle(),
								1,
								1,
								this.imageFactory.generateTileImage(t.getRight(), false)
						)
				);
			}

			// highlight currently played move
			if (!preparedTiles.isEmpty()) {
				html.append("\n");
				html.append(
						getHtmlPositionedImgCode(
								preparedAction.startSquare.x,
								preparedAction.startSquare.y,
								preparedAction.getWidth(),
								preparedAction.getHeight(),
								this.imageFactory.generateCellBox(
										preparedAction.getWidth(),
										preparedAction.getHeight()
								)
						)
				);
			}

			// arrow for the currently played move
			html.append("\n");
			html.append(
					getHtmlPositionedImgCode(
							preparedAction.startSquare.x,
							preparedAction.startSquare.y,
							1,
							1,
							this.imageFactory.generateDirectionArrowImage(preparedAction.getDirection())
					)
			);
		}

		return html.toString();
	}

	/**
	 * @param squareX
	 * @param squareY
	 * @param width   in number of cells
	 * @param height  in number of cells
	 * @param png
	 * @return
	 */
	private String getHtmlPositionedImgCode(
			final int squareX,
			final int squareY,
			final int width,
			final int height,
			final byte[] png
	) {
		return createHtmlEmbeddedImgCode(
				CELL_DIMENSION,
				png,
				String.format(
						// "pointer-event:none" makes this code transparent to input events, wherefore the parent one becomes them
						// and the coordinates of the event are related to the parent (grid) component and not this code.
						"position:absolute; top:%spx; left:%spx; height:%spx; width:%spx; pointer-events:none",
						CELL_SIZE * squareY + CELL_BORDER,
						CELL_SIZE * squareX + CELL_BORDER,
						(CELL_SIZE * height) - 2 * CELL_BORDER,
						(CELL_SIZE * width) - 2 * CELL_BORDER
				)
		);
	}

	@SuppressWarnings("SameParameterValue")
	private String createHtmlEmbeddedImgCode(final Dimension dimension, final byte[] png, final String cssStyle) {
		return createHtmlImgCode(
				dimension,
				"data:image/png;base64," + Base64.getEncoder().encodeToString(png),
				cssStyle
		);
	}

	private String createHtmlImgCode(final Dimension dimension, String imageUrl, final String cssStyle) {
		return String.format(
				"<img style='display:block pointer-events:none; %s' width=%d height=%d id='base64image' src='%s' />",
				cssStyle,
				((int) dimension.getWidth()),
				((int) dimension.getHeight()),
				imageUrl
		);
	}

	private String createRackHTML() throws ScrabbleException {
		final Bag rack = this.ctx.server.getRack(this.ctx.game.getId(), this.ctx.humanPlayer);

		final StringBuilder html = new StringBuilder();
		for (int i = 0; i < rack.tiles.size(); i++) {
			html.append("\n");
			html.append(
					createHtmlEmbeddedImgCode(
							CELL_DIMENSION,
							this.imageFactory.generateTileImage(rack.tiles.get(i), false),
							""
					)
			);
		}

		return html.toString();
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

	/**
	 * Update the content of the component with the actual state of the game, but without recreating them.
	 */
	private void update() throws ScrabbleException {
		final GameState state = this.ctx.game.getGameState();
		final VaadinSession session = this.ui.getSession();
		session.lock();
		try {
			this.historyComponent.setItems(state.playedActions);
			this.historyComponent.scrollToEnd();
			this.playerComponent.playerOnTurn.set(state.playerOnTurn);
			this.playerComponent.setItems(state.players);
			this.rackComponent.getElement().setProperty("innerHTML", createRackHTML());
			this.grid.actualize();

			this.inputTextField.setEnabled(state.playerOnTurn == this.ctx.humanPlayer);

			final Bag rack = this.ctx.server.getRack(this.ctx.game.getId(), this.ctx.game.getPlayerOnTurnUUID());
			this.possibleMovesDisplayer.refresh(
					this.ctx.server,
					this.ctx.game.getGameState(),
					rack.getChars()
			);
			this.possibleMovesDisplayer.strategyComboBox.setItems(this.possibleMovesDisplayer.strategies.keySet());
		} finally {
			this.inputTextField.focus();
			session.unlock();
		}
	}

	class GridComponent extends Div {
		GridComponent() {
			actualize();
			getElement().addEventListener("click", this::handleClick)
					.addEventData("event.offsetX")
					.addEventData("event.offsetY")
					.addEventData("element.clientWidth")
					.addEventData("element.clientHeight");
			getElement().setAttribute("style", "position:relative");
		}

		private void handleClick(DomEvent event) {
			JsonObject eventData = event.getEventData();
			double x = eventData.getNumber("event.offsetX");
			double y = eventData.getNumber("event.offsetY");
			double w = eventData.getNumber("element.clientWidth");
			double h = eventData.getNumber("element.clientHeight");

			char column = (char) ('A' + (int) (x * 17 / w) - 1);
			int row = (int) (y * 17 / h);

			if ('A' <= column && column <= 'O' && 1 <= row && row <= 15) {
				Pair<Coordinate, String> oldValues = null;
				try {
					oldValues = oscrabble.controller.Action.parsePlayNotation(ScrabbleView.this.inputTextField.getValue());
				} catch (ScrabbleException.NotParsableException | ClassCastException | IllegalCoordinate e) {
					// ok
				}

				final oscrabble.data.objects.Grid.Direction direction;
				if (oldValues != null) {
					final Coordinate oldCoordinate = oldValues.getLeft();
					if (oldCoordinate.getColumn() == column && oldCoordinate.getRow() == row) {
						direction = oldCoordinate.direction.other();
					} else {
						direction = oldCoordinate.direction;
					}
				} else {
					direction = oscrabble.data.objects.Grid.Direction.HORIZONTAL;
				}
				final StringBuilder sb = new StringBuilder();
				sb.append(new Coordinate(column, row, direction).getNotation());
				sb.append(" ");
				if (oldValues != null) {
					sb.append(oldValues.getRight());
				}
				ScrabbleView.this.inputTextField.setValue(sb.toString());
				ScrabbleView.this.inputTextField.focus();
			}
		}

		private void actualize() {
			getElement().setProperty("innerHTML", createGridHTML());
		}
	}

	static class RackComponent extends Div {
		RackComponent() {
			final Style style = getElement().getStyle();
			style.set("position", "relative");
			style.set("height", CELL_SIZE + "px");
		}
	}

	private static void setThemeVariants(Grid<?> grid) {
		grid.addThemeVariants(GridVariant.LUMO_COMPACT);
		grid.addThemeVariants(GridVariant.LUMO_NO_ROW_BORDERS);
	}

	private static void setMonospacedFont(HasStyle component) {
		final Style style = component.getStyle();
		style.set("font-family", "Nanum Gothic Coding, monospace");
		style.set("font-weight", "normal");
	}

	private static void setDefaultFont(HasStyle component) {
		final Style style = component.getStyle();
		style.remove("font-family");
		style.remove("font-weight");
		style.remove("font-size");

	}

	@SuppressWarnings("SameParameterValue")
	private static void setFontSize(HasStyle component, String size) {
		component.getStyle().set("font-size", size);
	}

	static class PlayerComponent extends Grid<Player> {
		private final AtomicReference<UUID> playerOnTurn = new AtomicReference<>();

		PlayerComponent() {
			final ItemLabelGenerator<Player> onTurn =
					player -> player.getId().equals(this.playerOnTurn.get())
							? "\u27A4"
							: "";

			addColumn(new TextRenderer<>(onTurn));
			addColumn(Player::getName);
			addColumn(Player::getScore);

			setSelectionMode(SelectionMode.NONE);
			setHeight("120px");
			final Iterator<Column<Player>> it = getColumns().iterator();
			it.next().setWidth("5px");
			it.next().setAutoWidth(true);
			it.next().setWidth("50px");
			setThemeVariants(this);
			setDefaultFont(this);
		}
	}

	private class PossibleMovesDisplayer extends AbstractPossibleMoveDisplayer {
		final Grid<Score> grid;
		private final Select<Strategy> strategyComboBox;
		private final LinkedHashMap<Strategy, String> strategies;
		private Strategy selectedStrategy = null;

		public PossibleMovesDisplayer(IDictionary dictionary) {
			super(dictionary);

			this.grid = new Grid<>();
			this.grid.setHeight("150px");
			this.grid.addColumn(SCORE_RENDERER);
			this.grid.addItemDoubleClickListener(
					event -> {
						ScrabbleView.this.inputTextField.setValue(event.getItem().getNotation());
						play();
					}
			);
			setThemeVariants(this.grid);
			setMonospacedFont(this.grid);

			this.strategies = getStrategyList();

			this.strategyComboBox = new Select<>();
			final ItemLabelGenerator<Strategy> labelGenerator = item -> this.strategies.get(item);
			this.strategyComboBox.setRenderer(new TextRenderer<>(labelGenerator));
			this.strategyComboBox.addValueChangeListener(a -> {
				this.selectedStrategy = a.getValue();
				refresh();
			});
		}

		public Component createComponent() {
			final VerticalLayout verticalLayout = new VerticalLayout(this.strategyComboBox, this.grid);
			verticalLayout.getStyle().set("padding", "0px");
			return verticalLayout;
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

	private class HistoryComponent extends Grid<Action> {
		public HistoryComponent() {
			addColumn(Action::getNotation);
			addColumn(ACTION_RENDERER);
			setHeight("150px");
			setMonospacedFont(this);
			setFontSize(this, "small");
			setItemDetailsRenderer(new ComponentRenderer<>(
					action -> {
						final VerticalLayout layout = new VerticalLayout();
						layout.add(action.getNotation());
						layout.add(" (" + getPlayerName(action.getPlayer()) + ")");
						return layout;
					}
			));
		}

		private String getPlayerName(UUID id) {
			for (final Player p : ScrabbleView.this.ctx.game.getGameState().getPlayers()) {
				if (p.id == id) {
					return p.getName();
				}
			}
			throw new IllegalArgumentException("No player found with id " + id);
		}
	}

	private static class VersionLabel extends Label {
		public VersionLabel() {
			super();
			setText(Application.getFormattedVersion());
			setFontSize(this, "small");
		}
	}
}