package edu.kit.aifb.ma.search;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.openrdf.repository.RepositoryException;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import edu.kit.aifb.ma.constant.CharacterConstants;
import edu.kit.aifb.ma.constant.DBConstants;
import edu.kit.aifb.ma.constant.LanguageConstants;
import edu.kit.aifb.ma.util.DBPediaDSQuerier;
import edu.kit.aifb.ma.util.MathUtil;
import edu.kit.aifb.ma.util.MongoResource;

public class DocumentsRanker {
  private static final Logger _logger = Logger.getLogger(DocumentsRanker.class);

  private DBCollection _nfarticleCol;

  private DBCollection _entityNFArticleCol;

  private LanguageLinker _langlinker;

  private DBPediaDSQuerier _querier;

  public static void main(String[] args) {
    String str = "Barack Obama^0.915702029|United States^0.876321";
    try {
      long start = System.currentTimeMillis();
      DocumentsRanker ranker = new DocumentsRanker();
      ranker.getDocumentsByEntities(str, true, LanguageConstants.EN, LanguageConstants.DE);
      long end = System.currentTimeMillis();
      System.out.println("Total time: " + (end - start) + " ms.");
    } catch (IOException e) {
      e.printStackTrace();
    } catch (RepositoryException e) {
      e.printStackTrace();
    }
  }

  public DocumentsRanker() throws UnknownHostException {
    _nfarticleCol = MongoResource.INSTANCE.getDB().getCollection(DBConstants.NFARTICLE_COLLECTION);
    _entityNFArticleCol = MongoResource.INSTANCE.getDB().getCollection(DBConstants.ENTITY_NFARTICLE_INDEX);
    _langlinker = new LanguageLinker();
    _querier = new DBPediaDSQuerier();
  }

