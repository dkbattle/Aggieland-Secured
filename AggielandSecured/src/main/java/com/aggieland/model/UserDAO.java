package com.aggieland.model;

import com.aggieland.auth.AuthoizationUtil;
import com.aggieland.database.DatabaseDAO;


import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import java.io.IOException;

import java.sql.*;
import java.util.ArrayList;

public class UserDAO extends BasicDAO {

  public UserDAO(String databaseConnectionURL, String databaseUsername, String databasePassword) {
    super(databaseConnectionURL, databaseUsername, databasePassword);
  }


  public boolean verifiedUser(String userName, String password) throws SQLException {

    boolean isVerified = false;

    connect();

    PreparedStatement statement = getDatabaseConnection().prepareStatement(DatabaseDAO.FIND_USER_QUERY);

    statement.setString(1, userName);

    ResultSet result = statement.executeQuery();

    if (result.next()) {

      String dbSaltedPassword = result.getString(6);
      String dbSalt = result.getString(7);

      StringBuilder enteredSaltedPassword = new StringBuilder(300);
      enteredSaltedPassword.append(password);
      enteredSaltedPassword.append(dbSalt);

      isVerified = AuthoizationUtil.checkPassword(enteredSaltedPassword.toString(), dbSaltedPassword);

      System.out.println(isVerified);


    }

    disconnect();

    return isVerified;
  }

  public boolean userExists(String userName) throws SQLException, IOException {

    return getUser(userName) != null;

  }

  public User getUser(String userName) throws SQLException, IOException {

    User foundUser = null;

    connect();

    PreparedStatement statement = getDatabaseConnection().prepareStatement(DatabaseDAO.FIND_USER_QUERY);

    statement.setString(1, userName);

    ResultSet result = statement.executeQuery();

    if (result.next()) {
      foundUser = User.createUser(result);
    }

    disconnect();

    return foundUser;
  }

  public User addUser(HttpServletRequest request) throws SQLException {

    connect();

    User user = User.createUser(request);

    PreparedStatement statement = getDatabaseConnection().prepareStatement(DatabaseDAO.ADD_USER_QUERY, Statement.RETURN_GENERATED_KEYS);

    String password = request.getParameter("password").trim();

    String salt = AuthoizationUtil.generateSalt();
    StringBuilder saltedPassword = new StringBuilder(50);
    saltedPassword.append(password);
    saltedPassword.append(salt);

    String hashedPassword = AuthoizationUtil.hashSaltedPassword(saltedPassword.toString(), salt);

    statement.setString(1, user.getFirstName());
    statement.setString(2, user.getLastName());
    statement.setString(3, user.getUserName());
    statement.setString(4, hashedPassword);
    statement.setString(5, salt);
    statement.setString(6, user.getEmail());
    statement.setDate(7, user.getDateCreatedAsDate());
    statement.setNull(8, Types.NULL);
    statement.setNull(9, Types.NULL);
    statement.setString(10, user.getMajor());

    boolean userAdded = statement.executeUpdate() > 0;

    ResultSet primaryKey = statement.getGeneratedKeys();

    long userId = -1;

    if (primaryKey.next()) {
      userId = primaryKey.getInt(1);
    }

    user.setUserId(userId);

    disconnect();

    return userAdded ? user : null;
  }

  public User updateAccount(HttpServletRequest request) throws SQLException, IOException, ServletException {
    User updatedUser = null;

    Part filePart = request.getPart("profilePicture");

    User.updateUser(request);

    connect();

    PreparedStatement statement = getDatabaseConnection().prepareStatement(DatabaseDAO.UPDATE_USER_QUERY);

    updatedUser = (User) request.getSession(false).getAttribute("user");

    statement.setString(1, updatedUser.getFirstName());
    statement.setString(2, updatedUser.getLastName());
    statement.setString(3, updatedUser.getEmail());
    statement.setString(4, updatedUser.getProfilePictureBase64());
    statement.setString(5, updatedUser.getUserInfo());
    statement.setString(6, updatedUser.getMajor());
    statement.setLong(7, updatedUser.getUserId());

    boolean userUpdated = statement.executeUpdate() > 0;

    if (userUpdated) {
      System.out.println("user updated");
    } else {
      System.out.println("error updating");
    }

    disconnect();

    return updatedUser;
  }

  public User getUser(long userID) throws SQLException, IOException {

    User foundUser = null;

    PreparedStatement statement = getDatabaseConnection().prepareStatement(DatabaseDAO.FIND_USER_BY_ID);

    statement.setLong(1, userID);

    ResultSet result = statement.executeQuery();

    if (result.next()) {
      foundUser = User.createUserFromSearch(result);
    }

    return foundUser;
  }

  public ArrayList<User> getFriends(long userID) throws SQLException, IOException {

    ArrayList<User> friends = new ArrayList<>();

    connect();

    PreparedStatement statement = getDatabaseConnection().prepareStatement(DatabaseDAO.GET_FRIENDS_QUERY);

    statement.setLong(1, userID);
    statement.setLong(2, userID);
    statement.setLong(3,userID);

    ResultSet results = statement.executeQuery();

    while (results.next()) {

      long friendID = results.getLong(1) != userID ? results.getLong(1) : results.getLong(2);

      if (results.getLong(4) == DatabaseDAO.IS_FRIEND) {

        User currentFriend = getUser(friendID);
        System.out.println(currentFriend.getUserName());

        if (currentFriend != null) {
          friends.add(currentFriend);
        }
      }

    }

    disconnect();

    return friends;

  }

  public long areFriends(long userID, long myID) throws SQLException {

    long friendStatus = -1;

    connect();

    PreparedStatement statement = getDatabaseConnection().prepareStatement(DatabaseDAO.ARE_FRIENDS_QUERY);

    statement.setLong(1, Long.min(userID, myID));
    statement.setLong(2, Long.max(userID, myID));


    ResultSet result = statement.executeQuery();


    while (result.next()) {
      System.out.print("User one id: " + result.getLong(1));
      System.out.print("Useer two id: " + result.getLong(2));
      System.out.print("recent_user id: " + result.getLong(3));
      System.out.print("status: " + result.getLong(4));
      System.out.println();

      friendStatus = result.getLong(4);
    }

    disconnect();

    return friendStatus;
  }

  public boolean removeFriend(long userID, long myID) throws SQLException {

    connect();

    PreparedStatement statement = getDatabaseConnection().prepareStatement(DatabaseDAO.DELETE_FRIEND_QUERY);

    statement.setLong(1, Long.min(userID, myID));
    statement.setLong(2, Long.max(userID, myID));

    boolean result = statement.executeUpdate() > 0;

    disconnect();

    return result;

  }

  public boolean updateFriendStatus(long userID, long myID, int status) throws SQLException {

    connect();

    PreparedStatement statement = getDatabaseConnection().prepareStatement(DatabaseDAO.UPDATE_FRIEND_QUERY);

    statement.setLong(1, Long.min(userID, myID));
    statement.setLong(2, Long.max(userID, myID));
    statement.setLong(3, status);

    boolean result = statement.executeUpdate() > 0;

    disconnect();

    return result;

  }

  public boolean addFriend(long userID, long myID) throws SQLException {
    connect();

    PreparedStatement statement = getDatabaseConnection().prepareStatement(DatabaseDAO.ADD_FRIEND_QUERY);

    statement.setLong(1, Long.min(userID, myID));
    statement.setLong(2, Long.max(userID, myID));
    statement.setLong(3,myID);
    statement.setLong(4,0);

    boolean result = statement.executeUpdate() > 0;

    disconnect();

    return result;
  }


}
