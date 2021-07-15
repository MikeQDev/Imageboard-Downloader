package something.imageboard.downloader;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

public class ThreadManager {
	private static ThreadManager tM;
	private static List<Downloader> threadList;
	private static GUI parentGUI;

	public void notifyGUI() {
		parentGUI.updateTable();
	}

	public void addThread(String tURL, boolean getWebm, String saveLoc) {
		try {
			final Downloader d = new Downloader(tURL, getWebm, saveLoc, this);
			new Thread(new Runnable() {
				public void run() {
					threadList.add(d);
					d.download();
				}
			}).start();
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Invalid URL:\n" + tURL);
		}

		updateMainTable();
	}

	public void updateMainTable() {
		parentGUI.updateTable();
	}

	/**
	 * @param threadNum
	 *            selected thread
	 * @return the selected thread
	 */
	public Downloader getThread(int threadNum) {
		return threadList.get(threadNum);
	}

	/**
	 * @return amount of active threads
	 */
	public int getThreadCount() {
		return threadList.size();
	}

	/**
	 * @param threadNum
	 *            selected thread
	 * @return information about selected thread
	 */
	public Object[] getThreadInfo(int threadNum) {
		Downloader d = threadList.get(threadNum);
		return new Object[] {
				d.getThreadURLExcludeBaseURL(),
				d.getSaveLocation(),
				d.getWebm(),
				d.getPercentDone() + "%",
				d.getStatus() + "(" + d.getDownloadedImageCount() + "/"
						+ d.getTotalImageCount() + ")" };
	}

	/**
	 * Removes the selected thread
	 * 
	 * @param threadNum
	 *            selected thread
	 * @throws Exception
	 */
	public void removeThread(int threadNum) throws Exception {
		threadList.remove(threadNum);
		updateMainTable();
	}

	/**
	 * Starts the newest thread added
	 */
	@Deprecated
	public void startLastAdded() {
		threadList.get(threadList.size() - 1).download();
		updateMainTable();
	}

	/**
	 * Starts the selected thread
	 * 
	 * @param threadNum
	 *            selected thread
	 */
	public void startThread(int threadNum) {
		threadList.get(threadNum).download();
		updateMainTable();
	}

	/**
	 * Stops the selected thread
	 * 
	 * @param threadNum
	 *            selected thread
	 * @throws Exception
	 */
	public void stopThread(int threadNum) throws Exception {
		threadList.get(threadNum).stopDownload();
		updateMainTable();
	}

	public void resumeThread(int threadNum) {
		//##A do this on a new thread, otherwise GUI thread freezes
		threadList.get(threadNum).download();
	}

	public static ThreadManager getInstance(GUI pGUI) {
		if (tM == null)
			tM = new ThreadManager();
		if (threadList == null)
			threadList = new ArrayList<Downloader>();
		if (parentGUI == null)
			parentGUI = pGUI;

		return tM;
	}

	private ThreadManager() {

	}
}
