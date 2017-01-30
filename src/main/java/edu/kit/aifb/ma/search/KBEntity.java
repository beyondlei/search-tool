package edu.kit.aifb.ma.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represent the vertex in the graph.
 * 
 */
public class KBEntity {
  private String _name;

  private double _score;

  private Map<KBEntity, List<KBCursor>> _cursors = new HashMap<>();

  public KBEntity(String name, double score) {
    _name = name;
    _score = score;
  }

  public void addCursor(KBEntity origin, KBCursor cursor) {
    if (_cursors.containsKey(origin)) {
      _cursors.get(origin).add(cursor);
    } else {
      List<KBCursor> cursors = new ArrayList<>();
      cursors.add(cursor);
      _cursors.put(origin, cursors);
    }
  }

  public Map<KBEntity, List<KBCursor>> get_cursors() {
    return _cursors;
  }

  public String get_name() {
    return _name;
  }

  public double get_score() {
    return _score;
  }

  @Override
  public int hashCode() {
    return _name.hashCode();
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }

    if (object == null) {
      return false;
    }

    if (!(object instanceof KBEntity)) {
      return false;
    }

    KBEntity entity = (KBEntity) object;

    if (_name.equals(entity.get_name()) && _score == entity.get_score()) {
      return true;
    }

    return false;
  }

}
