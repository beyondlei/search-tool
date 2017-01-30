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
import org.openrdf.repository.RepositoryException;

import edu.kit.aifb.ma.constant.LanguageConstants;
import edu.kit.aifb.ma.search.TopKGraphsGenerator;

public class SubgraphsFinderServlet extends HttpServlet {
  private static final String TARGET_LANGUAGE = "t_lang";

  private static final String SOURCE_LANGUAGE = "s_lang";

  private static final String QUERY = "query";

  private static final long serialVersionUID = 6225853882698327219L;

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String query = "";
    String s_lang = "";
    String t_lang = "";
    Map<String, String[]> parameterMap = request.getParameterMap();

    for (String parameter : parameterMap.keySet()) {
      if (parameter.equals(QUERY)) {
        query = parameterMap.get(QUERY)[0];
      }
      if (parameter.equals(SOURCE_LANGUAGE)) {
        s_lang = parameterMap.get(SOURCE_LANGUAGE)[0];
      }
      if (parameter.equals(TARGET_LANGUAGE)) {
        t_lang = parameterMap.get(TARGET_LANGUAGE)[0];
      }
    }

    TopKGraphsGenerator gen = new TopKGraphsGenerator();
    JSONObject subgraphs = new JSONObject();
    if (LanguageConstants.ZH.equals(s_lang)) {
      query = new String(query.getBytes("ISO-8859-1"));
    }
    try {
      if (!query.isEmpty()) {
        subgraphs = gen.getSubgraphs(query, s_lang, t_lang);
      }
    } catch (JSONException e) {
      e.printStackTrace();
    } catch (RepositoryException e) {
      e.printStackTrace();
    }
    response.setContentType("application/json");
    response.setHeader("Cache-Control", "nocache");
    response.setCharacterEncoding("utf-8");
    PrintWriter out = response.getWriter();
    out.print(subgraphs);

  }

  /**
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
   */
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doGet(request, response);
  }

}
