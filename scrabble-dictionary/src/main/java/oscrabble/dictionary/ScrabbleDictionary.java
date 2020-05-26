package oscrabble.dictionary;

import oscrabble.data.IDictionary;
import oscrabble.data.ScrabbleRules;

public class ScrabbleDictionary extends Dictionary implements IDictionary
{
	final Language language;
	private final ScrabbleRules rules;

	public ScrabbleDictionary(final Language language)
	{
		super(language);
		this.language = language;
		rules = ScrabbleRulesFactory.create(this.language);
	}

	@Override
	public boolean isAdmissible(final String word)
	{
		return getAdmissibleWords().contains(toUpperCase(word));
	}

	@Override
	public ScrabbleRules getScrabbleRules()
	{
		return this.rules;
	}
}
