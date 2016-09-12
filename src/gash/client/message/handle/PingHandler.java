package gash.client.message.handle;

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import routing.Pipe.CommandMessage;

public class PingHandler implements Handler {

    private Handler successor = null;
    protected static Logger logger = LoggerFactory.getLogger("handler");

    

    @Override
    public void handleRequest(CommandMessage msg, Channel channel) {
        if (msg.hasPing()) {
            
            logger.info("ping from " + msg.getHeader().getNodeId());
        } else if (successor != null) {
            successor.handleRequest(msg, channel);
        }
    }

    @Override
    public void setSuccessor(Handler next) {
        this.successor = next;
    }

}
