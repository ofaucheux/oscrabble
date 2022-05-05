package oscrabble.client.vaadin;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import oscrabble.ScrabbleException;
import oscrabble.client.AbstractPossibleMoveDisplayer;
import oscrabble.client.Application;
import oscrabble.client.JGrid;
import oscrabble.client.JRack;
import oscrabble.client.utils.I18N;
import oscrabble.data.*;
import oscrabble.player.ai.Strategy;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

@Route(value = "scrabble")
@PageTitle("Scrabble | By Olivier")
public class ScrabbleView extends HorizontalLayout
{
	private static final Logger LOGGER = LoggerFactory.getLogger(ScrabbleView.class);

	private static final TextRenderer<Action> ACTION_RENDERER = new TextRenderer<>(a -> a.getScore() + " pts");
	private static final TextRenderer<Score> SCORE_RENDERER = new TextRenderer<>(score -> score.getNotation() + " " + score.getScore() + " pts");

	private final TextField inputTextField;
	private final GridComponent grid;
	private final PlayerComponent playerComponent;
	private final HistoryComponent historyComponent;
	private final RackComponent rackComponent;
	private final PossibleMovesDisplayer possibleMovesDisplayer;
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

		this.grid = new GridComponent();
		centerColumn.add(this.grid);
		this.inputTextField = new TextField();
		centerColumn.add(this.inputTextField);
		this.inputTextField.addValueChangeListener(
			ev -> play()
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
		addTitledComponent(rightColumn, I18N.get("server.configuration"), new Label());
		rightColumn.add(new Button(I18N.get("rollback"))); // fixme: migrate into history panel

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
			this.inputTextField.setHelperText(e.toString());
		}
	}

	private String createGridHTML() {
		final oscrabble.data.objects.Grid comp = Context.get().game.getGrid();
		return createHtmlImgCode(JGrid.createImage(comp));
	}

	private String createHtmlImgCode(final Pair<Dimension, byte[]> pair) {
		final String encoded = Base64.getEncoder().encodeToString(pair.getRight());
		final Dimension dimension = pair.getLeft();
		return String.format(
				"<img style='display:block' width=%d height=%d id='base64image' src='data:image/png;base64,%s' />",
				((int) dimension.getWidth()),
				((int) dimension.getHeight()),
				encoded
		);
	}

	private String createRackHTML() throws ScrabbleException {
		final Context ctx = Context.get();
		final Bag rack = ctx.server.getRack(ctx.game.getId(), ctx.humanPlayer);
		return createHtmlImgCode(JRack.createImage(rack.getTiles()));
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
		}

		private void actualize() {
			getElement().setProperty("innerHTML", createGridHTML());
		}
	}

	static class RackComponent extends Div {
		RackComponent() {
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

	private static class PossibleMovesDisplayer extends AbstractPossibleMoveDisplayer {
		final Grid<Score> grid;
		private final Select<Strategy> strategyComboBox;
		private final LinkedHashMap<Strategy, String> strategies;
		private Strategy selectedStrategy = null;

		public PossibleMovesDisplayer(IDictionary dictionary) {
			super(dictionary);

			this.grid = new Grid<>();
			this.grid.setHeight("150px");
			this.grid.addColumn(SCORE_RENDERER);
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