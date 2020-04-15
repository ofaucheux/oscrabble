package oscrabble.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import oscrabble.dictionary.Dictionary;
import oscrabble.json.messages.*;
import oscrabble.json.messages.InternalError;
import oscrabble.server.IServer;
import oscrabble.server.Server;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

@SuppressWarnings("unused")
public class HttpServer extends AbstractHandler
{
	public static final String PARAM_MAX_WAIT = "maxWait";
	public static final String PARAM_MAX_WAIT_UNIT = "maxWaitUnit";

	public static final Logger LOGGER = Logger.getLogger(HttpServer.class);

	private final IServer server;

	/**
	 * Mapping receiver->queue
	 */
	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	final private Map<String, Map<String, ArrayBlockingQueue<JsonMessage>>> queues = new HashMap<>();

	public HttpServer(final IServer server)
	{
		this.server = server;
	}

	@Override
	public void handle(final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response) throws IOException
	{
		final String posted = IOUtils.toString(baseRequest.getReader());
		final JsonMessage post;
		try
		{
			post = JsonMessage.parse(posted);
		}
		catch (JsonProcessingException e)
		{
			final StringWriter stack = new StringWriter();
			e.printStackTrace(new PrintWriter(stack));

			LOGGER.info(e.getMessage() + " with content:\n" + posted);
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Cannot parse posted message\n\n" + e.toString() + "\n" + stack);
			return;
		}

		JsonMessage responseMessage;
		try
		{
			responseMessage = treat(post);
		}
		catch (Throwable e)
		{
			LOGGER.error("Treatment failed", e);

			final StringWriter stack = new StringWriter();
			e.printStackTrace(new PrintWriter(stack));

			responseMessage = JsonMessage.instantiate(InternalError.class, post.getGame(), this.server.getUUID().toString(), post.getTo());
			((InternalError) responseMessage).setErrorMessage(e.toString() + "\n\n" + stack);
		}
		response.getWriter().write(responseMessage.toJson());
		response.setContentType("application/json");
		response.setStatus(HttpServletResponse.SC_OK);
		baseRequest.setHandled(true);
	}

	/**
	 * Treat a message. This method delegate the treatment to the particular #treat methods.
	 *
	 * @param post message to treat
	 * @return json response
	 */
	JsonMessage treat(final JsonMessage post) throws Exception
	{
		//noinspection unchecked
		final Class<JsonMessage> postMessageClass = (Class<JsonMessage>) Class.forName(NoMessage.class.getPackageName() + "." + post.getCommand());
		final Method treat = this.getClass().getMethod("treat" + postMessageClass.getSimpleName(), postMessageClass);
		return (JsonMessage) treat.invoke(this, post);
	}

	public JsonMessage treatPoolMessage(final PoolMessage post) throws InterruptedException
	{
		final ArrayBlockingQueue<JsonMessage> queue =
				this.queues.getOrDefault(post.getGame(), new HashMap<>()).getOrDefault(post.getFrom(), new ArrayBlockingQueue<>(256));

		final JsonMessage waitingMessageForClient = queue.poll(
				post.getTimeout(),
				post.getTimeoutUnit()
		);
		final String game = post.getGame();
		return waitingMessageForClient == null
				? JsonMessage.instantiate(NoMessage.class, game, this.server.getUUID().toString(), post.getFrom())
				: waitingMessageForClient;
	}

	public JsonMessage treatAddPlayer(final AddPlayer post)
	{
		final PlayerStub stub = new PlayerStub();
		this.server.addPlayer(stub);
		final PlayerAdded response = JsonMessage.instantiate(PlayerAdded.class,
				post.getGame(),
				this.server.getUUID().toString(),
				post.getFrom()
		);
		response.setPlayerName(stub.getName());
		response.setPlayerKey(stub.getPlayerKey().toString());
		return response;
	}

	public static void main(String[] args) throws Exception
	{
		oscrabble.server.Server scrabbleServer = new Server(Dictionary.getDictionary(Dictionary.Language.FRENCH));
		final HttpServer httpServer = new HttpServer(scrabbleServer);

		int port = 8080;
		final org.eclipse.jetty.server.Server jettyServer = new org.eclipse.jetty.server.Server(port);
		jettyServer.setHandler(httpServer);
		jettyServer.start();
		jettyServer.join();
	}
}
