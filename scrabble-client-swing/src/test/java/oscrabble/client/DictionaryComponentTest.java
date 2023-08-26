package oscrabble.client;

import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import oscrabble.client.utils.SwingUtils;
import oscrabble.data.DictionaryEntry;
import oscrabble.data.IDictionary;
import oscrabble.dictionary.DictionaryException;

import java.util.List;

class DictionaryComponentTest {

    @Test
    @Ignore
    void showDescription() throws DictionaryException {
        IDictionary dictionary = Mockito.mock(IDictionary.class);
        DictionaryEntry entry = Mockito.mock(DictionaryEntry.class);

        String word = "WORD";
        Mockito.when(dictionary.getEntry(word)).thenReturn(entry);

        Mockito.when(entry.getDefinitions()).thenReturn(List.of(
                "big line with a lot of text so the display has",
                "big line with a lot of text so the display has to be break"));

        DictionaryComponent component = new DictionaryComponent(dictionary);
        component.showDescription(word);
        SwingUtils.displayInNewFrame(component);
    }
}