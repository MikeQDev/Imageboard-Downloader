package something.imageboard.downloader;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;

/**
 * Contains the GUI for the imageboard downloader
 * 
 * @author Mike
 *
 */
public class GUI extends JFrame {
	private static JTextField txtSaveDestination;
	private static JSpinner spinner;
	private static JButton btnStop;
	private static JButton btnDownload;
	private static JProgressBar progressBar;
	private static JTextField txtpnThreadUrl;
	private static JCheckBox chckbxDownloadWebm;
	private static ThreadedDownloader tD;
	private static JCheckBox chckbxAutorefreshThread;
	private static JLabel statusLabel;
	private static int refreshTime = -1;
	private JLabel lblThreads;
	private JLabel lblThreadLink;
	private JLabel lblSaveDestination;

	/**
	 * Main method
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		new GUI();
	}

	/**
	 * Builds the GUI
	 */
	public GUI() {
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setTitle("Imageboard Downloader v1.2");

		// Download button
		btnDownload = new JButton("Download");
		btnDownload.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				// boolean dontDownload = false;
				if (!btnDownload.isEnabled())
					return;
				disableGUI();
				if (tD != null)
					tD = null; // Safecheck to remove old instances of
								// downloader
								// if exist
				try {
					tD = new ThreadedDownloader(txtpnThreadUrl.getText(),
							txtSaveDestination.getText(), (int) spinner
									.getValue(), chckbxDownloadWebm
									.isSelected(), refreshTime);
				} catch (FileNotFoundException fExc) { // Notify user that
														// thread 404ed
					GUI.resetGUI();
					JOptionPane.showMessageDialog(null,
							"Thread 404; unable to download.", "404",
							JOptionPane.WARNING_MESSAGE);
					// dontDownload = true;
					return;
				} catch (IOException exc) { // Other error catching
					GUI.resetGUI();
					JOptionPane.showMessageDialog(null,
							"An error has occurred:\n" + exc.getMessage(),
							"Error", JOptionPane.WARNING_MESSAGE);
					// dontDownload = true;
					return;
				} catch (UserMessedUpException e1) {
					GUI.resetGUI();
					// dontDownload = true;
					return;
				}
				// if (!dontDownload)
				// t.start();
			}
		});
		btnDownload.setBounds(468, 22, 137, 23);

		// Stop button
		btnStop = new JButton("Stop Download");
		btnStop.setEnabled(false);
		btnStop.setBounds(468, 57, 137, 23);
		btnStop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!btnStop.isEnabled())
					return;
				if (tD != null)
					tD.stopDownload();
				statusLabel.setText(tD.getDownloadStatus());
				resetGUI();
			}
		});

		chckbxDownloadWebm = new JCheckBox("Get .webm's", true);
		chckbxDownloadWebm.setBounds(261, 57, 102, 23);

		spinner = new JSpinner();
		spinner.setModel(new SpinnerNumberModel(1, 1, 10, 1));
		spinner.setBounds(218, 58, 37, 20);
		txtpnThreadUrl = new JTextField();
		txtpnThreadUrl.setBounds(10, 23, 445, 20);

		progressBar = new JProgressBar();
		progressBar.setBounds(10, 89, 595, 20);

		txtSaveDestination = new JTextField();
		txtSaveDestination.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (!txtSaveDestination.isEnabled())
					return;
				JFileChooser fC = new JFileChooser();
				fC.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				fC.showSaveDialog(null);
				try {
					txtSaveDestination.setText(fC.getSelectedFile()
							.getAbsolutePath());
				} catch (NullPointerException ex) {
				}
			}
		});
		txtSaveDestination.setBounds(10, 58, 143, 20);
		txtSaveDestination.setColumns(10);

		lblThreads = new JLabel("Threads: ");
		lblThreads.setBounds(163, 61, 54, 14);
		getContentPane().setLayout(null);

		chckbxAutorefreshThread = new JCheckBox("Auto-refresh");
		chckbxAutorefreshThread.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String userInput = null;
				boolean errorOccured = false;
				int updateSeconds = -1;
				if (chckbxAutorefreshThread.isSelected()) {

					do {
						errorOccured = false;
						userInput = JOptionPane
								.showInputDialog("How often to refresh thread? (1-300 seconds)");
						if (userInput == null || userInput.equals("")) { // user
																			// cancelled
																			// input
							updateSeconds = -1;
							chckbxAutorefreshThread.setSelected(false);
							break;
						}
						try {
							updateSeconds = Integer.parseInt(userInput);
						} catch (Exception ex) {
							errorOccured = true;
						}
					} while (updateSeconds < 1 || updateSeconds > 300
							|| errorOccured);
				} else { // User is deselecting the box
					updateSeconds = -1;
				}
				refreshTime = updateSeconds;
			}
		});
		chckbxAutorefreshThread.setBounds(365, 57, 102, 23);
		getContentPane().add(chckbxAutorefreshThread);
		getContentPane().add(progressBar);
		getContentPane().add(txtSaveDestination);
		getContentPane().add(chckbxDownloadWebm);
		getContentPane().add(spinner);
		getContentPane().add(lblThreads);
		getContentPane().add(btnStop);
		getContentPane().add(txtpnThreadUrl);
		getContentPane().add(btnDownload);

		lblThreadLink = new JLabel("Thread URL");
		lblThreadLink.setBounds(10, 9, 73, 14);
		getContentPane().add(lblThreadLink);

		lblSaveDestination = new JLabel("Save destination");
		lblSaveDestination.setBounds(10, 44, 143, 14);
		getContentPane().add(lblSaveDestination);

		statusLabel = new JLabel("Status: waiting for user input");
		statusLabel.setBounds(10, 112, 595, 14);
		getContentPane().add(statusLabel);

		setSize(622, 157);
		setResizable(false);
		setVisible(true);
	}

	/**
	 * Updates the status of 'status' label on GUI
	 */
	public static void updateStatus() {
		try {
			if (tD == null) {
				return;
			}
			statusLabel.setText(tD.getDownloadStatus());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Fills the progress bar completely
	 */
	public static void fillProgressBar() {
		if (progressBar.getValue() != 100)
			progressBar.setValue(100);
	}

	/**
	 * Disables GUI buttons because the downloader is running
	 */
	public static void disableGUI() {
		btnDownload.setEnabled(false);
		chckbxAutorefreshThread.setEnabled(false);
		txtSaveDestination.setEnabled(false);
		txtpnThreadUrl.setEnabled(false);
		chckbxDownloadWebm.setEnabled(false);
		spinner.setEnabled(false);
		btnStop.setEnabled(true);
	}

	/**
	 * Resets the GUI to starting state
	 */
	public static void resetGUI() {
		statusLabel.setText("Status: waiting for user input");
		progressBar.setValue(0);
		chckbxAutorefreshThread.setEnabled(true);
		btnDownload.setEnabled(true);
		txtSaveDestination.setEnabled(true);
		txtpnThreadUrl.setEnabled(true);
		chckbxDownloadWebm.setEnabled(true);
		spinner.setEnabled(true);
		btnStop.setEnabled(false);
	}

	public static void updateGUI() {
		if (tD != null) {
			progressBar.setValue(tD.getPercentDone());
			statusLabel.setText(tD.getDownloadStatus());
		}
	}
}
