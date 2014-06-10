/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package chucklechatserver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 *
 * @author Leon
 */
public class SQLHandler {

    private List<Connection> connections;
    
    public SQLHandler() {
        //Create Connections
        connections = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            try {
                connections.add(DriverManager.getConnection("jdbc:mysql://localhost:3306/chucklechat", "root", null));
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized(this) {
                    try {
                        Thread.sleep(360000);
                    } catch (InterruptedException e) {}
                }
                
                HashSet<Connection> conns = new HashSet<>();
                synchronized(connections) {
                    conns.addAll(connections);
                }
                for (Connection c : conns) {
                    try {
                        c.isValid(5);
                    } catch (Exception e) {}
                }                
            }
        });
    }

    public Connection getConnection() {
        synchronized(connections) {
            Connection c = connections.get(0);
            connections.remove(0);
            return c;
        }
    }
    
    public void returnConnection(Connection c) {
        synchronized(connections) {
            connections.add(c);
        }
    }
    
    
    
    
    
    
    
    
    
}
