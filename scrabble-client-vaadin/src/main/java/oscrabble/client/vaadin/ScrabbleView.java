package oscrabble.client.vaadin;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.apache.commons.io.FileUtils;
import oscrabble.ScrabbleException;
import oscrabble.client.JGrid;
import oscrabble.data.Action;
import oscrabble.data.Player;
import oscrabble.dictionary.Dictionary;
import oscrabble.dictionary.Language;
import oscrabble.server.Game;
import oscrabble.server.Server;

import javax.swing.*;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;

@Route(value = "")
@PageTitle("Scrabble | By Olivier")
public class ScrabbleView extends VerticalLayout {

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

	private String createGridHTML() {
		final JGrid jGrid = new JGrid();
		jGrid.setGrid(this.game.getGrid());

		final JFrame frame = new JFrame();
		File file = null;
		try {
			frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			frame.add(jGrid);
			frame.pack();

			final byte[] image = jGrid.getImage();
			file = new File(String.format("C:/temp/2022-04-20/out_%s.png", RANDOM.nextLong()));
			FileUtils.writeByteArrayToFile(file, image);
			final String encoded = Base64.getEncoder().encodeToString(image);
			return String.format(
					"<img style='display:block' id='base64image' src='data:image/png;base64,%s' />",
					encoded
			);
		} catch (IOException e) {
			throw new IOError(e);
		} finally {
			frame.dispose();
			FileUtils.deleteQuietly(file);
		}
	}
}