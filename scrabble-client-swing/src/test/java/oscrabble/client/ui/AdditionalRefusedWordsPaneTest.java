package oscrabble.client.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import oscrabble.server.Server;

import javax.swing.*;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdditionalRefusedWordsPaneTest {

	@Mock
	Server server;

	@Test
	public void test() {
		final HashSet<String> refused = new HashSet<>();
		doAnswer(invocation -> new HashSet<>(refused))
				.when(this.server)
				.getAdditionalRefusedWords(any());
		doAnswer(invocation -> {
			refused.clear();
			refused.addAll(invocation.getArgument(1, Set.class));
			return null;
		}).when(this.server).setRefusedWords(any(), any());

		JOptionPane.showMessageDialog(null, new ServerConfigPanel(this.server));
	}
}