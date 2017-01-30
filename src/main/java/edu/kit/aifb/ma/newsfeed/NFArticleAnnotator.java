package edu.kit.aifb.ma.newsfeed;

import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.dom.DOMSource;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Parse entities from the annotated text.
 * 
 */
public class NFArticleAnnotator {

  private static final String EMPTY_STRING = "";

  private static final String WIKIFIED_DOCUMENT = "WikifiedDocument";

  private static final String DISPLAY_NAME = "displayName";

  private static final String WEIGHT = "weight";

  private static final String DETECTED_TOPIC = "DetectedTopic";

  private static final String DETECTED_TOPICS = "DetectedTopics";

  private DOMSource _ds;

  private static Logger _logger = Logger.getLogger(NFArticleAnnotator.class);

  public NFArticleAnnotator(DOMSource ds) {
    _ds = ds;
  }

  /**
   * Parse the DOM Source and retrieve the annotated text
   * 
   * @param ds DOM Source
   * @return
   */
  public String retrieveAnnotatedText() {
    Node node = _ds.getNode();
    NodeList annotatedTextNodeList = null;
    if (node.getNodeType() == Node.ELEMENT_NODE) {
      Element element = (Element) node;
      annotatedTextNodeList = element.getElementsByTagName(WIKIFIED_DOCUMENT);
    }
    if (annotatedTextNodeList.getLength() > 0) {
      return annotatedTextNodeList.item(0).getTextContent();
    }
    return EMPTY_STRING;
  }

  /**
   * Parse the DOM Source and retrieve all annotations and the corresponding number of them
   * 
   * @param ds DOM Source
   * @return
   */
  public Map<String, Integer> retrieveAnnotationWithNum(String annotatedText) {
    Map<String, Integer> annotationWithNum = new HashMap<>();
    Node node = _ds.getNode();
    NodeList topics = null;
    if (node.getNodeType() == Node.ELEMENT_NODE) {
      Element element = (Element) node;
      topics = element.getElementsByTagName(DETECTED_TOPICS);
    }
    Element topicsElement = (Element) topics.item(0) != null ? (Element) topics.item(0) : null;
    if (topicsElement != null) {
      NodeList topic = topicsElement.getElementsByTagName(DETECTED_TOPIC);
      if (topic.getLength() > 0) {
        for (int i = 0; i < topic.getLength(); i++) {
          Node thisTopic = topic.item(i);
          String annotation = getAttribute(thisTopic, DISPLAY_NAME);
          if (annotatedText.contains("[[" + annotation + "]]") || annotatedText.contains("[[" + annotation + "|")) {
            annotationWithNum.put(annotation, countMatches(annotatedText, "[[" + annotation + "]]") + countMatches(annotatedText, "[[" + annotation + "|"));
          }
        }
      } else {
        _logger.warn("The node <DetectedTopic> is not exist!!!");
      }
    } else {
      _logger.warn("The node <DetectedTopics> is not exist!!!");
    }
    return annotationWithNum;
  }

  public Map<String, Double> retrieveAnnotationWithWeight(String annotatedText) {
    Map<String, Double> annotationWithWeight = new HashMap<>();
    Node node = _ds.getNode();
    NodeList topics = null;
    if (node.getNodeType() == Node.ELEMENT_NODE) {
      Element element = (Element) node;
      topics = element.getElementsByTagName(DETECTED_TOPICS);
    }
    Element topicsElement = (Element) topics.item(0) != null ? (Element) topics.item(0) : null;
    if (topicsElement != null) {
      NodeList topic = topicsElement.getElementsByTagName(DETECTED_TOPIC);
      if (topic.getLength() > 0) {
        for (int i = 0; i < topic.getLength(); i++) {
          Node thisTopic = topic.item(i);
          String annotation = getAttribute(thisTopic, DISPLAY_NAME);
          double weight = Double.parseDouble(getAttribute(thisTopic, WEIGHT));
          if (annotatedText.contains("[[" + annotation + "]]") || annotatedText.contains("[[" + annotation + "|")) {
            annotationWithWeight.put(annotation, weight);
          }
        }
      } else {
        _logger.warn("The node <DetectedTopic> is not exist!!!");
      }
    } else {
      _logger.warn("The node <DetectedTopics> is not exist!!!");
    }
    return annotationWithWeight;
  }

  /**
   * Counts how many times the substring appears in the larger String. A <code>null</code> or empty ("") String input returns <code>0</code>.
   * 
   * @param str the String to check, may be null
   * @param sub the substring to count, may be null
   * @return the number of occurrences, 0 if either String is <code>null</code>
   */
  public static int countMatches(String str, String sub) {
    if (isEmpty(str) || isEmpty(sub)) {
      return 0;
    }
    int count = 0;
    int idx = 0;
    while ((idx = str.indexOf(sub, idx)) != -1) {
      count++;
      idx += sub.length();
    }
    return count;
  }

  /**
   * Checks if a String is empty ("") or null.
   * 
   * @param str the String to check, may be null
   * @return <code>true</code> if the String is empty or null
   */
  public static boolean isEmpty(String str) {
    return str == null || str.length() == 0;
  }

  /**
   * Get attribute value by name
   * 
   * @param topic a node element represents a topic
   * @param name attribute name
   * @return attribute value
   */
  private static String getAttribute(Node topic, String name) {
    if (topic != null && topic.getNodeType() == Node.ELEMENT_NODE) {
      Element topicElement = (Element) topic;
      return topicElement.getAttribute(name);
    }
    return null;
  }

}
