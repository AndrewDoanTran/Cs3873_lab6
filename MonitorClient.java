import java.io.IOException;
import java.net.*;
import java.util.*;

public class MonitorClient {
    // Constants
    static final int MAX_MSG_LEN = 1024;
    static final int PERIOD = 1000; // Timeout in milliseconds for receiving replies
    static final int PERIOD_END = 5000; // Wait period for delayed replies
    static final int NUM_REQUESTS = 40; // Number of echo requests to send
    static final double ALPHA = 0.125; // Weight for EstimatedRTT
    static final double BETA = 0.25; // Weight for DevRTT

    private DatagramSocket clientSocket;
    private InetAddress serverAddress;
    private int serverPort;

    // Constructor
    public MonitorClient(String host, int port)
    {   
        try
        {
            clientSocket = new DatagramSocket();
            serverAddress = InetAddress.getByName(host);
            serverPort = port;
            clientSocket.setSoTimeout(PERIOD);
            System.out.println("MonitorClient initialized. Sending requests to " + host + ":" + port);
    
        }
        catch(SocketException e)
        {
            System.out.println("Error creating Socket" + e);
        }
        catch(UnknownHostException e)
        {
            System.out.println("Cannot find port" + e);
        }
    }
    
    public void run() 
    {
        long[] sendTimes = new long[NUM_REQUESTS];
        long[] receiveTimes = new long[NUM_REQUESTS];
        long[] RTTs = new long[NUM_REQUESTS];
        Arrays.fill(RTTs, -1); 
        double estimatedRTT = 0.0;
        double devRTT = 0.0;

        for (int i = 0; i < NUM_REQUESTS; i++) 
        {
            try 
            {

                String messageToSend = "Hello " + i;
                byte[] sendData = messageToSend.getBytes();


                sendTimes[i] = System.currentTimeMillis();

                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, serverPort);
                clientSocket.send(sendPacket);

                byte[] receiveData = new byte[MAX_MSG_LEN];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

                clientSocket.receive(receivePacket);
                receiveTimes[i] = System.currentTimeMillis();


                String reply = new String(receivePacket.getData(), 0, receivePacket.getLength());
                int replyId = Integer.parseInt(reply.split(" ")[1]);


                if (replyId == i) 
                {
                    RTTs[i] = receiveTimes[i] - sendTimes[i];
                    System.out.printf("Request %d: RTT = %d ms\n", i, RTTs[i]);

                    
                    if (estimatedRTT == 0.0) 
                    {
                        // First sample
                        estimatedRTT = (double) RTTs[i];
                        devRTT = RTTs[i] / 2.0;
                    } 
                    else 
                    {
                        double sampleRTT = RTTs[i];
                        estimatedRTT = (1 - ALPHA) * estimatedRTT + ALPHA * sampleRTT;
                        devRTT = (1 - BETA) * devRTT + BETA * Math.abs(sampleRTT - estimatedRTT);
                    }
                }

            } 
            catch (SocketTimeoutException e) 
            {
                System.out.printf("Request %d: no reply\n", i);
            } 
            catch (IOException e) 
            {
                System.err.println("Error: " + e.getMessage());
            }
        }


        delayedReplies();


        printResults(RTTs, estimatedRTT, devRTT);
    }

    private void delayedReplies() 
    {
        long endTime = System.currentTimeMillis() + PERIOD_END;
        System.out.println("Monitoring delayed replies...");
        while (System.currentTimeMillis() < endTime) 
        {
            try 
            {
                byte[] receiveData = new byte[MAX_MSG_LEN];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                clientSocket.receive(receivePacket);

                // Decode delayed reply
                String reply = new String(receivePacket.getData(), 0, receivePacket.getLength());
                System.out.println("Delayed Reply: " + reply);

            } 
            catch (SocketTimeoutException e) 
            {
                
                break;
            } catch (IOException e) {
                System.err.println("Error receiving delayed reply: " + e.getMessage());
            }
        }
    }

    private void printResults(long[] RTTs, Double estimatedRTT, Double devRTT) 
    {
        System.out.println("\nFinal Results:");
        for (int i = 0; i < RTTs.length; i++) {
            if (RTTs[i] == -1) 
            {
                System.out.printf("Request %d: no reply\n", i);
            } else 
            {
                System.out.printf("Request %d: RTT = %d ms\n", i, RTTs[i]);
            }
        }
        if (estimatedRTT != null && devRTT != null) 
        {
            System.out.printf("EstimatedRTT: %.2f ms\n", estimatedRTT);
            System.out.printf("DevRTT: %.2f ms\n", devRTT);
        } 
        else 
        {
            System.out.println("No RTT samples received. Cannot compute EstimatedRTT or DevRTT.");
        }
    }

    public void close() 
    {
        
            clientSocket.close();
            System.out.println("Client socket closed.");
        
    }

    public static void main(String[] args)
    {


        try 
        {
            String host = args[0];
            int port = Integer.parseInt(args[1]);

            MonitorClient client = new MonitorClient(host, port);
            client.run();
            client.close();

        } 
        catch (Exception e) 
        {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
