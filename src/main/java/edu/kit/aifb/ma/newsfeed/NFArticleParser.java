package edu.kit.aifb.ma.newsfeed;

import edu.kit.aifb.ma.constant.NodeConstants;
import edu.kit.aifb.ma.constant.LanguageConstants;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Parse ijs news file for retrieving required information of an article.
 * 
 */
public class NFArticleParser {

  private static final String EMPTY_STRING = "";

  private Logger _logger = Logger.getLogger(NFArticleParser.class);

  private Document _doc;

  public static void main(String[] args) {
    NFArticleParser parser = new NFArticleParser(new File("/Users/zhongwenjiang/workspace/AnnotationBasedIRS/src/main/resources/public-news-2014-08-19T10-11-15.xml"));
    parser.parseArticleNode();
  }

  /**
   * Initialize a Document instance using the given XML file.
   * 
   * @param xmlFile
   */
  public NFArticleParser(File xmlFile) {
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    try {
      _doc = dbFactory.newDocumentBuilder().parse(xmlFile);
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
    } catch (SAXException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Parse the nodes <Article>.
   */
  public Map<String, NFArticle> parseArticleNode() {
    Map<String, NFArticle> idWithArticle = new HashMap<String, NFArticle>();
    NodeList articles = _doc.getElementsByTagName(NodeConstants.ARTICLE);
    if (articles.getLength() > 0) {
      for (int i = 0; i < articles.getLength(); i++) {
        Node node = articles.item(i);
        if (node.getNodeType() == Node.ELEMENT_NODE) {
          Element element = (Element) node;
          // process attribute "id", if exists, store all required information in an Article object.
          if (element.hasAttribute(NodeConstants.ID)) {
            NFArticle article = new NFArticle();
            if (LanguageConstants.ENG.equals(getLang(element)) || LanguageConstants.DEU.equals(getLang(element)) || LanguageConstants.ZHO.equals(getLang(element))) {
              _logger.info("Parsing node <article> with id = " + element.getAttribute(NodeConstants.ID) + ", language = " + getLang(element));
              article.set_articleID(element.getAttribute(NodeConstants.ID));
              parseSourceNode(element, article);
              element.removeChild(element.getElementsByTagName(NodeConstants.SOURCE).item(0));
              parseFeedNode(element, article);
              // delete the node <feed>, so that the following nodes <uri> and <title> can be parsed properly. 
              element.removeChild(element.getElementsByTagName(NodeConstants.FEED).item(0));
              parseGenericNode(element, NodeConstants.URI, article);
              parseGenericNode(element, NodeConstants.LANG, article);
              parseGenericNode(element, NodeConstants.PUBLISH_DATE, article);
              parseGenericNode(element, NodeConstants.RETRIEVED_DATE, article);
              parseTagsNode(element, article, false);
              parseGenericNode(element, NodeConstants.STORY_ID, article);
              parseGenericNode(element, NodeConstants.IMG, article);
              parseGenericNode(element, NodeConstants.TITLE, article);
              parseGenericNode(element, NodeConstants.BODY_CLEARTEXT, article);
              parseParagraphs(element, article);
              idWithArticle.put(element.getAttribute(NodeConstants.ID), article);
            }
          } else {
            _logger.warn("The node <article> has no id, it will be discarded!!!");
          }
        } else {
          _logger.warn("The node <article> is not an element type, it will be discarded!!!");
        }
      }
    } else {
      _logger.warn("The node <article> is an empty node!!!");
    }
    return idWithArticle;
  }

  private void parseParagraphs(Element parentElement, NFArticle article) {
    Node bcTextNode = null;
    if ((bcTextNode = parentElement.getElementsByTagName("body-cleartext").item(0)) != null) {
      NodeList paragraphs = bcTextNode.getChildNodes();
      List<String> paraList = new ArrayList<>();
      for (int i = 0; i < paragraphs.getLength(); i++) {
        Node paragraph = paragraphs.item(i);
        if (paragraph.getNodeType() == Node.ELEMENT_NODE) {
          Element element = (Element) paragraph;
          paraList.add(element.getTextContent());
        }
      }
      article.set_paragraphs(paraList);
    }
  }

  private String getLang(Element parentElement) {
    Node childNode;
    if ((childNode = parentElement.getElementsByTagName(NodeConstants.LANG).item(0)) != null) {
      return childNode.getTextContent();
    }
    return EMPTY_STRING;
  }

  /**
   * Parse the node <tags>, be caution please, it is not the sub node <tags> under <source>.
   * 
   * @param element
   * @param article
   */
  private void parseTagsNode(Element element, NFArticle article, boolean isSubNode) {
    Node tagsNode;
    if ((tagsNode = element.getElementsByTagName(NodeConstants.TAGS).item(0)) != null) {
      if (tagsNode.getNodeType() == Node.ELEMENT_NODE) {
        NodeList tags = tagsNode.getChildNodes();
        String tagsText = EMPTY_STRING;
        for (int j = 0; j < tags.getLength(); j++) {
          Node tagNode = tags.item(j);
          if (tagNode.getNodeType() == Node.ELEMENT_NODE) {
            tagsText = tagsText + "," + ((Element) tagNode).getTextContent();
          } else {
            _logger.warn("The node <tag> is not an element type, it will be discarded!!!");
          }
        }
        if (isSubNode) {
          article.set_srcTags(tags.getLength() > 0 ? tagsText.substring(1) : tagsText);
        } else {
          article.set_tags(tags.getLength() > 0 ? tagsText.substring(1) : tagsText);
        }
      } else {
        _logger.warn("The node <tags> is not an element type, it will be discarded!!!");
      }
    } else {
      _logger.warn("The node <tags> is not exist!!!");
    }
  }

  /**
   * Parse the node <source>
   * 
   * @param element
   * @param article
   */
  private void parseSourceNode(Element element, NFArticle article) {
    Node srcNode;
    if ((srcNode = element.getElementsByTagName(NodeConstants.SOURCE).item(0)) != null) {
      if (srcNode.getNodeType() == Node.ELEMENT_NODE) {
        Element srcElement = (Element) srcNode;
        parseGenericNode(srcElement, NodeConstants.NAME, article);
        parseGenericNode(srcElement, NodeConstants.HOSTNAME, article);
        Node locNode;
        if ((locNode = srcElement.getElementsByTagName(NodeConstants.LOCATION).item(0)) != null) {
          if (locNode.getNodeType() == Node.ELEMENT_NODE) {
            Element locElement = (Element) locNode;
            parseGenericNode(locElement, NodeConstants.LATITUDE, article);
            parseGenericNode(locElement, NodeConstants.LONGITUDE, article);
            parseGenericNode(locElement, NodeConstants.COUNTRY, article);
          }
        } else {
          _logger.warn("The node <location> is not exist!!!");
        }
        parseTagsNode(srcElement, article, true);
      } else {
        _logger.warn("The node <source> is not an element type, it will be discarded!!!");
      }
    } else {
      _logger.warn("The node <source> is not exist!!!");
    }
  }

  /**
   * Parse the node <feed>.
   * 
   * @param artElement
   * @param article
   */
  private void parseFeedNode(Element artElement, NFArticle article) {
    Node node;
    if ((node = artElement.getElementsByTagName(NodeConstants.FEED).item(0)) != null) {
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        Element element = (Element) node;
        Node titleNode;
        Node uriNode;
        if ((titleNode = element.getElementsByTagName(NodeConstants.TITLE).item(0)) != null) {
          article.set_feedTitle(titleNode.getTextContent());
        } else {
          _logger.warn("The sub node <title> under <feed> is not exist!!!");
        }
        if ((uriNode = element.getElementsByTagName(NodeConstants.URI).item(0)) != null) {
          article.set_feedUri(uriNode.getTextContent());
        } else {
          _logger.warn("The sub node <uri> under <feed> is not exist!!!");
        }
      } else {
        _logger.warn("The node <feed> is not an element type, it will be discarded!!!");
      }
    } else {
      _logger.warn("The node <feed> is not exist!!!");
    }
  }

  /**
   * Parse child node of the @parentElement and store the text content of the node into article.
   * 
   * @param parentElement
   * @param childNodeName
   * @param article
   */
  private void parseGenericNode(Element parentElement, String childNodeName, NFArticle article) {
    Node childNode;
    if ((childNode = parentElement.getElementsByTagName(childNodeName).item(0)) != null) {
      String childNodeText = childNode.getTextContent();
      switch (childNodeName) {
        case NodeConstants.ARTICLE_ID:
          article.set_articleID(childNodeText);
          break;
        case NodeConstants.NAME:
          article.set_srcName(childNodeText);
        case NodeConstants.HOSTNAME:
          article.set_hostname(childNodeText);
          break;
        case NodeConstants.LONGITUDE:
          article.set_longitude(childNodeText);
          break;
        case NodeConstants.LATITUDE:
          article.set_latitude(childNodeText);
          break;
        case NodeConstants.COUNTRY:
          article.set_country(childNodeText);
          break;
        case NodeConstants.URI:
          article.set_uri(childNodeText);
          break;
        case NodeConstants.PUBLISH_DATE:
          article.set_publishDate(parseDate(childNodeText));
          break;
        case NodeConstants.RETRIEVED_DATE:
          article.set_retrievedDate(parseDate(childNodeText));
          break;
        case NodeConstants.LANG:
          article.set_lang(childNodeText);
          break;
        case NodeConstants.STORY_ID:
          article.set_storyID(childNodeText);
          break;
        case NodeConstants.IMG:
          article.set_img(childNodeText);
          break;
        case NodeConstants.TITLE:
          article.set_title(childNodeText);
          break;
        case NodeConstants.BODY_CLEARTEXT:
          article.set_bodyClearText(childNodeText);
          break;
        default:
          break;
      }
    } else {
      _logger.warn("The node <" + childNodeName + "> is not exist!!!");
    }
  }

  private String parseDate(String dateString) {
    return dateString.substring(0, dateString.indexOf("T")).replaceAll("-", "");
  }

}
