package edu.kit.aifb.ma.util;

import java.io.File;

import org.apache.log4j.Logger;

public class FileFilter {
  private Logger _logger = Logger.getLogger(FileFilter.class);

  private String _path;

  private String _ext;

  private String[] _files;

  public FileFilter(String path, String ext) {
    _path = path;
    _ext = ext;
  }

  public String[] filter() {
    File dir = new File(_path);
    if (dir.isDirectory()) {
      _files = dir.list(new FileExtensionFilter(_ext));
    } else {
      _logger.error("Directory " + Property.getValue("news_path") + " is not existed!!!");
    }
    return _files;
  }
}
