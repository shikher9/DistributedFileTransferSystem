package gash.server.message.command.handle;

import gash.router.container.RoutingConf;
import gash.router.server.ServerInfo;
import io.netty.channel.Channel;
import routing.Pipe.CommandMessage;

public class MessageHandler implements Handler {

    private Handler successor = null;
    private RoutingConf conf;
    private ServerInfo si;

    public MessageHandler(RoutingConf conf, ServerInfo si) {
        this.conf = conf;
        this.si = si;
    }

    

    @Override
    public void handleRequest(CommandMessage msg, Channel channel) {
        if (msg.hasMessage()) {
            //TODO logic for handling message
        } else if (successor != null) {
            successor.handleRequest(msg, channel);
        }
    }

    @Override
    public void setSuccessor(Handler next) {
        this.successor = next;
    }

}
