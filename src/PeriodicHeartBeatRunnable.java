import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;

public class PeriodicHeartBeatRunnable implements Runnable {

    private HashMap<ServerInfo, Date> serverStatus;
    private int sequenceNumber;
    private int serverPort;

    public PeriodicHeartBeatRunnable(HashMap<ServerInfo, Date> serverStatus, int serverPort) {
        this.serverStatus = serverStatus;
        this.sequenceNumber = 0;
        this.serverPort = serverPort;
    }

    @Override
    public void run() {
        while(true) {

            ArrayList<Thread> threadArrayList = new ArrayList<>();
	        try {
	            for (Entry<ServerInfo, Date> server : serverStatus.entrySet()) {

	            	if(new Date().getTime() - server.getValue().getTime() > 4000){
	            		serverStatus.remove(server);
	            	}
	            	
	            	
	            	Thread thread = new Thread(new BlockchainServerSending(server.getKey(), "hb|" + serverPort + "|" + sequenceNumber));
	                threadArrayList.add(thread);
	                thread.start();
	            }
	
	            for (Thread thread : threadArrayList) {
	                try {
	                    thread.join();
	                } catch (InterruptedException e) {
	                	
	                }
	            }
	
	            // increment the sequenceNumber
	            sequenceNumber += 1;
	
	            // sleep for two seconds
	            
	            Thread.sleep(2000);
            } catch (InterruptedException e) {
            	continue;
            }
        }
    }
}
