import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BlockchainCatchUpRunnable implements Runnable {
	private ServerInfo server;
	private Blockchain blockchain;
	String message;
	private ConcurrentLinkedQueue<Block> waitingBlocks;
	
	public BlockchainCatchUpRunnable(ServerInfo server, Blockchain blockchain, String message, ConcurrentLinkedQueue<Block> waitingBlocks) {
		this.blockchain = blockchain;
        this.server = server;
        this.message = message;
        this.waitingBlocks = waitingBlocks;
    }
	
	@Override
    public void run() {
		try {
			
			if(message.equals("cu")){
				Socket localSocket = new Socket();
				
				localSocket.connect(new InetSocketAddress(server.getHost(), server.getPort()));
				
				PrintWriter printWriter = new PrintWriter(localSocket.getOutputStream(), true);
								
					printWriter.println(message);
					
					ObjectOutputStream oos = new ObjectOutputStream(localSocket.getOutputStream());
					ObjectInputStream ois = new ObjectInputStream(localSocket.getInputStream());
					Block returnBlock = (Block) ois.readObject();
					
					
					if(returnBlock == null) {
						localSocket.close();
						ois.close();
						return;
					}
					
				
					blockchain.setHead(returnBlock);
					
					
					int length = blockchain.getLength()+1;
					blockchain.setLength(length);
					
					ois.close();
				
				localSocket.close();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block

    	}	
	}
}
