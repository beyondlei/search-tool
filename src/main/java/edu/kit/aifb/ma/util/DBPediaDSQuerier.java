package edu.kit.aifb.ma.util;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONObject;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.nativerdf.NativeStore;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import edu.kit.aifb.ma.constant.DBConstants;
import edu.kit.aifb.ma.constant.LanguageConstants;
import edu.kit.aifb.ma.search.Pair;
import edu.kit.aifb.ma.search.Triple;

public class DBPediaDSQuerier {
  private static final String EMPTY_STRING = "";

  private static final String TYPE_SPOC = "spoc";

  private static final String NEIGHBOR_EN = "/Users/zhongwenjiang/Desktop/MA/Data/sesame_repository/neighbor_en/";

  private static final String NEIGHBOR_DE = "/Users/zhongwenjiang/Desktop/MA/Data/sesame_repository/neighbor_de/";

  private static final String NEIGHBOR_ZH = "/Users/zhongwenjiang/Desktop/MA/Data/sesame_repository/neighbor_zh/";

  private static final String IMAGES_PATH_EN = "/Users/zhongwenjiang/Desktop/MA/Data/sesame_repository/images_en/";

  private static final String PERSONDATA_PATH_EN = "/Users/zhongwenjiang/Desktop/MA/Data/sesame_repository/persondata_en/";

  private static final String SHORT_ABSTRACTS_PATH_EN = "/Users/zhongwenjiang/Desktop/MA/Data/sesame_repository/short_abstracts_en/";

  private static final String MAPPINGBASED_PROPERTIES_PATH_EN = "/Users/zhongwenjiang/Desktop/MA/Data/sesame_repository/mappingbased_properties_en/";

  private static final String RAW_INFOBOX_PATH_EN = "/Users/zhongwenjiang/Desktop/MA/Data/sesame_repository/raw_infobox_properties_en/";

  private static final String IMAGES_PATH_DE = "/Users/zhongwenjiang/Desktop/MA/Data/sesame_repository/images_de/";

  private static final String PERSONDATA_PATH_DE = "/Users/zhongwenjiang/Desktop/MA/Data/sesame_repository/persondata_de/";

  private static final String SHORT_ABSTRACTS_PATH_DE = "/Users/zhongwenjiang/Desktop/MA/Data/sesame_repository/short_abstracts_de/";

  private static final String MAPPINGBASED_PROPERTIES_PATH_DE = "/Users/zhongwenjiang/Desktop/MA/Data/sesame_repository/mappingbased_properties_de/";

  private static final String RAW_INFOBOX_PATH_DE = "/Users/zhongwenjiang/Desktop/MA/Data/sesame_repository/raw_infobox_properties_de/";

  private static final String SHORT_ABSTRACTS_PATH_ZH = "/Users/zhongwenjiang/Desktop/MA/Data/sesame_repository/short_abstracts_zh/";

  private static final String RAW_INFOBOX_PATH_ZH = "/Users/zhongwenjiang/Desktop/MA/Data/sesame_repository/raw_infobox_properties_zh/";

  private static final String URI_PREFIX_EN = "http://dbpedia.org/resource/";

  private static final String URI_PREFIX_DE = "http://de.dbpedia.org/resource/";

  private static final String URI_PREFIX_ZH = "http://zh.dbpedia.org/resource/";

  private static final String MAPPINGBASED_PROPERTIES = "mappingbasedProperties";

  private static final String RAW_INFOBOX = "rawInfobox";

  private static final String PERSONDATA = "persondata";

  private Repository rep;

