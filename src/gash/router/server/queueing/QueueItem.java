package gash.router.server.queueing;

import io.netty.channel.Channel;
import pipe.work.Work.WorkMessage;



public class QueueItem {
    private WorkMessage msg;
    private Channel channel;

    public QueueItem(WorkMessage msg, Channel channel) {
        this.msg = msg;
        this.channel = channel;
    }

    public WorkMessage getMsg() {
        return msg;
    }

    public Channel getChannel() {
        return channel;
    }
        
}
