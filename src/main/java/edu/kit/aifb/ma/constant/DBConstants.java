package edu.kit.aifb.ma.constant;

/**
 * This class stores the connection information of the MongoDB Database and all required collections in our system, including the fields of each
 * collection.
 * 
 */
public final class DBConstants {

  // the connection information of the mongoDB

  //  public static final String MONGODB_HOSTNAME = "localhost";
  //
  //  public static final String MONGODB_NAME = "ABIRS";
  //
  //  public static final int MONGODB_PORT = 27017;

  /* ======================= NFArticleIndex ======================= */

  public static final String NFARTICLE_COLLECTION = "NFArticleIndex";

  public static final String ARTICLE_ID = "articleID";

  public static final String ORIGINAL_ENTITY_VECTOR = "originalEV";

  public static final String ENGLISH_ENTITY_VECTOR = "englishEV";

  public static final String ANNOTATION_WITH_NUM = "annotationWithNum";

  public static final String TITLE = "title";

  public static final String URI = "uri";

  public static final String LANG = "lang";

  public static final String COUNTRY = "country";

  public static final String RETRIEVED_DATE = "retrievedDate";

  public static final String LONGITUDE = "longitude";

  public static final String LATITUDE = "latitude";

  public static final String IMG = "img";

  public static final String ANNOTATED_TEXT = "annotatedText";

  public static final String BODY_CLEARTEXT = "bodyCleartext";

  public static final String WORD_NUMBER = "wordNum";

  public static final String NUMBER_OF_ANNOTATION = "annotationNum";

  public static final String ANNOTATION_PERCENTAGE = "annotationPercentage";

  /* ======================= EntityIndex ======================= */

  public static final String ENTITY_COLLECTION_EN = "EntityIndex_EN";

  public static final String ENTITY_COLLECTION_DE = "EntityIndex_DE";

  public static final String ENTITY_COLLECTION_ZH = "EntityIndex_ZH";

  public static final String GENERALITY = "generality";

  public static final String TOTAL_LINKS_OUT_COUNT = "totalLinksOutCount";

  public static final String TOTAL_LINKS_IN_COUNT = "totalLinksInCount";

  public static final String DISTINCT_LINKS_OUT_COUNT = "distinctLinksOutCount";

  public static final String DISTINCT_LINKS_IN_COUNT = "distinctLinksInCount";

  public static final String ID = "id";

  public static final String ENTITY = "entity";

  /* ======================= LabelIndex ======================= */

  public static final String LABEL_COLLECTION_EN = "LabelIndex_EN";

  public static final String LABEL_COLLECTION_DE = "LabelIndex_DE";

  public static final String LABEL_COLLECTION_ZH = "LabelIndex_ZH";

  public static final String LINK_OCC_COUNT = "linkOccCount";

  public static final String LINK_DOC_COUNT = "linkDocCount";

  public static final String OCC_COUNT = "occCount";

  public static final String DOC_COUNT = "docCount";

  // the probability that label appears in an article as an anchor text

  public static final String PROBABILITY = "probability";

  public static final String LABEL = "label";

  /* ======================= LabelEntityIndex ======================= */

  public static final String LABEL_ENTITY_COLLECTION_EN = "LabelEntityIndex_EN";

  public static final String LABEL_ENTITY_COLLECTION_DE = "LabelEntityIndex_DE";

  public static final String LABEL_ENTITY_COLLECTION_ZH = "LabelEntityIndex_ZH";

  // association strength between label and entity

  public static final String ASSOCIATION_STRENGTH = "associationStrength";

  // the probability that this label is used as a link in Wikipedia

  public static final String WEIGHT = "weight";

  /* ======================= LanglinksIndex ======================= */

  public static final String LANGLINKS_COLLECTION = "LanglinksIndex";

  public static final String LANGLINKS_COLLECTION_DEZHEN = "LanglinksIndex_DE_ZH_EN";

  public static final String SOURCE_ID = "s_id";

  public static final String SOURCE_TITLE = "s_title";

  public static final String SOURCE_LANGUAGE = "s_lang";

  public static final String TARGET_LANGUAGE = "t_lang";

  public static final String TARGET_TITLE = "t_title";

  /* ======================= ResourceRelatednessIndex ======================= */

  public static final String RESOURCERELATEDNESS_COLLECTION_EN = "ResourceRelatednessIndex_EN";

  public static final String RESOURCERELATEDNESS_COLLECTION_DE = "ResourceRelatednessIndex_DE";

  public static final String RESOURCERELATEDNESS_COLLECTION_ZH = "ResourceRelatednessIndex_ZH";

  public static final String TARGET_ID = "t_id";

  public static final String SCORE = "score";

  /* ======================= EntityNFArticleIndex ======================= */

  public static final String ENTITY_NFARTICLE_INDEX = "EntityNFArticleIndex";

  /* ======================= ShortAbstractsIndex ======================= */

  public static final String SHORT_ABSTRACTS_INDEX = "ShortAbstractsIndex";

  public static final String SA = "sa";

  /* ======================= ImageIndex ======================= */

  public static final String IMAGE_INDEX = "ImageIndex";

  public static final String TYPE = "type";

  public static final String IMAGE_URI = "imgUri";

  /* ======================= NeighborsIndex ======================= */

  public static final String NEIGHBORS_INDEX = "NeighborsIndex";

  public static final String SUBJECT = "subject";

  public static final String PREDICATE = "predicate";

  public static final String OBJECT = "object";

  /* ======================= InfoBoxIndex ======================= */

  public static final String INFOBOX_INDEX = "InfoBoxIndex";

}
