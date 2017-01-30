package edu.kit.aifb.ma.util;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Set;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

import edu.kit.aifb.ma.constant.DBConstants;
import edu.kit.aifb.ma.constant.LanguageConstants;

public class ProbabilityCalculator {

  private DBCollection _labelCollection;

  private DBCollection _entityCollection;

  private DBCollection _labelEntityCollection;

  private DBCollection _resourceRelatednessCollection;

  private String _lang;

  public ProbabilityCalculator(String lang) throws UnknownHostException {
    _lang = lang;
    if (LanguageConstants.EN.equals(lang)) {
      _labelCollection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.LABEL_COLLECTION_EN);
      _entityCollection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.ENTITY_COLLECTION_EN);
      _labelEntityCollection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.LABEL_ENTITY_COLLECTION_EN);
      _resourceRelatednessCollection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.RESOURCERELATEDNESS_COLLECTION_EN);
    } else if (LanguageConstants.DE.equals(lang)) {
      _labelCollection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.LABEL_COLLECTION_DE);
      _entityCollection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.ENTITY_COLLECTION_DE);
      _labelEntityCollection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.LABEL_ENTITY_COLLECTION_DE);
      _resourceRelatednessCollection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.RESOURCERELATEDNESS_COLLECTION_DE);
    } else if (LanguageConstants.ZH.equals(lang)) {
      _labelCollection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.LABEL_COLLECTION_ZH);
      _entityCollection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.ENTITY_COLLECTION_ZH);
      _labelEntityCollection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.LABEL_ENTITY_COLLECTION_ZH);
      _resourceRelatednessCollection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.RESOURCERELATEDNESS_COLLECTION_ZH);
    }
  }

  /**
   * the probability that an entity is related to the query, P(c|T)
   * 
   * @param entityName
   * @param labels
   * @return
   * @throws IOException
   */
  public Double calProbEntityRelatedToQuery(String entityName, Set<String> labels) throws IOException {
    double product = 1.0;
    double generality = getGenerality(entityName);
    for (String label : labels) {
      BasicDBObject dbObject = new BasicDBObject(DBConstants.LABEL, label);
      DBObject lMatch = _labelCollection.findOne(dbObject);
      double probability = lMatch == null ? 0.0 : (Double) lMatch.get(DBConstants.PROBABILITY);// P(tk in T)
      dbObject.append(DBConstants.ENTITY, entityName);
      DBObject leMatch = _labelEntityCollection.findOne(dbObject);
      double score = leMatch == null ? 0.0 : (Double) leMatch.get(DBConstants.ASSOCIATION_STRENGTH);// P(c|tk)
      product *= (probability * score + (1 - probability) * generality);
    }
    return MathUtil.round(product / Math.pow(generality, labels.size() - 1), 10);
  }

  /**
   * Calculate the semantic relatedness between two entities
   * 
   * @param oneEntity
   * @param theOtherEntity
   * @param searcher
   * @return
   * @throws IOException
   */
  public double calRelatedness(String oneEntity, String theOtherEntity) throws IOException {
    DBObject oneEntityObject = _entityCollection.findOne(new BasicDBObject(DBConstants.ENTITY, oneEntity));
    int oneID = oneEntityObject != null ? (Integer) oneEntityObject.get(DBConstants.ID) : -1;
    DBObject theOtherEntityObject = _entityCollection.findOne(new BasicDBObject(DBConstants.ENTITY, theOtherEntity));
    int theOtherID = theOtherEntityObject != null ? (Integer) theOtherEntityObject.get(DBConstants.ID) : -1;
    if (oneID != -1 && theOtherID != -1) {
      if (oneID > theOtherID) {
        if (LanguageConstants.EN.equals(_lang)) { // the data type is different in English version and German version
          DBObject match = _resourceRelatednessCollection.findOne(new BasicDBObject(DBConstants.SOURCE_ID, theOtherID).append(DBConstants.TARGET_ID, oneID));
          return match == null ? 0.0 : (Double) match.get(DBConstants.SCORE);
        } else {
          DBObject match = _resourceRelatednessCollection.findOne(new BasicDBObject(DBConstants.SOURCE_ID, theOtherID).append(DBConstants.TARGET_ID, oneID));
          return match == null ? 0.0 : (Double) match.get(DBConstants.SCORE);
        }
      } else {
        if (LanguageConstants.EN.equals(_lang)) {
          DBObject match = _resourceRelatednessCollection.findOne(new BasicDBObject(DBConstants.SOURCE_ID, oneID).append(DBConstants.TARGET_ID, theOtherID));
          return match == null ? 0.0 : (Double) match.get(DBConstants.SCORE);
        } else {
          DBObject match = _resourceRelatednessCollection.findOne(new BasicDBObject(DBConstants.SOURCE_ID, oneID).append(DBConstants.TARGET_ID, theOtherID));
          return match == null ? 0.0 : (Double) match.get(DBConstants.SCORE);
        }
      }
    } else {
      return 0.0;
    }
  }

  /**
   * The generality of entity, the more general an entity is, the more likely it is to appear in texts. P(c)
   * 
   * @param entityName
   * @return
   * @throws IOException
   */
  private double getGenerality(String entityName) throws IOException {
    DBObject match = _entityCollection.findOne(new BasicDBObject(DBConstants.ENTITY, entityName));
    return match == null ? 0.0 : (Double) match.get(DBConstants.GENERALITY);
  }

}
