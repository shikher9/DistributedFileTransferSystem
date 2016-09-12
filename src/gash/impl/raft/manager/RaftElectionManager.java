package gash.impl.raft.manager;

import gash.impl.raft.election.ElectionListener;
import gash.router.container.RoutingConf;
import gash.router.server.ServerInfo;
import gash.router.server.ServerOpUtil;
import gash.router.server.edges.EdgeList;

import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pipe.common.Common.Header;
import pipe.work.Work.AcknowledgementPayload.ResponseAction;
import pipe.work.Work.RaftHeader;
import pipe.work.Work.RaftMessage;
import pipe.work.Work.RaftMessagePayload;
import pipe.work.Work.RaftMessagePayload.RaftAction;
import pipe.work.Work.WorkMessage;

public class RaftElectionManager implements ElectionListener {

    protected static Logger logger = LoggerFactory.getLogger("RaftElectionManager");
    protected static AtomicReference<RaftElectionManager> instance = new AtomicReference<RaftElectionManager>();
    private ServerInfo _siInfo = null;

    static Integer leaderNode;
    private boolean isElectionInProgress = false;

    public boolean isElectionInProgress() {
        return isElectionInProgress;
    }

    public void setElectionInProgress(boolean isElectionInProgress) {
        this.isElectionInProgress = isElectionInProgress;
    }

    public static RoutingConf conf;
    private int electionCycle;
    long lastknownBeat;

    public NodeState currentState;

    public NodeState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(NodeState currentState) {
        this.currentState = currentState;
    }
    public NodeState leaderState;
    public NodeState followerState;
    public NodeState candidateState;

    private RaftElectionManager() {
        leaderNode = -1;
        electionCycle = -1;
        lastknownBeat = 0;
        leaderState = new LeaderState();
        followerState = new FollowerState();
        candidateState = new CandidateState();
        currentState = followerState;
        isElectionInProgress = false;
    }

    public static RaftElectionManager initManager(RoutingConf conf) {
        RaftElectionManager.conf = conf;
        instance.compareAndSet(null, new RaftElectionManager());
        return instance.get();
    }

    public static Integer getLeaderNode() {
        return leaderNode;
    }

    public static void setLeaderNode(int leader) {
        leaderNode = leader;
    }

    @SuppressWarnings("static-access")
    public Integer whoIsTheLeader() {
        return this.leaderNode;
    }

    public static RaftElectionManager getInstance() {
        return instance.get();
    }

    public void SetServerInfo(ServerInfo siInfo) {
        _siInfo = siInfo;

    }

    public ServerInfo GetSetServerInfo() {
        return _siInfo;
    }

    @SuppressWarnings("static-access")
    public void startElection() {

        //clear previous election values
        leaderNode = -1;
        NodeDataManager.getInstance().getNodeData().setVotedFor("");

        logger.info("Node " + conf.getNodeId() + " is starting election..");

        RaftHeader.Builder rh = RaftHeader.newBuilder();
        rh.setOriginatorNodeId(conf.getNodeId());
        rh.setTime(System.currentTimeMillis());

        RaftMessagePayload.Builder rp = RaftMessagePayload.newBuilder();

        //increment term
        electionCycle = NodeDataManager.getInstance().getNodeData().getCurrentTerm() + 1;
        NodeDataManager.getInstance().getNodeData().setCurrentTerm(electionCycle);
        NodeDataManager.getInstance().getNodeData().setVoteCount(1);

        //ask for votes from other nodes
        rp.setAction(RaftAction.REQUESTVOTE);
        rp.setTerm(electionCycle);

        logger.info("Setting term of node " + conf.getNodeId() + " to " + electionCycle);

        WorkMessage.Builder b = WorkMessage.newBuilder();
        RaftMessage.Builder rfmessage = RaftMessage.newBuilder();
        rfmessage.setRaftHeader(rh.build());
        rfmessage.setRaftMessagePayload(rp.build());
        b.setRaftMessage(rfmessage);


        this.setCurrentState(candidateState);

        logger.info("CurrentState" + candidateState.toString());
        Header.Builder hb = Header.newBuilder();
        hb.setNodeId(conf.getNodeId());
        hb.setDestination(2);
        hb.setTime(System.currentTimeMillis());
        b.setHeader(hb);
        b.setSecret(999999);

        //broadcast vote request to all the nodes in the cluster
        int _totalActiveConnections = ServerOpUtil.broadcast(b.build());
        if (_totalActiveConnections < 1) {
            logger.info("No followers exist. Settign current node as leader");
            concludeWith(true, conf.getNodeId());
            this.setCurrentState(leaderState);
        }

        //ConnectionManager.broadcast(b.build());
    }

    //@SuppressWarnings("static-access")
    public void processRequest(WorkMessage mgmt) {

        //Server has received a vote request from other node+
        if (mgmt.getRaftMessage().getRaftMessagePayload().getAction() == RaftAction.REQUESTVOTE) {
            logger.info("I (" + conf.getNodeId() + ") got a request to vote from " + mgmt.getRaftMessage().getRaftHeader().getOriginatorNodeId());
            currentState.SetServerInfo(_siInfo);
            currentState.repondToVoteRequest(mgmt);
        } //Server has received a vote from other node
        else if (mgmt.getRaftMessage().getAckPayload().getAction() == ResponseAction.CASTVOTE) {
            logger.info("I (" + conf.getNodeId() + ") got a vote from " + mgmt.getRaftMessage().getRaftHeader().getOriginatorNodeId());
            currentState.SetServerInfo(_siInfo);
            currentState.repondToCastVote(mgmt);
        } //Server has received a leadership declaration from other node
        else if (mgmt.getRaftMessage().getRaftMessagePayload().getAction() == RaftAction.THELEADERIS || mgmt.getRaftMessage().getRaftMessagePayload().getAction() == RaftAction.LEADER) {
            logger.info("I (" + conf.getNodeId() + ") got a leadership decralation from " + mgmt.getRaftMessage().getRaftHeader().getOriginatorNodeId());
            RaftTimer.setLastBeatTime(System.currentTimeMillis());
            logger.info("Set timer to" + System.currentTimeMillis());
            currentState.SetServerInfo(_siInfo);
            currentState.respondToLeaderElected(mgmt);
        }

    }

    @SuppressWarnings("static-access")
    @Override
    public void concludeWith(boolean success, Integer leaderID) {
        if (success) {
            logger.info("The leader is " + leaderID);
            this.leaderNode = leaderID;
            if(_siInfo == null) {
                logger.info("Server Info null");
            }
            _siInfo.setLeaderNodeId(leaderID);
        }
    }

}
