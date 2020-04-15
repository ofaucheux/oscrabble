package oscrabble.json;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import oscrabble.Grid;
import oscrabble.Rack;
import oscrabble.action.Action;
import oscrabble.configuration.Configuration;
import oscrabble.dictionary.Dictionary;
import oscrabble.json.messages.AddPlayer;
import oscrabble.player.IPlayer;
import oscrabble.server.IPlayerInfo;
import oscrabble.server.IServer;
import oscrabble.server.Play;
import oscrabble.server.Server;

import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class ServerStub implements IServer
{

	private final org.eclipse.jetty.client.HttpClient httpClient = new HttpClient();

	@Override
	public void setState(final State state)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public void addPlayer(final IPlayer player)
	{
		final AddPlayer message = new AddPlayer();
		message.setName(player.getName());
		send(message);
	}

	@Override
	public void addListener(final Server.GameListener listener)
	{
		send(ADD_LISTENER, listener);
	}

	@Override
	public int play(final UUID clientKey, final Pla+y play, final Action action)
	{
		return (int) send(PLAY, clientKey, play, action);
	}

	@Override
	public IPlayer getPlayerToPlay()
	{
		return (IPlayer) send(GET_PLAYER_TO_PLAY);
	}

	@Override
	public List<IPlayerInfo> getPlayers()
	{
		return (List<IPlayerInfo>) send(GET_PLAYERS);
	}

	@Override
	public Iterable<Server.HistoryEntry> getHistory()
	{
		return (Iterable<Server.HistoryEntry>) send(GET_HISTORY);
	}

	@Override
	public Dictionary getDictionary()
	{
		return (Dictionary) send(GET_DICTIONARY);
	}

	@Override
	public Grid getGrid()
	{
		return (Grid) send(GET_GRID);
	}

	@Override
	public Rack getRack(final IPlayer player, final UUID clientKey)
	{
		return (Rack) send(GET_RACK, player, clientKey);
	}

	@Override
	public int getScore(final IPlayer player)
	{
		return (int) send(GET_SCORE, player);
	}

	@Override
	public void editParameters(final UUID caller, final IPlayerInfo player)
	{
		send(EDIT_PARAMETERS, caller, player);
	}

	@Override
	public void sendMessage(final IPlayer sender, final String message)
	{
		send(SEND_MESSAGE, sender, message);
	}

	@Override
	public void quit(final IPlayer player, final UUID key, final String message)
	{
		send(QUIT, player, key, message);
	}

	@Override
	public Configuration getConfiguration()
	{
		return (Configuration) send(GET_CONFIGURATION);
	}

	@Override
	public boolean isLastPlayError(final IPlayer player)
	{
		return (boolean) send(IS_LAST_PLAY_ERROR, player);
	}

	@Override
	public void playerConfigHasChanged(final IPlayer player, final UUID playerKey)
	{
		send(PLAYER_CONFIG_HAS_CHANGED, player, playerKey);
	}

	@Override
	public int getNumberTilesInBag()
	{
		return (int) send(GET_NUMBER_TILES_IN_BAG);
	}

	@Override
	public int getRequiredTilesInBagForExchange()
	{
		return (int) send(GET_REQUIRED_TILES_IN_BAG_FOR_EXCHANGE);
	}

	@Override
	public UUID getUUID()
	{
		throw new AssertionError();
//		return null;// TODO
	}

	/**
	 * Send a message to the server.
	 * @param message message to send
	 */
	private void send(final JsonMessage message) throws IOScrabbleError
	{
		try
		{
			final Request request = this.httpClient.newRequest(new URI("http://localhost:" + HttpServer.DEFAULT_PORT));
			final String input = message.toJson();
			request.content(new StringContentProvider(input, "application/json"));
			final ContentResponse response = request.send();
			final String output = response.getContentAsString();
			if (response.getStatus() != HttpServletResponse.SC_OK)
			{
				final StringBuffer sb = new StringBuffer();
				sb.append("Connection with server returned status ").append(response.getStatus()).append("\n");
				sb.append("\nInput:\n").append(input);
				sb.append("\nOuput:\n");
				sb.append(output);
				throw new IOScrabbleError(sb.toString(), null);
			}
		}
		catch (URISyntaxException | InterruptedException | TimeoutException | ExecutionException e)
		{
			throw new IOScrabbleError(e.getMessage(), e);
		}
	}
}