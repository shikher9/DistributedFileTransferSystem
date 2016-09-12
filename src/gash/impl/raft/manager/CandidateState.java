package gash.impl.raft.manager;


import gash.impl.raft.manager.NodeData.RaftStatus;
import gash.jdbc.persistance.*;
import gash.router.server.ServerInfo;
import gash.router.server.ServerOpUtil;
import gash.router.server.edges.EdgeInfo;
import gash.router.server.edges.EdgeList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.Descriptors.FieldDescriptor;

import pipe.common.Common.Header;
import pipe.work.Work.AcknowledgementPayload;
import pipe.work.Work.LogList;
import pipe.work.Work.RaftHeader;
import pipe.work.Work.RaftMessage;
import pipe.work.Work.RaftMessageOrBuilder;
import pipe.work.Work.RaftMessagePayload;
import pipe.work.Work.RaftMessagePayload.RaftAction;
import pipe.work.Work.WorkMessage;


public class CandidateState implements NodeState{

	protected static Logger logger = LoggerFactory.getLogger("node");
	ServerInfo _siInfo = new ServerInfo();
	RaftElectionManager mgr=null;
	//int voteCount;

	public int getVoteCount() {
		return NodeDataManager.getInstance().getNodeData().getVoteCount();
	}

	
	public void SetServerInfo(ServerInfo siInfo)
	{
		_siInfo=siInfo;
	}
	
	public ServerInfo  GetSetServerInfo()
	{
		return _siInfo;
	}
	
	public CandidateState() {

		mgr = RaftElectionManager.getInstance();
		
		NodeDataManager.getInstance().getNodeData().setVoteCount(1);
		//voteCount = 1;
	}

	@Override
	public void repondToVoteRequest(WorkMessage msg) {

		msg.getRaftMessage().getAckPayload();
		WorkMessage.newBuilder();

		System.out.println("I am already a candidate. I would not vote");
		System.out.println("transforming");
	
		//ChangeCandidateToFollower(msg);
		//repondToCastVote(msg);
	}

	public void ChangeCandidateToFollower(WorkMessage msg)
	{
		RaftElectionManager.getInstance().setCurrentState(new FollowerState());
		RaftElectionManager.getInstance().processRequest(msg);
	}

	@SuppressWarnings("static-access")
	@Override
	public void repondToCastVote(WorkMessage msg) {

		AcknowledgementPayload ap = msg.getRaftMessage().getAckPayload();
		WorkMessage.Builder response = WorkMessage.newBuilder();
		int totalConnections = ServerOpUtil.GetTotalConnectionCount();

		logger.info("Candidate received a vote..");

		int currentTerm = NodeDataManager.getInstance().getNodeData().getCurrentTerm();

		int myNodeId = Integer.parseInt(NodeDataManager.getInstance().getNodeData().getNodeId());
		RaftHeader.Builder responseHeader = RaftHeader.newBuilder();
		responseHeader.setOriginatorNodeId(myNodeId);

		RaftMessagePayload.Builder responsePayload = RaftMessagePayload.newBuilder();

		if(ap.getResponse() == "YES"||ap.getResponse().equals("YES") )
		{
			//this.setVoteCount(this.getVoteCount()+1);
			NodeDataManager.getInstance().getNodeData().setVoteCount(NodeDataManager.getInstance().getNodeData().getVoteCount()+1);
		}

		if (((totalConnections+1)/2) <= this.getVoteCount() ) {					
			logger.info("For node: "+myNodeId+", total count of votes is: "+this.getVoteCount() +" and total connections: "+totalConnections);
			logger.info("This candidiate has received the majority. Node + myNodeId + has been elected as the leader..");

			responsePayload.setAction(RaftAction.LEADER);
			responsePayload.setLeaderId(myNodeId);			
			responsePayload.setTerm(currentTerm);
			
			responseHeader.setOriginatorNodeId(myNodeId);
			
			RaftElectionManager.getInstance().concludeWith(true, myNodeId);
			RaftElectionManager.getInstance().setElectionInProgress(false);
			
			NodeDataManager.getNodeData().setNodeStatus(RaftStatus.LEADER);
			
			RaftElectionManager.setLeaderNode(myNodeId);
			//RaftMessageOrBuilder. rfmessage = RaftMessageOrBuilder.
			RaftMessage.Builder rfmessage = RaftMessage.newBuilder();
			
			rfmessage.setRaftHeader(responseHeader.build());
			rfmessage.setRaftMessagePayload(responsePayload.build());

			response.setRaftMessage(rfmessage);

			RaftElectionManager.getInstance().setCurrentState(RaftElectionManager.getInstance().leaderState);
			_siInfo.setLeader(true);
			_siInfo.setLeaderNodeId(myNodeId);
			
		}
		else
		{	
			System.out.println("Node: "+myNodeId+" (term: "+currentTerm+") has received vote from node "+msg.getRaftMessage(). getRaftHeader().getOriginatorNodeId());
			System.out.println("Total count of votes with node: "+myNodeId+" is "+this.getVoteCount());
			_siInfo.setLeader(false);
		}
		
		
		//ConnectionManager.broadcast(response.build());
		Header.Builder hb = Header.newBuilder();
	    hb.setNodeId(response.getRaftMessage().getRaftHeader().getOriginatorNodeId());
	   // hb.setDestination(mgmt.getRaftHeader().g);
	    hb.setTime(System.currentTimeMillis());
        response.setHeader(hb);
        response.setSecret(999999);
        
        // Build log entry
        LogList.Builder log = LogList.newBuilder();
        pipe.work.Work.LogEntry.Builder logEntry =  pipe.work.Work.LogEntry.newBuilder();
        pipe.work.Work.Operation.Builder operation =  pipe.work.Work.Operation.newBuilder();
        
         //Set userid
        operation.setCmd(NodeDataManager.getInstance().getNodeData().getNodeId());
        logEntry.setTerm(myNodeId);
        //set file name
        operation.setFileName("TEST99.txt");
        logEntry.setOperation(operation.build());
        log.addLogEntry(logEntry);
        //log.setLogEntry(1, logEntry.build());
        //broadcast log entried
		response.setLogList(log);
		
		ReplicationManager.ReplicateFileToFollowers(myNodeId, response.build(), _siInfo);
		
	     //Store to db
        //RaftDAO _dao = new RaftDAO();
        try {
			//_dao.SetLogEntries(logEntry);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
	    ServerOpUtil.broadcast(response.build());

	}

	@Override
	public void respondToLeaderElected(WorkMessage msg) {

		RaftElectionManager.getInstance().setCurrentState(RaftElectionManager.getInstance().followerState);
		//voteCount = 0;
		RaftElectionManager.getInstance().concludeWith(true, msg.getRaftMessage().getRaftHeader().getOriginatorNodeId());
		RaftElectionManager.getInstance().setElectionInProgress(false);
		System.out.println("Saving the leader state. Its is "+msg.getRaftMessage().getRaftHeader().getOriginatorNodeId());


	}

	@Override
	public void respondToTheLeaderIs(WorkMessage msg) {

		RaftElectionManager.getInstance().setCurrentState(RaftElectionManager.getInstance().followerState);



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

	@Override
	public void processAckAppendMessage(WorkMessage msg) {

	}

	

	

}