  public static void main(String[] args) {
    long start = System.currentTimeMillis();
    try {
      DBPediaDSQuerier querier = new DBPediaDSQuerier();
      //      System.out.println("Raw InfoBox========================");
      //      querier.queryDataset("联邦储备系统", RAW_INFOBOX, "zh");
      //      System.out.println("Mappingbased Properties========================");
      //      querier.queryDataset("Berlin", MAPPINGBASED_PROPERTIES, "en");
      //      System.out.println("Short Abstract=================");
      querier.querySAFromSesame("联邦储备系统", "zh");
      //            System.out.println("Persondata=====================");
      //            querier.queryDataset("Barack Obama", PERSONDATA, "de");
      //            System.out.println("Images=========================");
      querier.queryImagesFromMongoDB("Peking", "de");
      System.out.println("Property Object================");
      querier.queryPOFromSesame("Obama,Fukui", "en");
      querier.queryNeighborsFromMongoDB("Deutschland", "de");
      //      querier.queryPropertyObject("德国", "zh");
      querier.queryNeighborsFromMongoDB("德国", "zh");
      //      querier.queryPropertyObject("Barack Obama", "de");
      //      querier.queryShortAbstract("Solid", "en");
      long end = System.currentTimeMillis();
      System.out.println("Total time: " + (end - start) + " ms.");
    } catch (RepositoryException e) {
      e.printStackTrace();
    }
  }

