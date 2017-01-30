package edu.kit.aifb.ma.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.openrdf.repository.RepositoryException;

import edu.kit.aifb.ma.util.DBPediaDSQuerier;
import edu.kit.aifb.ma.util.MathUtil;

/**
 * The class to generate top k subgraphs with the given max distance.
 * 
 */
public class TopKGraphsGenerator {
  private static final Logger _logger = Logger.getLogger(TopKGraphsGenerator.class);

  private static final String ENTITY_SEPARATOR = "*";

  private static final String EMPTY_STRING = "";

  private static final String SUBGRAPH = "subgraph";

  private static final String SCORE = "score";

  private static final String PATHS = "paths";

  private static final String ENTITIES = "entities";

  private static final String PROBABILITY_VECTOR = "probVector";

  private static final String HAS_SUBGRAPH = "hasSubgraph";

  private static final int MAX_DISTANCE = 3;

  private static final int TOP_K = 5;

  private String s_lang;

  private String t_lang;

  private TreeNode _rootNode;

  private Set<TreeNode> _uniqueLabel = new HashSet<>();

  private Map<KBEntity, TreeNode> _entityWithLabel = new HashMap<>();

  private Map<KBEntity, Double> _entityWithProb = new HashMap<>();

  private Map<String, KBEntity> _visitedEntities = new HashMap<>(); // there could be two entities with same name but different scores, e.g. for input "United States"

  private Map<KBEntity, Queue<KBCursor>> _originWithCursors = new HashMap<>();

  private Map<Pair<String, String>, String> _soWithProperty = new HashMap<>();

  private Queue<Subgraph> _candidates = new PriorityQueue<Subgraph>(TOP_K, subgraphComparator);

  public JSONObject getSubgraphs(String rawContent, String s_lang, String t_lang) throws JSONException, RepositoryException, IOException {
    long start = System.currentTimeMillis();
    this.s_lang = s_lang;
    this.t_lang = t_lang;
    Queue<Subgraph> topK = new PriorityQueue<Subgraph>(TOP_K, subgraphComparator);

    init(rawContent);

    topK = exploreCursor();

    if (!topK.isEmpty()) {
      JSONObject json = new JSONObject();
      json.put(HAS_SUBGRAPH, true);
      JSONObject probJSON = new JSONObject();
      for (Subgraph subgraph : topK) {
        for (KBEntity entity : subgraph.get_entities()) {
          probJSON.put(entity.get_name(), _entityWithProb.get(entity));
        }
      }
      json.put(PROBABILITY_VECTOR, probJSON);
      for (Subgraph subgraph : topK) {
        JSONObject subgraphJSON = new JSONObject();
        String entities = EMPTY_STRING;
        for (KBEntity entity : subgraph.get_entities()) {
          entities += (entity.get_name() + ENTITY_SEPARATOR);
        }
        if (!entities.isEmpty()) {
          subgraphJSON.put(ENTITIES, entities.substring(0, entities.length() - 1));
        }
        subgraphJSON.put(PATHS, subgraph.get_paths());
        subgraphJSON.put(SCORE, subgraph.get_score());
        json.append(SUBGRAPH, subgraphJSON);
      }
      long stop = System.currentTimeMillis();
      System.out.println(json);
      System.out.println("The total searching time is: " + (stop - start) + " ms.");
      return json;
    } else {
      JSONObject json = new JSONObject();
      if (_rootNode == null || _rootNode != null && _rootNode.getChildren().isEmpty()) {
        json.put(HAS_SUBGRAPH, false);
        System.out.println(json);
        return json;
      } else {
        json.put(HAS_SUBGRAPH, true);
        List<TreeNode> lcNodes = _rootNode.getChildren();
        for (TreeNode lcNode : lcNodes) {
          JSONObject subgraphJSON = new JSONObject();
          JSONObject probJSON = new JSONObject();
          List<KBEntity> entitiesInSubgraph = new ArrayList<>();
          String entityString = EMPTY_STRING;
          String pathString = EMPTY_STRING;
          double score = 0.0;
          List<TreeNode> labelNodes = lcNode.getChildren();
          for (TreeNode labelNode : labelNodes) {
            entitiesInSubgraph.add((KBEntity) labelNode.getChildren().get(0).getData());
          }
          for (KBEntity entity : entitiesInSubgraph) {
            score += entity.get_score();
            probJSON.put(entity.get_name(), entity.get_score());
            entityString += (entity.get_name() + ENTITY_SEPARATOR);
            pathString += ("\"" + entity.get_name() + "\";");
          }
          subgraphJSON.put(ENTITIES, entityString.substring(0, entityString.length() - 1));
          subgraphJSON.put(PATHS, pathString);
          subgraphJSON.put(SCORE, score);
          json.append(SUBGRAPH, subgraphJSON);
          json.put(PROBABILITY_VECTOR, probJSON);
        }
        System.out.println(json);
        long stop = System.currentTimeMillis();
        System.out.println("The total searching time is: " + (stop - start) + " ms.");
        return json;
      }
    }
  }

