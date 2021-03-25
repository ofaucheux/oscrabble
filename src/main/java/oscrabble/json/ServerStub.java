package oscrabble.json;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import oscrabble.Grid;
import oscrabble.Rack;
import oscrabble.ScrabbleException;
import oscrabble.action.Action;
import oscrabble.configuration.Configuration;
import oscrabble.dictionary.DictionaryException;
import oscrabble.dictionary.ScrabbleLanguageInformation;
import oscrabble.json.messages.reponses.AddPlayerResponse;
import oscrabble.json.messages.requests.AddPlayer;
import oscrabble.player.IPlayer;
import oscrabble.server.IPlayerInfo;
import oscrabble.server.IServer;
import oscrabble.server.Play;
import oscrabble.server.Server;

import java.net.InetSocketAddress;
import java.util.*;

public class ServerStub extends Stub implements IServer {
	/**
	 * Listening server for incoming requests
	 */
	private final org.eclipse.jetty.server.Server jettyServer;

	/**
	 * Registered listeners
	 */
	private final HashSet<Server.GameListener> listeners = new HashSet<>();
	private String playerKey;

	public ServerStub(final InetSocketAddress serverAddress) {
		super(serverAddress);

		// start server
		final int listeningPort = new Random().nextInt(2000) + 1024;
		this.jettyServer = new org.eclipse.jetty.server.Server(listeningPort);
//		jettyServer.setHandler(); // TODO
		try {
			jettyServer.start();
			jettyServer.join();
		} catch (Exception e) {
			throw new Error(e);
		}
	}

	@Override
	public void setState(final State state) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addPlayer(final IPlayer player) {
		assert player instanceof PlayerStub;
		final AddPlayer request = new AddPlayer();
		request.setName(player.getName());
		final NetworkConnector c = (NetworkConnector) this.jettyServer.getConnectors()[0];
		request.setInetSocketAddress(c.getHost() + ":" + c.getPort());
		final AddPlayerResponse response = sendRequest(request);
		this.playerKey = response.getPlayerKey();
	}

	@Override
	public void addListener(final Server.GameListener listener) {
		// TODO
	}

	@Override
	public int play(final UUID clientKey, final Play play, final Action action) {
		return 0;// TODO
	}

	@Override
	public IPlayer getPlayerToPlay() {
		return null;// TODO
	}

	@Override
	public List<IPlayerInfo> getPlayers() {
		return null;// TODO
	}

	@Override
	public Iterable<Server.HistoryEntry> getHistory() {
		return null;// TODO
	}

	@Override
	public ScrabbleLanguageInformation getScrabbleLanguageInformation() {
		return null;// TODO
	}

	@Override
	public Grid getGrid() {
		return null;// TODO
	}

	@Override
	public Rack getRack(final IPlayer player, final UUID clientKey) {
		return null;// TODO
	}

	@Override
	public int getScore(final IPlayer player) {
		return 0;// TODO
	}

	@Override
	public void editParameters(final UUID caller, final IPlayerInfo player) {
		// TODO
	}

	@Override
	public void sendMessage(final IPlayer sender, final String message) {
		// TODO
	}

	@Override
	public void quit(final IPlayer player, final UUID key, final String message) {
		// TODO
	}

	@Override
	public Configuration getConfiguration() {
		return null;// TODO
	}

	@Override
	public boolean isLastPlayError(final IPlayer player) {
		return false;// TODO
	}

	@Override
	public void playerConfigHasChanged(final IPlayer player, final UUID playerKey) {
		// TODO
	}

	@Override
	public int getNumberTilesInBag() {
		return 0;// TODO
	}

	@Override
	public int getRequiredTilesInBagForExchange() {
		return 0;// TODO
	}

	@Override
	public UUID getUUID() {
		return null;// TODO
	}

	@Override
	public Collection<String> getMutations(final String word) {
		return null;// TODO
	}

	@Override
	public Iterable<String> getDefinitions(final String word) throws DictionaryException {
		return null;// TODO
	}
}