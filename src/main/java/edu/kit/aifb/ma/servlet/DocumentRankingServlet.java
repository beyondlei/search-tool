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

import edu.kit.aifb.ma.constant.CharacterConstants;
import edu.kit.aifb.ma.constant.LanguageConstants;
import edu.kit.aifb.ma.search.DocumentsRanker;

public class DocumentRankingServlet extends HttpServlet {
  private static final String QUERY = "query";

  private static final String SOURCE_LANG = "s_lang";

  private static final String KB_LANG = "kb_lang";

  private static final String TARGET_LANG = "t_lang";

  private static final String ENTITIES = "entities";

  private static final String MODE = "mode";

  private static final long serialVersionUID = 1L;

  /**
   * @throws JSONException
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String entitiesWithProb = CharacterConstants.EMPTY_STRING;
    String s_lang = CharacterConstants.EMPTY_STRING;
    String kb_lang = CharacterConstants.EMPTY_STRING;
    String t_lang = CharacterConstants.EMPTY_STRING;
    String query = CharacterConstants.EMPTY_STRING;
    boolean orMode = true;
    Map<String, String[]> parameterMap = request.getParameterMap();
    DocumentsRanker ranker = new DocumentsRanker();
    for (String parameter : parameterMap.keySet()) {
      if (parameter.equals(ENTITIES)) {
        entitiesWithProb = parameterMap.get(ENTITIES)[0];
      } else if (parameter.equals(SOURCE_LANG)) {
        s_lang = parameterMap.get(SOURCE_LANG)[0];
      } else if (parameter.equals(QUERY)) {
        query = parameterMap.get(QUERY)[0];
      } else if (parameter.equals(TARGET_LANG)) {
        t_lang = parameterMap.get(TARGET_LANG)[0];
      } else if (parameter.equals(KB_LANG)) {
        kb_lang = parameterMap.get(KB_LANG)[0];
      } else if (parameter.equals(MODE)) {
        if (!"or".equals(parameterMap.get(MODE)[0])) {
          orMode = false;
        }
      }
    }

    if (LanguageConstants.ZH.equals(kb_lang)) {
      entitiesWithProb = new String(entitiesWithProb.getBytes("ISO-8859-1"));
    }
    if (LanguageConstants.ZH.equals(s_lang)) {
      query = new String(query.getBytes("ISO-8859-1"));
    }

    JSONObject documents = null;
    try {
      if (!entitiesWithProb.isEmpty()) {
        documents = ranker.getDocumentsByEntities(entitiesWithProb, orMode, kb_lang, t_lang.substring(0, t_lang.length() - 1));
      }
      if (!query.isEmpty()) {
        documents = ranker.getDocumentsByQuery(query, orMode, s_lang, t_lang.substring(0, t_lang.length() - 1));
      }
      response.setContentType("application/json");
      response.setHeader("Cache-Control", "nocache");
      response.setCharacterEncoding("utf-8");
      PrintWriter out = response.getWriter();
      out.print(documents);
    } catch (RepositoryException e) {
      e.printStackTrace();
    }

  }

  /**
   * @throws JSONException
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
   */
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doGet(request, response);
  }
}
