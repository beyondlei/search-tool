package edu.kit.aifb.ma.newsfeed;

/**
 * This class used to store the mapping information between entity name and article id.
 * 
 */
public class Annotation implements Comparable<Annotation> {
  private String _id;

  private String _name;

  private double _weight;

  public Annotation(String id, String name, double weight) {
    _id = id;
    _name = name;
    _weight = weight;
  }

  public String get_id() {
    return _id;
  }

  public String get_name() {
    return _name;
  }

  public double get_weight() {
    return _weight;
  }

  @Override
  public int compareTo(Annotation entity) {
    double compareScore = entity.get_weight();
    double scoreDiff = compareScore - _weight;
    if (scoreDiff > 0) {
      return 1;
    } else if (scoreDiff < 0) {
      return -1;
    } else {
      return 0;
    }
  }
}
