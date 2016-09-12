package gash.impl.raft.manager;

import gash.router.server.ServerInfo;
import pipe.work.Work.WorkMessage;



public interface NodeState {

	public void repondToVoteRequest(WorkMessage msg);
	
	public void repondToCastVote(WorkMessage msg);
	
	public void respondToLeaderElected(WorkMessage msg);
	
	public void appendLogs(WorkMessage msg);
	
	public void replicateLogs(WorkMessage msg);
	
	public void respondToTheLeaderIs(WorkMessage msg);
	
	public void respondToWhoIsTheLeader(WorkMessage msg);
	
	public void processAckAppendMessage(WorkMessage msg);
	
	public void SetServerInfo(ServerInfo siInfo);
		
	public ServerInfo  GetSetServerInfo();
	

}
