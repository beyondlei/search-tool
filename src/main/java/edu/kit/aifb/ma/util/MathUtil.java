package edu.kit.aifb.ma.util;

import java.math.BigDecimal;

public class MathUtil {

  public static double round(double score, int scale) {
    if (Double.isInfinite(score) || Double.isNaN(score)) {
      score = 0.0;
    } else {
      BigDecimal bdScore = new BigDecimal(score);
      bdScore = bdScore.setScale(scale, BigDecimal.ROUND_HALF_UP);
      score = Double.parseDouble(bdScore.toString());
    }
    return score;
  }
}
