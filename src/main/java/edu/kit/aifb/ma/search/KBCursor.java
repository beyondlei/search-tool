package edu.kit.aifb.ma.search;

/**
 * Cursor used to record the path of the graph, each cursor is connected with it's parent cursor, so that we can build the path.
 * 
 */
public class KBCursor implements Comparable<KBCursor> {
  private String _path;

  private KBEntity _justVisited;

  private KBEntity _origin;

  private KBCursor _parentCursor;

  private int _distance;

  private double _score;

  public KBCursor(KBEntity justVisited, KBEntity origin, KBCursor parentCursor, int distance, double score) {
    _path = (parentCursor == null ? "null" : parentCursor.get_path()) + "->" + justVisited.get_name();
    _justVisited = justVisited;
    _origin = origin;
    _parentCursor = parentCursor;
    _distance = distance;
    _score = score;
  }

  public KBCursor(KBEntity justVisited, KBCursor parentCursor) {
    _path = (parentCursor == null ? "null" : parentCursor.get_path()) + "->" + justVisited.get_name();
    _justVisited = justVisited;
    _origin = parentCursor.get_origin();
    _parentCursor = parentCursor;
    _distance = parentCursor.get_distance() + 1;
    _score = (parentCursor.get_score() + justVisited.get_score()) / (_distance * _distance);
  }

  public String get_path() {
    return _path;
  }

  public KBEntity get_justVisited() {
    return _justVisited;
  }

  public KBEntity get_origin() {
    return _origin;
  }

  public KBCursor get_parentCursor() {
    return _parentCursor;
  }

  public int get_distance() {
    return _distance;
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

    if (!(object instanceof KBCursor)) {
      return false;
    }

    KBCursor cursor = (KBCursor) object;
    boolean bool = _parentCursor == null ? cursor.get_parentCursor() == null : _parentCursor.equals(cursor.get_parentCursor());
    if (bool && _justVisited.equals(cursor.get_justVisited()) && _origin.equals(cursor.get_origin()) && _distance == cursor.get_distance() && _score == cursor.get_score()) {
      return true;
    }

    return false;
  }

  @Override
  public int compareTo(KBCursor other) {
    if (_score > other._score) {
      return -1;
    }
    if (_score < other._score) {
      return 1;
    }
    return 0;
  }

}
