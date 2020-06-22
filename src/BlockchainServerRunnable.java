import java.io.*;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
 
public class BlockchainServerRunnable implements Runnable{
 
    private Socket clientSocket;
    private Blockchain blockchain;
    private HashMap<ServerInfo, Date> serverStatus;
    private ConcurrentLinkedQueue<Block> waitingBlocks;
    private int waitingBlocksSize;
 
    public BlockchainServerRunnable(Socket clientSocket, Blockchain blockchain, HashMap<ServerInfo, Date> serverStatus, ConcurrentLinkedQueue<Block> waitingBlocks, int waitingBlocksSize) {
        this.clientSocket = clientSocket;
        this.blockchain = blockchain;
        this.serverStatus = serverStatus;
        this.waitingBlocks = waitingBlocks;
        this.waitingBlocksSize = waitingBlocksSize;
    }
    
    private int compareHash(byte[] hashA, byte[] hashB){
    	
    	for(int i = 0; i < 32; i ++) {
    		if(hashA[i] < hashB[i]){
    			return -1;
    		}else if(hashA[i] > hashB[i]){
    			return 1;
    		}else {
    			continue;
    		}
    	}
    	return 0;
    }
    
    public void run() {
        try {
            serverHandler(clientSocket.getInputStream(), clientSocket.getOutputStream());
            clientSocket.close();
        } catch (IOException e) {
        }
    }
 
