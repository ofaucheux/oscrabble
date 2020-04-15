package oscrabble.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import oscrabble.dictionary.Dictionary;
import oscrabble.json.messages.*;
import oscrabble.server.IServer;
import oscrabble.server.Server;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.regex.Pattern;

public class HttpServer extends AbstractHandler
{
	public static final String PARAM_MAX_WAIT = "maxWait";
	public static final String PARAM_MAX_WAIT_UNIT = "maxWaitUnit";

	public static final Pattern NEXT_PATTERN = Pattern.compile("/next/(.*)");
	public static final Logger LOGGER = Logger.getLogger(HttpServer.class);

	private final IServer server;

	/**
	 * Mapping receiver->queue
	 */
	final private Map<UUID, Map<UUID, ArrayBlockingQueue<JsonMessage>>> queues = new HashMap();

	public HttpServer(final IServer server)
	{
		this.server = server;
	}

	@Override
	public void handle(final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException
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

			responseMessage = JsonMessage.instantiate(InternalErrorMessage.class, post.getGame(), server.getUUID(), post.getTo());
			((InternalErrorMessage) responseMessage).setErrorMessage(e.toString() + "\n\n" + stack);
		}
		response.getWriter().write(responseMessage.toJson());
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
		final Class<JsonMessage> postMessageClass = (Class<JsonMessage>) Class.forName(NoMessageMessage.class.getPackageName() + "." + post.getCommand());
		final Method treat = this.getClass().getMethod("treat", postMessageClass);
		return (JsonMessage) treat.invoke(this, post);
	}

	public JsonMessage treat(final PoolMessage post) throws InterruptedException
	{
		final ArrayBlockingQueue<JsonMessage> queue =
				this.queues.getOrDefault(post.getGame(), new HashMap<>()).getOrDefault(post.getFrom(), new ArrayBlockingQueue<>(256));

		final JsonMessage waitingMessageForClient = queue.poll(
				post.getTimeout(),
				post.getTimeoutUnit()
		);
		final UUID game = post.getGame();
		return waitingMessageForClient == null
				? JsonMessage.instantiate(NoMessageMessage.class, game, this.server.getUUID(), post.getFrom())
				: waitingMessageForClient;
	}

	private JsonMessage treat(final AddPlayer post)
	{
		final PlayerStub stub = new PlayerStub();
		this.server.addPlayer(stub);
		return JsonMessage.instantiate(PlayerAddedMessage.class,
				post.getGame(),
				this.server.getUUID(),
				post.getFrom()
		);
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