package edu.kit.aifb.ma.index.mangodb;

import java.io.IOException;
import java.net.UnknownHostException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

import edu.kit.aifb.ma.constant.DBConstants;
import edu.kit.aifb.ma.util.LuceneUtil;
import edu.kit.aifb.ma.util.MongoResource;
import edu.kit.aifb.ma.util.Property;

/**
 * This class is responsible to build the collection named "LanglinksIndex_DE_ZH_EN" in MongoDB. This language links index could be improved in the
 * future by adding the de <-> zh links into this collection, since right now if you want to find a corresponding Chinese entity for a given German
 * entity or vice versa, basically you need to use the English entity as the bridge, in other words, two times retrieval are required
 * 
 */
public class LanglinksMongoDBIndexer {

  private DBCollection _langlinksCollection;

  private DBCollection _entityCollectionEN;

  private DBCollection _entityCollectionDE;

  private DB _db;

  public LanglinksMongoDBIndexer(int port, String dbName, String colName) throws UnknownHostException {
    _db = MongoResource.INSTANCE.mongoClient.getDB(dbName);
    _langlinksCollection = _db.getCollection(colName);
    _entityCollectionEN = _db.getCollection(DBConstants.ENTITY_COLLECTION_EN);
    _entityCollectionDE = _db.getCollection(DBConstants.ENTITY_COLLECTION_DE);
  }

  public static void main(String[] args) {
    try {
      LanglinksMongoDBIndexer indexer = new LanglinksMongoDBIndexer(Integer.parseInt(Property.getValue("mongodb_port")), Property.getValue("mongodb_name"), DBConstants.LANGLINKS_COLLECTION_DEZHEN);
      indexer.insertData();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void insertData() throws IOException {
    IndexReader reader = LuceneUtil.getIndexReader("/Volumes/My Passport/Data for MA/langlinks/index");
    for (int i = 0; i < reader.maxDoc(); i++) {
      Document doc = reader.document(i);
      String s_lang = doc.get(DBConstants.SOURCE_LANGUAGE);
      String t_lang = doc.get(DBConstants.TARGET_LANGUAGE);
      int s_id = Integer.parseInt(doc.get(DBConstants.SOURCE_ID));
      //      if (s_lang.equals("de") && t_lang.equals("en")) {
      //        DBObject findOne = _entityCollectionDE.findOne(new BasicDBObject(DBConstants.ID, s_id));
      //        if (null != findOne) {
      //          BasicDBObject dbObject =
      //                                   new BasicDBObject(DBConstants.SOURCE_ID, s_id).append(DBConstants.SOURCE_TITLE, findOne.get(DBConstants.ENTITY))
      //                                       .append(DBConstants.SOURCE_LANGUAGE, "de").append(DBConstants.TARGET_TITLE, doc.get(DBConstants.TARGET_TITLE));
      //          _langlinksCollection.insert(dbObject);
      //        }
      //      }
      //      if (s_lang.equals("zh") && t_lang.equals("en")) {
      //        DBObject findOne = _entityCollectionZH.findOne(new BasicDBObject(DBConstants.ID, s_id));
      //        if (null != findOne) {
      //          BasicDBObject dbObject =
      //                                   new BasicDBObject(DBConstants.SOURCE_ID, s_id).append(DBConstants.SOURCE_TITLE, findOne.get(DBConstants.ENTITY))
      //                                       .append(DBConstants.SOURCE_LANGUAGE, "zh").append(DBConstants.TARGET_TITLE, doc.get(DBConstants.TARGET_TITLE));
      //          _langlinksCollection.insert(dbObject);
      //        }
      //      }
      if (s_lang.equals("zh") && t_lang.equals("de")) {
        DBObject findOne = _entityCollectionEN.findOne(new BasicDBObject(DBConstants.ID, s_id));
        if (null != findOne) {
          BasicDBObject dbObject = new BasicDBObject(DBConstants.SOURCE_TITLE, doc.get(DBConstants.TARGET_TITLE)).append(DBConstants.TARGET_TITLE, findOne.get(DBConstants.ENTITY));
          _langlinksCollection.insert(dbObject);
        }
      }
      if (s_lang.equals("de") && t_lang.equals("zh")) {
        DBObject findOne = _entityCollectionDE.findOne(new BasicDBObject(DBConstants.ID, s_id));
        if (null != findOne) {
          BasicDBObject dbObject = new BasicDBObject(DBConstants.SOURCE_TITLE, findOne.get(DBConstants.ENTITY)).append(DBConstants.TARGET_TITLE, doc.get(DBConstants.TARGET_TITLE));
          _langlinksCollection.insert(dbObject);
        }
      }
    }
    System.out.println(reader.maxDoc());
  }
}