  public void addRDF() {
    Repository rep = new SailRepository(new NativeStore(new File("/Users/zhongwenjiang/Desktop/MA/Data/sesame_repository/raw_infobox_properties_de"), TYPE_SPOC));
    try {
      rep.initialize();
      RepositoryConnection conn = rep.getConnection();
      conn.add(new File("/Volumes/My Passport/Data for MA/ttl/de/raw_infobox_properties_de.ttl"), "http://de.dbpedia.org", RDFFormat.TURTLE);
    } catch (RepositoryException e) {
      e.printStackTrace();
    } catch (RDFParseException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * query the property and object(must be resource) from the DBPedia dataset "mappingbased_properties".
   * 
   * @param entity
   * @param s_lang
   * @return
   * @throws RepositoryException
   */
  public Set<Pair<String, String>> queryPOFromSesame(String entity, String s_lang) throws RepositoryException {
    Set<Pair<String, String>> propertyWithObject = new HashSet<>();
    //    Repository rep = null;
    String resURIPrefix = EMPTY_STRING;
    if (LanguageConstants.EN.equals(s_lang)) {
      rep = new SailRepository(new NativeStore(new File(MAPPINGBASED_PROPERTIES_PATH_EN), TYPE_SPOC));
      resURIPrefix = URI_PREFIX_EN;
    } else if (LanguageConstants.DE.equals(s_lang)) {
      rep = new SailRepository(new NativeStore(new File(RAW_INFOBOX_PATH_DE), TYPE_SPOC));
      resURIPrefix = URI_PREFIX_DE;
    } else if (LanguageConstants.ZH.equals(s_lang)) {
      rep = new SailRepository(new NativeStore(new File(RAW_INFOBOX_PATH_ZH), TYPE_SPOC));
      resURIPrefix = URI_PREFIX_ZH;
    }
    String resURI = resURIPrefix + entity.replaceAll(" ", "_");
    if (!rep.isInitialized()) {
      rep.initialize();
    }
    RepositoryConnection conn = rep.getConnection();
    try {
      String queryString = String.format("select ?p ?o  where {<%s> ?p ?o}", resURI);
      TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
      TupleQueryResult result = tupleQuery.evaluate();
      System.out.println("Entity: " + entity + "====================");
      while (result.hasNext()) {
        BindingSet bindingSet = result.next();
        String property = bindingSet.getValue("p").stringValue();
        String object = bindingSet.getValue("o").stringValue();
        if (object.startsWith(resURIPrefix)) {
          propertyWithObject.add(new Pair<String, String>(property, object));
          System.out.println(property + ", " + object);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      conn.close();
      rep.shutDown();
    }
    return propertyWithObject;
  }

  public Set<Triple<String, String, Double>> queryPOFromMongoDB(String subject, String s_lang) {
    Set<Triple<String, String, Double>> propertyWithObject = new HashSet<>();
    ProbabilityCalculator calculator;
    try {
      calculator = new ProbabilityCalculator(s_lang);
      DBCollection collection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.NEIGHBORS_INDEX);
      DBCursor match = collection.find(new BasicDBObject(DBConstants.SUBJECT, subject).append(DBConstants.LANG, s_lang));
      while (match.hasNext()) {
        DBObject next = match.next();
        String predicate = (String) next.get(DBConstants.PREDICATE);
        String object = (String) next.get(DBConstants.OBJECT);
        double relatedness = MathUtil.round(calculator.calRelatedness(subject, object), 10);
        if (relatedness > 0) {
          propertyWithObject.add(new Triple<String, String, Double>(predicate, object, relatedness));
        }
      }
    } catch (UnknownHostException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return propertyWithObject;
  }

  public JSONObject queryNeighborsFromMongoDB(String entity, String s_lang) {
    JSONObject jsonObject = new JSONObject();
    ProbabilityCalculator calculator;
    try {
      calculator = new ProbabilityCalculator(s_lang);
      DBCollection collection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.NEIGHBORS_INDEX);
      DBCursor match = collection.find(new BasicDBObject(DBConstants.SUBJECT, entity).append(DBConstants.LANG, s_lang));
      while (match.hasNext()) {
        DBObject next = match.next();
        String predicate = (String) next.get(DBConstants.PREDICATE);
        String object = (String) next.get(DBConstants.OBJECT);
        double relatedness = MathUtil.round(calculator.calRelatedness(entity, object), 10);
        if (relatedness > 0) {
          jsonObject.put(predicate, object + "|" + relatedness);
          System.out.println(predicate + ", " + object);
        }
      }
    } catch (UnknownHostException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return jsonObject;
  }

  /**
   * query neighbors for an entity using the DBpedia dataset "mappingbased_properties"
   * 
   * @param entity
   * @param s_lang
   * @return
   * @throws RepositoryException
   * @throws IOException
   */
  public JSONObject queryNeighborsFromSesame(String entity, String s_lang) throws RepositoryException, IOException {
    JSONObject jsonObj = new JSONObject();
    ProbabilityCalculator calculator = new ProbabilityCalculator(s_lang);
    String resURIPrefix = EMPTY_STRING;
    //    Repository rep = null;
    if (LanguageConstants.EN.equals(s_lang)) {
      resURIPrefix = URI_PREFIX_EN;
      rep = new SailRepository(new NativeStore(new File(NEIGHBOR_EN), TYPE_SPOC));
    } else if (LanguageConstants.DE.equals(s_lang)) {
      resURIPrefix = URI_PREFIX_DE;
      rep = new SailRepository(new NativeStore(new File(NEIGHBOR_DE), TYPE_SPOC));
    } else if (LanguageConstants.ZH.equals(s_lang)) {
      resURIPrefix = URI_PREFIX_ZH;
      rep = new SailRepository(new NativeStore(new File(NEIGHBOR_ZH), TYPE_SPOC));
    }
    String resURI = resURIPrefix + entity.replaceAll(" ", "_");
    if (!rep.isInitialized()) {
      rep.initialize();
    }
    RepositoryConnection conn = rep.getConnection();
    try {
      String queryString = String.format("select ?p ?o  where {<%s> ?p ?o}", resURI);
      TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
      TupleQueryResult result = tupleQuery.evaluate();
      while (result.hasNext()) {
        BindingSet bindingSet = result.next();
        String property = bindingSet.getValue("p").stringValue();
        String object = bindingSet.getValue("o").stringValue();
        if (object.startsWith(resURIPrefix)) {
          String propText = property.contains("/") ? property.substring(property.lastIndexOf("/") + 1, property.length()).replaceAll("_", " ") : property.replaceAll("_", " ");
          String objText = object.contains("/") ? object.substring(object.lastIndexOf("/") + 1, object.length()).replaceAll("_", " ") : object.replaceAll("_", " ");
          double relatedness = calculator.calRelatedness(entity, objText);
          if (relatedness > 0) {
            jsonObj.put(propText, objText + "|" + relatedness);
            System.out.println(property + ", " + object);
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      conn.close();
      rep.shutDown();
    }
    return jsonObj;
  }

  public JSONObject queryInfoBoxFromMongoDB(String entity, String s_lang) {
    JSONObject jsonObj = new JSONObject();
    DBCollection collection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.INFOBOX_INDEX);
    DBCursor match = collection.find(new BasicDBObject(DBConstants.SUBJECT, entity).append(DBConstants.LANG, s_lang));
    while (match.hasNext()) {
      DBObject next = match.next();
      String predicate = (String) next.get(DBConstants.PREDICATE);
      String object = (String) next.get(DBConstants.OBJECT);
      jsonObj.put(predicate, object);
    }
    return jsonObj;
  }

  /**
   * query all RDF data from various DBpedia datasets
   * 
   * @param entity
   * @param type
   * @param s_lang
   * @return
   * @throws RepositoryException
   */
  public JSONObject queryInfoBoxFromSesame(String entity, String type, String s_lang) throws RepositoryException {
    JSONObject jsonObj = new JSONObject();
    String path = EMPTY_STRING;
    String resURIPrefix = EMPTY_STRING;
    if (LanguageConstants.EN.equals(s_lang)) {
      resURIPrefix = URI_PREFIX_EN;
      if (MAPPINGBASED_PROPERTIES.equals(type)) {
        path = MAPPINGBASED_PROPERTIES_PATH_EN;
      } else if (PERSONDATA.equals(type)) {
        path = PERSONDATA_PATH_EN;
      } else if (RAW_INFOBOX.equals(type)) {
        path = RAW_INFOBOX_PATH_EN;
      }
    } else if (LanguageConstants.DE.equals(s_lang)) {
      resURIPrefix = URI_PREFIX_DE;
      if (MAPPINGBASED_PROPERTIES.equals(type)) {
        path = MAPPINGBASED_PROPERTIES_PATH_DE;
      } else if (PERSONDATA.equals(type)) {
        path = PERSONDATA_PATH_DE;
      } else if (RAW_INFOBOX.equals(type)) {
        path = RAW_INFOBOX_PATH_DE;
      }
    } else if (LanguageConstants.ZH.equals(s_lang)) {
      resURIPrefix = URI_PREFIX_ZH;
      if (RAW_INFOBOX.equals(type)) {
        path = RAW_INFOBOX_PATH_ZH;
      }
    }
    String resURI = resURIPrefix + entity.replaceAll(" ", "_");
    rep = new SailRepository(new NativeStore(new File(path), TYPE_SPOC));
    if (!rep.isInitialized()) {
      rep.initialize();
    }
    RepositoryConnection conn = rep.getConnection();
    try {
      String queryString = String.format("select ?p ?o  where {<%s> ?p ?o}", resURI);
      TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
      TupleQueryResult result = tupleQuery.evaluate();
      while (result.hasNext()) {
        BindingSet bindingSet = result.next();
        String property = bindingSet.getValue("p").stringValue();
        String object = bindingSet.getValue("o").stringValue();
        jsonObj.put(property.contains("/") ? property.substring(property.lastIndexOf("/") + 1, property.length()).replaceAll("_", " ") : property.replaceAll("_", " "),
                    object.contains("/") ? object.substring(object.lastIndexOf("/") + 1, object.length()).replaceAll("_", " ") : object.replaceAll("_", " "));
        System.out.println(property + ", " + object);
      }
      conn.close();
      rep.shutDown();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return jsonObj;
  }

  public JSONObject querySAFromMongoDB(String entity, String s_lang) {
    JSONObject jsonObj = new JSONObject();
    DBCollection collection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.SHORT_ABSTRACTS_INDEX);
    BasicDBObject dbObject = new BasicDBObject(DBConstants.ENTITY, entity).append(DBConstants.LANG, s_lang);
    DBObject match = collection.findOne(dbObject);
    if (null != match) {
      jsonObj.put("http://www.w3.org/2000/01/rdf-schema#comment", (String) match.get(DBConstants.SA));
    }
    return jsonObj;
  }

  public JSONObject querySAFromSesame(String entity, String s_lang) throws RepositoryException {
    JSONObject jsonObj = new JSONObject();
    Repository rep = null;
    String resURIPrefix = EMPTY_STRING;
    if (LanguageConstants.EN.equals(s_lang)) {
      resURIPrefix = URI_PREFIX_EN;
      rep = new SailRepository(new NativeStore(new File(SHORT_ABSTRACTS_PATH_EN), TYPE_SPOC));
    } else if (LanguageConstants.DE.equals(s_lang)) {
      resURIPrefix = URI_PREFIX_DE;
      rep = new SailRepository(new NativeStore(new File(SHORT_ABSTRACTS_PATH_DE), TYPE_SPOC));
    } else if (LanguageConstants.ZH.equals(s_lang)) {
      resURIPrefix = URI_PREFIX_ZH;
      rep = new SailRepository(new NativeStore(new File(SHORT_ABSTRACTS_PATH_ZH), TYPE_SPOC));
    }
    String resURI = resURIPrefix + entity.replaceAll(" ", "_");
    if (!rep.isInitialized()) {
      rep.initialize();
    }
    RepositoryConnection conn = rep.getConnection();
    try {
      String queryString = String.format("select ?p ?o  where {<%s> ?p ?o}", resURI);
      TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
      TupleQueryResult result = tupleQuery.evaluate();
      while (result.hasNext()) {
        BindingSet bindingSet = result.next();
        String property = bindingSet.getValue("p").stringValue();
        String object = bindingSet.getValue("o").stringValue();
        if (property.equals("http://www.w3.org/2000/01/rdf-schema#comment")) {
          jsonObj.put(property, object);
          System.out.println(property + ", " + object);
        }
      }
      conn.close();
      rep.shutDown();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return jsonObj;
  }

  public JSONObject queryImagesFromMongoDB(String entity, String s_lang) {
    JSONObject jsonObject = new JSONObject();
    DBCollection collection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.IMAGE_INDEX);
    DBObject match = collection.findOne(new BasicDBObject(DBConstants.ENTITY, entity).append(DBConstants.LANG, s_lang).append(DBConstants.TYPE, "thumbnail"));
    if (null == match) {
      match = collection.findOne(new BasicDBObject(DBConstants.ENTITY, entity).append(DBConstants.LANG, s_lang).append(DBConstants.TYPE, "depiction"));
      if (null != match) {
        jsonObject.put("image", (String) match.get(DBConstants.IMAGE_URI));
      }
    } else {
      jsonObject.put("image", (String) match.get(DBConstants.IMAGE_URI));
    }
    return jsonObject;
  }

  public JSONObject queryImagesFromSesame(String entity, String s_lang) throws RepositoryException {
    JSONObject jsonObj = new JSONObject();
    //    Repository rep = null;
    String resURIPrefix = EMPTY_STRING;
    if (LanguageConstants.EN.equals(s_lang)) {
      resURIPrefix = URI_PREFIX_EN;
      rep = new SailRepository(new NativeStore(new File(IMAGES_PATH_EN), TYPE_SPOC));
    } else if (LanguageConstants.DE.equals(s_lang)) {
      resURIPrefix = URI_PREFIX_DE;
      rep = new SailRepository(new NativeStore(new File(IMAGES_PATH_DE), TYPE_SPOC));
    } else if (LanguageConstants.ZH.equals(s_lang)) {
      resURIPrefix = URI_PREFIX_ZH;
      rep = new SailRepository(new NativeStore(new File(IMAGES_PATH_EN), TYPE_SPOC));
    }
    String resURI = resURIPrefix + entity.replaceAll(" ", "_");
    if (!rep.isInitialized()) {
      rep.initialize();
    }
    RepositoryConnection conn = rep.getConnection();
    try {
      String queryString = String.format("select ?p ?o  where {<%s> ?p ?o}", resURI);
      TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
      TupleQueryResult result = tupleQuery.evaluate();
      while (result.hasNext()) {
        BindingSet bindingSet = result.next();
        String property = bindingSet.getValue("p").stringValue();
        String object = bindingSet.getValue("o").stringValue();
        if (property.equals("http://dbpedia.org/ontology/thumbnail")) {
          jsonObj.put(property, object);
          System.out.println(property + ", " + object);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      conn.close();
      rep.shutDown();
    }
    return jsonObj;
  }

}
