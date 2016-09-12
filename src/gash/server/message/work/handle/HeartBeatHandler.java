package gash.server.message.work.handle;

import gash.impl.raft.manager.RaftElectionManager;
import gash.impl.raft.manager.RaftTimer;
import gash.router.server.ServerInfo;
import gash.router.server.ServerState;
import io.netty.channel.Channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pipe.work.Work.WorkMessage;
import pipe.work.Work.Heartbeat;


public class HeartBeatHandler implements Handler{

    private Handler successor = null;
    protected static Logger logger = LoggerFactory.getLogger("cmd handler");
    protected ServerState state;
    private ServerInfo si;
    
    public HeartBeatHandler(ServerState state, ServerInfo si) {
        this.state = state;
        this.si = si;
    }


    @Override
    public void handleRequest(WorkMessage msg, Channel channel) {
    	
        if(msg.hasBeat()) {
          
        	int nodeId = msg.getHeader().getNodeId();
        	
            Heartbeat hb = msg.getBeat();
            logger.info("Received heartbeat from " + msg.getHeader().getNodeId() +" to node "+ msg.getHeader().getDestination());
            
        

            if(RaftElectionManager.getLeaderNode() == nodeId ||  msg.getRaftMessage().getRaftMessagePayload().getLeaderId() == nodeId)
            {
                logger.info("Current leader " +nodeId);
            	logger.info("Current leader heart beat time"+System.currentTimeMillis());
            	RaftTimer.setLastBeatTime(System.currentTimeMillis());
            } 
            
            //store last heartbeat time stamp from leader
            if(!si.isLeader()) {
            	if(si.getLeaderNodeId() == nodeId) {
            		RaftTimer.setLastBeatTime(System.currentTimeMillis());
            		si.setLastHBTSFromLeader(System.currentTimeMillis());
            	}
            }
            
            //logger.info("heartbeat from " + msg.getHeader().getNodeId());
        } else {
            if(successor != null) {
                successor.handleRequest(msg, channel);
            }
        }
    }

    @Override
    public void setSuccessor(Handler next) {
        this.successor = next;
    }

}
