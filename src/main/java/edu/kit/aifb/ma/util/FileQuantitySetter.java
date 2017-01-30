package edu.kit.aifb.ma.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Set the maximum number of news files stored in the folder.
 * 
 */
public class FileQuantitySetter {

  public static void setMaximum(int maximum) {
    File newsDir = new File(Property.getValue("news_path"));
    File[] xmlFiles = newsDir.listFiles(new FileExtensionFilter("xml"));
    List<File> xmlFileList = new ArrayList<File>(Arrays.asList(xmlFiles));
    List<Long> lastModifieds = new ArrayList<Long>();

    if (newsDir.isDirectory() && xmlFiles.length > maximum - 1) {
      for (File xmlFile : xmlFileList) {
        lastModifieds.add(xmlFile.lastModified());
      }
      Collections.sort(lastModifieds);
      for (File xmlFile : xmlFileList) {
        if (xmlFile.lastModified() == lastModifieds.get(0)) {
          xmlFile.delete();
        }
      }
    }
  }
}
