package something.imageboard.downloader;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;

/**
 * Takes an imageboard's thread URL and searches it for posted images
 * 
 * @author Mike
 *
 */
public class ImageFinder {
	private static boolean getWebm = true;

	/**
	 * 
	 * @param URL
	 *            Thread URL
	 * @param getW
	 *            Download .webm filetype too?
	 * 
	 * @return The Vector holding image locations
	 * @throws FileNotFoundException
	 *             if the thread 404's
	 * @throws IOException
	 *             any other errors that may occur
	 */
	public static Vector<String> getImageLinks(String URL, boolean getW)
			throws FileNotFoundException, IOException {
		getWebm = getW;
		Vector<String> imageLinks = new Vector<>();
		URL tehURL = new URL(URL); // Creates URL
		URLConnection tehConn = tehURL.openConnection(); // Connects to URL

		BufferedReader readr = new BufferedReader(new InputStreamReader(
				tehConn.getInputStream())); // Creates source page reader
		String pageSource = readr.readLine(); // Stores page source in
												// pageSource
		// System.out.println(pageSource);
		String pictureURLRegex = "href=\"//i.4cdn.org/"; // Looks for this
															// string - the
															// starting code of
															// a posted image
		String[] splitString = pageSource.split(pictureURLRegex);

		String fileName;
		for (int i = 1; i < splitString.length; i += 2) {
			fileName = splitString[i].split("\"")[0];
			if (fileName.endsWith(".webm") && !getWebm)
				; // Do nothing - this is webm & user does not want them
			else
				imageLinks.add(fileName);
		}
		return imageLinks;
	}
}