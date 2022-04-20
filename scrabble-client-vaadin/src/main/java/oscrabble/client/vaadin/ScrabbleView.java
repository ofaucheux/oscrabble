package oscrabble.client.vaadin;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import oscrabble.ScrabbleException;
import oscrabble.client.JGrid;
import oscrabble.data.Action;
import oscrabble.data.Player;
import oscrabble.dictionary.Dictionary;
import oscrabble.dictionary.Language;
import oscrabble.server.Game;
import oscrabble.server.Server;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

@Route(value = "")
@PageTitle("Scrabble | By Olivier")
public class ScrabbleView extends VerticalLayout {

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

	private String createGridHTML() {
		final JGrid jGrid = new JGrid();
		jGrid.setGrid(this.game.getGrid());
		final byte[] image = jGrid.getImage();
		try {
			FileUtils.writeByteArrayToFile(new File("C:\\temp\\2022-04-20\\out.png"), image);
		} catch (IOException e) {
			throw new IOError(e);
		}
		final String encoded = Base64.getEncoder().encodeToString(image);
		return String.format(
				"<img style='display:block' id='base64image' src='data:image/png;base64,%s' />",
				encoded
		);
	}
}