package gash.server.message.work.handle;

import io.netty.channel.Channel;
import pipe.work.Work.WorkMessage;
import gash.router.server.ServerInfo;
import gash.router.server.ServerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.common.Common;

public class FailureHandler implements Handler {

    private ServerState state;
    private ServerInfo si;
    private Handler successor = null;
    protected static Logger logger = LoggerFactory.getLogger("work handler");

    public FailureHandler(ServerState state, ServerInfo si) {
        this.state = state;
        this.si = si;
    }

    @Override
    public void handleRequest(WorkMessage msg, Channel channel) {
        if (msg.hasErr()) {
            //TODO logic for handling file request
            Common.Failure err = msg.getErr();
            logger.error("failure from " + msg.getHeader().getNodeId());
        } else if (successor != null) {
            successor.handleRequest(msg, channel);
        }
    }

    @Override
    public void setSuccessor(Handler next) {
        this.successor = next;
    }

}
