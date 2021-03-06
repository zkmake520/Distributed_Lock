import java.util.*;
import java.io.*;
import java.net.*;

public class Node extends Thread{
	private String id;
	private int port;
	private Client client;
	private Server server;
	private List<Integer> ports;
	private TimeStamp timeStamp;
	private Queue<String> waitingQueue;
	private HashMap<String,Socket> waitingSockets;
	private NodeState state;


	public Node(String id,int port){
		this.id = id;
		this.port = port;
		timeStamp = new TimeStamp();
		waitingQueue = new LinkedList<>();
		waitingSockets = new HashMap<>();
		client = new Client(id,port,timeStamp,waitingQueue);
		server = new Server(id,port,state,timeStamp,waitingQueue,waitingSockets);
		startListen();
	}	
	public void setNeighboors(List<Integer> ports){
		this.ports = ports;
	}

	public List<Integer> getNeighboors(){
		return ports;
	}

	private boolean sendRequestMessage(){
		timeStamp.increTime();
		setState(NodeState.WAIT);
		Log.out("Node: "+id+" current timeStamp:"+timeStamp.getTime()+" state:"+state);
		boolean rst = this.client.sendRequestMessage(ports);
		return rst;
	}

	private void setState(NodeState state){
		this.state = state;
		this.server.setState(state);
	}
	private void startListen(){
		server.start();
	}

	private void releaseLock(){
		setState(NodeState.FREE);
		this.client.sendLockReleaseMessage(waitingSockets);
	}
	private void cleanup(){
		if(!waitingQueue.isEmpty()){
			while(!waitingQueue.isEmpty()){
				String addr = waitingQueue.poll();	
				Socket socket = waitingSockets.get(addr);
				try{
					socket.close();
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		this.server.finishServer();
	}
	@Override
	public void run(){
		int cnt = 0;
		while(cnt++ < 2){
			try{
				Thread.sleep(500);
				boolean requestLock = sendRequestMessage();
				if(requestLock == true){
					//Critical Section
					setState(NodeState.HOLD);
					timeStamp.updateTimeStamp(id);
					Log.out("Node: "+id+" achieved the lock current timeStamp:"+timeStamp.getTime()+" state:"+state);
					Log.out("Node: "+id+" enter critical section");
					Thread.sleep(500);
					Log.out("Node: "+id+" leave critial section and release lock");
					releaseLock();
				}
				else{
					Log.out("Node: "+id+" request lock failed");
				}	
			}catch(Exception e){
				e.printStackTrace();
			} 
		}
		Log.out("Node: "+id+" finished job");
		cleanup();	
		return;

	}
}