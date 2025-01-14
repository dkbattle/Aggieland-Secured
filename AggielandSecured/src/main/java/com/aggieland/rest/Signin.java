package com.aggieland.rest;

import com.aggieland.model.User;
import com.aggieland.model.UserDAO;
import sun.misc.Request;
import java.io.*;
import java.sql.SQLException;
import java.util.logging.Logger;
import javax.servlet.*;
import javax.servlet.http.*;

public class Signin extends AggielandSecuredServlet {
    UserDAO userDAO;
    private static final Logger LOG = Logger.getLogger(Signin.class.getName());

  @Override
    public void init() throws ServletException {
        super.init();
        userDAO = new UserDAO(getDatabaseConnectionURL(),getDatabaseUsername(),getDatabasePassword());
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        RequestDispatcher serverResponse;

        if(session != null && !session.isNew()) {
          response.sendRedirect("profile");
            
        } else {
          serverResponse = request.getRequestDispatcher("html/Signin.html");
          serverResponse.include(request, response);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RequestDispatcher serverResponse;

        try {
            boolean successfulLogin = userDAO.verifiedUser(request.getParameter("userName"),request.getParameter("password"));

            if(successfulLogin) {
                LOG.info("Successful login, Creating new session");
                User user = userDAO.getUser(request.getParameter("userName"));

                HttpSession session = request.getSession(true);
                session.setMaxInactiveInterval(600);
                session.setAttribute("user",user);
                response.sendRedirect("profile");
                
            } else {
              LOG.info("Failed login, Redirecting to signup");
              response.sendRedirect("signup");
            }

        } catch (SQLException throwables) {
            throwables.printStackTrace();
            serverResponse = request.getRequestDispatcher("html/500.html");
            serverResponse.forward(request,response);
        }

    }
    
    private boolean signInUser(HttpServletRequest request) throws SQLException, IOException {
        String userName = request.getParameter("userName");
        String password = request.getParameter("password");
        User user = null; 

        if(userDAO.verifiedUser(userName,password)) {
            user = userDAO.getUser(userName);
            request.getSession().setAttribute("user",user);
        }
        return user != null;
    }
}
