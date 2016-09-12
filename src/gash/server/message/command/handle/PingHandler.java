package gash.server.message.command.handle;

import gash.router.container.RoutingConf;
import gash.router.server.ServerInfo;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import routing.Pipe.CommandMessage;

public class PingHandler implements Handler {

    private RoutingConf conf;
    private Handler successor = null;
    protected static Logger logger = LoggerFactory.getLogger("command-handler");
    private ServerInfo si;

    public PingHandler(RoutingConf conf, ServerInfo si) {
        this.conf = conf;
        this.si = si;
    }

    @Override
    public void handleRequest(CommandMessage msg, Channel channel) {
        if (msg.hasPing()) {
            //TODO logic for handling ping

            CommandMessage.Builder rb = CommandMessage.newBuilder();
            rb.setMessage("Ping result : " + conf.getNodeId());
            channel.writeAndFlush(rb);

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
