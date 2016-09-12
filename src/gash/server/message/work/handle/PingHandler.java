package gash.server.message.work.handle;

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.common.Common;
import pipe.work.Work.WorkMessage;
import gash.router.server.ServerInfo;
import gash.router.server.ServerState;

public class PingHandler implements Handler {
    
    

    private Handler successor = null;
    protected static Logger logger = LoggerFactory.getLogger("work handler");
    protected ServerState state;
    private ServerInfo si;

    public PingHandler(ServerState state, ServerInfo si) {
        this.state = state;
        this.si = si;
    }
    
    

    @Override
    public void handleRequest(WorkMessage msg, Channel channel) {
        if (msg.hasPing()) {
        	
            //TODO logic for handling ping
            logger.info("ping from " + msg.getHeader().getNodeId());
            logger.info("ping to current server " + msg.getHeader().getDestination());

            //boolean p = msg.getPing();
            Common.Header.Builder hb = Common.Header.newBuilder();
            hb.setNodeId(state.getConf().getNodeId());
            hb.setTime(System.currentTimeMillis());
            hb.setDestination(msg.getHeader().getNodeId());

            WorkMessage.Builder wb = WorkMessage.newBuilder();
            wb.setHeader(hb);
            wb.setSecret(123456789);
            wb.setPing(true);

            channel.writeAndFlush(wb.build());

        } else if (successor != null) {
            successor.handleRequest(msg, channel);
        }
    }

    @Override
    public void setSuccessor(Handler next) {
        this.successor = next;
    }

}
