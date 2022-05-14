package oscrabble.client.vaadin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import elemental.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.ScrabbleException;
import oscrabble.client.*;
import oscrabble.client.Application;
import oscrabble.client.utils.I18N;
import oscrabble.controller.Action.PlayTiles;
import oscrabble.data.*;
import oscrabble.data.objects.Square;
import oscrabble.player.ai.Strategy;
import oscrabble.server.Game;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

@Route(value = "scrabble")
@PageTitle("Scrabble | By Olivier")
@CssImport("./scrabble.css")
public class ScrabbleView extends HorizontalLayout
{
	private static final int CELL_SIZE = 36;
	public static final Dimension CELL_DIMENSION = new Dimension(CELL_SIZE, CELL_SIZE);
	public static final Dimension GRID_DIMENSION = new Dimension(17 * CELL_SIZE, 17 * CELL_SIZE);
	private static final int CELL_BORDER = 1;

	private static final Logger LOGGER = LoggerFactory.getLogger(ScrabbleView.class);

	private static final TextRenderer<Action> ACTION_RENDERER = new TextRenderer<>(a -> a.getScore() + " pts");
	private static final TextRenderer<Score> SCORE_RENDERER = new TextRenderer<>(score -> score.getNotation() + " " + score.getScore() + " pts");

	private final TextField inputTextField;
	private final GridComponent grid;
	private final PlayerComponent playerComponent;
	private final HistoryComponent historyComponent;
	private final RackComponent rackComponent;
	private final PossibleMovesDisplayer possibleMovesDisplayer;
	private final ImageGenerator imageFactory = new ImageGenerator();
	private UI ui;

	public ScrabbleView() {
		final Style style = getElement().getStyle();
		style.set(
				"background-color",
				"#eeeeee"
		); // fixme: not a good thing to change the style here
		style.set("font-family", "Tahoma");
		style.set("font-weight", "bold");
		style.set("font-size", "12px");

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
			ev -> this.grid.actualize()
		);
		this.inputTextField.addKeyPressListener(
				ev -> {
					if (ev.getKey() == Key.ENTER) {
						play();
					}
				}
		);
		add(centerColumn);
		centerColumn.setWidth(this.grid.getWidth());

		//
		// Right column
		//

		final VerticalLayout rightColumn = new VerticalLayout();

		this.playerComponent = new PlayerComponent();
		addTitledComponent(rightColumn, I18N.get("border.title.score"), this.playerComponent);
		this.rackComponent = new RackComponent();
		rightColumn.add(this.rackComponent);
		this.possibleMovesDisplayer = new PossibleMovesDisplayer(Context.get().game.getDictionary());
		addTitledComponent(rightColumn, I18N.get("possible.moves"), this.possibleMovesDisplayer.createComponent());
		this.historyComponent = new HistoryComponent();
		addTitledComponent(rightColumn, I18N.get("moves"), this.historyComponent);

		rightColumn.add(new VersionLabel());

		add(rightColumn);
		rightColumn.setPadding(false);
		rightColumn.setHeight(centerColumn.getHeight());

		//
		// start a thread for watching the game
		//

		final BlockingQueue<Long> listener = new LinkedBlockingQueue<>();
		Context.get().game.addListener(listener);
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
		final Context ctx = Context.get();
		final oscrabble.data.Action action = oscrabble.data.Action.builder()
				.player(ctx.humanPlayer)
				.turnId(UUID.randomUUID()) //TODO: the game should give the id
				.notation(this.inputTextField.getValue().toUpperCase())
				.build();
		try {
			final PlayActionResponse response = ctx.server.play(ctx.game.getId(), action);
			LOGGER.info(response.message);
			if (response.success) {
				this.inputTextField.clear();
				this.inputTextField.setHelperText("");
			} else {
				this.inputTextField.setHelperText(response.message);
			}
		} catch (ScrabbleException | InterruptedException e) {
			LOGGER.error(e.toString(), e);
			this.inputTextField.setErrorMessage(e.toString());
		}

		this.grid.actualize();
	}

	private String createGridHTML() {
		final Game game = Context.get().game;
		final oscrabble.data.objects.Grid grid = game.getGrid();

		// prepare action
		PlayTiles preparedAction;
		try {
			final String notation = this.inputTextField.getValue().toUpperCase();
			final oscrabble.controller.Action action = PlayTiles.parse(Context.get().humanPlayer, notation);
			preparedAction = action instanceof PlayTiles ? ((PlayTiles) action) : null;
		} catch (ScrabbleException.NotParsableException e) {
			// ok
			preparedAction = null;
		}

		final StringBuilder html = new StringBuilder();

		// grid
		html.append(createHtmlImgCode(GRID_DIMENSION, ImageServlet.urlForGrid(), ""));

		// letters
		final List<Action> playedActions = game.getGameState().getPlayedActions();
		final UUID lastTurnId = playedActions.isEmpty() ? null : playedActions.get(playedActions.size() - 1).turnId;
		for (final Square square : grid.getAllSquares()) {
			if (square.tile != null) {
				html.append("\n");
				html.append(
						getHtmlPositionedImgCode(
								square.getX(),
								square.getY(),
								1,
								1,
								this.imageFactory.generateTileImage(square.tile, square.tile.turn == lastTurnId)
						));
			}
		}

		// arrow for the currently played move
		if (preparedAction != null) {
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

			// highlight currently played move
			if (preparedAction.word.length() > 0) {
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
		}

		return html.toString();
	}

	/**
	 * @param squareX
	 * @param squareY
	 * @param width in number of cells
	 * @param height in number of cells
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
						"position:absolute; top:%spx; left:%spx; height:%spx; width:%spx",
						CELL_SIZE * squareY + CELL_BORDER,
						CELL_SIZE * squareX + CELL_BORDER,
						(CELL_SIZE * height) - 2 * CELL_BORDER,
						(CELL_SIZE * width) - 2 * CELL_BORDER
				)
		);
	}

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
		final Context ctx = Context.get();
		final Bag rack = ctx.server.getRack(ctx.game.getId(), ctx.humanPlayer);

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

	private void update() throws ScrabbleException {
		final Context ctx = Context.get();
		final GameState state = ctx.game.getGameState();
		final VaadinSession session = this.ui.getSession();
		session.lock();
		try {
			this.historyComponent.setItems(state.playedActions);
			this.historyComponent.scrollToEnd();
			this.playerComponent.playerOnTurn.set(state.playerOnTurn);
			this.playerComponent.setItems(state.players);
			this.rackComponent.getElement().setProperty("innerHTML", createRackHTML());
			this.grid.actualize();

			this.inputTextField.setEnabled(state.playerOnTurn == ctx.humanPlayer);

			final Bag rack = ctx.server.getRack(ctx.game.getId(), ctx.game.getPlayerOnTurnUUID());
			this.possibleMovesDisplayer.refresh(
					ctx.server,
					ctx.game.getGameState(),
					rack.getChars()
			);
			this.possibleMovesDisplayer.strategyComboBox.setItems(this.possibleMovesDisplayer.strategies.keySet());
		} finally {
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
				ScrabbleView.this.inputTextField.setValue(String.format("%s%s ", column, row));
				// todo: don't send the value to the vaadin server
				// or send it and receive the new image.

				// todo: swap column / row if clicked twice
				// todo: use already tipped word. Perhaps reuse code of swing.
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
					event -> ScrabbleView.this.inputTextField.setValue(event.getItem().getNotation())
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

	private static class HistoryComponent extends Grid<Action> {
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
			for (final Player p : Context.get().game.getGameState().getPlayers()) {
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