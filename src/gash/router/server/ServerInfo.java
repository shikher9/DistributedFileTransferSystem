package gash.router.server;

import java.util.concurrent.LinkedBlockingDeque;

import gash.impl.raft.manager.FileTransferInfo;
import gash.router.container.RoutingConf;
import gash.router.server.edges.EdgeInfo;
import gash.router.server.edges.EdgeList;
import gash.router.server.queueing.QueueItem;

/*
 * A class for storing server information
 * 
 */
public class ServerInfo {

	// secret
	private int secret;

	// variable to hold leader status.
	private boolean isLeader;

	// current leader id
	private int leaderNodeId;
	
	private int nodeId;
	
	//total connected edges
	private static int totalConnections;

	// information about all connected servers to this server
	private static EdgeList connectedEdges;

	// timestamp of last heartbeat received from leader
	private long lastHBTSFromLeader;
	
	private FileTransferInfo _fileTransferInfo;
	
    private LinkedBlockingDeque<QueueItem> pendingQueue;
    
    private RoutingConf _routingConf;

	public ServerInfo() {
		connectedEdges = new EdgeList();
	}

	public boolean isLeader() {
		return isLeader;
	}

	public void setLeader(boolean isLeader) {
		this.isLeader = isLeader;
	}

	public int getLeaderNodeId() {
		return leaderNodeId;
	}

	public void setLeaderNodeId(int leaderNodeId) {
		this.leaderNodeId = leaderNodeId;
	}

	public static EdgeList getConnectedEdges() {
		return connectedEdges;
	}
	
	public static int GetTotalConnections()
	{
		return totalConnections;
		
	}
	public  void setConnectedEdges(EdgeList connectedEdges) {
		this.connectedEdges = connectedEdges;
	}

	public long getLastHBTSFromLeader() {
		return lastHBTSFromLeader;
	}

	public void setLastHBTSFromLeader(long lastHBTSFromLeader) {
		this.lastHBTSFromLeader = lastHBTSFromLeader;
	}

	public int getSecret() {
		return secret;
	}

	public void setSecret(int secret) {
		this.secret = secret;
	}
	public LinkedBlockingDeque<QueueItem> getPendingQueue() {
        return pendingQueue;
    }

    public void setPendingQueue(LinkedBlockingDeque<QueueItem> pendingQueue) {
        this.pendingQueue = pendingQueue;
    }
    public void setConf(RoutingConf routingConf) {
		this._routingConf = routingConf;
	}

	public RoutingConf getConf() {
		return _routingConf;
	}
	

	public int getFileTransferInfo() {
		return secret;
	}

	public void setFileTransferInfo(int secret) {
		this.secret = secret;
	}
	
	public void addFileToSendList(FileTransferInfo fileTransferInfo) {
		for(EdgeInfo ei : connectedEdges.map.values()) {
			ei.GetFilesToTransfer().add(_fileTransferInfo);
		}
	}

	public int getNodeId() {
		return nodeId;
	}

	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
	}
	
	
		
	
}
