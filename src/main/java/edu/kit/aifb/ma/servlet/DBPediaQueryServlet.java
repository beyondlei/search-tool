package edu.kit.aifb.ma.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.*;

import edu.kit.aifb.ma.constant.LanguageConstants;
import edu.kit.aifb.ma.util.DBPediaDSQuerier;

public class DBPediaQueryServlet extends HttpServlet {

  private static final long serialVersionUID = 1L;

  private static final String EMPTY_STRING = "";

  private static final String TYPE = "type";

  private static final String SOURCE_LANG = "s_lang";

  private static final String ENTITY = "entity";

  private static final String IMAGES = "images";

  private static final String SHORT_ABSTRACT = "shortAbstract";

  private static final String INFO_BOX = "infoBox";

  /**
   * @throws JSONException
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String type = EMPTY_STRING;
    String entity = EMPTY_STRING;
    String s_lang = EMPTY_STRING;

    Map<String, String[]> parameterMap = request.getParameterMap();

    for (String parameter : parameterMap.keySet()) {
      if (parameter.equals(TYPE)) {
        type = parameterMap.get(TYPE)[0];
      } else if (parameter.equals(ENTITY)) {
        entity = parameterMap.get(ENTITY)[0];
      } else if (parameter.equals(SOURCE_LANG)) {
        s_lang = parameterMap.get(SOURCE_LANG)[0];
      }
    }

    JSONObject jsonObj = new JSONObject();
    DBPediaDSQuerier querier = new DBPediaDSQuerier();
    if (LanguageConstants.ZH.equals(s_lang)) {
      entity = new String(entity.getBytes("ISO-8859-1"));
    }
    if ("imgsa".equals(type)) {
      if (!LanguageConstants.ZH.equals(s_lang)) {
        jsonObj.put(IMAGES, querier.queryImagesFromMongoDB(entity, s_lang));
      }
      jsonObj.put(SHORT_ABSTRACT, querier.querySAFromMongoDB(entity, s_lang));
    } else {
      if (!LanguageConstants.ZH.equals(s_lang)) {
        jsonObj.put(IMAGES, querier.queryImagesFromMongoDB(entity, s_lang));
      }
      jsonObj.put(SHORT_ABSTRACT, querier.querySAFromMongoDB(entity, s_lang));
      jsonObj.put(INFO_BOX, querier.queryInfoBoxFromMongoDB(entity, s_lang));
    }
    response.setContentType("application/json");
    response.setHeader("Cache-Control", "nocache");
    response.setCharacterEncoding("utf-8");
    PrintWriter out = response.getWriter();
    out.print(jsonObj);
  }

  /**
   * @throws JSONException
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
   */
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doGet(request, response);
  }
}
