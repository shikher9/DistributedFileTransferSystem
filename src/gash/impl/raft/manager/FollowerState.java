package gash.impl.raft.manager;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import gash.router.server.ServerInfo;
import gash.router.server.ServerOpUtil;
import gash.router.server.edges.EdgeList;
import gash.router.server.edges.EdgeMonitor;

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



public class FollowerState implements NodeState {
	protected static Logger logger = LoggerFactory.getLogger("node");
	ServerInfo _siInfo = new ServerInfo();
	
	private RaftElectionManager electionManager;

	public FollowerState()
	{
		this.electionManager = RaftElectionManager.getInstance();
	}
	
	@Override
	public void SetServerInfo(ServerInfo siInfo)
	{
		_siInfo=siInfo;
	}
	
	@Override
	public ServerInfo  GetSetServerInfo()
	{
		return _siInfo;
	}

	@Override
	public void repondToVoteRequest(WorkMessage mgmt) {
		
		RaftMessagePayload rp = mgmt.getRaftMessage().getRaftMessagePayload();
		
		WorkMessage.Builder response = WorkMessage.newBuilder();
		
		if (rp.getAction() == RaftAction.REQUESTVOTE) 		
		{
			RaftElectionManager.getInstance().setElectionInProgress(true);	
			String votedFor = NodeDataManager.getInstance().getNodeData().getVotedFor();
			int currentTerm = NodeDataManager.getInstance().getNodeData().getCurrentTerm();
			int myNodeId = Integer.parseInt(NodeDataManager.getInstance().getNodeData().getNodeId());
			String orginatorId = String.valueOf(mgmt.getRaftMessage().getRaftHeader().getOriginatorNodeId());
			RaftHeader.Builder responseHeader = RaftHeader.newBuilder();
			responseHeader.setOriginatorNodeId(myNodeId);
			AcknowledgementPayload.Builder ackPayload = AcknowledgementPayload.newBuilder();		
			
			NodeDataManager.getNodeData().setCurrentTerm(rp.getTerm());
			
			ackPayload.setTerm(rp.getTerm());
				
			/*if(mgmt.getRaftMessagePayload().getTerm() > currentTerm && votedFor.isEmpty()){
				ackPayload.setResponse("YES");
				NodeDataManager.getInstance().getNodeData().setVotedFor(orginatorId);
			} */
			if(mgmt.getRaftMessage().getRaftMessagePayload().getTerm() > currentTerm && votedFor.isEmpty()){
				ackPayload.setResponse("YES");
				NodeDataManager.getInstance().getNodeData().setVotedFor(orginatorId);
			}
			else{
				ackPayload.setResponse("NO");
			}
				
			RaftElectionManager.getInstance().concludeWith(false, -1);
			ackPayload.setAction(ResponseAction.CASTVOTE);
			
            RaftMessage.Builder rfmessage = RaftMessage.newBuilder();
			
			rfmessage.setRaftHeader(responseHeader.build());
			rfmessage.setAckPayload(ackPayload.build());

			response.setRaftMessage(rfmessage);

			response.setSecret(999999);
			Header.Builder hb = Header.newBuilder();
		    hb.setNodeId(mgmt.getRaftMessage().getRaftHeader().getOriginatorNodeId());
		   // hb.setDestination(mgmt.getRaftHeader().g);
		    hb.setTime(System.currentTimeMillis());
            response.setHeader(hb);
			
			logger.info("Node "+myNodeId+" casted a vote to node "+ orginatorId);
		    Channel ch =ServerOpUtil.GetConnectionChannel(mgmt.getRaftMessage().getRaftHeader().getOriginatorNodeId());
		    ChannelFuture ft =  ch.writeAndFlush(response.build());
			logger.info("Status"+ft);
		   

			//ConnectionManager.getConnection(mgmt.getRaftHeader().getOriginatorNodeId(), true).writeAndFlush(response.build());

		}
		
	}

	@Override
	public void repondToCastVote(WorkMessage msg) {
		
	}

	@Override
	public void respondToLeaderElected(WorkMessage msg) {
		RaftElectionManager.getInstance().concludeWith(true,msg.getRaftMessage().getRaftHeader().getOriginatorNodeId());
		RaftElectionManager.getInstance().setElectionInProgress(false);
		RaftElectionManager.getInstance().setCurrentState(RaftElectionManager.getInstance().followerState);
	}

	@Override
	public void respondToTheLeaderIs(WorkMessage msg) {
		// TODO Auto-generated method stub

	}

	@Override
	public void respondToWhoIsTheLeader(WorkMessage msg) {
		// TODO Auto-generated method stub

	}

	@Override
	public void appendLogs(WorkMessage msg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void replicateLogs(WorkMessage msg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void processAckAppendMessage(WorkMessage msg) {
		// TODO Auto-generated method stub
		
	}


}
