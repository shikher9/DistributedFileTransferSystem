package gash.server.message.work.handle;

import io.netty.channel.Channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pipe.common.Common;
import pipe.work.Work.WorkMessage;
import pipe.work.Work.RaftMessagePayload.RaftAction;
import gash.impl.raft.manager.RaftElectionManager;
import gash.router.server.ServerInfo;
import gash.router.server.ServerState;

public class RaftMessageHandler implements Handler {
    
    

    private Handler successor = null;
    protected static Logger logger = LoggerFactory.getLogger("work handler");
    protected ServerState state;
    private ServerInfo si;

    public RaftMessageHandler(ServerState state,ServerInfo si) {
        this.state = state;
        this.si = si;
    }

    @Override
    public void handleRequest(WorkMessage msg, Channel channel) {
    	
    	if(msg.hasRaftMessage()) {

    	 int leader = msg.getRaftMessage().getRaftMessagePayload().getLeaderId();
    	 
    	 //update server info
    	 if(leader == state.getConf().getNodeId()) {
    		 si.setLeader(true);
    		 si.setLeaderNodeId(leader);
    	 } else {
    		 si.setLeader(false);
    		 si.setLeaderNodeId(leader);
    	 }
         RaftElectionManager.getInstance().processRequest(msg);

    	} else if (successor != null) {
			successor.handleRequest(msg, channel);
		}
       
    }

    @Override
    public void setSuccessor(Handler next) {
        this.successor = next;
    }

}

