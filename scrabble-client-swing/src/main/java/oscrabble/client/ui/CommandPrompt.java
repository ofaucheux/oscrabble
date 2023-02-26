package oscrabble.client.ui;

import oscrabble.client.Client;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.Set;

public class CommandPrompt {

    /**
     * Filter, das alles Eingetragene Uppercase schreibt
     */
    private final static DocumentFilter UPPER_CASE_DOCUMENT_FILTER = new DocumentFilter() {
        public void insertString(DocumentFilter.FilterBypass fb, int offset,
                                 String text, AttributeSet attr
        ) throws BadLocationException {

            fb.insertString(offset, toUpperCase(text), attr);
        }

        public void replace(DocumentFilter.FilterBypass fb, int offset, int length,
                            String text, AttributeSet attrs
        ) throws BadLocationException {

            fb.replace(offset, length, toUpperCase(text), attrs);
        }

        /**
         * Entfernt die Umlaute und liefert alles Uppercase.
         * TODO: für Frz. sinnvoll, für Deutsch aber sicherlich nicht..
         */
        private String toUpperCase(String text) {
            text = Normalizer.normalize(text, Normalizer.Form.NFD);
            text = text.replaceAll("[^\\p{ASCII}]", ""); //NON-NLS
            text = text.replaceAll("\\p{M}", ""); //NON-NLS
            return text.toUpperCase();
        }
    };
    
    private final JTextField textField;
    private final Client client;

    public CommandPrompt(final Client client) {
        this.client = client;
        final CommandPromptAction promptListener = new CommandPromptAction();

        this.textField = new JTextField();
        this.textField.addActionListener(promptListener);
        this.textField.setFont(this.textField.getFont().deriveFont(20f));

        final AbstractDocument document = (AbstractDocument) this.textField.getDocument();
        document.addDocumentListener(promptListener);
        document.setDocumentFilter(UPPER_CASE_DOCUMENT_FILTER);

//        client.addListener(
//                (GameState state) -> {
//                    int turnId = state.getTurnId();
//                    if (turnId != this.currentTurnId) {
//                        this.currentTurnId = turnId;
//                        this.textField.setText("");
//                    }
//                }
//        );
    }

    private final Set<Runnable> changeListeners = new HashSet<>();

    public void addChangeListener(final Runnable runnable) {
        this.changeListeners.add(runnable);
    }

    public void setCommand(String command) {
        this.textField.setText(command);
    }

    public Component getComponent() {
        return this.textField;
    }

    public String getCommand() {
        return this.textField.getText();
    }

    public void clear(final boolean keepCoordinate) {
        if (keepCoordinate) {
            String content = this.textField.getText();
            int space = content.indexOf(' ');
            this.textField.setText(space == -1 ? "" : content.substring(0, space + 1));
        } else {
            this.textField.setText("");
        }
    }

    private class CommandPromptAction extends AbstractAction implements DocumentListener {

        @Override
        public void actionPerformed(final ActionEvent e) {
            SwingUtilities.invokeLater(() -> CommandPrompt.this.client.executeCommand(getCommand()));
        }

        @Override
        public void insertUpdate(final DocumentEvent e) {
            changedUpdate(e);
        }

        @Override
        public void removeUpdate(final DocumentEvent e) {
            changedUpdate(e);
        }

        @Override
        public void changedUpdate(final DocumentEvent e) {

            CommandPrompt.this.changeListeners.forEach(Runnable::run);
        }
    }
}
