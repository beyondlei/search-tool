package edu.kit.aifb.ma.index.mangodb;

import java.io.File;
import java.io.IOException;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.util.PageIterator;
import edu.kit.aifb.ma.constant.DBConstants;
import edu.kit.aifb.ma.util.MongoResource;

/**
 * This class is responsible to build the collection named "EntityIndex_**" in MongoDB
 * 
 */
public class EntityMongoDBIndexer {

  private Wikipedia _wikipedia;

  private DBCollection _dbCollection;

  private long _allLinksCount = 0;

  public EntityMongoDBIndexer(String colName, String dbDir) throws Exception {
    _dbCollection = MongoResource.INSTANCE.getDB().getCollection(colName);
    _wikipedia = new Wikipedia(new File(dbDir), false);
    _allLinksCount = calAllLinksCount();
  }

  public static void main(String[] args) {
    double start = System.currentTimeMillis() / 1000 / 60;
    try {
      /*
       * EntityMongoDBIndexer indexer_en = new EntityMongoDBIndexer(DBConstants.ENTITY_COLLECTION_EN, "configs/wikipedia-template-en.xml");
       * EntityMongoDBIndexer indexer_de = new EntityMongoDBIndexer(DBConstants.ENTITY_COLLECTION_DE, "configs/wikipedia-template-de.xml");
       */
      EntityMongoDBIndexer indexer_zh = new EntityMongoDBIndexer(DBConstants.ENTITY_COLLECTION_ZH, "configs/wikipedia-template-zh.xml");
      indexer_zh.insertData();
    } catch (Exception e) {
      e.printStackTrace();
    }
    double end = System.currentTimeMillis() / 1000 / 60;
    System.out.println("The total time in min: " + (end - start));
  }

  public void insertData() throws IOException {
    PageIterator pageIterator = _wikipedia.getPageIterator();
    while (pageIterator.hasNext()) {
      Page page = pageIterator.next();
      if (page instanceof Article) {
        Article article = (Article) page;
        String title = article.getTitle();
        BasicDBObject dbObject =
                                 new BasicDBObject(DBConstants.ID, article.getId()).append(DBConstants.ENTITY, title)
                                     .append(DBConstants.GENERALITY, calGeneralityOfEntity(article)).append(DBConstants.DISTINCT_LINKS_IN_COUNT, article.getDistinctLinksInCount())
                                     .append(DBConstants.DISTINCT_LINKS_OUT_COUNT, article.getDistinctLinksOutCount())
                                     .append(DBConstants.TOTAL_LINKS_IN_COUNT, article.getTotalLinksInCount())
                                     .append(DBConstants.TOTAL_LINKS_OUT_COUNT, article.getTotalLinksOutCount());
        _dbCollection.insert(dbObject);
      }
    }
    pageIterator.close();
    _wikipedia.close();
  }

  /**
   * this method is used to calculate the generality of an entity, the approach used is from the paper
   * "Semantic Similarity Measurements for Multi-lingual Short Texts Using Wikipedia_Nakamura", i.e., the equation (6)
   */
  private double calGeneralityOfEntity(Article article) {

    return (1.0 * (article.getTotalLinksInCount() + article.getTotalLinksOutCount())) / (1.0 * _allLinksCount);
  }

  /**
   * en: 185364999
   * 
   * @return
   */
  private long calAllLinksCount() {
    PageIterator pi = _wikipedia.getPageIterator();
    long allLinksCount = 0;
    while (pi.hasNext()) {
      Page page = pi.next();
      if (page instanceof Article) {
        Article art = (Article) page;
        allLinksCount += (art.getTotalLinksInCount() + art.getTotalLinksOutCount());
      }
    }
    pi.close();
    return allLinksCount;
  }
}
