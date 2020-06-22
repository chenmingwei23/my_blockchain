import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class BlockchainServerSending implements Runnable{

    private ServerInfo server;
    private String message;

    public BlockchainServerSending(ServerInfo server, String message) {
        this.server = server;
        this.message = message;
    }

    @Override
    public void run() {
    	
        
        Socket localServer = new Socket();
        
      
        	try {
        		localServer.connect(new InetSocketAddress(server.getHost(), server.getPort()), 2000);
				PrintWriter printWriter = new PrintWriter(localServer.getOutputStream(), true);
				printWriter.println(message);
	        	printWriter.flush();

	        // close printWriter and socket
	        	printWriter.close();
	        	localServer.close();
        	} catch (IOException e) {

        	}
    	return;
    }
}
