package oscrabble.dictionary;

public interface WordMetainformationProvider
{
	/**
	 * Sucht die Definitionen eines Wortes, in Plain-Text oder HTML, in welchem Fall sie mit <pre>&lt;html&gt;</pre> anfangen.
	 * @param word das gesuchte Wort
	 * @return die Beschreibung, wenn gefunden, ansonsten {@code null}.
	 */
	Iterable<String> getDefinitions(String word) throws DictionaryException;
}
