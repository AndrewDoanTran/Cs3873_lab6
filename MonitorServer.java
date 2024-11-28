/*
  Adapted from resources provided with textbook "Computer Networking: A Top-Down Approach"
  by Kurose and Ross.
 */

import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.concurrent.TimeUnit;

class MonitorServer {
    static final int MAX_MSG_LEN = 1024;
    static final int SERVER_PORT = 9000;
    static final int MAX_DELAY = 2000; // in milliseconds
    static final double LOSS_PROBABILITY = 0.4;  // probability a reply is not returned (i.e., lost)
    static final int RND_SEED = 1234;

    public static void main(String[] args) {
        System.out.println("Server started at port: " + SERVER_PORT);

        /* Create server and its socket */
        MonitorServer server = new MonitorServer();
        DatagramSocket serverSocket = null;
        try {
            serverSocket = new DatagramSocket(SERVER_PORT);
        } catch (SocketException e) {
            System.out.println("Error creating socket: " + e);
        }

        /* Receive request and send back reply */
        byte[] receiveData = new byte[MAX_MSG_LEN];
        byte[] sendData;
        Random rand = new Random(RND_SEED);
        while (true) {
            // Receive request
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            try {
                assert serverSocket != null;
                serverSocket.receive(receivePacket);
            } catch (IOException e) {
                System.out.println("Error receiving packet: " + e);
            }
            String receiveMsg = new String(receivePacket.getData());
            InetAddress fromAddress = receivePacket.getAddress();
            int fromPort = receivePacket.getPort();

            System.out.println("From client at " + fromAddress + ": " + receiveMsg);

            // Simulate loss
            double randProb = rand.nextDouble();  // Random number between 0.0 and 1.0
            if (randProb <= LOSS_PROBABILITY) {
                continue;  // Don't send reply, continue to receive next, simulating rely is lost
            }

            // Return reply after a random delay period
            int randNum = rand.nextInt(MAX_DELAY);  // Random delay period
            try {
                TimeUnit.MILLISECONDS.sleep(randNum);
            } catch (InterruptedException e) {
                System.out.println("Error setting delay: " + e);
            }

            // Send back reply
            String sendMsg = receiveMsg.toUpperCase();   // Returned reply capitalizes the received message
            sendData = sendMsg.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, fromAddress, fromPort);
            try {
                serverSocket.send(sendPacket);
            } catch (IOException e) {
                System.out.println("Error sending packet: " + e);
            }
        }
    }
}
