import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BlockchainServer {

    public static void main(String[] args) {

        if (args.length != 3) {
            return;
        }

        int localPort = 0;
        int remotePort = 0;
        String remoteHost = null;

        try {
            localPort = Integer.parseInt(args[0]);
            remoteHost = args[1];
            remotePort = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            return;
        }
        
        Blockchain blockchain = new Blockchain();
        ConcurrentLinkedQueue<Block> waitingBlocks = new ConcurrentLinkedQueue<Block>();
        int waitingBlocksSize = 0;
        
        if(args[1].equals("localhost")) {
        	remoteHost = "127.0.0.1";
        }

        HashMap<ServerInfo, Date> serverStatus = new HashMap<ServerInfo, Date>();
        serverStatus.put(new ServerInfo(remoteHost, remotePort), new Date());
    	
    	new Thread(new BlockchainCatchUpRunnable(new ServerInfo(remoteHost,remotePort), blockchain, "cu", waitingBlocks)).start();

	    PeriodicCommitRunnable pcr = new PeriodicCommitRunnable(blockchain);
        Thread pct = new Thread(pcr);
        pct.start();
        ServerSocket serverSocket = null;
        
        try {      
        	serverSocket = new ServerSocket(localPort);
        	new Thread(new PeriodicHeartBeatRunnable(serverStatus, localPort)).start();
        	new Thread(new BlockchainLatestBlockRunnable(serverStatus, blockchain, localPort)).start();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new BlockchainServerRunnable(clientSocket, blockchain, serverStatus, waitingBlocks, waitingBlocksSize)).start();
            }
        } catch (IllegalArgumentException e) {
        } catch (IOException e) {
        } finally {
            try {
                pcr.setRunning(false);
                pct.join();
                if (serverSocket != null)
                    serverSocket.close();
            } catch (IOException e) {
            } catch (InterruptedException e) {
            }
        }
    }
}
