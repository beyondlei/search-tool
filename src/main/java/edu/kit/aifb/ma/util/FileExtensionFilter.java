package edu.kit.aifb.ma.util;

import java.io.File;
import java.io.FilenameFilter;

public class FileExtensionFilter implements FilenameFilter {

  private String extension;

  public FileExtensionFilter(String extension) {
    this.extension = extension;
  }

  public boolean accept(File directory, String filename) {

    return filename.endsWith("."+extension);
  }
}
