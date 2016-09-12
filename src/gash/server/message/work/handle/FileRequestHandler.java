package gash.server.message.work.handle;

import gash.router.server.ServerInfo;
import gash.router.server.ServerState;
import io.netty.channel.Channel;
import pipe.work.Work.WorkMessage;

public class FileRequestHandler implements Handler {

    private Handler successor = null;
    protected ServerState state;
    private ServerInfo si;

    public FileRequestHandler(ServerState state, ServerInfo si) {
        this.state = state;
        this.si = si;
    }

    @Override
    public void handleRequest(WorkMessage msg, Channel channel) {
    	if (msg.hasFilerequest()) {
        //if (msg.hasFilerequest()) {
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
