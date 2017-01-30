package edu.kit.aifb.ma.index.mangodb;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

import edu.kit.aifb.gwifi.model.Article;
import edu.kit.aifb.gwifi.model.Page;
import edu.kit.aifb.gwifi.model.Wikipedia;
import edu.kit.aifb.gwifi.model.Article.Label;
import edu.kit.aifb.gwifi.model.Label.Sense;
import edu.kit.aifb.gwifi.util.PageIterator;
import edu.kit.aifb.ma.constant.DBConstants;
import edu.kit.aifb.ma.util.MathUtil;
import edu.kit.aifb.ma.util.MongoResource;

/**
 * This class is responsible to build the collection named "LabelEntityIndex_**" in MongoDB
 * 
 */
public class LabelEntityMongoDBIndexer {

  private Wikipedia _wikipedia;

  private DBCollection _dbCollection;

  public LabelEntityMongoDBIndexer(String colName, String dbDir) throws Exception {
    _dbCollection = MongoResource.INSTANCE.getDB().getCollection(colName);
    _wikipedia = new Wikipedia(new File(dbDir), false);
  }

  public static void main(String[] args) {
    double start = System.currentTimeMillis() / 1000 / 60;
    try {
      /*
       * LabelEntityMongoDBIndexer indexer_en = new LabelEntityMongoDBIndexer(DBConstants.LABEL_ENTITY_COLLECTION_EN,
       * "configs/wikipedia-template-en.xml"); LabelEntityMongoDBIndexer indexer_de = new
       * LabelEntityMongoDBIndexer(DBConstants.LABEL_ENTITY_COLLECTION_DE, "configs/wikipedia-template-de.xml");
       */
      LabelEntityMongoDBIndexer indexer_zh = new LabelEntityMongoDBIndexer(DBConstants.LABEL_ENTITY_COLLECTION_ZH, "configs/wikipedia-template-zh.xml");
      indexer_zh.insertData();
    } catch (Exception e) {
      e.printStackTrace();
    }
    double end = System.currentTimeMillis() / 1000 / 60;
    System.out.println("The total time is: " + (end - start) + " min.");
  }

  public void insertData() throws IOException {
    PageIterator pageIterator = _wikipedia.getPageIterator();
    while (pageIterator.hasNext()) {
      Page page = pageIterator.next();
      if (page instanceof Article) {
        Article article = (Article) page;
        int articleID = article.getId();
        String title = article.getTitle();
        Label[] labels = article.getLabels();
        for (Label label : labels) {
          double associationStrength = 0.0;
          // association strength between label and entity
          // double associationStrength = calProbEntityRelatedToLabel(article, label); 
          long totalLinkOccCount = 0;
          long linkOccCount = label.getLinkOccCount();
          Sense[] senses = _wikipedia.getLabel(label.getText()).getSenses();
          for (Sense sense : senses) {
            totalLinkOccCount += sense.getLinkOccCount();
          }
          if (totalLinkOccCount != 0) {
            associationStrength = (1.0 * linkOccCount) / (1.0 * totalLinkOccCount);
          }
          // edu.kit.aifb.gwifi.model.Label genericLabel = _wikipedia.getLabel(label.getText());
          // double weight = genericLabel.getLinkProbability();
          // double probability = (1.0 * genericLabel.getLinkOccCount()) / (1.0 * (genericLabel.getLinkOccCount() + genericLabel.getLinkDocCount()));
          BasicDBObject dbObject =
                                   new BasicDBObject(DBConstants.ID, articleID).append(DBConstants.LABEL, label.getText()).append(DBConstants.ENTITY, title)
                                       .append(DBConstants.ASSOCIATION_STRENGTH, associationStrength);
          _dbCollection.insert(dbObject);
        }
      }
    }
    pageIterator.close();
    _wikipedia.close();
  }

  /**
   * the probability that an entity is related to a label, the approach here used is from the paper
   * "Semantic Similarity Measurements for Multi-lingual Short Texts Using Wikipedia_Nakamura", i.e., the equation (5)
   * 
   * @param article
   * @param label
   * @return
   */
  @SuppressWarnings("unused")
  private double calProbEntityRelatedToLabel(Article article, Label label) {
    double prob = 0.0;
    Sense[] senses = _wikipedia.getLabel(label.getText()).getSenses();
    for (Sense sense : senses) {
      if (sense instanceof Article) {
        Article art = (Article) sense;
        prob += calProbEntityRelatedToEntity(article, art) * calProbLabelLinkedToEntity(art, label);
      }
    }
    return MathUtil.round(prob, 10);
  }

  /**
   * The number of links between two entities
   * 
   * @param art1 the first entity
   * @param art2 the second entity
   * @return
   */
  private int getNumOfLinks(Article art1, Article art2) {
    int numOfLinks = 0;
    for (Article art : art1.getLinksIn()) {
      if (art.getTitle().equals(art2.getTitle())) {
        numOfLinks++;
      }
    }
    for (Article art : art1.getLinksOut()) {
      if (art.getTitle().equals(art2.getTitle())) {
        numOfLinks++;
      }
    }
    return numOfLinks;
  }

  /**
   * the probability that one entity is related to the other entity, the approach used is from the paper
   * "Semantic Similarity Measurements for Multi-lingual Short Texts Using Wikipedia_Nakamura", i.e., equation (4)
   * 
   * @param oneArticle one entity
   * @param theOtherArticle the other entity
   * @return
   */
  private double calProbEntityRelatedToEntity(Article oneArticle, Article theOtherArticle) {
    return (1.0 * getNumOfLinks(oneArticle, theOtherArticle)) / (1.0 * (oneArticle.getTotalLinksInCount() + oneArticle.getTotalLinksOutCount()));
  }

  /**
   * the probability that a label is linked to an entity, the approach here used is from the paper
   * "Semantic Similarity Measurements for Multi-lingual Short Texts Using Wikipedia_Nakamura", i.e., the equation (3)
   * 
   * @param article
   * @param label
   * @return
   */
  private double calProbLabelLinkedToEntity(Article article, Label label) {
    double prob = 0.0;
    Label[] labels = article.getLabels();
    if (Arrays.asList(labels).contains(labels)) {
      long totalLinkOccCount = 0;
      Sense[] senses = _wikipedia.getLabel(label.getText()).getSenses();
      for (Sense sense : senses) {
        totalLinkOccCount += sense.getLinkOccCount();
      }
      if (totalLinkOccCount != 0) {
        prob = (1.0 * label.getLinkOccCount()) / (1.0 * totalLinkOccCount);
      }
    }
    return prob;
  }
}
