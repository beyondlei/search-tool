package edu.kit.aifb.ma.index.mangodb;

import java.io.File;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

import edu.kit.aifb.gwifi.model.Label;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.util.LabelIterator;
import edu.kit.aifb.ma.constant.DBConstants;
import edu.kit.aifb.ma.util.MongoResource;

/**
 * This class is responsible to build the collection named "LabelIndex_**" in MongoDB
 * 
 */
public class LabelMongoDBIndexer {

  private DBCollection _dbCollection;

  private Wikipedia _wikipedia;

  public LabelMongoDBIndexer(String colName, String dbDir) throws Exception {
    _dbCollection = MongoResource.INSTANCE.getDB().getCollection(colName);
    _wikipedia = new Wikipedia(new File(dbDir), false);
  }

  public static void main(String[] args) {
    double start = System.currentTimeMillis() / 1000 / 60;
    try {
      /*
       * LabelMongoDBIndexer indexer_en = new LabelMongoDBIndexer(DBConstants.LABEL_COLLECTION_EN, "configs/wikipedia-template-en.xml");
       * LabelMongoDBIndexer indexer_de = new LabelMongoDBIndexer(DBConstants.LABEL_COLLECTION_DE, "configs/wikipedia-template-de.xml");
       */
      LabelMongoDBIndexer indexer_zh = new LabelMongoDBIndexer(DBConstants.LABEL_COLLECTION_ZH, "configs/wikipedia-template-zh.xml");
      indexer_zh.insertData();
    } catch (Exception e) {
      e.printStackTrace();
    }
    double end = System.currentTimeMillis() / 1000 / 60;
    System.out.println("The total time in min: " + (end - start));
  }

  public void insertData() throws Exception {
    LabelIterator labelIterator = _wikipedia.getLabelIterator(null);
    while (labelIterator.hasNext()) {
      Label label = labelIterator.next();
      long docCount = label.getDocCount();
      long linkDocCount = label.getLinkDocCount();
      BasicDBObject dbObject =
                               new BasicDBObject(DBConstants.LABEL, label.getText()).append(DBConstants.PROBABILITY, label.getLinkProbability())
                                   .append(DBConstants.DOC_COUNT, docCount).append(DBConstants.OCC_COUNT, label.getOccCount()).append(DBConstants.LINK_DOC_COUNT, linkDocCount)
                                   .append(DBConstants.LINK_OCC_COUNT, label.getLinkOccCount());
      _dbCollection.insert(dbObject);
    }
    labelIterator.close();
    _wikipedia.close();
  }
}