  public JSONObject getDocumentsByEntities(String entitiesWithProb, boolean orMode, String kb_lang, String t_lang) throws IOException, RepositoryException {
    JSONObject json = new JSONObject();
    Map<String, Double> queryEntityVector = new HashMap<>();
    Map<String, DBObject> idWithDoc = new HashMap<>();
    String[] entityWithProb = entitiesWithProb.substring(0, entitiesWithProb.length() - 1).split("\\|");

    for (int i = 0; i < entityWithProb.length; i++) {
      String[] entity_Prob = entityWithProb[i].split("\\^");
      queryEntityVector.put(entity_Prob[0], Double.parseDouble(entity_Prob[1]));
    }

    TreeMap<Double, Set<String>> ssWithID = new TreeMap<>();
    if (kb_lang.equals(t_lang)) { // use local language to search
      Map<String, Double> extendedQueryEntityVector = extendsToLocalEntities(queryEntityVector, kb_lang);
      if (orMode) {
        idWithDoc = findOrDocs(queryEntityVector, t_lang);
      } else {
        idWithDoc = findAndDocs(queryEntityVector, t_lang);
      }
      for (String id : idWithDoc.keySet()) {
        DBObject doc = idWithDoc.get(id);
        String originalDEV = (String) doc.get(DBConstants.ORIGINAL_ENTITY_VECTOR);
        String annotationWithNum = (String) doc.get(DBConstants.ANNOTATION_WITH_NUM);
        int numOfAnnotation = (Integer) doc.get(DBConstants.NUMBER_OF_ANNOTATION);
        if (originalDEV.length() > 0) {
          Map<String, Double> docEntityVector = new HashMap<>();
          Map<String, Integer> entityNumVector = new HashMap<>();
          String[] originalDEVArr = originalDEV.substring(1, originalDEV.length() - 1).split("\\|");
          String[] annotationWithNumArr = annotationWithNum.substring(1, annotationWithNum.length() - 1).split("\\|");
          for (int i = 0; i < originalDEVArr.length; i++) {
            String[] entity_weight = originalDEVArr[i].split("\\^");
            String[] entity_num = annotationWithNumArr[i].split("\\^");
            docEntityVector.put(entity_weight[0], Double.parseDouble(entity_weight[1]));
            entityNumVector.put(entity_num[0], Integer.parseInt(entity_num[1]));
          }
          double ss = calSemanticSimilarity(extendedQueryEntityVector, docEntityVector, entityNumVector);
          double scoreOfAllRE = calScoreOfAllRE(extendedQueryEntityVector, entityNumVector);
          double ratioOfRE2DEV = calRatioOfRE2DEV(extendedQueryEntityVector, docEntityVector, entityNumVector, numOfAnnotation);
          double ratioOfRE2EQEV = calRatioOfRE2EQEV(extendedQueryEntityVector, entityNumVector);
          double score = MathUtil.round(ss + scoreOfAllRE * ratioOfRE2DEV * ratioOfRE2EQEV, 3);
          if (score > 0) {
            if (!ssWithID.keySet().contains(score)) {
              Set<String> ids = new HashSet<>();
              ids.add(id);
              ssWithID.put(score, ids);
            } else {
              ssWithID.get(score).add(id);
            }
          }
        }
      }
      for (String entity : queryEntityVector.keySet()) {
        extendedQueryEntityVector.remove(entity);
      }
      json = buildJSONObject(ssWithID, idWithDoc, queryEntityVector.keySet().toString(), extendedQueryEntityVector.keySet().toString());
    } else { // use English as the connecting language
      Map<String, Double> extendedENEntityVector = extendsToENEntities(queryEntityVector, kb_lang);
      Map<String, Double> enQueryEntityVector = convertToEnEntityVector(queryEntityVector, kb_lang);
      if (LanguageConstants.EN.equals(kb_lang)) {
        if (orMode) {
          idWithDoc = findOrDocs(queryEntityVector, t_lang);
        } else {
          idWithDoc = findAndDocs(queryEntityVector, t_lang);
        }
      } else {
        if (orMode) {
          idWithDoc = findOrDocs(enQueryEntityVector, t_lang);
        } else {
          idWithDoc = findAndDocs(enQueryEntityVector, t_lang);
        }
      }

      for (String id : idWithDoc.keySet()) {
        String englishEV = (String) idWithDoc.get(id).get(DBConstants.ENGLISH_ENTITY_VECTOR);
        String annotationWithNum = (String) idWithDoc.get(id).get(DBConstants.ANNOTATION_WITH_NUM);
        int annotationNum = (Integer) idWithDoc.get(id).get(DBConstants.NUMBER_OF_ANNOTATION);
        if (englishEV.length() > 0) {
          Map<String, Double> docEntityVector = new HashMap<>();
          Map<String, Integer> entityNumVector = new HashMap<>();
          String[] split1 = englishEV.substring(1, englishEV.length() - 1).split("\\|");
          String[] split2 = annotationWithNum.substring(1, annotationWithNum.length() - 1).split("\\|");
          for (int i = 0; i < split2.length; i++) {
            String[] entity_weight = split1[i].split("\\^");
            docEntityVector.put(entity_weight[0], Double.parseDouble(entity_weight[1]));
            String[] entity_num = split2[i].split("\\^");
            entityNumVector.put(entity_weight[0], Integer.parseInt(entity_num[1]));
          }
          double ss = calSemanticSimilarity(extendedENEntityVector, docEntityVector, entityNumVector);
          double scoreOfAllRE = calScoreOfAllRE(extendedENEntityVector, entityNumVector);
          double ratioOfRE2DEV = calRatioOfRE2DEV(extendedENEntityVector, docEntityVector, entityNumVector, annotationNum);
          double ratioOfRE2EQEV = calRatioOfRE2EQEV(enQueryEntityVector, entityNumVector);
          double score = MathUtil.round(ss + scoreOfAllRE * ratioOfRE2DEV * ratioOfRE2EQEV, 3);
          if (score > 0) {
            if (!ssWithID.keySet().contains(score)) {
              Set<String> ids = new HashSet<>();
              ids.add(id);
              ssWithID.put(score, ids);
            } else {
              ssWithID.get(score).add(id);
            }
          }
        }
      }
      List<String> extendedDEEntityVector = convertToDEEntityVector(extendedENEntityVector);
      List<String> extendedZHEntityVector = convertToZHEntityVector(extendedENEntityVector);
      for (String entity : enQueryEntityVector.keySet()) {
        extendedENEntityVector.remove(entity);
      }
      List<String> oDEEntityVector = convertToDEEntityVector(enQueryEntityVector);
      List<String> oZHEntityVector = convertToZHEntityVector(enQueryEntityVector);
      for (String entity : oDEEntityVector) {
        extendedDEEntityVector.remove(entity);
      }
      for (String entity : oZHEntityVector) {
        extendedZHEntityVector.remove(entity);
      }
      json =
             buildJSONObject(ssWithID, idWithDoc, enQueryEntityVector.keySet(), extendedENEntityVector.keySet(), oDEEntityVector, extendedDEEntityVector, oZHEntityVector,
                             extendedZHEntityVector);
    }

    return json;
  }

