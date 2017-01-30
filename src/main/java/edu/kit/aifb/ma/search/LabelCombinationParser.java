package edu.kit.aifb.ma.search;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import edu.kit.aifb.ma.constant.DBConstants;
import edu.kit.aifb.ma.constant.LanguageConstants;
import edu.kit.aifb.ma.util.MongoResource;

/**
 * Parse query to get labels combination and the corresponding entities.
 * 
 */
public class LabelCombinationParser {

  //  private final double FACTOR = 1.2;

  private static final double FACTOR = 1.2;

  private final int MAX_ENTITY_SIZE = 5;

  private String _sLang;

  private String _tLang;

  private DBCollection _labelCollection;

  private DBCollection _labelEntityCollection;

  private DBCollection _langlinksCollection;

  public static void main(String[] args) {
    String rawContent = "USA Obama USA USA";
    try {
      new LabelCombinationParser(LanguageConstants.EN, LanguageConstants.DE).getLabelCombinationWithEntities(rawContent);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public LabelCombinationParser(String s_lang, String t_lang) throws UnknownHostException {
    _sLang = s_lang;
    _tLang = t_lang;
    _langlinksCollection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.LANGLINKS_COLLECTION_DEZHEN);
    if (LanguageConstants.EN.equals(s_lang)) {
      _labelCollection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.LABEL_COLLECTION_EN);
      _labelEntityCollection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.LABEL_ENTITY_COLLECTION_EN);
    } else if (LanguageConstants.DE.equals(s_lang)) {
      _labelCollection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.LABEL_COLLECTION_DE);
      _labelEntityCollection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.LABEL_ENTITY_COLLECTION_DE);
    } else if (LanguageConstants.ZH.equals(s_lang)) {
      _labelCollection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.LABEL_COLLECTION_ZH);
      _labelEntityCollection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.LABEL_ENTITY_COLLECTION_ZH);
    }
  }

  /**
   * Find out the best label combination and the corresponding entity lists
   * 
   * @param query
   * @throws IOException
   */
  protected TreeNode getLabelCombinationWithEntities(String query) throws IOException {
    TreeNode root = new TreeNode(null, "LabelCombinations");
    Map<String, Pair<String, Double>> mentionWithLabelProbPair = new HashMap<>();
    List<String> invalidGrams = new ArrayList<>(); // the gram which can not be found in label index
    List<String> nGrams = new ArrayList<>();

    String plainText = QueryProcessor.preprocess(query);
    List<List<String>> mentionCombinations = QueryProcessor.getMentionCombinations(plainText);

    for (List<String> nGram : QueryProcessor.getNGrams(plainText).values()) {
      nGrams.addAll(nGram);
    }

    for (String ngram : nGrams) {
      Pair<String, Double> labelProbPair = findLabelAndProbability(ngram);
      if (null == labelProbPair) {
        invalidGrams.add(ngram); // gather invalid ngrams which can not be found as a label
      } else {
        mentionWithLabelProbPair.put(ngram, labelProbPair); // gather valid ngrams and the corresponding label with probability
      }
    }

    Queue<LabelCombination> labelCombinationQueue = new PriorityQueue<LabelCombination>(5, lcComparator);
    List<List<String>> validMentionCombinations = new ArrayList<>(mentionCombinations);

    if (!invalidGrams.isEmpty()) {
      for (List<String> mentionCombination : mentionCombinations) {
        for (String invalidGram : invalidGrams) {
          if (mentionCombination.contains(invalidGram)) {
            validMentionCombinations.remove(mentionCombination); // remove the invalid mention combinations
            break;
          }
        }
      }
    }
    // calculate the score of label combination, i.e. for query "Barack Obama", the mention combination "Barack Obama" has larger score than "Barack, Obama" 
    for (List<String> mentionCombination : validMentionCombinations) {
      Set<String> labelCombination = new HashSet<>();
      double score = 0.0;
      for (String mention : mentionCombination) {
        Pair<String, Double> labelProbabilityPair = mentionWithLabelProbPair.get(mention);
        labelCombination.add(labelProbabilityPair.t);
        score += labelProbabilityPair.u;
      }
      labelCombinationQueue.add(new LabelCombination(labelCombination, score / mentionCombination.size()));
    }

    while (!labelCombinationQueue.isEmpty()) {
      LabelCombination firstLC = labelCombinationQueue.poll();
      TreeNode lcNode = new TreeNode(root, firstLC);
      for (String label : firstLC.getLabelCombination()) {
        TreeNode labelNode = new TreeNode(lcNode, label);
        lcNode.addChild(labelNode);
        List<KBEntity> candidateEntities = findEntities(label);
        for (KBEntity entity : candidateEntities) {
          labelNode.addChild(new TreeNode(labelNode, entity));
        }
      }
      root.addChild(lcNode);

      if (!labelCombinationQueue.isEmpty()) {
        LabelCombination secondLC = labelCombinationQueue.poll(); // TODO Jiang: if the scores of two lc are very approximate, then consider the whether all labels' weights are approximate 
        if (secondLC.getScore() * FACTOR >= firstLC.getScore()) { // only when the second label combination's score is larger than 0.2 times of first one, it is considered as a valid combination
          TreeNode lcNode1 = new TreeNode(root, secondLC);
          for (String label : secondLC.getLabelCombination()) {
            TreeNode labelNode = new TreeNode(lcNode1, label);
            List<KBEntity> findEntities = findEntities(label);
            for (KBEntity entity : findEntities) {
              labelNode.addChild(new TreeNode(labelNode, entity));
            }
          }
          root.addChild(lcNode1);
        } else {
          break;
        }
      }
    }

    output(root);
    return root;
  }

  /**
   * Using keyword to find the most similarity label and the probability that the label appears in an article as an anchor text
   * 
   * @param mention inputed by user, could be a word or a phrase
   * @return label in Label_Index
   * @throws IOException
   */
  private Pair<String, Double> findLabelAndProbability(String mention) throws IOException {
    DBObject match = _labelCollection.findOne(new BasicDBObject(DBConstants.LABEL, mention));
    if (match != null) {
      Pair<String, Double> labelProbPair = new Pair<String, Double>((String) match.get(DBConstants.LABEL), (double) match.get(DBConstants.PROBABILITY));
      return labelProbPair;
    }
    return null;
  }

  /**
   * Using label to find top k entities in Label_Entity_Index
   * 
   * @param label the label
   * @return topK entities
   * @throws IOException
   */
  private List<KBEntity> findEntities(String label) throws IOException {
    DBCursor cursor = _labelEntityCollection.find(new BasicDBObject(DBConstants.LABEL, label)).sort(new BasicDBObject(DBConstants.ASSOCIATION_STRENGTH, -1));
    List<KBEntity> entityList = new ArrayList<>();
    //    double lastScore = 0.0;
    int i = 0;
    if (_sLang.equals(_tLang)) {
      while (cursor.hasNext()) {
        if (i++ >= MAX_ENTITY_SIZE) {
          break;
        }
        DBObject next = cursor.next();
        double score = (Double) next.get(DBConstants.ASSOCIATION_STRENGTH);
        String entityName = (String) next.get(DBConstants.ENTITY);
        KBEntity entity = new KBEntity(entityName, score);
        entityList.add(entity);
      }
    } else {
      while (cursor.hasNext()) {
        if (i >= MAX_ENTITY_SIZE) {
          break;
        }
        DBObject next = cursor.next();
        double score = (Double) next.get(DBConstants.ASSOCIATION_STRENGTH);
        String entityName = (String) next.get(DBConstants.ENTITY);
        if (LanguageConstants.EN.equals(_sLang)) {
          DBObject match = _langlinksCollection.findOne(new BasicDBObject(DBConstants.TARGET_TITLE, entityName).append(DBConstants.SOURCE_LANGUAGE, _tLang));
          entityName = match == null ? "" : (String) match.get(DBConstants.SOURCE_TITLE);
        } else if (LanguageConstants.EN.equals(_tLang)) {
          DBObject match = _langlinksCollection.findOne(new BasicDBObject(DBConstants.SOURCE_TITLE, entityName).append(DBConstants.SOURCE_LANGUAGE, _sLang));
          entityName = match == null ? "" : (String) match.get(DBConstants.TARGET_TITLE);
        } else {
          DBObject match = _langlinksCollection.findOne(new BasicDBObject(DBConstants.SOURCE_TITLE, entityName));
          if (match != null) {
            String enEntity = (String) match.get(DBConstants.TARGET_TITLE);
            DBObject findOne = _langlinksCollection.findOne(new BasicDBObject(DBConstants.TARGET_TITLE, enEntity).append(DBConstants.SOURCE_LANGUAGE, _tLang));
            entityName = findOne == null ? "" : (String) findOne.get(DBConstants.SOURCE_TITLE);
          }
        }
        if ("" != entityName /* && lastScore <= score * FACTOR */) {
          i++;
          KBEntity entity = new KBEntity(entityName, score);
          entityList.add(entity);
        }
      }
    }
    return entityList;
  }

  private static void output(TreeNode root) {
    List<TreeNode> lcNodes = root.getChildren();
    int i = 1;
    for (TreeNode lcNode : lcNodes) {
      System.out.println("==============Label Combination " + i++ + "================");
      for (TreeNode labelNode : lcNode.getChildren()) {
        System.out.println("----------------------Label---------------------");
        System.out.println(labelNode.getData().toString());
        System.out.println("---------------Candidate Entities---------------");
        for (TreeNode entityNode : labelNode.getChildren()) {
          KBEntity entity = (KBEntity) entityNode.getData();
          System.out.println(entity.get_name() + " : " + entity.get_score());
        }
        System.out.println();
      }
    }

  }

  public static Comparator<LabelCombination> lcComparator = new Comparator<LabelCombination>() {

    @Override
    public int compare(LabelCombination lc1, LabelCombination lc2) {
      if (lc1.score - lc2.score > 0) {
        return -1;
      }
      if (lc1.score - lc2.score < 0) {
        return 1;
      }
      return 0;
    }
  };

}

class LabelCombination implements Comparable<LabelCombination> {

  Set<String> labelCombination;

  double score;

  public LabelCombination(Set<String> labelCombination, double score) {
    this.labelCombination = labelCombination;
    this.score = score;
  }

  public Set<String> getLabelCombination() {
    return labelCombination;
  }

  public double getScore() {
    return score;
  }

  @Override
  public int compareTo(LabelCombination other) {
    if (score > other.score) {
      return -1;
    }
    if (score < other.score) {
      return 1;
    }
    return 0;
  }
}
