package edu.kit.aifb.ma.index.mangodb;

import edu.kit.aifb.ma.constant.CharacterConstants;
import edu.kit.aifb.ma.constant.DBConstants;
import edu.kit.aifb.ma.constant.LanguageConstants;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.transform.dom.DOMSource;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

import edu.kit.aifb.gwifi.service.NGramAnnotationService;
import edu.kit.aifb.gwifi.service.NLPAnnotationService;
import edu.kit.aifb.ma.newsfeed.NFArticle;
import edu.kit.aifb.ma.newsfeed.NFArticleAnnotator;
import edu.kit.aifb.ma.search.LanguageLinker;
import edu.kit.aifb.ma.util.MongoResource;
import edu.kit.aifb.ma.util.Property;

/**
 * This class is responsible to build the collection named "NFArticleIndex" in MongoDB
 * 
 */

public class NFArticleMongoDBIndexer {

  private static final String MODE = "NGRAM";

  private DBCollection _nfarticleCol;

  private DBCollection _entityNFArticleCol;

  private LanguageLinker langlinker;

  private NFArticleAnnotator _annotator;
  
  private HashMap<String,NGramAnnotationService> lang2service;

  public NFArticleMongoDBIndexer() throws Exception {
    _nfarticleCol = MongoResource.INSTANCE.getDB().getCollection(DBConstants.NFARTICLE_COLLECTION);
    _entityNFArticleCol = MongoResource.INSTANCE.getDB().getCollection(DBConstants.ENTITY_NFARTICLE_INDEX);
    langlinker = new LanguageLinker();
    lang2service = new HashMap<String, NGramAnnotationService>();
    String anno_config = Property.getValue("annotation_configs");
    NGramAnnotationService annotationService =
            new NGramAnnotationService(anno_config + "configs/hub-template.xml", anno_config + "configs/wikipedia-template-en.xml", LanguageConstants.EN,
                LanguageConstants.EN);
    lang2service.put(LanguageConstants.ENG, annotationService);
    
    annotationService =
            new NGramAnnotationService(anno_config + "configs/hub-template.xml", anno_config + "configs/wikipedia-template-de.xml", LanguageConstants.DE,
                LanguageConstants.DE);
    lang2service.put(LanguageConstants.DEU, annotationService);
    
    annotationService =
            new NGramAnnotationService(anno_config + "configs/hub-template.xml", anno_config + "configs/wikipedia-template-zh.xml", LanguageConstants.ZH,
                LanguageConstants.ZH);
    lang2service.put(LanguageConstants.ZHO, annotationService);
  }

