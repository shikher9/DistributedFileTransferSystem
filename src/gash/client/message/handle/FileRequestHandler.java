package gash.client.message.handle;

import gash.server.message.command.handle.*;
import gash.router.container.RoutingConf;
import io.netty.channel.Channel;
import routing.Pipe.CommandMessage;

public class FileRequestHandler implements Handler {

    private Handler successor = null;

   
    @Override
    public void handleRequest(CommandMessage msg, Channel channel) {
        if (msg.hasFilerequest()) {
            //TODO logic for handling file request
        } else if (successor != null) {
            successor.handleRequest(msg, channel);
        }
    }

    @Override
    public void setSuccessor(Handler next) {
        this.successor = next;
    }

}