  public JSONObject getDocumentsByQuery(String query, boolean orMode, String s_lang, String t_lang) throws IOException, RepositoryException {
    LabelCombinationParser parser;
    if (s_lang.equals(t_lang)) {
      parser = new LabelCombinationParser(s_lang, t_lang);
      TreeNode labelCombinationWithEntities = parser.getLabelCombinationWithEntities(query);
      String entitiesWithProb = CharacterConstants.EMPTY_STRING;
      TreeNode lcNode = labelCombinationWithEntities.getChildren().get(0);
      List<TreeNode> labelNodes = lcNode.getChildren();
      for (TreeNode labelNode : labelNodes) {
        KBEntity entity = (KBEntity) labelNode.getChildren().get(0).getData();
        entitiesWithProb += (entity.get_name() + "^" + entity.get_score() + "|");
      }
      _logger.debug("The entity probability vector is: " + entitiesWithProb);
      return getDocumentsByEntities(entitiesWithProb, orMode, s_lang.equals(t_lang) ? s_lang : "en", t_lang);
    } else {
      parser = new LabelCombinationParser(s_lang, "en");
      TreeNode labelCombinationWithEntities = parser.getLabelCombinationWithEntities(query);
      String entitiesWithProb = CharacterConstants.EMPTY_STRING;
      TreeNode lcNode = labelCombinationWithEntities.getChildren().get(0);
      List<TreeNode> labelNodes = lcNode.getChildren();
      for (TreeNode labelNode : labelNodes) {
        KBEntity entity = (KBEntity) labelNode.getChildren().get(0).getData();
        entitiesWithProb += (entity.get_name() + "^" + entity.get_score() + "|");
      }
      _logger.debug("The entity probability vector is: " + entitiesWithProb);
      return getDocumentsByEntities(entitiesWithProb, orMode, s_lang.equals(t_lang) ? s_lang : "en", t_lang);
    }
  }

