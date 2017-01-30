package edu.kit.aifb.ma.index.mangodb;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

import edu.kit.aifb.ma.constant.DBConstants;
import edu.kit.aifb.ma.constant.LanguageConstants;
import edu.kit.aifb.ma.util.MongoResource;

/**
 * This class is responsible to build the collections which store the informations that parsed from the DBpedia datasets. Basically, this class could
 * be optimized in the future from the following aspects: 1. remove the duplicated indexes 2. check the integrity of the index, namely whether all
 * data sets are indexed or not? 3. integrate them into just one collection, if it is necessary and possible
 * 
 */

public class RDFMongoDBIndexer {
  public static void main(String[] args) {
    RDFMongoDBIndexer indexer = new RDFMongoDBIndexer();
    indexer.buildNeighborsIndex();
  }

  @SuppressWarnings("unused")
  /**
   * Build the collection "ShortAbstractsIndex"
   */
  private void buildShortAbstractsIndex() {
    BufferedReader bufferedReader = null;
    try {
      long start = System.currentTimeMillis() / 1000;
      String currentLine;
      bufferedReader = new BufferedReader(new FileReader("/Volumes/My Passport/Data for MA/ttl/en/short_abstracts_en.ttl"));
      while ((currentLine = bufferedReader.readLine()) != null) {
        String[] so = currentLine.split("<http://www.w3.org/2000/01/rdf-schema#comment>");
        String subject = so[0].substring(29, so[0].lastIndexOf(">")).replaceAll("_", " ");
        String object = so[1].substring(so[1].indexOf("\"") + 1, so[1].lastIndexOf("\""));
        DBCollection collection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.SHORT_ABSTRACTS_INDEX);
        BasicDBObject dbObject = new BasicDBObject(DBConstants.ENTITY, subject).append(DBConstants.LANG, LanguageConstants.EN).append(DBConstants.SA, object);
        collection.insert(dbObject);
      }
      long end = System.currentTimeMillis() / 1000;
      System.out.println("Total time in sec.: " + (end - start) + " s.");
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        if (null != bufferedReader) {
          bufferedReader.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  @SuppressWarnings("unused")
  /**
   * Build the collection "ImageIndex"
   */
  private void buildImageIndex() {
    BufferedReader bufferedReader = null;
    try {
      long start = System.currentTimeMillis() / 1000;
      String currentLine;
      bufferedReader = new BufferedReader(new FileReader("/Volumes/My Passport/Data for MA/ttl/en/images_en.ttl"));
      while ((currentLine = bufferedReader.readLine()) != null) {
        if (currentLine.startsWith("<http://dbpedia.org/resource/")) {
          String[] spo = currentLine.split(" ");
          String subject = spo[0].substring(29, spo[0].lastIndexOf(">")).replaceAll("_", " ");
          String predicate = spo[1].substring(spo[1].lastIndexOf("/") + 1, spo[1].lastIndexOf(">"));
          String object = spo[2].substring(1, spo[2].lastIndexOf(">"));
          DBCollection collection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.IMAGE_INDEX);
          BasicDBObject dbObject =
                                   new BasicDBObject(DBConstants.ENTITY, subject).append(DBConstants.TYPE, predicate).append(DBConstants.LANG, "en")
                                       .append(DBConstants.IMAGE_URI, object);
          collection.insert(dbObject);
          collection.createIndex(new BasicDBObject(DBConstants.ENTITY, 1).append(DBConstants.LANG, 1).append(DBConstants.TYPE, 1));
        }
      }
      long end = System.currentTimeMillis() / 1000;
      System.out.println("Total time: " + (end - start) + " s.");
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        if (null != bufferedReader) {
          bufferedReader.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Build the collection "NeighborsIndex"
   */
  private void buildNeighborsIndex() {
    BufferedReader bufferedReader = null;
    try {
      long start = System.currentTimeMillis() / 1000;
      String currentLine;
      bufferedReader = new BufferedReader(new FileReader("/Users/zhongwenjiang/Downloads/persondata_de.ttl"));
      DBCollection neighborCol = MongoResource.INSTANCE.getDB().getCollection(DBConstants.NEIGHBORS_INDEX);
      DBCollection infoboxCol = MongoResource.INSTANCE.getDB().getCollection(DBConstants.INFOBOX_INDEX);
      int i = 0;
      while ((currentLine = bufferedReader.readLine()) != null) {
        System.out.println(i++);
        if (currentLine.startsWith("<http://de.dbpedia.org/resource/")) {
          String[] spo = currentLine.split("> <");
          if (spo.length == 3) {
            String subject = spo[0].substring(32).replaceAll("_", " ");
            String predicate = spo[1].substring(spo[1].lastIndexOf("/") + 1).replaceAll("_", " ");
            if (spo[2].startsWith("http://de.dbpedia.org/resource/")) {
              String object = spo[2].substring(spo[2].lastIndexOf("/") + 1, spo[2].lastIndexOf(">")).replaceAll("_", " ");
              BasicDBObject dbObject =
                                       new BasicDBObject(DBConstants.SUBJECT, subject).append(DBConstants.PREDICATE, predicate).append(DBConstants.LANG, "de")
                                           .append(DBConstants.OBJECT, object);
              neighborCol.insert(dbObject);
              neighborCol.createIndex(new BasicDBObject(DBConstants.SUBJECT, 1).append(DBConstants.LANG, 1));
            } else {
              String object = spo[2].substring(spo[2].lastIndexOf("/") + 1, spo[2].lastIndexOf(">"));
              BasicDBObject dbObject =
                                       new BasicDBObject(DBConstants.SUBJECT, subject).append(DBConstants.PREDICATE, predicate).append(DBConstants.LANG, "de")
                                           .append(DBConstants.OBJECT, object);
              infoboxCol.insert(dbObject);
              infoboxCol.createIndex(new BasicDBObject(DBConstants.SUBJECT, 1).append(DBConstants.LANG, 1));
            }
          } else if (spo.length == 2) {
            String subject = spo[0].substring(32).replaceAll("_", " ");
            String[] po = spo[1].split("> \"");
            String predicate = po[0].substring(po[0].lastIndexOf("/") + 1).replaceAll("_", " ");
            String object = po[1].substring(0, po[1].indexOf("\""));
            BasicDBObject dbObject =
                                     new BasicDBObject(DBConstants.SUBJECT, subject).append(DBConstants.PREDICATE, predicate).append(DBConstants.LANG, "de")
                                         .append(DBConstants.OBJECT, object);
            infoboxCol.insert(dbObject);
            infoboxCol.createIndex(new BasicDBObject(DBConstants.SUBJECT, 1).append(DBConstants.LANG, 1));
          }

        }
      }
      long end = System.currentTimeMillis() / 1000;
      System.out.println("Total time: " + (end - start) + " s.");
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        if (null != bufferedReader) {
          bufferedReader.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
