package something.imageboard.downloader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Vector;

import javafx.util.converter.PercentageStringConverter;

import javax.swing.JOptionPane;

/**
 * Does the downloading part of the application
 * 
 * @author Mike
 *
 */
public class ThreadedDownloader {
	private String saveLocationStr;
	private String URL;
	private String status;
	private DownloadThread dT;
	private Thread updateThread;
	private Thread[] downloadThreads;
	private Vector<String> imageLocations;
	private Iterator<String> vecIterator;
	private int amountOfThreads = 1;
	private int downloadCount = 0;
	private int autoRefreshTime;
	private boolean getWebm;
	private boolean finishedMessageShowed;
	private boolean requestStop = false;
	private boolean isDoneDownloading = false;

	/**
	 * Gets the list of images to be downloaded in Vector format and starts the
	 * downloading thread(s)
	 * 
	 * @param URL
	 *            The URL of the thread on imageboard
	 * @param saveLoc
	 *            Destination for files to be saved
	 * @param threadAmt
	 *            Allows for multithreading
	 * @param getWebm
	 *            Determines whether or not webm files should be downloaded
	 * @throws IOException
	 *             some unknown error occurred
	 * @throws FileNotFoundException
	 *             thread is 404ed
	 * @throws UserMessedUpException
	 *             if the user doesn't provide enough information
	 */

	public ThreadedDownloader(String URL, String saveLoc, int threadAmt,
			boolean getWebm, int refreshTime) throws FileNotFoundException,
			IOException, UserMessedUpException {

		if (URL.equals("")) {
			GUI.resetGUI();
			JOptionPane.showMessageDialog(null, "Please provide a thread URL");
			throw new UserMessedUpException();
		}

		if (saveLoc.equals("")) {
			GUI.resetGUI();
			JOptionPane
					.showMessageDialog(null, "Please define a save location");
			throw new UserMessedUpException();
		}

		this.amountOfThreads = threadAmt;
		this.URL = URL;
		this.saveLocationStr = saveLoc;
		this.autoRefreshTime = refreshTime;
		this.getWebm = getWebm;
		this.downloadCount = 0;

		finishedMessageShowed = false;

		imageLocations = ImageFinder.getImageLinks(URL, this.getWebm);

		new File(saveLocationStr + "\\/").mkdir(); // creates the path if it
													// doesn't
													// exist

		dT = new DownloadThread();

		if (requestStop == true) // Make sure the downloading thread(s) are not
									// in request-to-stop mode
			requestStop = false;

		downloadThreads = new Thread[amountOfThreads];

		vecIterator = imageLocations.iterator();

		for (Thread t : downloadThreads) {
			t = new Thread(dT);
			t.start(); // Start the threads
		}
	}

	/**
	 * Set the requestStop flag to true, allowing running threads to halt
	 * execution and updates the status
	 */
	public void stopDownload() {
		requestStop = true;
		status = "Status: Download stopped @ " + downloadCount / 2 + "/"
				+ imageLocations.size() + " files";
		if (updateThread != null)
			updateThread.interrupt();
	}

	/**
	 * @return True if all images are finished downloading
	 */
	public boolean isDoneDownloading() {
		return isDoneDownloading;
	}

	/**
	 * @return Status of downloading
	 */
	public String getDownloadStatus() {
		return status;
	}

	/**
	 * @return % downloaded, for the progress bar
	 */
	public int getPercentDone() {
		if (downloadCount == 0)
			return 0;
		if (isDoneDownloading())
			return 100;
		return (int) (((double) downloadCount / imageLocations.size()) * 100);
	}

	/**
	 * Autorefreshes the thread until 404 or until user stops the autorefresh
	 * 
	 * @author Mike
	 *
	 */
	public void startAutoRefresh() {
		// Make sure all running threads are killed
		for (Thread t : downloadThreads) {
			try {
				t.interrupt();
				t = null;
			} catch (Exception ex) {

			}
		}
		updateThread = new Thread(new AutoRefreshThread());
		updateThread.start();
	}

