package edu.kit.aifb.ma.search;

import java.net.UnknownHostException;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

import edu.kit.aifb.ma.constant.DBConstants;
import edu.kit.aifb.ma.constant.LanguageConstants;
import edu.kit.aifb.ma.util.MongoResource;

/**
 * This class helps to find the corresponding German and Chinese entity of an English entity or find the corresponding English entity of a German or
 * Chinese entity.
 * 
 */
public class LanguageLinker {

  private DBCollection _langlinksCollection;
  
  public LanguageLinker() throws UnknownHostException {
    _langlinksCollection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.LANGLINKS_COLLECTION_DEZHEN);
  }

  public String findEnEntity(String s_title, String s_lang) {
    DBObject match = _langlinksCollection.findOne(new BasicDBObject(DBConstants.SOURCE_TITLE, s_title).append(DBConstants.SOURCE_LANGUAGE, s_lang));
    return match != null ? (String) match.get(DBConstants.TARGET_TITLE) : null;
  }

  public String findDeEntityByEnEntity(String t_title) {
    DBObject match = _langlinksCollection.findOne(new BasicDBObject(DBConstants.TARGET_TITLE, t_title).append(DBConstants.SOURCE_LANGUAGE, LanguageConstants.DE));
    return match != null ? (String) match.get(DBConstants.SOURCE_TITLE) : null;
  }

  public String findZhEntityByEnEntity(String t_title) {
    DBObject match = _langlinksCollection.findOne(new BasicDBObject(DBConstants.TARGET_TITLE, t_title).append(DBConstants.SOURCE_LANGUAGE, LanguageConstants.ZH));
    return match != null ? (String) match.get(DBConstants.SOURCE_TITLE) : null;
  }

}
