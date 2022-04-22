package oscrabble.client.vaadin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import oscrabble.ScrabbleException;
import oscrabble.client.JGrid;
import oscrabble.client.utils.SwingUtils;
import oscrabble.data.Action;
import oscrabble.data.Player;
import oscrabble.data.Score;
import oscrabble.dictionary.Dictionary;
import oscrabble.dictionary.Language;
import oscrabble.server.Game;
import oscrabble.server.Server;

import java.awt.*;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Route(value = "scrabble")
@PageTitle("Scrabble | By Olivier")
public class ScrabbleView extends HorizontalLayout {

	private final Game game;

	public ScrabbleView() throws ScrabbleException, InterruptedException {
		final Server server = new Server();
		this.game = new Game(server, Dictionary.getDictionary(Language.FRENCH), 2);
		final UUID eleonore = UUID.randomUUID();
		this.game.addPlayer(Player.builder().name("Eleonore").id(eleonore).build());
		this.game.startGame();
		this.game.play(Action.builder().notation("H8 GATE").player(eleonore).build());

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

		addTitledComponent(rightColumn, "scores", new PlayerComponent());
		addTitledComponent(rightColumn, "possible moves", new PossibleMoves());
		addTitledComponent(rightColumn, "history", new HistoryComponent());

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

	class PlayerComponent extends Grid<Player> {
		PlayerComponent() {
			super(Player.class, false);
			addColumn(Player::getName);
			addColumn(Player::getScore);
			setSelectionMode(SelectionMode.NONE);
			setItems(game.getGameState().players);
			setHeight("150px");
		}
	}

	private class PossibleMoves extends Grid<Score> {
		public PossibleMoves() {
			setHeight("150px");
		}
	}

	private class HistoryComponent extends Grid<Action> {
		public HistoryComponent() {
			setHeight("150px");
		}
	}
}