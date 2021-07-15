package something.imageboard.downloader;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
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
	private final static String PICTUREURLREGEX = "href=\"//i.4cdn.org/";

	private ImageFinder() {
	}

	/**
	 * 
	 * @param url
	 *            Thread URL
	 * @param getW
	 *            Download .webm filetype too?
	 * @return A String vector holding locations of each image in specified
	 *         thread URL
	 * @throws FileNotFoundException
	 *             if the thread 404's
	 * @throws IOException
	 *             any other IO exceptions that may occur
	 */
	public static Vector<String> getImageLinks(String url, boolean getW)
			throws FileNotFoundException, IOException {

		Vector<String> imageLinks = new Vector<String>();
		URLConnection conn = new URL(url).openConnection();

		BufferedReader readr = new BufferedReader(new InputStreamReader(
				conn.getInputStream())); // Creates source page reader

		String[] splitString = readr.readLine().split(PICTUREURLREGEX);

		String fileName;
		for (int i = 1; i < splitString.length; i += 2) {
			fileName = splitString[i].split("\"")[0];
			if (!fileName.endsWith(".webm")
					|| (fileName.endsWith(".webm") && getW))
				imageLinks.add(fileName);
		}
		return imageLinks;
	}

	/**
	 * Checks whether provided URL is valid
	 * 
	 * @param URL
	 *            url to check
	 * @return true if the URL is valid, false if the URL is invalid
	 */
	public static boolean isURLValid(String URL) {
		try {
			new BufferedReader(new InputStreamReader(new URL(URL)
					.openConnection().getInputStream()));
		} catch (Exception e) {
			return false;
		}
		return true;
	}
}