  /**
   * @param rawContent
   */
  public void init(String rawContent) {
    try {
      _rootNode = new LabelCombinationParser(s_lang, t_lang).getLabelCombinationWithEntities(rawContent);
      for (TreeNode lcNode : _rootNode.getChildren()) {
        if (lcNode.getChildren().size() == 1) {
          _uniqueLabel.add(lcNode.getChildren().get(0));
        }
        for (TreeNode labelNode : lcNode.getChildren()) {
          for (TreeNode entityNode : labelNode.getChildren()) {
            _entityWithLabel.put((KBEntity) entityNode.getData(), labelNode);
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    for (TreeNode lcNode : _rootNode.getChildren()) {
      for (TreeNode labelNode : lcNode.getChildren()) {
        for (TreeNode entityNode : labelNode.getChildren()) {
          Queue<KBCursor> cursorPQ = new PriorityQueue<KBCursor>(TOP_K, cursorComparator);
          KBEntity entity = (KBEntity) entityNode.getData();
          _entityWithProb.put(entity, MathUtil.round(entity.get_score(), 10));
          KBCursor cursor = new KBCursor(entity, entity, null, 1, MathUtil.round(entity.get_score(), 10)); // TODO Jiang: score calculation
          cursorPQ.add(cursor);
          _originWithCursors.put(entity, cursorPQ);
        }
      }
    }
  }

  /**
   * During exploration, the cursor with highest score created so far is selected for further expansion.
   * 
   * @throws RepositoryException
   * @throws IOException
   */
  private Queue<Subgraph> exploreCursor() throws RepositoryException, IOException {
    int j = 0;
    while (!isCursorPQEmpty()) {
      ++j;
      KBCursor maxScoreCursor = getMaxScoreCursor();
      KBEntity origin = maxScoreCursor.get_origin();
      Queue<KBCursor> cursorPQ = _originWithCursors.get(origin);
      cursorPQ.remove(maxScoreCursor);
      if (maxScoreCursor.get_distance() <= MAX_DISTANCE) {
        KBEntity currentEntity = maxScoreCursor.get_justVisited();
        String entityName = currentEntity.get_name();
        if (!_visitedEntities.keySet().contains(entityName)) {
          // add new cursor for each neighbor and put them into the corresponding cursor queue
          //          if (!neighbors.isEmpty()) {
          for (KBEntity neighbor : getNeighbors(currentEntity, maxScoreCursor.get_parentCursor())) {
            cursorPQ.add(new KBCursor(neighbor, maxScoreCursor));
          }
          //          }
          currentEntity.addCursor(origin, maxScoreCursor);
          _visitedEntities.put(entityName, currentEntity);
        }

        // assign the current cursor to the corresponding entity 
        else {
          _visitedEntities.get(entityName).addCursor(origin, maxScoreCursor);
        }

        if (isTopKFound(_visitedEntities.get(entityName))) {
          _logger.debug(j + " cursors have been explored......");
          return _candidates;
        }
      }
    }
    _logger.debug("All " + j + " cursors have been explored......");
    return _candidates;
  }

  /**
   * Get all neighbors of the given entity, except the parent entity of this given entity.
   * 
   * @param entity
   * @param parentCursor
   * @return
   * @throws RepositoryException
   * @throws IOException
   */
  private Set<KBEntity> getNeighbors(KBEntity entity, KBCursor parentCursor) throws RepositoryException, IOException {
    DBPediaDSQuerier querier = new DBPediaDSQuerier();
    Set<KBEntity> neighbors = new HashSet<KBEntity>();
    String entityName = entity.get_name();
    Set<Triple<String, String, Double>> poTriples = querier.queryPOFromMongoDB(entityName, t_lang); // get neighbors from DBpedia
    for (Triple<String, String, Double> poTriple : poTriples) {
      String object = poTriple.o;
      double neighborScore = MathUtil.round(poTriple.rs * entity.get_score(), 10);
      KBEntity neighbor = new KBEntity(object, neighborScore); // TODO Jiang: score calculation
      if (!_entityWithProb.containsKey(neighbor)) {
        _entityWithProb.put(neighbor, neighborScore);
      } else {
        double score = MathUtil.round((_entityWithProb.get(neighbor) + neighborScore) / 2, 10);
        _entityWithProb.put(neighbor, score);
      }
      _soWithProperty.put(new Pair<String, String>(entityName, object), poTriple.p);
      if (parentCursor != null) {
        if (!parentCursor.get_justVisited().get_name().equals(neighbor.get_name())) {
          neighbors.add(neighbor);
        }
      } else {
        neighbors.add(neighbor);
      }
    }
    return neighbors;
  }

  /**
   * The top-k algorithm
   * 
   * @param currentEntity
   * @return
   */
  private boolean isTopKFound(KBEntity currentEntity) {
    Queue<Subgraph> subgraphs = new PriorityQueue<Subgraph>(TOP_K, subgraphComparator);
    subgraphs = buildSubgraphs(currentEntity);
    if (!subgraphs.isEmpty()) {
      if (!_candidates.isEmpty()) {
        Queue<Subgraph> candidates = new PriorityQueue<Subgraph>(TOP_K, subgraphComparator);
        candidates.addAll(_candidates);
        // avoid adding a superset of a subgraph of current candidates
        while (!subgraphs.isEmpty()) {
          Subgraph subgraph = subgraphs.poll();
          int i = 0;
          for (Subgraph candidate : candidates) {
            if (!subgraph.get_cursors().containsAll(candidate.get_cursors()) && !candidate.get_cursors().containsAll(subgraph.get_cursors())) {
              i++;
            }
          }
          if (i == candidates.size()) {
            _candidates.add(subgraph);
            if (_candidates.size() >= TOP_K) {
              Subgraph[] candidateArray = new Subgraph[_candidates.size()];
              _candidates.toArray(candidateArray);
              double lowScore = candidateArray[TOP_K - 1].get_score();
              double highScore = 0.0;
              if (!isCursorPQEmpty()) {
                highScore = getMaxScoreCursor().get_score();
              }
              if (lowScore >= highScore) {
                return true;
              }
            }
          }
        }
      } else {
        _candidates.addAll(subgraphs);
      }
    }

    return false;
  }

  /**
   * Build subgraph if it is existed. Firstly, using the given vertex to check whether there exist one or more paths connected by this vertex.
   * Secondly, if paths existed, check whether these paths can construct a subgraph with the standard that the origins of these paths should contain
   * all keywords.
   * 
   * @param currentEntity
   * @return
   */
  private Queue<Subgraph> buildSubgraphs(KBEntity currentEntity) {
    Queue<Subgraph> subgraphs = new PriorityQueue<Subgraph>(TOP_K, subgraphComparator);
    Map<TreeNode, List<KBCursor>> originLabelWithCursors = new HashMap<>();
    Map<KBEntity, List<KBCursor>> originWithCursors = currentEntity.get_cursors();

    for (TreeNode lcNode : _rootNode.getChildren()) {
      if (lcNode.getChildren().size() == 1) {
        if (originWithCursors.size() == 1) {
          KBEntity origin = originWithCursors.keySet().iterator().next();
          List<KBCursor> cursors = originWithCursors.get(origin);
          if (_uniqueLabel.contains(_entityWithLabel.get(origin)) && cursors.size() == 1) {
            KBCursor cursor = cursors.get(0);
            if (cursor.get_distance() == 1) {
              Set<KBCursor> validCursors = new HashSet<>();
              validCursors.add(cursor);
              Subgraph subgraph = new Subgraph(validCursors, currentEntity);
              if (!subgraphs.contains(subgraph)) {
                subgraphs.add(subgraph);
              }
            }
          }
        }
      } else if (lcNode.getChildren().size() > 1) {
        for (KBEntity entity : originWithCursors.keySet()) {
          originLabelWithCursors.put(_entityWithLabel.get(entity), originWithCursors.get(entity));
        }
        if (originLabelWithCursors.keySet().size() >= lcNode.getChildren().size()) {
          Set<KBCursor> validCursors = new HashSet<>();
          for (TreeNode labelNode : lcNode.getChildren()) {
            validCursors.addAll(originLabelWithCursors.get(labelNode));
          }
          Subgraph subgraph = new Subgraph(validCursors, currentEntity);
          if (!subgraphs.contains(subgraph)) {
            subgraphs.add(subgraph);
          }
        }
      }
    }

    //    if (originWithCursors.size() == 1) {
    //      if (!_uniqueLabel.isEmpty()) {
    //        KBEntity origin = originWithCursors.keySet().iterator().next();
    //        List<KBCursor> cursors = originWithCursors.get(origin);
    //        if (_uniqueLabel.contains(_entityWithLabel.get(origin)) && cursors.size() == 1) {
    //          //          for (KBCursor cursor : cursors) {
    //          KBCursor cursor = cursors.get(0);
    //          if (cursor.get_distance() == 1) {
    //            Set<KBCursor> validCursors = new HashSet<>();
    //            validCursors.add(cursor);
    //            Subgraph subgraph = new Subgraph(validCursors, currentEntity);
    //            if (!subgraphs.contains(subgraph)) {
    //              subgraphs.add(subgraph);
    //            }
    //          }
    //        }
    //        //          }
    //      }
    //    } else {
    //      for (KBEntity entity : originWithCursors.keySet()) {
    //        TreeNode label = _entityWithLabel.get(entity);
    //        originLabels.add(label);
    //        if (!originLabelWithCursors.keySet().contains(label)) {
    //          Set<List<KBCursor>> cursors = new HashSet<>();
    //          cursors.add(originWithCursors.get(entity));
    //          originLabelWithCursors.put(label, cursors);
    //        } else {
    //          originLabelWithCursors.get(label).add(originWithCursors.get(entity));
    //        }
    //      }
    //      for (TreeNode lcNode : _rootNode.getChildren()) {
    //        if (originLabels.containsAll(lcNode.getChildren())) {
    //          if (lcNode.getChildren().size() == 1) {
    //
    //          } else {
    //
    //          }
    //          Set<KBCursor> validCursors = new HashSet<>();
    //          for (TreeNode labelNode : lcNode.getChildren()) {
    //            validCursors.addAll(originLabelWithCursors.get(labelNode));
    //          }
    //          Subgraph subgraph = new Subgraph(validCursors, currentEntity);
    //          if (!subgraphs.contains(subgraph)) {
    //            subgraphs.add(subgraph);
    //          }
    //        }
    //      }
    //    }
    if (subgraphs.size() > 1) {
      System.out.println(subgraphs.size() + " subgraphs have been found!");
    }
    return subgraphs;
  }

  private boolean isCursorPQEmpty() {
    for (Queue<KBCursor> cursorPQ : _originWithCursors.values()) {
      if (!cursorPQ.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  /**
   * Retrieve the cursor from all available cursors with the maximum score.
   * 
   * @return
   */
  private KBCursor getMaxScoreCursor() {
    Queue<KBCursor> cursorPQ = new PriorityQueue<KBCursor>(TOP_K, cursorComparator);
    for (Queue<KBCursor> cursors : _originWithCursors.values()) {
      if (cursors.size() > 0) {
        cursorPQ.add(cursors.peek());
      }
    }
    return cursorPQ.peek();
  }

  public static Comparator<KBCursor> cursorComparator = new Comparator<KBCursor>() {

    @Override
    public int compare(KBCursor c1, KBCursor c2) {
      if (c1.get_score() - c2.get_score() > 0) {
        return -1;
      }
      if (c1.get_score() - c2.get_score() < 0) {
        return 1;
      }
      return 0;
    }
  };

  public static Comparator<Subgraph> subgraphComparator = new Comparator<Subgraph>() {

    @Override
    public int compare(Subgraph s1, Subgraph s2) {
      if (s1.get_score() - s2.get_score() > 0) {
        return -1;
      }
      if (s1.get_score() - s2.get_score() < 0) {
        return 1;
      }
      return 0;
    }
  };

  class Subgraph implements Comparable<Subgraph> {
    private String _paths = EMPTY_STRING;

    private double _score;

    private Set<KBEntity> _entities = new HashSet<>();

    private Set<KBCursor> _cursors = new HashSet<>();

    private KBEntity _currentEntity = null;

    public Subgraph(Set<KBCursor> cursors, KBEntity currentEntity) {
      _cursors = cursors;
      _currentEntity = currentEntity;
      calPaths();
      calScore();
    }

    private void calPaths() {
      StringBuilder sb = new StringBuilder();
      if (_cursors.size() > 1) {
        for (KBCursor cursor : _cursors) {
          if (cursor.get_parentCursor() != null) {
            // remove the cursor to avoid duplication among subgraphs
            Map<KBEntity, List<KBCursor>> originWithCursors = _currentEntity.get_cursors();
            List<KBCursor> cursors = originWithCursors.get(cursor.get_origin());
            cursors.remove(cursor);
            if (cursors.isEmpty()) {
              originWithCursors.remove(cursor.get_origin());
            }

            String path = cursor.get_path();
            path = path.substring(path.indexOf(">") + 1, path.length());
            String[] resources = path.split("->");
            for (int i = 0; i < resources.length - 1; i++) {
              //              _entities.add(resources[i]);
              String prop = _soWithProperty.get(new Pair<String, String>(resources[i], resources[i + 1]));
              String propText = prop.substring(prop.lastIndexOf("/") + 1, prop.length()).replace("_", " ");
              sb.append("\"" + resources[i] + "\"->\"" + resources[i + 1] + "\"[label=\"" + propText + "\"];");
            }
            //            if (!_entities.contains(resources[resources.length - 1])) {
            //              _entities.add(resources[resources.length - 1]);
            //            }
            while (cursor.get_parentCursor() != null) {
              cursor = cursor.get_parentCursor();
              _entities.add(cursor.get_justVisited());
            }
          }
        }
        _entities.add(_currentEntity);
      } else {
        for (KBCursor cursor : _cursors) {
          String path = cursor.get_path();
          path = path.substring(path.indexOf(">") + 1, path.length());
          String[] resources = path.split("->");
          if (resources.length == 1) {
            sb.append("\"" + resources[0] + "\";");
            //            _entities.add(resources[0]);
            _entities.add(_currentEntity);
          }
        }
      }
      _paths = sb.toString();
    }

    private void calScore() {
      for (KBCursor cursor : _cursors) {
        _score += cursor.get_score();
      }
      _score /= _cursors.size();
    }

    public Set<KBEntity> get_entities() {
      return _entities;
    }

    public Set<KBCursor> get_cursors() {
      return _cursors;
    }

    public String get_paths() {
      return _paths.toString();
    }

    public double get_score() {
      return _score;
    }

    @Override
    public int hashCode() {
      return 1;
    }

    @Override
    public boolean equals(Object object) {
      if (this == object) {
        return true;
      }
      if (object == null) {
        return false;
      }
      if (!(object instanceof Subgraph)) {
        return false;
      }

      Subgraph subgraph = (Subgraph) object;

      if (_cursors.containsAll(subgraph._cursors) && (subgraph._cursors).containsAll(_cursors)) {
        return true;
      }

      return false;
    }

    @Override
    public int compareTo(Subgraph other) {
      if (_score > other._score) {
        return -1;
      }
      if (_score < other._score) {
        return 1;
      }
      return 0;
    }
  }
}
