package at.ccri.varan.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

public class TestDialog extends JDialog {

    public TestDialog() {
	setTitle("VARAN-GIE :: Datasets");
	setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	setMinimumSize(new Dimension(640, 300));

	JEditorPane p = new JEditorPane();
	p.setText("AHA");
	JScrollPane sp = new JScrollPane(p);

	JPanel catPanel = new JPanel();
	catPanel.setLayout(new GridLayout(10, 1));
	
//	catPanel.add(new JLabel("Filter By Category"));
	DefaultComboBoxModel<String> comboModel = new DefaultComboBoxModel<>();
	comboModel.addElement("ALL");
	JComboBox<String> catCombo = new JComboBox<>();
	catCombo.setModel(comboModel);
	catPanel.add(catCombo);
	catCombo.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1, true),
		"Filter By Category"));
		
	// TOP PANEL: descr + cat selection
	JPanel topPanel = new JPanel(new BorderLayout());
	topPanel.add(sp, BorderLayout.CENTER);
	topPanel.add(catPanel, BorderLayout.EAST);
	getContentPane().add(topPanel, BorderLayout.PAGE_START);

	getContentPane().add(new Label("TEST"), BorderLayout.CENTER);

	pack();
	setVisible(true);
    }

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
	// JFrame myframe = new JFrame("Hello world!");
	// myframe.setLayout(new BorderLayout());
	// myframe.setSize(500,500);
	//
	// //The CENTER component expands whenever the window is resized
	// myframe.add(new JScrollPane(new JTextArea()), BorderLayout.CENTER);
	// myframe.add(new JButton("Do something"), BorderLayout.EAST);
	//
	//
	// myframe.setVisible(true);

	try {
	    JDialog f = new TestDialog();
	    WindowListener exitListener = new WindowAdapter() {
		@Override
		public void windowClosing(WindowEvent e) {
		    System.out.println("BYE");
		    System.exit(0);
		}
	    };

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

}
