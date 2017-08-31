package org.broad.igv.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import at.ccri.varan.GIE;
import at.ccri.varan.GIEDataset;

import javax.swing.JTable;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.awt.event.ActionEvent;
import javax.swing.SwingConstants;
import javax.swing.JTextPane;
import javax.swing.JFormattedTextField;

public class TestDialog extends JDialog {

    private final JPanel contentPanel = new JPanel();
    private JTable table;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
	try {
	    TestDialog dialog = new TestDialog();
	    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
	    dialog.setVisible(true);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    /**
     * Create the dialog.
     */
    public TestDialog() {
	setBounds(100, 100, 600, 571);
	getContentPane().setLayout(new BorderLayout());
	contentPanel.setLayout(new FlowLayout());
	contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
	getContentPane().add(contentPanel, BorderLayout.CENTER);
	{
		Object columnNames[] = { "Name", "Original file", "Versions", "" };
		GIE gie = GIE.getInstance();
		Object[][] rowdata = new Object[gie.getDatasets().size()][];
		int i = 0;
		for (GIEDataset d : gie.getDatasets().values()) {
		    List<String> entry = new ArrayList<String>();
		    entry.add(d.getName());
		    entry.add(d.getOrig().toString());
		    entry.add(d.getVersions().size() + "");
		    entry.add("delete");
		    rowdata[i++] = entry.toArray();
		}
		DefaultTableModel model = new DefaultTableModel(rowdata, columnNames);
		JTable table = new JTable( model );
		contentPanel.add(table);
	}
	{
		JPanel panel = new JPanel();
		FlowLayout flowLayout = (FlowLayout) panel.getLayout();
		flowLayout.setAlignment(FlowLayout.LEFT);
		getContentPane().add(panel, BorderLayout.NORTH);
		{
			JFormattedTextField frmtdtxtfldGieDatasets = new JFormattedTextField();
			frmtdtxtfldGieDatasets.setEnabled(false);
			frmtdtxtfldGieDatasets.setEditable(false);
			frmtdtxtfldGieDatasets.setHorizontalAlignment(SwingConstants.LEFT);
			frmtdtxtfldGieDatasets.setText("GIE Datasets");
			panel.add(frmtdtxtfldGieDatasets);
		}
	}
	{
	    JPanel buttonPane = new JPanel();
	    buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
	    getContentPane().add(buttonPane, BorderLayout.SOUTH);
	    {
		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			}
		});
		{
			JButton addButton = new JButton("Add Dataset");
			addButton.setHorizontalAlignment(SwingConstants.LEFT);
			buttonPane.add(addButton);
		}
		closeButton.setActionCommand("Cancel");
		buttonPane.add(closeButton);
	    }
	}
    }
    
    


}
