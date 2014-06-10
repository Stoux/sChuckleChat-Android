/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package chucklechatserver;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;

/**
 *
 * @author Leon
 */
public class RequestHandle implements Runnable {

    private Socket s;
    private SQLHandler sql;
    
    public RequestHandle(Socket s, SQLHandler sql) {
        this.s = s;
        this.sql = sql;
    }
    
    @Override
    public void run() {
        try {
            //Create reader & writer
            BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream()));
            BufferedWriter w = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
                        
            System.out.println("Created");
            
            //Parse as JSON
            String jsonLine = r.readLine();
            System.out.println("Read: " + jsonLine);
            JsonObject obj = JsonObject.readFrom(jsonLine);
            
            String type = obj.get("type").asString();
            System.out.println(type);
            switch (type.toLowerCase()) {
                case "login": //Client tries to login
                    JsonObject credentials = obj.get("data").asObject();
                    
                    //Get Email & Pass
                    String email = credentials.get("username").asString().toLowerCase();
                    String pass = credentials.get("password").asString().toLowerCase();
                    
                    int responseCode = 0;
                    int userID = 0;
                    
                    Connection con = sql.getConnection();
                    try {
                        //Query SQL
                        PreparedStatement prep = con.prepareStatement("SELECT `user_id`,`password` FROM `users` WHERE `email` = ?;");
                        prep.setString(1, email);
                        ResultSet rs = prep.executeQuery();
                        
                        if (rs.next()) { //If result, username found
                            userID = rs.getInt(1);
                            String foundPass = rs.getString(2);
                            if(foundPass.equals(pass)) { //Check if pass is correct
                                responseCode = 202;
                            } else {
                                responseCode = 406;
                            }
                        } else { //No user
                            responseCode = 404;
                        }
                    } catch (SQLException e) {
                        s.close();
                    } finally {
                        sql.returnConnection(con);
                    }
                    
                    //Create response
                    JsonObject returnObj = new JsonObject();
                    returnObj.add("type", "loginResponse");
                    JsonObject returnData = new JsonObject();
                    returnData.add("code", responseCode);
                    if (responseCode == 202) {
                        returnData.add("user-id", userID);
                    }
                    returnObj.add("data", returnData);
                    
                    //Write response
                    w.write(returnObj.toString());
                    w.flush();
                    break;
                    
                case "register": //Client tries to register
                    credentials = obj.get("data").asObject();
                    
                    //Get Email & Pass
                    email = credentials.get("username").asString().toLowerCase();
                    pass = credentials.get("password").asString().toLowerCase();
                    
                    responseCode = 0;
                    userID = 0;
                    
                    con = sql.getConnection();
                    try {
                        //Check if username isn't taken already
                        PreparedStatement checkPrep = con.prepareStatement("SELECT `user_id` FROM `users` WHERE `email` = ?;");
                        checkPrep.setString(1, email);
                        ResultSet rs = checkPrep.executeQuery();
                        if (rs.next()) { //Shouldn't be one
                            responseCode = 403;
                        }
                        rs.close();
                        
                        if (responseCode != 403) {
                            PreparedStatement insertPrep = con.prepareStatement("INSERT INTO `chucklechat`.`users` (`user_id`, `email`, `password`, `display_name`) VALUES (NULL, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS);
                            insertPrep.setString(1, email);
                            insertPrep.setString(2, pass);
                            insertPrep.setString(3, email);
                            insertPrep.executeUpdate();
                            
                            //Get autogenerated
                            ResultSet genKeys = insertPrep.getGeneratedKeys();
                            if (genKeys.next()) {
                                userID = genKeys.getInt(1);
                                responseCode = 202;
                                
                                PreparedStatement changePrep = con.prepareStatement("INSERT INTO `chucklechat`.`user_changes` (`user_id`, `timestamp`, `type`) VALUES (?, ?, 'created');");
                                changePrep.setInt(1, userID);
                                changePrep.setLong(2, System.currentTimeMillis());
                                changePrep.executeUpdate();
                            } else {
                                responseCode = 403;
                            }
                        }
                    } catch (SQLException e) {
                        s.close();
                    } finally {
                        sql.returnConnection(con);
                    }
                    
                    //Create response
                    returnObj = new JsonObject();
                    returnObj.add("type", "loginResponse");
                    returnData = new JsonObject();
                    returnData.add("code", responseCode);
                    if (responseCode == 202) {
                        returnData.add("user-id", userID);
                    }
                    returnObj.add("data", returnData);
                    
                    //Write response
                    w.write(returnObj.toString());
                    w.flush();
                    
                    break;

                case "get_new": //Get new messages stuff
                    //Get data
                    JsonObject getNewData = obj.get("data").asObject();
                    userID = getNewData.get("user-id").asInt();
                    long lastCheck = getNewData.get("last-check").asLong();
                    
                    long newCheck = System.currentTimeMillis();
                    
                    //Create Data arrays
                    JsonArray userChanges = new JsonArray();
                    JsonArray newChats = new JsonArray();
                    JsonArray newMessages = new JsonArray();
                    
                    //TODO fill return
                    
                    //=> Fill user changes
                    con = sql.getConnection();
                    try {
                        //Check for any changes between new timezone
                        PreparedStatement prep = con.prepareStatement("SELECT DISTINCT `user_id` FROM `user_changes` WHERE `timestamp` > ? AND `timestamp` < ? AND `user_id` NOT IN ( ? );");
                        prep.setLong(1, lastCheck);
                        prep.setLong(2, newCheck);
                        prep.setInt(3, userID);
                        ResultSet changedUsersRS = prep.executeQuery();
                        HashSet<Integer> userIDs = new HashSet<>();
                        while (changedUsersRS.next()) { //Add all found users to a list
                            userIDs.add(changedUsersRS.getInt(1));
                        }
                        //Close SQL Stuff
                        changedUsersRS.close();
                        prep.close();
                        
                        //If any changes
                        if (!userIDs.isEmpty()) {
                            String whereClause = "";
                            for (Integer user : userIDs) { //Create one single where clause
                                if (!whereClause.isEmpty()) {
                                    whereClause += " OR ";
                                }
                                whereClause += "`user_id` = " + user;
                            }
                            
                            PreparedStatement usersRSPrep = con.prepareStatement("SELECT `user_id`, `email`, `display_name` FROM `users` WHERE (" + whereClause + ") AND `user_id` NOT IN ( ? );"); //Prep statement
                            usersRSPrep.setInt(1, userID); //Make sure not to return changes for this user
                            ResultSet usersRS = usersRSPrep.executeQuery();
                            while (usersRS.next()) { //Add users to UserArray
                                JsonObject userJson = new JsonObject();
                                userJson.add("user-id", usersRS.getInt(1));
                                userJson.add("username", usersRS.getString(2));
                                userJson.add("displayname", usersRS.getString(3));
                                userChanges.add(userJson);
                            }
                            //Close SQL stuff
                            usersRS.close();
                            usersRSPrep.close();
                        }
                    } catch (SQLException e) {
                        System.out.println("Failed to do SQL Stuff for users: " + e);
                    } finally {
                        sql.returnConnection(con);
                    } 
                    
                    
                    //=> Fill chat changes
                    con = sql.getConnection();
                    try {
                        PreparedStatement prep = con.prepareCall("SELECT `chat_id`, `groupchat`, `displayname`, `chatters` FROM `chats` WHERE `created` > ? AND `created` < ? AND `chatters` LIKE ? AND `created_by_user` NOT IN ( ? );");
                        prep.setLong(1, lastCheck); //Set timezone
                        prep.setLong(2, newCheck);
                        prep.setString(3, "%-" + userID + "-%"); //Looking for chats where this person has access to
                        prep.setInt(4, userID); //But not created by them, as they already know it exists
                        ResultSet newChatsRS = prep.executeQuery(); //Execute
                        while (newChatsRS.next()) {
                            //Add new chats to ChatsArray
                            JsonObject newChat = new JsonObject();
                            newChat.add("chat-id", newChatsRS.getLong(1));
                            //Check if group
                            boolean isGroupChat;
                            newChat.add("group-chat", isGroupChat = newChatsRS.getBoolean(2));
                            
                            //Get chatter(s)
                            String chatters = newChatsRS.getString(4);
                            String cChatters = chatters.replace("-", "");
                            
                            if (isGroupChat) { //Group, so multiple users & displayname
                                newChat.add("displayname", newChatsRS.getString(3));
                                JsonArray otherPeople = new JsonArray();
                                for (String chatter : cChatters.split("_")) {
                                    int chatterInt = Integer.parseInt(chatter);
                                    if (chatterInt != userID) {
                                        otherPeople.add(chatterInt);
                                    }
                                }
                                newChat.add("other-people", otherPeople);
                            } else {
                                String[] split = cChatters.split("_");
                                String otherPerson = (split[0].equals("" + userID) ? split[1] : split[0]);
                                newChat.add("other-person", Integer.parseInt(otherPerson));
                            }
                            
                            newChats.add(newChat);
                        }   
                        newChatsRS.close();
                        prep.close();
                    } catch (SQLException e) {
                        System.out.println("Failed to do SQL Stuff for chats: " + e);
                    } finally {
                        sql.returnConnection(con);
                    }
                    
                    
                    //=> Fill new messages
                    con = sql.getConnection();
                    try {
                        //Get all Chats that this pserson has access to
                        PreparedStatement chatIdsPrep = con.prepareStatement("SELECT DISTINCT `chat_id` FROM `chats` WHERE `chatters` LIKE ?;");
                        chatIdsPrep.setString(1, "%-" + userID + "-%");
                        ResultSet chatIdsRS = chatIdsPrep.executeQuery();
                        String chatIds = ""; //Add them all into one string
                        while (chatIdsRS.next()) {
                            if (!chatIds.isEmpty()) {
                                chatIds += ",";
                            }
                            chatIds += chatIdsRS.getInt(1);
                        }
                        //Close SQL Stuff
                        chatIdsRS.close();
                        chatIdsPrep.close();
                        
                        if (!chatIds.isEmpty()) { //If ChatIDs given
                            PreparedStatement prep = con.prepareStatement("SELECT `message_id`, `chat_id`, `by_user`, `timestamp`, `message` FROM `messages` WHERE `timestamp` > ? AND `timestamp` < ? AND `chat_id` IN ( " + chatIds + " ) AND `by_user` NOT IN ( ? );");
                            prep.setLong(1, lastCheck); //Set timezone
                            prep.setLong(2, newCheck);
                            prep.setInt(3, userID);
                            ResultSet messageRS = prep.executeQuery(); //Execute
                            while (messageRS.next()) {
                                JsonObject newMessage = new JsonObject();
                                newMessage.add("message-id", messageRS.getLong(1));
                                newMessage.add("chat-id", messageRS.getLong(2));
                                newMessage.add("user-id", messageRS.getInt(3));
                                newMessage.add("timestamp", messageRS.getLong(4));
                                newMessage.add("message", messageRS.getString(5));
                                newMessages.add(newMessage);
                            }
                            //Close SQL Stuff
                            messageRS.close();
                            prep.close();
                        }
                    } catch (SQLException e) {
                        System.out.println("Failed to do SQL Stuff for messages: " + e);
                    } finally {
                        sql.returnConnection(con);
                    }
                                    
                    
                    //Respond
                    returnObj = new JsonObject();
                    returnObj.add("type", "get_newResponse");
                    returnData = new JsonObject();
                    returnData.add("users", userChanges);
                    returnData.add("chats", newChats);
                    returnData.add("messages", newMessages);
                    returnData.add("last-check", newCheck);
                    returnObj.add("data", returnData);
                    
                    //=> Write response
                    String jsonReturn = returnObj.toString();
                    System.out.println("Responding: " + jsonReturn);
                    w.write(jsonReturn);
                    w.flush();
                    break;
                    
                case "send_message": //=> User sends a new message
                    JsonObject msgData = obj.get("data").asObject();
                    userID = msgData.get("user-id").asInt();
                    String message = msgData.get("message").asString();
                    long chatID = msgData.get("chat-id").asLong();
                    
                    long messageID = -1;
                    long messageTimestamp = System.currentTimeMillis();
                    
                    //Check for new chat
                    JsonValue newChatValue = msgData.get("new-chat");
                    if (newChatValue != null) { //=> New chat added
                        JsonObject newChat = newChatValue.asObject();
                        
                        //Get chat data
                        boolean groupchat = newChat.get("group-chat").asBoolean();
                        
                        //Possbile data
                        String displayname = null;
                        String otherPeople = "";
                        
                        if (groupchat) { //Group
                            displayname = newChat.get("displayname").asString();
                            
                            //=> Get people
                            JsonArray userArray = newChat.get("other-people").asArray();
                            userArray.add(userID);
                            for (JsonValue value : userArray) {
                                int user = value.asInt();
                                if (!otherPeople.isEmpty()) {
                                    otherPeople += "_";
                                }
                                otherPeople += "-" + user + "-";
                            }
                        } else {
                            otherPeople = "-" + userID + "-_-" + newChat.get("other-person").asInt() + "-";
                        }
                        
                        //=> Add to DB
                        con = sql.getConnection();
                        try {
                            //Insert into
                            PreparedStatement addChatPrep = con.prepareStatement("INSERT INTO `chats` (`chat_id`, `groupchat`, `displayname`, `created`, `created_by_user`, `chatters`) VALUES (NULL, ?, ?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS);
                            addChatPrep.setBoolean(1, groupchat);
                            if (displayname != null) { //Check if not null
                                addChatPrep.setString(2, displayname);
                            } else {
                                addChatPrep.setNull(2, java.sql.Types.VARCHAR);
                            }
                            addChatPrep.setLong(3, messageTimestamp = System.currentTimeMillis());
                            addChatPrep.setInt(4, userID);
                            addChatPrep.setString(5, otherPeople);
                            addChatPrep.executeUpdate();
                            
                            //Get key
                            ResultSet rs = addChatPrep.getGeneratedKeys();
                            if (rs.next()) {
                                chatID = rs.getLong(1);
                            }
                        } catch (SQLException e) {

                        } finally {
                            sql.returnConnection(con);
                        }
                    }
                    
                    //=> Insert message
                    con = sql.getConnection();
                    try {
                        PreparedStatement insertMessagePrep = con.prepareStatement(
                                "INSERT INTO `chucklechat`.`messages` (`message_id`, `chat_id`, `timestamp`, `by_user`, `message`) VALUES (NULL, ?, ?, ?, ?);", 
                                Statement.RETURN_GENERATED_KEYS
                        );
                        insertMessagePrep.setLong(1, chatID);
                        insertMessagePrep.setLong(2, System.currentTimeMillis());
                        insertMessagePrep.setInt(3, userID);
                        insertMessagePrep.setString(4, message);
                        insertMessagePrep.executeUpdate();
                        
                        //=> Get MessageID
                        ResultSet rs = insertMessagePrep.getGeneratedKeys();
                        if (rs.next()) {
                            messageID = rs.getLong(1);
                        }
                    } catch (SQLException e) {
                        
                    } finally {
                        sql.returnConnection(con);
                    }
                    
                    //Respond
                    returnObj = new JsonObject();
                    returnObj.add("type", "send_messageResponse");
                    returnData = new JsonObject();
                    returnData.add("message-id", messageID);
                    returnData.add("chat-id", chatID);
                    returnData.add("message-timestamp", messageTimestamp);
                    returnObj.add("data", returnData);
                    
                    //=> Write response
                    jsonReturn = returnObj.toString();
                    System.out.println("Responding: " + jsonReturn);
                    w.write(jsonReturn);
                    w.flush();
                    break;
                    
                case "change_displayname": //=> User changes their username
                    JsonObject displaynameData = obj.get("data").asObject();
                    userID = displaynameData.get("user-id").asInt();
                    String newDisplayname = displaynameData.get("new-displayname").asString();
                    
                    responseCode = 0;
                    
                    con = sql.getConnection();
                    try { 
                        //Insert new entry into changes
                        PreparedStatement prep = con.prepareStatement("INSERT INTO `user_changes` (`user_id`, `timestamp`, `type`) VALUES (?, ?, 'name_change');");
                        prep.setInt(1, userID);
                        prep.setLong(2, System.currentTimeMillis());
                        prep.executeUpdate();
                        
                        //Update name
                        PreparedStatement updatePrep = con.prepareStatement("UPDATE `users` SET `display_name` = ? WHERE `user_id` = ?;");
                        updatePrep.setString(1, newDisplayname);
                        updatePrep.setInt(2, userID);
                        updatePrep.executeUpdate();
                        
                        responseCode = 202;
                    } catch (SQLException e) {
                        
                    } finally {
                        sql.returnConnection(con);
                    }
                    
                    //Respond
                    returnObj = new JsonObject();
                    returnObj.add("type", "change_displaynameResponse");
                    returnData = new JsonObject();
                    returnData.add("code", responseCode);
                    returnObj.add("data", returnData);
                    
                    //=> Write response
                    jsonReturn = returnObj.toString();
                    System.out.println("Responding: " + jsonReturn);
                    w.write(jsonReturn);
                    w.flush();
                    break;
                
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            System.out.println("Closed");
            s.close();
        } catch (Exception ex) {
        }

    }
    
    
    
}