	/**
	 * Calls the sleep method for countdown until next refresh, and then calls
	 * another downloader to download newly posted images
	 * 
	 * @author Mike
	 *
	 */
	class AutoRefreshThread implements Runnable {
		public void run() {
			DownloadThread updater = new DownloadThread();

			try {
				GUI.fillProgressBar();
				requestStop = false;

				do {
					GUI.fillProgressBar();
					autoRefreshCountdown();
					imageLocations = ImageFinder.getImageLinks(URL, getWebm);
					downloadCount = imageLocations.size();
					vecIterator = imageLocations.iterator();

					status = "Status: Downloading new images";

					GUI.updateGUI();
					downloadCount = 0;
					// MAKE SURE TO CALL run() AND DON'T PUT IN A THREAD,
					// OTHERWISE A NEW THREAD IS STARTED !!
					updater.run();
				} while (!requestStop);
			} catch (IOException e) {
				// System.out.println(Thread.currentThread().getName()
				// + " hit 404");
				GUI.resetGUI();
				status = "Status: Thread 404ed. Downloaded "
						+ imageLocations.size()
						+ " images. Waiting for user input.";
				JOptionPane
						.showMessageDialog(
								null,
								"Error encountered while auto-refeshing. Possible thread 404?\nStopping download");
				return;
			}
			status = "Status: Auto-refresh aborted. Downloaded "
					+ imageLocations.size()
					+ " images. Waiting for user input.";
			GUI.updateStatus();
		}

	}

	/**
	 * Displays time until next auto-refresh
	 */
	void autoRefreshCountdown() {
		for (int i = autoRefreshTime; i > 0; i--) {
			status = "Status: Auto-refreshing thread in " + i + " seconds";
			GUI.updateStatus();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				requestStop = true;
				GUI.updateGUI();
			}
		}
		status = "Status: Checking for updates";
		GUI.updateGUI();
	}

	/**
	 * The runnable object that does the downloading
	 * 
	 * @author Mike
	 *
	 */
	class DownloadThread implements Runnable {
		public void run() {

			isDoneDownloading = false; // Just incase..

			final String urlBeginning = "http://i.4cdn.org/"; // Beginning of
																// the image URL
			while (vecIterator.hasNext()) {
				if (requestStop) {
					status = "Status: Download stopped @ " + downloadCount
							+ "/" + imageLocations.size() + " files";
					GUI.updateStatus();
					break;
				}
				status = "Status: Downloading " + downloadCount + "/"
						+ imageLocations.size();
				String curURL = null;
				String fileName = null;
				try {
					curURL = vecIterator.next(); // Board & file name
					fileName = curURL.split("/")[1]; // File name only
				} catch (Exception ex) {
					// Just so an error is not thrown.
				}

				FileOutputStream out = null;
				try {
					URL pictureURL = new URL(urlBeginning + curURL); // Connects
																		// to
																		// URL
					// synchronized (this) {
					downloadCount++; // Increment amount of images
										// downloaded
					// }
					// Check if file already exists, if does, skip downloading
					if (new File(saveLocationStr + "\\" + fileName).exists()) {
						// System.out.println(fileName+" Exists..");
						continue;
					}

					GUI.updateGUI();

					BufferedInputStream ins = new BufferedInputStream(
							pictureURL.openStream()); // Creates reader for
														// image data

					out = new FileOutputStream(saveLocationStr + "\\"
							+ fileName); // Creates file writer

					int next;
					// System.out.println("writing " + fileName);
					while ((next = ins.read()) != -1) {
						out.write(next); // Write to file
					}
					// System.out.println(fileName + "<-downloaded");
				} catch (FileNotFoundException e) {
					System.out.println("Error downloading file " + fileName
							+ " (not found)");
				} catch (MalformedURLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					if (out != null)
						try {
							out.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
				}
			}
			if (getPercentDone() == 100)
				isDoneDownloading = true;
			else
				return;

			if (finishedMessageShowed)
				return;

			finishedMessageShowed = true;

			if (autoRefreshTime == -1) {
				// GUI.resetGUI();
				status = "Status: Download complete (" + imageLocations.size()
						+ "/" + imageLocations.size()
						+ ") Waiting for user input.";
				System.out.println(status + "," + getPercentDone());
				GUI.resetGUI();
				GUI.updateGUI();
				JOptionPane.showMessageDialog(null,
						"Thread finished downloading.");
			} else {
				startAutoRefresh();
				JOptionPane.showMessageDialog(null,
						"Thread finished downloading.\nAuto-refreshing every "
								+ autoRefreshTime + " seconds.");
			}
		}

	}
}

/**
 * Exception that is thrown whenever user does not provide enough information
 * 
 * @author Mike
 *
 */
class UserMessedUpException extends Exception {
	@Override
	public String getMessage() {
		return "User did not provide enough information.";
	}
}