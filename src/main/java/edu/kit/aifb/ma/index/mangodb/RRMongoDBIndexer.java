package edu.kit.aifb.ma.index.mangodb;

import java.io.IOException;
import java.net.UnknownHostException;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import edu.kit.aifb.ma.constant.DBConstants;
import edu.kit.aifb.ma.util.MongoResource;

/**
 * This class is responsible to build the collection named "ResourceRelatednessIndex_**" in MongoDB. The significant problem existed now is that the
 * relatedness between resources is not quite accuracy, it has to be improved to achieve a better performance of our system.
 * 
 */
public class RRMongoDBIndexer {

  private DBCollection _dbCollection;

  public RRMongoDBIndexer(String colName) throws UnknownHostException {
    _dbCollection = MongoResource.INSTANCE.getDB().getCollection(colName);
  }

  public static void main(String[] args) {
    try {
      RRMongoDBIndexer indexer = new RRMongoDBIndexer(DBConstants.RESOURCERELATEDNESS_COLLECTION_DE);
      indexer.insertData();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void insertData() throws IOException {
    DBCursor cursor = MongoResource.INSTANCE.getDB().getCollection(DBConstants.RESOURCERELATEDNESS_COLLECTION_DE).find();
    DBCollection entityCol = MongoResource.INSTANCE.getDB().getCollection(DBConstants.ENTITY_COLLECTION_DE);
    while (cursor.hasNext()) {
      DBObject next = cursor.next();
      int s_id = (Integer) next.get(DBConstants.SOURCE_ID);
      int t_id = (Integer) next.get(DBConstants.TARGET_ID);
      DBObject s = entityCol.findOne(new BasicDBObject(DBConstants.ID, s_id));
      DBObject t = entityCol.findOne(new BasicDBObject(DBConstants.ID, t_id));
      if (null != s && null != t) {
        String s_title = (String) s.get(DBConstants.ENTITY);
        String t_title = (String) t.get(DBConstants.ENTITY);
        double score = (Double) next.get(DBConstants.SCORE);
        BasicDBObject dbObject =
                                 new BasicDBObject(DBConstants.SOURCE_ID, s_id).append(DBConstants.SOURCE_TITLE, s_title).append(DBConstants.TARGET_ID, t_id)
                                     .append(DBConstants.TARGET_TITLE, t_title).append(DBConstants.SCORE, score);
        _dbCollection.insert(dbObject);
      }
    }
  }
}
