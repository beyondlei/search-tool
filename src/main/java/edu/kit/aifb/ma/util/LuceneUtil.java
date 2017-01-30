package edu.kit.aifb.ma.util;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Version;

public class LuceneUtil {
  
  public static IndexWriter getIndexWriter(boolean create, String path) throws IOException {
    FSDirectory fsDir = FSDirectory.open(new File(path));
    IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_48, new StandardAnalyzer(Version.LUCENE_48));
    if (create) {
      // Create a new index in the directory, removing any
      // previously indexed documents:
      iwc.setOpenMode(OpenMode.CREATE);
    } else {
      // Add new documents to an existing index:
      iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
    }
    return new IndexWriter(fsDir, iwc);
  }

  public static IndexReader getIndexReader(String path) throws IOException {
    Directory fsDir = new SimpleFSDirectory(new File(path));
    return DirectoryReader.open(fsDir);
  }

  public static IndexSearcher getIndexSearcher(IndexReader reader) throws IOException {
    return new IndexSearcher(reader);
  }
}
