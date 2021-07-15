package something.imageboard.downloader;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

public class GUI extends JFrame implements ActionListener {
	private JButton buttonAdd, buttonResume, buttonStop, buttonDelete;
	private JTable downloadTable;
	private DefaultTableModel tableModel;
	private ThreadManager threadManager;

	public GUI() {
		super("ImageBoard Downloader V2");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		threadManager = ThreadManager.getInstance(this);

		buildGUI();

		setVisible(true);
		pack();
	}

	public void buildGUI() {
		buildTable();
		buildButtonPane();
	}

	@SuppressWarnings("serial")
	private void buildTable() {
		tableModel = new DefaultTableModel(new Object[] { "Thread",
				"Save Location", "Webm", "Progress", "Status" }, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}

		};
		downloadTable = new JTable(tableModel);
		downloadTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane table = new JScrollPane(downloadTable);
		table.setPreferredSize(new Dimension(520, 80));

		add(table);
	}
	
	public void updateTable() {
		int lastSelected = downloadTable.getSelectedRow();
		tableModel.setRowCount(0);
		try {
			for (int i = 0; i < threadManager.getThreadCount(); i++) {
				tableModel.addRow(threadManager.getThreadInfo(i));
			}
		} catch (Exception x) {

		}
		if (lastSelected != -1 && tableModel.getRowCount() != lastSelected)
			downloadTable.setRowSelectionInterval(lastSelected, lastSelected);
	}

	private void buildButtonPane() {
		JPanel buttonPanel = new JPanel(new GridLayout(0, 1));

		buttonAdd = new JButton("Add thread");
		buttonAdd.addActionListener(this);
		buttonResume = new JButton("Resume download");
		buttonResume.addActionListener(this);
		buttonStop = new JButton("Stop download");
		buttonStop.addActionListener(this);
		buttonDelete = new JButton("Delete thread");
		buttonDelete.addActionListener(this);

		buttonPanel.add(buttonAdd);
		buttonPanel.add(buttonResume);
		buttonPanel.add(buttonStop);
		buttonPanel.add(buttonDelete);

		add(buttonPanel, BorderLayout.EAST);

	}

	public static void main(String[] args) throws InterruptedException {
		new GUI();
	}

	public void actionPerformed(ActionEvent e) {
		int selectedRow = downloadTable.getSelectedRow();
		if (selectedRow == -1 && e.getSource() != buttonAdd)
			return;
		Downloader curThread = null;
		if (e.getSource() != buttonAdd)
			curThread = threadManager.getThread(selectedRow);

		if (e.getSource() == buttonAdd) {
			Object[] addThread = new AddThreadDialog(this).getInfo();

			if (addThread[0] == null)
				return;

			addThread[2] = (Integer) addThread[2] == 1 ? true : false;

			threadManager.addThread((String) addThread[0],
					(Boolean) addThread[2], (String) addThread[1]);

		} else if (e.getSource() == buttonResume) {
			threadManager.resumeThread(selectedRow);

		} else if (e.getSource() == buttonStop) {
			try {
				threadManager.stopThread(selectedRow);
			} catch (Exception ex) {
				JOptionPane
						.showMessageDialog(
								null,
								"An error occured while trying to stop selected thread",
								"Error", JOptionPane.ERROR_MESSAGE);
			}
		} else if (e.getSource() == buttonDelete) {
			if (curThread.getStatus().equalsIgnoreCase("done")
					|| curThread.getStatus().equalsIgnoreCase("stopped")) {
				try {
					if (selectedRow == downloadTable.getRowCount() - 1
							&& selectedRow != 0)
						downloadTable.setRowSelectionInterval(selectedRow - 1,
								selectedRow - 1);
					threadManager.removeThread(selectedRow);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				return;
			}
			System.out.println("Stop thread before removing");
		}
	}
}

class AddThreadDialog extends JDialog implements ActionListener {
	private JTextField textfieldThreadUrl, textfieldSavLoc;
	private JCheckBox checkboxGetWebm;
	private JButton buttonAddThread, buttonCancel, buttonSaveLoc;
	private String threadUrl, saveLoc;
	private int getWebm;

	public AddThreadDialog(JFrame owner) {
		super(owner, "Add new thread", true);

		buildInput();
		buildButtons();

		pack();
		setVisible(true);
	}

	/**
	 * 
	 * @return an array of the objects thread URL String, save location string,
	 *         and webm boolean
	 */
	public Object[] getInfo() {
		return new Object[] { threadUrl, saveLoc, getWebm };
	}

	public void buildInput() {
		JPanel input = new JPanel(new GridLayout(0, 1));
		JPanel topLevel = new JPanel(new FlowLayout());
		JPanel bottomLevel = new JPanel(new FlowLayout());

		JLabel labelInputThread = new JLabel("Thread URL:");
		JLabel labelSavLoc = new JLabel("Save Location:");

		buttonSaveLoc = new JButton("...");
		buttonSaveLoc.addActionListener(this);

		textfieldThreadUrl = new JTextField(20);
		textfieldSavLoc = new JTextField(22);

		textfieldSavLoc.setText("f:\\temp\\");

		checkboxGetWebm = new JCheckBox("Get webm", true);

		topLevel.add(labelInputThread);
		topLevel.add(textfieldThreadUrl);
		topLevel.add(checkboxGetWebm);

		bottomLevel.add(labelSavLoc);
		bottomLevel.add(textfieldSavLoc);
		bottomLevel.add(buttonSaveLoc);

		input.add(topLevel);
		input.add(bottomLevel);

		add(input);
	}

	public void buildButtons() {
		JPanel buttonPanel = new JPanel(new FlowLayout());
		buttonAddThread = new JButton("Add thread");
		buttonCancel = new JButton("Cancel");

		buttonAddThread.addActionListener(this);
		buttonCancel.addActionListener(this);

		buttonPanel.add(buttonAddThread);
		buttonPanel.add(buttonCancel);
		add(buttonPanel, BorderLayout.SOUTH);
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == buttonAddThread) {
			threadUrl = textfieldThreadUrl.getText();
			saveLoc = textfieldSavLoc.getText();
			getWebm = checkboxGetWebm.isSelected() ? 1 : 0;
			if (!threadUrl.startsWith("http://")
					&& !threadUrl.startsWith("https://"))
				threadUrl = "http://" + threadUrl;
			if (ImageFinder.isURLValid(threadUrl)) {
				dispose();
			} else {
				threadUrl = null;
				JOptionPane.showMessageDialog(null,
						"Please provide a valid thread URL", "Invalid URL",
						JOptionPane.ERROR_MESSAGE);
			}
		} else if (e.getSource() == buttonCancel) {
			threadUrl = null;
			dispose();
		} else if (e.getSource() == buttonSaveLoc) {
			JFileChooser fC = new JFileChooser();
			fC.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int returnValue = fC.showOpenDialog(this);
			if (returnValue == JFileChooser.APPROVE_OPTION)
				textfieldSavLoc.setText(fC.getSelectedFile().toString());
		}
	}
}