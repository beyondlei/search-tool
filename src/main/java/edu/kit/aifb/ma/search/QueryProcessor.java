package edu.kit.aifb.ma.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.kit.aifb.ma.constant.CharacterConstants;

/**
 * Split keywords into different segments, e.g. K1 K2 K3 K4 -> Segment1: K1|K2|K3|K4 Segment2: K1|K2|K3 K4 Segment3: K1|K2 K3|K4 Segment4: K1|K2 K3 K4
 * Segment5: K1 K2|K3|K4 Segment6: K1 K2|K3 K4 Segment7: K1 K2 K3|K4 Segment8: K1 K2 K3 K4
 * 
 */
public class QueryProcessor {

  private static final String SEPARATOR = ":";

  private final static String SPLITTER = "[\\s\\{\\}\\(\\)\"\'\\.\\,\\;\\:\\-\\_\\/\\[\\]]";

  public static void main(String[] args) {
    String testStr1 = "New South Wales";
    String plainText = preprocess(testStr1);
    getNGrams(plainText);
    getMentionCombinations(plainText);
  }

  /**
   * Remove all other characters except word.
   * 
   * @param rawContent
   * @return the plain text in the form of " word1 word2 word3 ... "
   */
  protected static String preprocess(String rawContent) {
    String plainText = CharacterConstants.EMPTY_STRING;
    String decoratedContent = "[" + rawContent + "]";
    Pattern p = Pattern.compile(SPLITTER);
    Matcher m = p.matcher(decoratedContent);
    Vector<Integer> matchIndexes = new Vector<Integer>();
    while (m.find()) {
      matchIndexes.add(m.start());
    }
    for (int i = 0; i < matchIndexes.size() - 1; i++) {
      if (matchIndexes.get(i) - matchIndexes.get(i + 1) == -1) {
        continue;
      } else {
        String keyword = decoratedContent.substring(matchIndexes.get(i) + 1, matchIndexes.get(i + 1));
        keyword = keyword.substring(0, 1).toUpperCase() + keyword.substring(1);
        plainText = plainText + keyword + CharacterConstants.WHITESPACE;
      }
    }

    return plainText;
  }

  /**
   * Process the plain text to get all mention combinations with different sizes
   * 
   * @param plainText
   */
  protected static List<List<String>> getMentionCombinations(String plainText) {
  
    Pattern p = Pattern.compile("\\s");
    Matcher m = p.matcher(plainText);
    Vector<Integer> matchIndexes = new Vector<Integer>();
    while (m.find()) {
      matchIndexes.add(m.start());
    }
    String[] keywords = plainText.split(CharacterConstants.WHITESPACE);
    List<String> keywordsList = Arrays.asList(keywords);
    @SuppressWarnings("rawtypes")
    TreeMap<String, TreeMap> tm = new TreeMap<String, TreeMap>();
    buildTree(keywordsList, tm);
    List<List<String>> mentionCombinations = new ArrayList<>();
    iterateTree(tm, CharacterConstants.EMPTY_STRING, mentionCombinations);
    outputMCs(mentionCombinations);
    return mentionCombinations;
  }

  /**
   * Generate the nGrams
   * 
   * @param num the size of a segment
   * @param matchIndexes the index of all whitespace
   * @param plainText plain text
   * @return a segment with a given size
   */
  static Map<Integer, List<String>> getNGrams(String plainText) {
    plainText = " " + plainText;
    Pattern p = Pattern.compile("\\s");
    Matcher m = p.matcher(plainText);
    Vector<Integer> matchIndexes = new Vector<Integer>();
    while (m.find()) {
      matchIndexes.add(m.start());
    }
    String[] keywords = plainText.split(CharacterConstants.WHITESPACE);
    List<String> keywordsList = Arrays.asList(keywords);
    Map<Integer, List<String>> nGrams = new HashMap<Integer, List<String>>();
    for (int i = 1; i < keywordsList.size(); i++) {
      List<String> ngram = new ArrayList<String>();
      for (int j = 0; j < matchIndexes.size() - i; j++) {
        String keyword = plainText.substring(matchIndexes.get(j) + 1, matchIndexes.get(j + i));
        ngram.add(keyword);
      }
      nGrams.put(i, ngram);
    }
    return nGrams;
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static void iterateTree(TreeMap<String, TreeMap> tm, String last, List<List<String>> segments) {
    String prefix = last;
    for (String key : tm.keySet()) {
      String str = prefix + SEPARATOR + key;
      TreeMap<String, TreeMap> subTree = tm.get(key);
      if (!subTree.isEmpty()) {
        iterateTree(subTree, str, segments);
      } else {
        String[] keywords = (str.substring(1)).split(SEPARATOR);
        List<String> segment = Arrays.asList(keywords);
        segments.add(segment);
      }
    }
  }

  @SuppressWarnings("rawtypes")
  private static TreeMap<String, TreeMap> buildTree(List<String> keywordsList, TreeMap<String, TreeMap> tm) {
    for (int j = 1; j <= keywordsList.size(); j++) {
      List<String> start = keywordsList.subList(0, j);
      String[] array = new String[start.size()];
      start.toArray(array);
      String str = CharacterConstants.EMPTY_STRING;
      for (int i = 0; i < array.length; i++) {
        str = str + CharacterConstants.WHITESPACE + array[i];
      }
      List<String> end = keywordsList.subList(j, keywordsList.size());
      tm.put(str.trim(), buildTree(end, new TreeMap<String, TreeMap>()));
    }

    return tm;
  }

  private static void outputMCs(Collection<List<String>> collection) {
    int i = 1;
    for (List<String> seg : collection) {
      System.out.print("Mention Combination " + i++ + ": |");
      for (String key : seg) {
        System.out.print(key + "|");
      }
      System.out.println();
    }
  }

}
