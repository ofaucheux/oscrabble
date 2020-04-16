package oscrabble.json;

import org.apache.log4j.Logger;
import oscrabble.dictionary.Dictionary;
import oscrabble.dictionary.Language;
import oscrabble.json.messages.reponses.AddPlayerResponse;
import oscrabble.json.messages.reponses.VoidResponse;
import oscrabble.json.messages.requests.AddPlayer;
import oscrabble.json.messages.requests.PoolMessage;
import oscrabble.server.IServer;
import oscrabble.server.Server;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class HttpServer extends MessageHandler
{
	public static final String PARAM_MAX_WAIT = "maxWait";
	public static final String PARAM_MAX_WAIT_UNIT = "maxWaitUnit";

	public static final Logger LOGGER = Logger.getLogger(HttpServer.class);
	public static final int DEFAULT_PORT = 8080;

	private final IServer server;
	private final HashMap<String, PlayerStub> playerStubs = new HashMap<>();

	public HttpServer(final IServer server)
	{
		this.server = server;
	}

	/**
	 * Treat a message. This method delegate the treatment to the particular #treat methods.
	 *
	 * @param post message to treat
	 * @return json response
	 */
	protected JsonMessage treat(final JsonMessage post) throws Exception
	{
		//noinspection unchecked
		final Class<JsonMessage> postMessageClass = (Class<JsonMessage>) Class.forName(VoidResponse.class.getPackageName() + "." + post.getCommand());
		final Method treat = this.getClass().getMethod("treat" + postMessageClass.getSimpleName(), postMessageClass);
		return (JsonMessage) treat.invoke(this, post);
	}

	public JsonMessage treatPoolMessage(final PoolMessage post) throws InterruptedException
	{
		final BlockingQueue<Server.ScrabbleEvent> queue = this.playerStubs.get(post.getFrom()).getIncomingEventQueue();

		final Server.ScrabbleEvent event = queue.poll(
				post.getTimeout(),
				post.getTimeoutUnit()
		);
		final String game = post.getGame();
		if (event == null)
		{
			JsonMessage.instantiate(VoidResponse.class, game, this.server.getUUID().toString(), post.getFrom());
			return new VoidResponse(); // TODO: should be something else
		}
		else
		{
			return null; // TODO: pack the event in a message and send it.
		}
	}

	public AddPlayerResponse treatAddPlayer(final AddPlayer post)
	{
		final Matcher m = Pattern.compile("(.*):(.*)").matcher(post.getInetSocketAddress());
		if (!m.matches())
		{
			throw new IOScrabbleError("Incorrect formatted socket address: " + post.getInetSocketAddress());
		}

		final PlayerStub stub = new PlayerStub(new InetSocketAddress(m.group(1), Integer.parseInt(m.group(2))));
		this.server.addPlayer(stub);
//		this.server.addListener(new JsonGameListener(stub));  // TODO
		final AddPlayerResponse response = JsonMessage.instantiate(AddPlayerResponse.class,
				post.getGame(),
				this.server.getUUID().toString(),
				post.getFrom()
		);
		final String name = stub.getName();
		this.playerStubs.put(post.getFrom(), stub);
		response.setPlayerName(name);
		response.setPlayerKey(stub.getPlayerKey().toString());
		return response;
	}

	public static void main(String[] args) throws Exception
	{
		final Language language = Language.FRENCH;
		oscrabble.server.Server scrabbleServer = new Server(language, Dictionary.getDictionary(language));
		final HttpServer httpServer = new HttpServer(scrabbleServer);

		int port = DEFAULT_PORT;
		final org.eclipse.jetty.server.Server jettyServer = new org.eclipse.jetty.server.Server(port);
		jettyServer.setHandler(httpServer);
		jettyServer.start();
		jettyServer.join();
	}
}
