package oscrabble.dictionary;

public enum Language
{
	FRENCH("french"),
	GERMAN("german"),
	TEST("test")
	;
	final String directoryName;

	Language(final String directoryName)
	{
		this.directoryName = directoryName;
	}
}
