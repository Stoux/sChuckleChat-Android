/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package chucklechatserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author Leon
 */
public class ChuckleChatServer {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            //Create the server socket
            ServerSocket ss = new ServerSocket(55422);
            
            //Create thread pool
            ExecutorService service = Executors.newFixedThreadPool(10); //Create 10 threads
            
            //Create Connections
            SQLHandler sqlHandler = new SQLHandler();
                        
            System.out.println("Listening!");
                    
            Socket s;
            while((s = ss.accept()) != null) {
                System.out.println("New one! " + s.getInetAddress().getHostAddress());
                service.execute(new RequestHandle(s, sqlHandler));
            }
            
            
            
        } catch (IOException e) {
            
        }
        
        
    }
    
}
