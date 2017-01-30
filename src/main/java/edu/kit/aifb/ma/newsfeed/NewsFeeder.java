package edu.kit.aifb.ma.newsfeed;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import edu.kit.aifb.ma.util.Property;

/**
 * Periodically downloading the news from ijs news stream.
 * 
 */
public class NewsFeeder {

  private Logger _logger = Logger.getLogger(NewsFeeder.class);

  private HttpURLConnection _urlConnection;

  private ReadableByteChannel _rbChannel;

  private FileOutputStream _fos;

  private File _file;

  /**
   * Initialize the HttpURLConnection and prepare the .gz file to be written by the data from input stream.
   * 
   * @param startTime
   */
  protected void init() {
    SimpleDateFormat dateFormat = new SimpleDateFormat(Property.getValue("date_format"));
    String dateString = dateFormat.format(new Date(new Date().getTime() - 3 * 60 * 60 * 1000));

    String authString = Property.getValue("username") + ":" + Property.getValue("password");
    authString = Base64.encodeBase64String(authString.getBytes());

    String customizedURL = Property.getValue("base_feed_url") + "?after=" + dateString;

    String fileName = "public-news-" + dateString + ".xml.gz";

    try {
      URL url = new URL(customizedURL);
      _urlConnection = (HttpURLConnection) url.openConnection();
      _urlConnection.setRequestProperty("Authorization", "Basic " + authString);
      _logger.debug("Connection is established!");
      _file = new File(Property.getValue("news_path") + File.separator + fileName);
      if (!_file.exists()) {
        _file.createNewFile();
        _logger.debug("Empty file named <" + _file.getName() + "> is created in the path <" + _file.getAbsolutePath() + ">");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Retrieve the data from channel and write it into file.
   */
  protected void retrieveData() {
    try {
      _rbChannel = Channels.newChannel(_urlConnection.getInputStream());
      _fos = new FileOutputStream(_file);
      _fos.getChannel().transferFrom(_rbChannel, 0, Long.MAX_VALUE);
    } catch (IOException e) {
      e.printStackTrace();
      _file.delete(); // if the file is not complete retrieved, then delete it.
      _logger.error("Error occurred during data retrieval!");
    }
  }

  /**
   * Close data stream, channel and URL connection.
   */
  protected void close() {
    try {
      _fos.close();
      _logger.debug("The File OutputStream is closed!");
    } catch (IOException e) {
      e.printStackTrace();
      _logger.error("The File OutoutStream can not be closed!");
    }
    if (_rbChannel.isOpen()) {
      try {
        _rbChannel.close();
        _logger.debug("The Readable ByteChannel is closed!");
      } catch (IOException e) {
        e.printStackTrace();
        _logger.error("The Readable ByteChannel can not be closed!");
      }
    }
    _urlConnection.disconnect();
    _logger.debug("The URL Connection is disconnected!");
  }

}
