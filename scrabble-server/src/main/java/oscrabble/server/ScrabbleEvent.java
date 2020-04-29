package oscrabble.server;

import java.util.function.Consumer;

/**
 * An event
 */
public interface ScrabbleEvent extends Consumer<GameListener>
{
}
