package edu.kit.aifb.ma.newsfeed;

import java.util.List;

/**
 * Data template of article.
 * 
 */
public class NFArticle {
  private String _articleID;

  private String _srcName;

  private String _hostName;

  private String _longitude;

  private String _latitude;

  private String _country;

  private String _srcTags;

  private String _tags;

  private String _feedTitle;

  private String _feedUri;

  private String _uri;

  private String _publishDate;

  private String _retrievedDate;

  private String _lang;

  private String _storyID;

  private String _img;

  private String _title;

  private String _bodyClearText;

  private List<String> _paragraphs;

  public String get_articleID() {
    return _articleID;
  }

  public void set_articleID(String articleID) {
    _articleID = articleID;
  }

  public String get_srcName() {
    return _srcName;
  }

  public void set_srcName(String srcName) {
    _srcName = srcName;
  }

  public String get_hostname() {
    return _hostName;
  }

  public void set_hostname(String hostname) {
    _hostName = hostname;
  }

  public String get_longitude() {
    return _longitude;
  }

  public void set_longitude(String longitude) {
    _longitude = longitude;
  }

  public String get_latitude() {
    return _latitude;
  }

  public void set_latitude(String latitude) {
    _latitude = latitude;
  }

  public String get_country() {
    return _country;
  }

  public void set_country(String country) {
    _country = country;
  }

  public String get_srcTags() {
    return _srcTags;
  }

  public void set_srcTags(String tags) {
    _srcTags = tags;
  }

  public String get_tags() {
    return _tags;
  }

  public void set_tags(String tags) {
    _tags = tags;
  }

  public String get_feedTitle() {
    return _feedTitle;
  }

  public void set_feedTitle(String feedTitle) {
    _feedTitle = feedTitle;
  }

  public String get_feedUri() {
    return _feedUri;
  }

  public void set_feedUri(String feedUri) {
    _feedUri = feedUri;
  }

  public String get_uri() {
    return _uri;
  }

  public void set_uri(String uri) {
    _uri = uri;
  }

  public String get_publishDate() {
    return _publishDate;
  }

  public void set_publishDate(String publishDate) {
    _publishDate = publishDate;
  }

  public String get_retrievedDate() {
    return _retrievedDate;
  }

  public void set_retrievedDate(String retrievedDate) {
    _retrievedDate = retrievedDate;
  }

  public String get_lang() {
    return _lang;
  }

  public void set_lang(String lang) {
    _lang = lang;
  }

  public String get_storyID() {
    return _storyID;
  }

  public void set_storyID(String storyID) {
    _storyID = storyID;
  }

  public String get_img() {
    return _img;
  }

  public void set_img(String img) {
    _img = img;
  }

  public String get_title() {
    return _title;
  }

  public void set_title(String title) {
    _title = title;
  }

  public String get_bodyClearText() {
    return _bodyClearText;
  }

  public void set_bodyClearText(String bodyClearText) {
    _bodyClearText = bodyClearText;
  }

  public List<String> get_paragraphs() {
    return _paragraphs;
  }

  public void set_paragraphs(List<String> paragraphs) {
    _paragraphs = paragraphs;
  }

}
