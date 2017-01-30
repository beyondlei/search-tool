package edu.kit.aifb.ma.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import edu.kit.aifb.ma.constant.LanguageConstants;
import edu.kit.aifb.ma.util.DBPediaDSQuerier;

public class NeighborsQueryServlet extends HttpServlet {
  private static final String SOURCE_LANG = "s_lang";

  private static final String ENTITY = "entity";

  private static final String EMPTY_STRING = "";

  private static final long serialVersionUID = 1L;

  /**
   * @throws JSONException
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String entity = EMPTY_STRING;
    String lang = EMPTY_STRING;

    Map<String, String[]> parameterMap = request.getParameterMap();

    for (String parameter : parameterMap.keySet()) {
      if (parameter.equals(ENTITY)) {
        entity = parameterMap.get(ENTITY)[0];
      } else if (parameter.equals(SOURCE_LANG)) {
        lang = parameterMap.get(SOURCE_LANG)[0];
      }
    }

    JSONObject neighbors;
    DBPediaDSQuerier querier = new DBPediaDSQuerier();
    if (LanguageConstants.ZH.equals(lang)) {
      entity = new String(entity.getBytes("ISO-8859-1"));
    }
    neighbors = querier.queryNeighborsFromMongoDB(entity, lang);
    JSONObject jsonObj = new JSONObject();
    jsonObj.put("neighbors", neighbors);
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