    public void serverHandler(InputStream clientInputStream, OutputStream clientOutputStream) {
        
    	
    	
    	
    	BufferedReader inputReader = new BufferedReader(
                new InputStreamReader(clientInputStream));
        PrintWriter outWriter = new PrintWriter(clientOutputStream, true);
        
        try {
    		
        	
            while (true) {
            	
                String inputLine = inputReader.readLine();
                if (inputLine == null) {
                    break;
                }
                
                String[] tokens = inputLine.split("\\|");
               
                //System.out.println(" I am receiving " + inputLine);

                switch (tokens[0]) {
                    case "tx":
                        if (blockchain.addTransaction(inputLine))
                            outWriter.print("Accepted\n\n");
                        else
                            outWriter.print("Rejected\n\n");
                        outWriter.flush();
                        break;
                    case "pb":
                        outWriter.print(blockchain.toString() + "\n");
                        outWriter.flush();
                        break;
                    case "cc":
                        return;
                    case "hb":
                    	ServerInfo local = new ServerInfo((((InetSocketAddress) clientSocket.getLocalSocketAddress()).getAddress()).toString().replace("/", ""), clientSocket.getLocalPort());
                        ServerInfo remote = new ServerInfo((((InetSocketAddress) clientSocket.getRemoteSocketAddress()).getAddress()).toString().replace("/", ""), Integer.parseInt(tokens[1])); 
                        try {
	                    	serverStatus.put(remote, new Date());
	                    	int count = 0;
	                        if (tokens[2].equals("0")) {
	                            ArrayList<Thread> threadArrayList = new ArrayList<>();
	                            for (ServerInfo server : serverStatus.keySet()) {
	                                if (server.equals(local) || server.equals(remote)) {
	                                    continue;
	                                }
	                                
	                                Thread thread = new Thread(new BlockchainServerSending(server, "si|" + local.getPort() + "|" + remote.getHost() + "|" + remote.getPort()));
	                                threadArrayList.add(thread);
	                                thread.start();
	                                
	                            }
	                            for (Thread thread : threadArrayList) {
										thread.join();
	                            }
	                            
	                        }
	          
                        } catch (InterruptedException e) {

						}
                    	break;
                    	
                    case "si":
                    	ServerInfo localSi = new ServerInfo((((InetSocketAddress) clientSocket.getLocalSocketAddress()).getAddress()).toString().replace("/", ""), clientSocket.getLocalPort());
                        ServerInfo remoteSi = new ServerInfo((((InetSocketAddress) clientSocket.getRemoteSocketAddress()).getAddress()).toString().replace("/", ""), Integer.parseInt(tokens[1]));
                    	try{
	                    	ServerInfo newServer = new ServerInfo(tokens[2], Integer.parseInt(tokens[3]));

	                    	serverStatus.put(newServer, new Date());
	                            // relay
	                            ArrayList<Thread> threadArrayList = new ArrayList<>();
	                            for (ServerInfo server : serverStatus.keySet()) {
	                            	if (server.equals(localSi) || server.equals(remoteSi) || server.equals(newServer)) {
	                                    continue;
	                                }

	                            	Thread thread = new Thread(new BlockchainServerSending(server, "si|" + localSi.getPort() + "|" + newServer.getHost() + "|" + newServer.getPort()));
	                                threadArrayList.add(thread);
	                                thread.start();
	                            }
	                            for (Thread thread : threadArrayList) {
	                                thread.join();
	                            }
	                        //}
                    	} catch (InterruptedException e) {

						}
                    	break;
                    case "lb":
                    	
                    	int remoteBCLength = Integer.parseInt(tokens[2]);
                    	byte[] hash = Base64.getDecoder().decode(tokens[3]);
                        ServerInfo remoteServer = new ServerInfo((((InetSocketAddress) clientSocket.getRemoteSocketAddress()).getAddress()).toString().replace("/", ""), Integer.parseInt(tokens[1])); 
                        byte[] head = blockchain.getHead().calculateHash();
                        blockchain.tryLock();
                    	if (blockchain.getLength() < remoteBCLength) {
                    			Block next = blockchain.getHead();
                    			byte[] b = hash;
                    			int i = 0;
                    			blockchain.setLength(0);
                    			while (next != null) {
                    				try {
                    					Socket localSocket = new Socket();
	                    				
	                    				localSocket.connect(new InetSocketAddress(remoteServer.getHost(), remoteServer.getPort()));
	                    				
	                    				PrintWriter printWriter = new PrintWriter(localSocket.getOutputStream(), true);
	                    				
                    					printWriter.println("cu|" + Base64.getEncoder().encodeToString(b));
                    					ObjectOutputStream oos = new ObjectOutputStream(localSocket.getOutputStream());
                    					ObjectInputStream ois = new ObjectInputStream(localSocket.getInputStream());
                    					
                    					Block returnBlock = (Block) ois.readObject();
                    					                    					
                    					next.setPreviousBlock(returnBlock);
                    					next = next.getPreviousBlock();
                    					
                    					b = returnBlock.getPreviousHash();
                    					
                    					
                    					if (i == 0) {
                    			            blockchain.setHead(returnBlock);
                    			            i++;
                    			            int length = blockchain.getLength()+1;
	                    					blockchain.setLength(length);
                    			            continue;
                    			        }
                    				                     					
                    					int length = blockchain.getLength()+1;
                    					blockchain.setLength(length);
                    					
                    					
                    					i ++;
                    					ois.close();
                    					localSocket.close();
                    				} catch (ClassNotFoundException e) {
									}
                    			}
	                    	
                    	blockchain.unLock();
                    		                  		
                    	} else if (blockchain.getLength() == remoteBCLength){
                    		blockchain.tryLock();
                    		if (compareHash(hash, head) == -1) {
                    				Block next = blockchain.getHead();
	                    			byte[] b = hash;
	                    			int i = 0;
	                    			blockchain.setLength(0);
	                    			while (blockchain.getLength() < remoteBCLength) {
	                    				
	                    				try {
	                    					Socket localSocket = new Socket();
		                    				
		                    				localSocket.connect(new InetSocketAddress(remoteServer.getHost(), remoteServer.getPort()));
		                    				
		                    				PrintWriter printWriter = new PrintWriter(localSocket.getOutputStream(), true);
		                    				
	                    					printWriter.println("cu|" + Base64.getEncoder().encodeToString(b));
	                    					ObjectOutputStream oos = new ObjectOutputStream(localSocket.getOutputStream());
	                    					ObjectInputStream ois = new ObjectInputStream(localSocket.getInputStream());
	                    					
	                    					Block returnBlock = (Block) ois.readObject();
	                    						                    					
	                    					next.setPreviousBlock(returnBlock);
	                    					next = next.getPreviousBlock();
	                    					
	                    					b = returnBlock.getPreviousHash();
	                    					
	                    					
	                    					if (i == 0) {
	                    			            blockchain.setHead(returnBlock);
	                    			            i++;
	                    			            int length = blockchain.getLength()+1;
		                    					blockchain.setLength(length);
	                    			            continue;
	                    			        }
	                    				                     					
	                    					int length = blockchain.getLength()+1;
	                    					blockchain.setLength(length);
	                    					
	                    					
	                    					i ++;
	                    					ois.close();
	                    					localSocket.close();
	                    				} catch (ClassNotFoundException e) {
										}
	                    			}	
                    		}
                    		blockchain.unLock();
                    	}
                    	break;
                    case "cu":
                    	
                    	ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
                		if(blockchain.getHead() == null) {
                			clientSocket.close();
                			return;
                		}
                    	if (tokens.length == 1) {
                    		oos.writeObject(blockchain.getHead());
                    		oos.flush();
                    	}
                    	else if(tokens.length == 2){
                    		
                    		byte[] requestHash = Base64.getDecoder().decode(tokens[1]);
                    		Block indexBlock = blockchain.getHead();
                  
                    		for(int i = 0; i < blockchain.getLength(); i ++) {
                    			if(compareHash(requestHash, indexBlock.calculateHash()) == 0) {
                    				oos.writeObject(indexBlock);
                            		oos.flush();
                    			}
                    			indexBlock = indexBlock.getPreviousBlock();
                    		}
                    	}
                    	break;
                    		
                    default:
                        outWriter.print("Error\n\n");
                        outWriter.flush();
                }
            }
            outWriter.flush();
        }  catch (IOException e) {
        } 
    }
}