  private static JSONObject buildJSONObject(TreeMap<Double, Set<String>> ssWithID, Map<String, DBObject> idWithDoc, Set<String> oENEntities, Set<String> eENEntities,
      List<String> oDEEntities, List<String> eDEEntities, List<String> oZHEntities, List<String> eZHEntities) throws IOException {
    JSONObject json = new JSONObject();
    JSONObject docJSONS = new JSONObject();
    for (double ss : ssWithID.descendingKeySet()) {
      Set<String> ids = ssWithID.get(ss);
      JSONObject docsJSON = new JSONObject();
      try {
        for (String id : ids) {
          JSONObject docJSON = new JSONObject();
          DBObject dbObject = idWithDoc.get(id);
          docJSON.put(DBConstants.TITLE, (String) dbObject.get(DBConstants.TITLE));
          docJSON.put(DBConstants.URI, (String) dbObject.get(DBConstants.URI));
          docJSON.put(DBConstants.LANG, (String) dbObject.get(DBConstants.LANG));
          docJSON.put(DBConstants.IMG, (String) dbObject.get(DBConstants.IMG));
          docJSON.put(DBConstants.COUNTRY, (String) dbObject.get(DBConstants.COUNTRY));
          docJSON.put(DBConstants.LONGITUDE, (String) dbObject.get(DBConstants.LONGITUDE));
          docJSON.put(DBConstants.LATITUDE, (String) dbObject.get(DBConstants.LATITUDE));
          docJSON.put(DBConstants.RETRIEVED_DATE, (String) dbObject.get(DBConstants.RETRIEVED_DATE));
          docJSON.put(DBConstants.ANNOTATED_TEXT, (String) dbObject.get(DBConstants.ANNOTATED_TEXT));
          docsJSON.append("doc", docJSON);
        }
        docJSONS.put(String.valueOf(ss), docsJSON);
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }
    json.put("docJSONS", docJSONS);
    json.put("oENEntities", oENEntities.toString());
    json.put("eENEntities", eENEntities.toString());
    json.put("oDEEntities", oDEEntities.toString());
    json.put("eDEEntities", eDEEntities.toString());
    json.put("oZHEntities", oZHEntities.toString());
    json.put("eZHEntities", eZHEntities.toString());
    return json;
  }

  private static JSONObject buildJSONObject(TreeMap<Double, Set<String>> ssWithID, Map<String, DBObject> idWithDoc, String originEntities, String extendedEntities)
      throws IOException {
    JSONObject json = new JSONObject();
    JSONObject docJSONS = new JSONObject();
    for (double ss : ssWithID.descendingKeySet()) {
      Set<String> ids = ssWithID.get(ss);
      JSONObject docsJSON = new JSONObject();
      try {
        for (String id : ids) {
          JSONObject docJSON = new JSONObject();
          DBObject dbObject = idWithDoc.get(id);
          docJSON.put(DBConstants.TITLE, (String) dbObject.get(DBConstants.TITLE));
          docJSON.put(DBConstants.URI, (String) dbObject.get(DBConstants.URI));
          docJSON.put(DBConstants.LANG, (String) dbObject.get(DBConstants.LANG));
          docJSON.put(DBConstants.IMG, (String) dbObject.get(DBConstants.IMG));
          docJSON.put(DBConstants.COUNTRY, (String) dbObject.get(DBConstants.COUNTRY));
          docJSON.put(DBConstants.LONGITUDE, (String) dbObject.get(DBConstants.LONGITUDE));
          docJSON.put(DBConstants.LATITUDE, (String) dbObject.get(DBConstants.LATITUDE));
          docJSON.put(DBConstants.RETRIEVED_DATE, (String) dbObject.get(DBConstants.RETRIEVED_DATE));
          docJSON.put(DBConstants.ANNOTATED_TEXT, (String) dbObject.get(DBConstants.ANNOTATED_TEXT));
          docsJSON.append("doc", docJSON);
        }
        docJSONS.put(String.valueOf(ss), docsJSON);
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }
    json.put("docJSONS", docJSONS);
    json.put("oEntities", originEntities);
    json.put("eEntities", extendedEntities);
    return json;
  }

  private Map<String, DBObject> findOrDocs(Map<String, Double> entityProbQuery, String lang) {
    Map<String, DBObject> idWithDoc = new HashMap<>();
    String[] split = lang.split(",");
    for (int i = 0; i < split.length; i++) {
      for (String entity : entityProbQuery.keySet()) {
        BasicDBObject dbObject = new BasicDBObject(DBConstants.ENTITY, entity).append(DBConstants.LANG, split[i]);
        DBCursor cursor = _entityNFArticleCol.find(dbObject);
        while (cursor.hasNext()) {
          DBObject next = cursor.next();
          String id = (String) next.get(DBConstants.ARTICLE_ID);
          if (!idWithDoc.keySet().contains(id)) {
            DBObject doc = _nfarticleCol.findOne(new BasicDBObject(DBConstants.ARTICLE_ID, id));
            if (null != doc) {
              idWithDoc.put(id, doc);
            }
          }
        }
      }
    }
    return idWithDoc;
  }

  private Map<String, DBObject> findAndDocs(Map<String, Double> queryEntityVector, String lang) {
    Map<String, DBObject> idWithDoc = new HashMap<>();
    String[] split = lang.split(",");
    for (int i = 0; i < split.length; i++) {
      for (String entity : queryEntityVector.keySet()) {
        BasicDBObject dbObject = new BasicDBObject(DBConstants.ENTITY, entity).append(DBConstants.LANG, split[i]);
        DBCursor cursor = _entityNFArticleCol.find(dbObject);
        while (cursor.hasNext()) {
          DBObject next = cursor.next();
          String id = (String) next.get(DBConstants.ARTICLE_ID);
          DBObject doc = _nfarticleCol.findOne(new BasicDBObject(DBConstants.ARTICLE_ID, id));
          if (null != doc) {
            String originalDEV = (String) doc.get(DBConstants.ORIGINAL_ENTITY_VECTOR);
            String englishDEV = (String) doc.get(DBConstants.ENGLISH_ENTITY_VECTOR);
            if (isAndDoc(queryEntityVector, englishDEV) || isAndDoc(queryEntityVector, originalDEV)) {
              if (!idWithDoc.keySet().contains(id)) {
                idWithDoc.put(id, doc);
              }
            }
          }
        }
      }
    }
    return idWithDoc;
  }

  /**
   * check if a document contains all the query entities at least once
   * 
   * @param entityProbQuery
   * @param dev
   * @return
   */
  private boolean isAndDoc(Map<String, Double> entityProbQuery, String dev) {
    for (String entity : entityProbQuery.keySet()) {
      if (!dev.contains("|" + entity + "^")) {
        return false;
      }
    }
    return true;
  }

  private Map<String, Double> extendsToLocalEntities(Map<String, Double> entityWithProb, String lang) throws RepositoryException, IOException {
    Map<String, Double> neighborProbQuery = new HashMap<>();
    neighborProbQuery.putAll(entityWithProb);
    for (String entityName : entityWithProb.keySet()) {
      for (Triple<String, String, Double> neighbor : _querier.queryPOFromMongoDB(entityName, lang)) {
        String neighborName = neighbor.o.substring(neighbor.o.lastIndexOf("/") + 1, neighbor.o.length()).replaceAll("_", " ");
        neighborProbQuery.put(neighborName, entityWithProb.get(entityName) * neighbor.rs);
      }
    }
    return neighborProbQuery;
  }

  /**
   * Extends users' entities into English entities. See the approaches SimpleMap and ProbMap in paper
   * "Semantic Similarity Measurements for Multi-lingual Short Texts Using Wikipedia"
   * 
   * @param entityWithProb
   * @param rrSearcher
   * @return
   * @throws RepositoryException
   * @throws IOException
   */
  private Map<String, Double> extendsToENEntities(Map<String, Double> entityWithProb, String lang) throws RepositoryException, IOException {
    Map<String, Double> neighborProbQuery = new HashMap<>();
    if (LanguageConstants.EN.equals(lang)) {
      neighborProbQuery.putAll(entityWithProb);
      for (String entityName : entityWithProb.keySet()) {
        for (Triple<String, String, Double> neighbor : _querier.queryPOFromMongoDB(entityName, lang)) {
          String neighborName = neighbor.o.substring(neighbor.o.lastIndexOf("/") + 1, neighbor.o.length()).replaceAll("_", " ");
          neighborProbQuery.put(neighborName, entityWithProb.get(entityName) * neighbor.rs);
        }
      }
    } else {
      Map<String, Double> enEntitiesWithProb = convertToEnEntityVector(entityWithProb, lang);
      neighborProbQuery.putAll(enEntitiesWithProb);
      for (String entityName : enEntitiesWithProb.keySet()) {
        for (Triple<String, String, Double> neighbor : _querier.queryPOFromMongoDB(entityName, LanguageConstants.EN)) {
          String neighborName = neighbor.o.substring(neighbor.o.lastIndexOf("/") + 1, neighbor.o.length()).replaceAll("_", " ");
          if (!neighborProbQuery.keySet().contains(neighborName)) {
            neighborProbQuery.put(neighborName, enEntitiesWithProb.get(entityName) * neighbor.rs);
          } else {
            neighborProbQuery.put(neighborName, neighborProbQuery.get(neighborName) + enEntitiesWithProb.get(entityName) * neighbor.rs);
          }
        }
      }
      for (String entityName : entityWithProb.keySet()) {
        for (Triple<String, String, Double> neighbor : _querier.queryPOFromMongoDB(entityName, lang)) {
          String neighborName = neighbor.o.substring(neighbor.o.lastIndexOf("/") + 1, neighbor.o.length()).replaceAll("_", " ");
          String enEntity = _langlinker.findEnEntity(neighborName, lang);
          if (null != enEntity) {
            if (!neighborProbQuery.keySet().contains(enEntity)) {
              neighborProbQuery.put(enEntity, entityWithProb.get(entityName) * neighbor.rs);
            } else {
              neighborProbQuery.put(enEntity, neighborProbQuery.get(enEntity) + entityWithProb.get(entityName) * neighbor.rs);
            }
          }
        }
      }
    }
    return neighborProbQuery;
  }

  /**
   * Convert all non-English entities into the corresponding English entities
   * 
   * @param entityProbQuery
   * @param lang
   * @return
   */
  private Map<String, Double> convertToEnEntityVector(Map<String, Double> entityProbQuery, String lang) {
    Map<String, Double> enEntityVector = new HashMap<>();
    for (String entity : entityProbQuery.keySet()) {
      String enEntity = _langlinker.findEnEntity(entity, lang);
      if (null != enEntity) {
        enEntityVector.put(enEntity, entityProbQuery.get(entity));
      } else {
        enEntityVector.put(entity, entityProbQuery.get(entity));
      }
    }
    return enEntityVector;
  }

  /**
   * Convert English entities into the corresponding German entities
   * 
   * @param extendedENEntityVector
   * @return
   */
  private List<String> convertToDEEntityVector(Map<String, Double> extendedENEntityVector) {
    List<String> deEntityVector = new ArrayList<String>();
    for (String entity : extendedENEntityVector.keySet()) {
      String deEntity = _langlinker.findDeEntityByEnEntity(entity);
      if (null != deEntity) {
        deEntityVector.add(deEntity);
      } else {
        deEntityVector.add(entity);
      }
    }
    return deEntityVector;
  }

  /**
   * Convert English entities into the corresponding Chinese entities
   * 
   * @param extendedENEntityVector
   * @return
   */
  private List<String> convertToZHEntityVector(Map<String, Double> extendedENEntityVector) {
    List<String> zhEntityVector = new ArrayList<String>();
    for (String entity : extendedENEntityVector.keySet()) {
      String zhEntity = _langlinker.findZhEntityByEnEntity(entity);
      if (null != zhEntity) {
        zhEntityVector.add(zhEntity);
      } else {
        zhEntityVector.add(entity);
      }
    }
    return zhEntityVector;
  }

  /**
   * calculate the total score of all related entities
   * 
   * @param extendedQueryEntityVector
   * @param entityNumVector
   * @return
   */
  private double calScoreOfAllRE(Map<String, Double> extendedQueryEntityVector, Map<String, Integer> entityNumVector) {
    Map<String, Double> copyOfEQEV = new HashMap<>(extendedQueryEntityVector);
    Set<String> extendedEntities = copyOfEQEV.keySet();
    Set<String> docEntities = entityNumVector.keySet();
    double score = 0.0;
    double percent = 0.0;
    extendedEntities.retainAll(docEntities);
    if (!extendedEntities.isEmpty()) {
      percent = (1.0 * extendedEntities.size()) / (1.0 * extendedQueryEntityVector.size());
      for (String entity : extendedEntities) {
        score += extendedQueryEntityVector.get(entity) * entityNumVector.get(entity);
      }
      return percent * score;
    } else {
      return 0.0;
    }
  }

  /**
   * The ratio of related entities to all entities in the query entity vector
   * 
   * @param queryEntityVector
   * @param entityNumVector
   * @return
   */
  private double calRatioOfRE2EQEV(Map<String, Double> queryEntityVector, Map<String, Integer> entityNumVector) {
    Map<String, Double> copyOfQEV = new HashMap<>(queryEntityVector);
    Set<String> entities = copyOfQEV.keySet();
    Set<String> docEntities = entityNumVector.keySet();
    entities.retainAll(docEntities);
    if (!entities.isEmpty()) {
      return (1.0 * entities.size()) / (1.0 * queryEntityVector.keySet().size());
    } else {
      return 0.0;
    }
  }

  /**
   * Calculate the semantic similarity between query entity vector and document entity vector using cosine semantic similarity function.
   * 
   * @param extendedQueryEntityVector
   * @param docEntityVector
   * @return
   * @throws RepositoryException
   * @throws IOException
   */
  private static double calSemanticSimilarity(Map<String, Double> extendedQueryEntityVector, Map<String, Double> docEntityVector, Map<String, Integer> entityNumVector)
      throws RepositoryException, IOException {
    Map<String, Double> entityProbQueryCopy = new HashMap<>(extendedQueryEntityVector);
    Set<String> keySetQuery = entityProbQueryCopy.keySet();
    Set<String> keySetDoc = docEntityVector.keySet();
    keySetQuery.retainAll(keySetDoc);

    double numerator = 0.0;
    if (!keySetQuery.isEmpty()) {
      for (String entity : keySetQuery) {
        int num = entityNumVector.containsKey(entity) ? entityNumVector.get(entity) : 1;
        numerator += (entityProbQueryCopy.get(entity) * docEntityVector.get(entity) * num);
      }
    } else {
      return 0.0;
    }

    double denominator = 0.0;
    double tmp1 = 0.0;
    double tmp2 = 0.0;
    for (double prob : entityProbQueryCopy.values()) {
      tmp1 += (prob * prob);
    }
    for (double prob : docEntityVector.values()) {
      tmp2 += (prob * prob);
    }
    denominator = Math.sqrt(tmp1) * Math.sqrt(tmp2);

    return MathUtil.round(numerator / denominator, 10);
  }

  /**
   * calculate the ratio of related entities to all entities in document
   * 
   * @param extendedQueryEntityVector
   * @param docEntityVector
   * @return
   */
  private double calRatioOfRE2DEV(Map<String, Double> extendedQueryEntityVector, Map<String, Double> docEntityVector, Map<String, Integer> entityNumVector, int annotationNum) {
    Map<String, Double> entityProbQueryCopy = new HashMap<>(extendedQueryEntityVector);
    Set<String> keySetQuery = entityProbQueryCopy.keySet();
    Set<String> keySetDoc = docEntityVector.keySet();
    keySetQuery.retainAll(keySetDoc);
    int numOfRelatedEntities = 0;
    if (!keySetQuery.isEmpty()) {
      for (String entity : keySetQuery) {
        numOfRelatedEntities += entityNumVector.get(entity);
      }
      return (1.0 * numOfRelatedEntities) / (1.0 * annotationNum);
    } else {
      return 0.0;
    }
  }
}
