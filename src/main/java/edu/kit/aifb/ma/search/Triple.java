package edu.kit.aifb.ma.search;

public class Triple<P, O, RS> {
  public final P p;

  public final O o;

  public final RS rs;

  public Triple(P t, O u, RS s) {
    this.p = t;
    this.o = u;
    this.rs = s;
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

    if (!(obj instanceof Triple)) {
      return false;
    }

    @SuppressWarnings({"unchecked"})
    Triple<P, O, RS> triple = (Triple<P, O, RS>) obj;

    if (p.equals(triple.p) && o.equals(triple.o) && rs.equals(triple.rs)) {
      return true;
    }

    return false;
  }
}