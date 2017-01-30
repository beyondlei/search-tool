package edu.kit.aifb.ma.search;

import java.util.regex.Pattern;

import org.json.JSONArray;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

import edu.kit.aifb.ma.constant.DBConstants;
import edu.kit.aifb.ma.constant.LanguageConstants;
import edu.kit.aifb.ma.util.MongoResource;

public class LabelSuggestion {
  // db.LabelIndex_DE.find({label: {$regex:/^Barack.*/}}).sort({probability:-1}).limit(15)
  String regex;

  DBCollection collection;

  public LabelSuggestion(String inputText, String lang) {
    regex = "^" + inputText + ".*";
    if (LanguageConstants.EN.equals(lang)) {
      collection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.LABEL_COLLECTION_EN);
    } else if (LanguageConstants.DE.equals(lang)) {
      collection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.LABEL_COLLECTION_DE);
    } else if (LanguageConstants.ZH.equals(lang)) {
      collection = MongoResource.INSTANCE.getDB().getCollection(DBConstants.LABEL_COLLECTION_ZH);
    }
  }

  public JSONArray getTop15Labels() {
    JSONArray top15Labels = new JSONArray();
    Pattern pattern = Pattern.compile(regex);
    DBCursor cursor = collection.find(new BasicDBObject("label", pattern)).sort(new BasicDBObject("probability", -1)).limit(15);
    while (cursor.hasNext()) {
      DBObject next = cursor.next();
      String label = next.get("label").toString();
      top15Labels.put(label);
    }

    return top15Labels;
  }

  public static void main(String[] args) {
    LabelSuggestion labelSuggestion = new LabelSuggestion("Barack", "de");
    labelSuggestion.getTop15Labels();

  }

}
