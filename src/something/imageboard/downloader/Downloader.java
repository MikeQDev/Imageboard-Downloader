package something.imageboard.downloader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Downloader {
	private final String IMAGE_BASE_URL = "http://i.4cdn.org/";
	private final String THREADURL;
	private final String SAVELOC;
	private final ThreadManager THREADMANAGER;
	private final char GETWEBMYN;
	private final int STATUS_STOPPED = 0, STATUS_RUNNING = 1, STATUS_DONE = 2;
	private ExecutorService exeServ;
	private int status = 0;
	private int totalImages = 0;
	private int downloadCount = 0;
	private Iterator<String> vecIterator;

	private Runnable downloadAndSave = new Runnable() {
		public void run() {
			// Make sure vector has not already been iterated through

			if (!vecIterator.hasNext())
				return;

			String curImageLoc = vecIterator.next();
			String curFullURL = IMAGE_BASE_URL + curImageLoc;
			String fileName = curImageLoc.split("/")[1];

			BufferedInputStream ins = null;
			BufferedOutputStream out = null;

			try {
				// Check if file already exists; if file exists, skip
				// downloading

				if (new File(SAVELOC + "\\" + fileName).exists()) {
					downloadCount++;
					return;
				}

				// Image reader
				ins = new BufferedInputStream(new URL(curFullURL).openStream());

				// Image writer
				out = new BufferedOutputStream(new FileOutputStream(SAVELOC
						+ "\\" + fileName));

				int nextByte;

				while ((nextByte = ins.read()) != -1)
					out.write(nextByte);

				downloadCount++;

				updateGUI();
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (ins != null)
						ins.close();
					if (out != null)
						out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	};

	public void stopDownload() {
		if (status != STATUS_DONE)
			new Thread(new Runnable() {
				public void run() {
					exeServ.shutdownNow();
					while (!exeServ.isTerminated())
						;
					status = STATUS_STOPPED;
					updateGUI();
				}
			}).start();
	}

	/**
	 * Executor services execute downloading
	 */
	public void download() {
		downloadCount = 0;
		for (int i = 0; i < totalImages; i++) {
			exeServ.execute(downloadAndSave);
		}
		exeServ.shutdown();
		while (!exeServ.isTerminated())
			status = STATUS_RUNNING;
		if (getPercentDone() == 100) {
			status = STATUS_DONE;
		} else {
			status = STATUS_STOPPED;
		}
		updateGUI();
	}

	/**
	 * 
	 * @param imageLocs
	 *            String vector of image locations on server
	 * @param saveLoc
	 *            save location to save images on hard drive
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public Downloader(String tURL, boolean getWebm, String saveLoc,
			ThreadManager thrdMngr) throws FileNotFoundException, IOException {
		File f = new File(saveLoc);
		f.mkdir();

		this.THREADURL = tURL;
		this.GETWEBMYN = getWebm ? 'Y' : 'N';
		this.SAVELOC = f.getAbsoluteFile().toString();
		this.THREADMANAGER = thrdMngr;

		Vector<String> imageLocs = ImageFinder.getImageLinks(tURL, getWebm);

		vecIterator = imageLocs.iterator();
		totalImages = imageLocs.size();

		exeServ = Executors.newFixedThreadPool(5);
	}

	/**
	 * Resumes a paused/stopped thread
	 */
	public void resumeThread() {
		//##A maybe do this on a new thread?
		download();
	}

	/**
	 * Notifies thread manager to notify main GUI to update table
	 */
	public void updateGUI() {
		THREADMANAGER.updateMainTable();
	}

	public String getSaveLocation() {
		return SAVELOC;
	}

	/**
	 * @return the percentage of images downloaded
	 */
	public int getPercentDone() {
		return (int) (((double) downloadCount / totalImages) * 100);
	}

	/**
	 * @return status of current thread
	 */
	public String getStatus() {
		switch (status) {
		case STATUS_DONE:
			return "Done";
		case STATUS_RUNNING:
			return "Running";
		case STATUS_STOPPED:
			return "Stopped";
		default:
			return "Unknown";
		}
	}

	public String getThreadURL() {
		return THREADURL;
	}

	public String getThreadURLExcludeBaseURL() {
		StringBuilder sB = new StringBuilder();
		String[] spl = THREADURL.split("/");
		for (int i = 3; i < spl.length; i++) {
			sB.append("/" + spl[i]);
		}
		return sB.toString();
	}

	public char getWebm() {
		return GETWEBMYN;
	}

	public int getTotalImageCount() {
		return totalImages;
	}

	public int getDownloadedImageCount() {
		return downloadCount;
	}
}
