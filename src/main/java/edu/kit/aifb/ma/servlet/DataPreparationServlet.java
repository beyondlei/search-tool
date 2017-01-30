package edu.kit.aifb.ma.servlet;

import java.io.IOException;
import java.util.Timer;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import edu.kit.aifb.ma.newsfeed.LogicController;

/**
 * Servlet implementation class DataPreparationServlet
 */
@WebServlet("/DataPreparationServlet")
public class DataPreparationServlet extends HttpServlet {
  private Logger _logger = Logger.getLogger(DataPreparationServlet.class);

  private LogicController _logicController;

  private static final long serialVersionUID = 1L;

  /**
   * @see HttpServlet#HttpServlet()
   */
  public DataPreparationServlet() {
    super();
  }

  public void init() {
    _logger.info("DataPreparationServlet is started......");
    Timer timer = new Timer();
    _logicController = new LogicController();
    timer.schedule(_logicController, 0, 600000);
  }

  /**
   * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
   */
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doPost(request, response);
  }

  /**
   * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
   */
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
  }

}
