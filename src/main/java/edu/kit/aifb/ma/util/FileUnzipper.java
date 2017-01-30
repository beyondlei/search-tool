package edu.kit.aifb.ma.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;

public class FileUnzipper {
  private Logger _logger = Logger.getLogger(FileUnzipper.class);

  /**
   * Unzip the given .gz file and then delete it.
   * 
   * @param gzFileName
   * @return
   */
  public File gunzipFile(String gzFilePath, File gzFile) {
    String gzFileName = gzFile.getName();
    String xmlFileName = gzFileName.substring(0, gzFileName.lastIndexOf(".gz"));
    File xmlFile = new File(gzFilePath + File.separator + xmlFileName);
    try {
      if (!xmlFile.exists()) {
        xmlFile.createNewFile();
      } else {
        xmlFile.delete();
        xmlFile.createNewFile();
      }
      GZIPInputStream gzipInputStream = new GZIPInputStream(new FileInputStream(gzFile));
      FileOutputStream fos = new FileOutputStream(xmlFile);
      byte[] buffer = new byte[1024];
      int len;
      _logger.debug("Starting unzip file " + gzFileName + "......");
      while ((len = gzipInputStream.read(buffer)) != -1) {
        fos.write(buffer, 0, len);
      }
      _logger.debug("Finished unzip file " + gzFileName + "......");
      fos.close();
      gzipInputStream.close();
      if (!gzFile.delete()) {
        _logger.warn("The file " + gzFileName + " can not be deleted!!!");
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return xmlFile;
  }
}