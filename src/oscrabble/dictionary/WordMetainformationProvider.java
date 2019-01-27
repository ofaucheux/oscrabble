package oscrabble.dictionary;

public interface WordMetainformationProvider
{
	/**
	 * Sucht die Beschreibung eines Wortes, in Plain-Text oder HTML, in welchem Fall sie mit <pre>&lt;html&gt;</pre> anfängt.
	 * @param word das gesuchte Wort
	 * @return die Beschreibung, wenn gefunden, ansonsten {@code null}.
	 */
	String getDescription(String word);
}
