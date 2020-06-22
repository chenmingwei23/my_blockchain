import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

public class BlockchainLatestBlockRunnable implements Runnable {
	
	private HashMap<ServerInfo, Date> serverStatus;
	private Blockchain blockchain;
	private int localPort;
	
	public BlockchainLatestBlockRunnable(HashMap<ServerInfo, Date> serverStatus, Blockchain blockchain, int localPort) {
		this.blockchain = blockchain;
        this.serverStatus = serverStatus;
        this.localPort = localPort;
    }
	
	@Override
    public void run() {
		while(true) {	
			if(blockchain.getLength() != 0) {
					
				
				if(serverStatus.size() < 5) {

					ArrayList<Thread> threadArrayList = new ArrayList<>();
					
					for(Entry<ServerInfo, Date> server : serverStatus.entrySet()) {
						
						byte[] head = blockchain.getHead().calculateHash();						
						Thread thread = new Thread(new BlockchainServerSending(server.getKey(), "lb|" + localPort +"|"+blockchain.getLength() + "|" + Base64.getEncoder().encodeToString(head)));
		                threadArrayList.add(thread);
		                thread.start();
					}
					
					for (Thread thread : threadArrayList) {
		                try {
		                    thread.join();
		                } catch (InterruptedException e) {
		                	
		                }
		            }
				}
				else {
					ArrayList<Thread> threadArrayList = new ArrayList<>();
					HashMap<ServerInfo, Date> randomServerStatus = new HashMap<ServerInfo, Date>();
					int i = 0;
					while(i < 5) {
						
						Random random = new Random();
						List<ServerInfo> servers = new ArrayList<ServerInfo>(serverStatus.keySet());
						ServerInfo randomServer = servers.get(random.nextInt(servers.size()));
						if(randomServerStatus.containsKey(randomServer)){
							continue;
						}
						i++;
						randomServerStatus.put(randomServer, serverStatus.get(randomServer));
						
						byte[] head = blockchain.getHead().getCurrentHash();
						Thread thread = new Thread(new BlockchainServerSending(randomServer, "lb|" + localPort +"|"+blockchain.getLength() + "|" + Base64.getEncoder().encodeToString(head)));
		                threadArrayList.add(thread);
		                thread.start();
		                
					}
					for (Thread thread : threadArrayList) {
		                try {
		                    thread.join();
		                } catch (InterruptedException e) {
		                	
		                }
		            }	
				}
			}
			try{
				System.out.println("latest blockchain is \n"+blockchain.toString());
				Thread.sleep(2000);
		} catch(InterruptedException e) {
		}
	}
	}
}
