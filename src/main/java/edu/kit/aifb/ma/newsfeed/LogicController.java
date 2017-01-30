package edu.kit.aifb.ma.newsfeed;

import java.io.File;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import edu.kit.aifb.ma.index.mangodb.NFArticleMongoDBIndexer;
import edu.kit.aifb.ma.util.FileFilter;
import edu.kit.aifb.ma.util.FileQuantitySetter;
import edu.kit.aifb.ma.util.FileUnzipper;
import edu.kit.aifb.ma.util.Property;

/**
 * The control logic of the data level, firstly retrieving data from ijs news stream, secondly uncompressing the file and parse the xml file, at the
 * end building the lucene indexers using the parsing results.
 * 
 */
public class LogicController extends TimerTask {

  private static final int FILE_MAXIMUM = 100;

  private NFArticleMongoDBIndexer _nfArticleMongoDBIndexer;

  private NFArticleParser _nfArticleParser;

  private NewsFeeder _newsFeeder;

  private Logger _logger = Logger.getLogger(LogicController.class);

  public static void main(String[] args) {
    Timer timer = new Timer();
    LogicController controller = new LogicController();
    timer.schedule(controller, 0, 1000 * 60 * 60 * 5);
  }

  public LogicController() {
    initDirs();
    try {
      _nfArticleMongoDBIndexer = new NFArticleMongoDBIndexer();
      _newsFeeder = new NewsFeeder();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void run() {
    FileQuantitySetter.setMaximum(FILE_MAXIMUM);
    delOldgzFiles();
    retrieveNews();
    String[] gzFiles = (new FileFilter(Property.getValue("news_path"), "gz")).filter();
    List<File> xmlFiles = unzipFile(gzFiles);
    for (File file : xmlFiles) {
      _logger.debug("Start parsing articles......");
      _nfArticleParser = new NFArticleParser(file);
      Map<String, NFArticle> articles = _nfArticleParser.parseArticleNode();
      _logger.debug("Finish parsing articles......");
      try {
        _logger.debug("Start building index......");
        _nfArticleMongoDBIndexer.insertData(articles);
        _logger.debug("Finish building index......");
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Remove the old gz. files to avoid parse the same new files.
   */
  private void delOldgzFiles() {
    _logger.debug("Start deleting old .gz files......");
    String[] gzFiles = (new FileFilter(Property.getValue("news_path"), "gz")).filter();
    if (gzFiles.length > 0) {
      for (int i = 0; i < gzFiles.length; i++) {
        File gzFile = new File(Property.getValue("news_path") + gzFiles[i]);
        gzFile.delete();
      }
    }
    _logger.debug("Finish deleting old .gz files......");
  }

  /**
   * Download the compressed news file from ijs news feed stream.
   */
  private void retrieveNews() {
    //    NewsFeeder newsFeeder = new NewsFeeder();
    _logger.debug("Start retrieving news from server......");
    _newsFeeder.init();
    _newsFeeder.retrieveData();
    _newsFeeder.close();
    _logger.debug("Finish retrieving news from server......");
  }

  /**
   * Uncompress the .gz files.
   * 
   * @param gzFiles
   * @return
   */
  private List<File> unzipFile(String[] gzFiles) {
    _logger.debug("Start unzipping file......");
    FileUnzipper unzipper = new FileUnzipper();
    List<File> xmlFiles = new ArrayList<>();
    if (gzFiles != null) {
      for (int i = 0; i < gzFiles.length; i++) {
        File gzFile = new File(Property.getValue("news_path") + gzFiles[i]);
        if (gzFile.canRead()) {
          File file = unzipper.gunzipFile(Property.getValue("news_path"), gzFile);
          xmlFiles.add(file);
        }
      }
    }
    _logger.debug("Finish unzipping file......");
    return xmlFiles;
  }

  /**
   * Check the necessary directories, if not exist, then create a new one.
   */
  public void initDirs() {
    _logger.debug("Start initializing directories......");
    File newsDir = new File(Property.getValue("news_path"));
    File logDir = new File(Property.getValue("logs_path"));
    if (!newsDir.exists()) {
      newsDir.mkdirs();
    }
    if (!logDir.exists()) {
      logDir.mkdirs();
    }
    _logger.debug("Finish initializing directories......");
  }

}
