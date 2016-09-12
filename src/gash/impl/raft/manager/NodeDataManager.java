package gash.impl.raft.manager;

import gash.router.container.RoutingConf;

import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class NodeDataManager {
	private static NodeData nodeData = null; 
	protected static Logger logger = LoggerFactory.getLogger("NodeDataManager");
	protected static AtomicReference<NodeDataManager> instance = new AtomicReference<NodeDataManager>();
	

	@SuppressWarnings("static-access")
	public static NodeDataManager initManager(RoutingConf conf) {
		instance.compareAndSet(null, new NodeDataManager());
		instance.get().setNodeData(0, ""+conf.getNodeId(), "", gash.impl.raft.manager.NodeData.RaftStatus.FOLLOWER);
		
		return instance.get();
	}

	public static NodeDataManager getInstance() {
		return instance.get();
	}

	public static void setNodeData(int currentTerm, String nodeId, String votedFor, NodeData.RaftStatus status) {
		nodeData.setCurrentTerm(currentTerm);
		nodeData.setNodeId(nodeId);
		nodeData.setNodeStatus(status);
		nodeData.setVotedFor(votedFor);
	}
	
	public static NodeData getNodeData() {
		return nodeData;
	}

	protected NodeDataManager() {
		nodeData = new NodeData();
	}
}