  public void insertData(Map<String, NFArticle> articles) throws Exception {
    Iterator<String> iterator = articles.keySet().iterator();
    while (iterator.hasNext()) {
//      NLPAnnotationService annotationService = null;
    	NGramAnnotationService annotationService = null;
      String articleID = iterator.next();
      NFArticle article = articles.get(articleID);
//      String anno_config = Property.getValue("annotation_configs");
//      if (LanguageConstants.ENG.equals(article.get_lang())) {
//        annotationService =
//                            new NLPAnnotationService(anno_config + "configs/hub-template.xml", anno_config + "configs/wikipedia-template-en.xml", anno_config + "configs/NLPConfig.properties", LanguageConstants.EN,
//                                LanguageConstants.EN, MODE);
//      } else if (LanguageConstants.DEU.equals(article.get_lang())) {
//        annotationService =
//                            new NLPAnnotationService(anno_config + "configs/hub-template.xml", anno_config + "configs/wikipedia-template-de.xml", anno_config + "configs/NLPConfig.properties", LanguageConstants.DE,
//                                LanguageConstants.DE, MODE);
//      } else if (LanguageConstants.ZHO.equals(article.get_lang())) {
//        annotationService =
//                            new NLPAnnotationService(anno_config + "configs/hub-template.xml", anno_config + "configs/wikipedia-template-zh.xml", anno_config + "configs/NLPConfig.properties", LanguageConstants.ZH,
//                                LanguageConstants.ZH, MODE);
//      }
      if (LanguageConstants.ENG.equals(article.get_lang())) {
          	annotationService = lang2service.get(LanguageConstants.ENG);
        } else if (LanguageConstants.DEU.equals(article.get_lang())) {
        	annotationService = lang2service.get(LanguageConstants.DEU);
        } else if (LanguageConstants.ZHO.equals(article.get_lang())) {
        	annotationService = lang2service.get(LanguageConstants.ZHO);
        }
      if (annotationService != null) {
        String bodycleartext = article.get_bodyClearText();
        int wordNum = bodycleartext.split(" ").length;
        // TODO Jiang: sometimes the body clear text can not be annotated, in this situation we choose to discard the article with no annotations, it should be fixed in the future
        DOMSource domSource = annotationService.getDOMSource(bodycleartext, null, null);
        if (domSource != null) { 
          // if the unimportant annotations are not considered, add the removeUnimportantEntities()
          String originalEV = CharacterConstants.EMPTY_STRING;
          String englishEV = CharacterConstants.EMPTY_STRING;
          String annotationNum = CharacterConstants.EMPTY_STRING;
          _annotator = new NFArticleAnnotator(domSource);
          String annotatedText = _annotator.retrieveAnnotatedText();
          Map<String, Integer> annotationWithNum = _annotator.retrieveAnnotationWithNum(annotatedText);
          Map<String, Double> annotationWithWeight = _annotator.retrieveAnnotationWithWeight(annotatedText);
          if (!annotationWithNum.isEmpty()) {
            int totalAnnotationNum = 0;
            for (int num : annotationWithNum.values()) {
              totalAnnotationNum += num;
            }
            String title = article.get_title() != null ? article.get_title() : CharacterConstants.EMPTY_STRING;
            String uri = article.get_uri() != null ? article.get_uri() : CharacterConstants.EMPTY_STRING;
            String lang = article.get_lang() != null ? article.get_lang().substring(0, 2) : CharacterConstants.EMPTY_STRING;
            String img = article.get_img() != null ? article.get_img() : CharacterConstants.EMPTY_STRING;
            String longitude = article.get_longitude() != null ? article.get_longitude() : CharacterConstants.EMPTY_STRING;
            String latitude = article.get_latitude() != null ? article.get_latitude() : CharacterConstants.EMPTY_STRING;
            String country = article.get_country() != null ? article.get_country() : CharacterConstants.EMPTY_STRING;
            String retrievedDate = article.get_retrievedDate() != null ? article.get_retrievedDate() : CharacterConstants.EMPTY_STRING;
            if (!"en".equals(lang)) {
              for (String annotation : annotationWithWeight.keySet()) {
                String englishEntity = langlinker.findEnEntity(annotation, lang);
                _entityNFArticleCol.insert(new BasicDBObject(DBConstants.ENTITY, annotation).append(DBConstants.ARTICLE_ID, articleID).append(DBConstants.LANG, lang));
                if (null != englishEntity) {
                  _entityNFArticleCol.insert(new BasicDBObject(DBConstants.ENTITY, englishEntity).append(DBConstants.ARTICLE_ID, articleID).append(DBConstants.LANG, lang));
                }
                _entityNFArticleCol.createIndex(new BasicDBObject(DBConstants.ENTITY, 1).append(DBConstants.LANG, 1));
                _entityNFArticleCol.createIndex(new BasicDBObject(DBConstants.ENTITY, 1));
                annotationNum += (annotation + CharacterConstants.ANNOTATION_SCORE_SEPARATOR + annotationWithNum.get(annotation) + CharacterConstants.SCORE_SEPARATOR);
                originalEV += (annotation + CharacterConstants.ANNOTATION_SCORE_SEPARATOR + annotationWithWeight.get(annotation) + CharacterConstants.SCORE_SEPARATOR);
                if (null != englishEntity) {
                  englishEV += (englishEntity + CharacterConstants.ANNOTATION_SCORE_SEPARATOR + annotationWithWeight.get(annotation) + CharacterConstants.SCORE_SEPARATOR);
                } else {
                  englishEV += (annotation + CharacterConstants.ANNOTATION_SCORE_SEPARATOR + annotationWithWeight.get(annotation) + CharacterConstants.SCORE_SEPARATOR);
                }
              }
            } else {
              for (String annotation : annotationWithWeight.keySet()) {
                _entityNFArticleCol.insert(new BasicDBObject(DBConstants.ENTITY, annotation).append(DBConstants.ARTICLE_ID, articleID).append(DBConstants.LANG, lang));
                _entityNFArticleCol.createIndex(new BasicDBObject(DBConstants.ENTITY, 1).append(DBConstants.LANG, 1));
                _entityNFArticleCol.createIndex(new BasicDBObject(DBConstants.ENTITY, 1));
                annotationNum += (annotation + CharacterConstants.ANNOTATION_SCORE_SEPARATOR + annotationWithNum.get(annotation) + CharacterConstants.SCORE_SEPARATOR);
                originalEV += (annotation + CharacterConstants.ANNOTATION_SCORE_SEPARATOR + annotationWithWeight.get(annotation) + CharacterConstants.SCORE_SEPARATOR);
              }
              englishEV = originalEV;
            }
            annotationNum = CharacterConstants.SCORE_SEPARATOR + annotationNum;
            originalEV = CharacterConstants.SCORE_SEPARATOR + originalEV;
            englishEV = CharacterConstants.SCORE_SEPARATOR + englishEV;
            BasicDBObject dbObject =
                                     new BasicDBObject(DBConstants.ARTICLE_ID, articleID).append(DBConstants.ORIGINAL_ENTITY_VECTOR, originalEV)
                                         .append(DBConstants.ENGLISH_ENTITY_VECTOR, englishEV).append(DBConstants.ANNOTATION_WITH_NUM, annotationNum)
                                         .append(DBConstants.WORD_NUMBER, wordNum).append(DBConstants.NUMBER_OF_ANNOTATION, totalAnnotationNum)
                                         .append(DBConstants.ANNOTATION_PERCENTAGE, (1.0 * totalAnnotationNum) / (1.0 * wordNum)).append(DBConstants.URI, uri)
                                         .append(DBConstants.TITLE, title).append(DBConstants.LONGITUDE, longitude).append(DBConstants.LATITUDE, latitude)
                                         .append(DBConstants.COUNTRY, country).append(DBConstants.RETRIEVED_DATE, retrievedDate).append(DBConstants.LANG, lang)
                                         .append(DBConstants.IMG, img).append(DBConstants.ANNOTATED_TEXT, annotatedText);

            _nfarticleCol.insert(dbObject);
            _nfarticleCol.createIndex(new BasicDBObject(DBConstants.ARTICLE_ID, 1));
          }
        }
      }
    }
  }
}
