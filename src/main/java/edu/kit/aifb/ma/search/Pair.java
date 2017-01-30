package edu.kit.aifb.ma.search;

public class Pair<T, U> {
  public final T t;

  public final U u;

  public Pair(T t, U u) {
    this.t = t;
    this.u = u;
  }

  @Override
  public int hashCode() {
    return 1;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (null == obj) {
      return false;
    }

    if (!(obj instanceof Pair)) {
      return false;
    }

    @SuppressWarnings("unchecked")
    Pair<T, U> pair = (Pair<T, U>) obj;

    if (t.equals(pair.t) && u.equals(pair.u)) {
      return true;
    }

    return false;
  }
}