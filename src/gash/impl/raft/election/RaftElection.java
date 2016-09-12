package gash.impl.raft.election;

import gash.impl.raft.manager.NodeDataManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pipe.work.Work.WorkMessage;


public class RaftElection implements Election{

	protected static Logger logger = LoggerFactory.getLogger("Raft");

	private Integer nodeId;
	private int count = 0;
	private ElectionListener listener;
	private boolean isActiveElection;
	NodeDataManager nodeDataManager = NodeDataManager.getInstance();
	private int voteCount;


	public RaftElection() {
		this.setVoteCount(1);
		this.setActiveElection(false);
	}

	@Override
	public void setListener(ElectionListener listener) {
		this.listener = listener;
	}

	public boolean isActiveElection() {
		return isActiveElection;
	}

	public void setActiveElection(boolean isActiveElection) {
		this.isActiveElection = isActiveElection;
	}

	@Override
	public WorkMessage process(WorkMessage mgmt) {

		//System.out.println("Raft Election called");

		WorkMessage.Builder response = WorkMessage.newBuilder();
		
		/*} else if (ap.getAction() == ResponseAction.CASTVOTE && RaftElectionManager.getLeaderNode() == -1) {

		
			} else {

				// this is me!
			}
		}*/
		return response.build();
	}


	public int getVoteCount() {
		return voteCount;
	}

	public void setVoteCount(int voteCount) {
		this.voteCount = voteCount;
	}

	@Override
	public Integer getElectionId() {
		// dnno if called
		return -1;
	}

	private void setElectionId(int id) {
		// dunno if called
	}

	public Integer getNodeId() {
		return nodeId;
	}

	@Override
	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
	}

	@Override
	public synchronized void clear() {
		setVoteCount(1);
		NodeDataManager.getNodeData().setVotedFor("");
	}

	@Override
	public boolean isElectionInprogress() {

		return this.isActiveElection;
	}

	private void notify(boolean success, Integer leader) {
		if (listener != null)
			listener.concludeWith(success, leader);
	}

	@Override
	public Integer createElectionID() {
		return ElectionIDGenerator.nextID();
	}

	@Override
	public Integer getWinner() {
		// dunno if called
		return null;
	}

	

}
