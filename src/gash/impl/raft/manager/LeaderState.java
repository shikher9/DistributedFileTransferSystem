package gash.impl.raft.manager;

import io.netty.channel.Channel;
import gash.impl.raft.manager.NodeData.RaftStatus;
import gash.router.server.ServerInfo;
import gash.router.server.ServerOpUtil;
import gash.router.server.edges.EdgeList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pipe.common.Common.Header;
import pipe.work.Work.AcknowledgementPayload;
import pipe.work.Work.AcknowledgementPayload.ResponseAction;
import pipe.work.Work.RaftHeader;
import pipe.work.Work.RaftMessage;
import pipe.work.Work.RaftMessagePayload;
import pipe.work.Work.RaftMessagePayload.RaftAction;
import pipe.work.Work.WorkMessage;



public class LeaderState implements NodeState {
	
	protected static Logger logger = LoggerFactory.getLogger("node");

	private static int countAckAppend = 0;
	
	ServerInfo _siInfo = new ServerInfo();
	
	
	public LeaderState()
	{
		RaftElectionManager.getInstance();
	}
	
	public void SetServerInfo(ServerInfo siInfo)
	{
		_siInfo=siInfo;
	}
	
	public ServerInfo  GetSetServerInfo()
	{
		return _siInfo;
	}

	@Override
	public void repondToVoteRequest(WorkMessage mgmt) {
		
		RaftMessagePayload rp = mgmt.getRaftMessage().getRaftMessagePayload();
		
		WorkMessage.Builder response = WorkMessage.newBuilder();
		
		if (rp.getAction() == RaftAction.REQUESTVOTE && RaftElectionManager.getInstance().isElectionInProgress() == false) 		
		{
			RaftElectionManager.getInstance().setElectionInProgress(true);
					
			String votedFor = NodeDataManager.getInstance().getNodeData().getVotedFor();
			int currentTerm = NodeDataManager.getInstance().getNodeData().getCurrentTerm();
			int myNodeId = Integer.parseInt(NodeDataManager.getInstance().getNodeData().getNodeId());
			String orginatorId = String.valueOf(mgmt.getRaftMessage().getRaftHeader().getOriginatorNodeId());
			RaftHeader.Builder responseHeader = RaftHeader.newBuilder();
			responseHeader.setOriginatorNodeId(myNodeId);
			AcknowledgementPayload.Builder ackPayload = AcknowledgementPayload.newBuilder();		
			logger.info("---> Voting for node : "+ orginatorId);
			NodeDataManager.getNodeData().setCurrentTerm(rp.getTerm());
			
			ackPayload.setTerm(rp.getTerm());
				
			if(mgmt.getRaftMessage().getRaftMessagePayload().getTerm() > currentTerm && votedFor.isEmpty()){
				ackPayload.setResponse("YES");
				NodeDataManager.getInstance().getNodeData().setVotedFor(orginatorId);
			}else{
				
				ackPayload.setResponse("NO");
			}
				
			RaftElectionManager.getInstance().concludeWith(false, -1);
			
			ackPayload.setAction(ResponseAction.CASTVOTE);
			
           RaftMessage.Builder rfmessage = RaftMessage.newBuilder();
			
			rfmessage.setRaftHeader(responseHeader.build());
			rfmessage.setAckPayload(ackPayload.build());

			response.setRaftMessage(rfmessage);

			response.setSecret(9999999);
			
			logger.info("Node "+myNodeId+" casted a vote to node "+ orginatorId);
		    //Channel ch =EdgeList.GetConnectionChannel(mgmt.getRaftHeader().getOriginatorNodeId());
		    //ch.writeAndFlush(response.build());
		    
			Header.Builder hb = Header.newBuilder();
		    hb.setNodeId(response.getRaftMessage().getRaftHeader().getOriginatorNodeId());
		   // hb.setDestination(mgmt.getRaftHeader().g);
		    hb.setTime(System.currentTimeMillis());
		    response.setSecret(999999);
	        response.setHeader(hb);
	        ServerOpUtil.GetConnectionChannel(mgmt.getRaftMessage().getRaftHeader().getOriginatorNodeId()).writeAndFlush(response.build());

					
			//ConnectionManager.getConnection(mgmt.getRaftHeader().getOriginatorNodeId(), true).writeAndFlush(response.build());
		}
		
	}

	@Override
	public void repondToCastVote(WorkMessage msg) {
	}

	@Override
	public void respondToLeaderElected(WorkMessage msg) {	
		RaftElectionManager.getInstance().setCurrentState(RaftElectionManager.getInstance().followerState);
		RaftElectionManager.getInstance().concludeWith(true, msg.getRaftMessage().getRaftHeader().getOriginatorNodeId());
		RaftElectionManager.getInstance().setElectionInProgress(false);
		NodeDataManager.getNodeData().setNodeStatus(RaftStatus.FOLLOWER);
	}

	@Override
	public void respondToTheLeaderIs(WorkMessage msg) {
	}

	@Override
	public void respondToWhoIsTheLeader(WorkMessage msg) {
	}

	@Override
	public void appendLogs(WorkMessage msg) {
	}

	@Override
	public void replicateLogs(WorkMessage msg) {
	}

	
	// call when ack_append msg is processed.
	@Override
	public void processAckAppendMessage(WorkMessage msg) {
		
		logger.info("Ack Append received ...");
		if(countAckAppend > (ServerOpUtil.GetTotalConnectionCount() +1)/2){
			countAckAppend = 0;
			//give response to client
		} else {
			countAckAppend++;
		}
		
		
	}

}
