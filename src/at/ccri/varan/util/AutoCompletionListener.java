package at.ccri.varan.util;

import java.awt.event.ActionEvent;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

/**
 * Implements a basic autocompletion for text components
 * 
 * @author niko.popitsch
 *
 */
public class AutoCompletionListener implements DocumentListener {

    JTextComponent textField;
    private Collection<String> tokens;

    public AutoCompletionListener(JTextComponent textField, Collection<String> tokens) {
	this.textField = textField;
	this.tokens = tokens;
	// add listener
	textField.getDocument().addDocumentListener(this);
	// add action when enter is pressed.
	InputMap im = textField.getInputMap();
	ActionMap am = textField.getActionMap();
	im.put(KeyStroke.getKeyStroke("ENTER"), "autocomplete");
	am.put("autocomplete", new AbstractAction() {
	    private static final long serialVersionUID = 1L;

	    @Override
	    public void actionPerformed(ActionEvent e) {
		if (textField.getSelectedText() != null && textField.getSelectedText().length() > 0) {
		    int pos = textField.getSelectionEnd();
		    textField.setCaretPosition(pos);
		}
	    }
	});

    }

    public Collection<String> getTokens() {
	return tokens;
    }

    public void setTokens(Collection<String> tokens) {
	this.tokens = tokens;
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
	if (e.getLength() != 1) {
	    return;
	}
	int off = e.getOffset();
	String content = "";
	try {
	    content = textField.getText(0, off + 1);
	} catch (BadLocationException e1) {
	    return;
	    // e1.printStackTrace();
	}
	// Find word start
	int ws;
	for (ws = off; ws >= 0; ws--) {
	    if (!Character.isLetter(content.charAt(ws))) {
		break;
	    }
	}

	String prefix = content.substring(ws + 1).toLowerCase();
	if (prefix.length() < 2)
	    return;
	for (String t : tokens) {
	    if (t.toLowerCase().startsWith(prefix)) {
		String toadd = t.substring(off - ws);
		// SwingUtilities.invokeLater(new CompletionTask(toadd, off + 1));
		SwingUtilities.invokeLater(new Runnable() {

		    @Override
		    public void run() {
			StringBuffer txt = new StringBuffer(textField.getText());
			txt.insert(off + 1, toadd);
			textField.setText(txt.toString());
			textField.setCaretPosition(off + 1 + toadd.length());
			textField.moveCaretPosition(off + 1);
		    }

		});
		return;
	    }
	}

    }

    @Override
    public void removeUpdate(DocumentEvent e) {
    }

    @Override
    public void changedUpdate(DocumentEvent e) {

    }

}
