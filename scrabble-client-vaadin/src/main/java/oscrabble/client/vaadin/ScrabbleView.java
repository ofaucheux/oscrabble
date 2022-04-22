package oscrabble.client.vaadin;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.AppShellSettings;
import oscrabble.ScrabbleException;
import oscrabble.client.JGrid;
import oscrabble.client.utils.SwingUtils;
import oscrabble.data.Action;
import oscrabble.data.Player;
import oscrabble.dictionary.Dictionary;
import oscrabble.dictionary.Language;
import oscrabble.server.Game;
import oscrabble.server.Server;

import java.util.Base64;
import java.util.Random;
import java.util.UUID;

@Route(value = "scrabble")
@PageTitle("Scrabble | By Olivier")
public class ScrabbleView extends VerticalLayout
		implements AppShellConfigurator
{
	private static final Random RANDOM = new Random();
	private final Game game;

	public ScrabbleView() throws ScrabbleException, InterruptedException {
		final Server server = new Server();
		this.game = new Game(server, Dictionary.getDictionary(Language.FRENCH), 2);
		final UUID eleonore = UUID.randomUUID();
		this.game.addPlayer(Player.builder().name("Eleonore").id(eleonore).build());
		this.game.startGame();
		this.game.play(Action.builder().notation("H8 GATE").player(eleonore).build());
		addClassName("scrabble-view");
		final Div div = new Div();
		add(div);
		div.getElement().setProperty("innerHTML", createGridHTML());
	}

	@Override
	public void configurePage(final AppShellSettings settings) {
		AppShellConfigurator.super.configurePage(settings);
		settings.addFavIcon("icon", "icons/oscrabble-icon.png", "192x192");
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
}