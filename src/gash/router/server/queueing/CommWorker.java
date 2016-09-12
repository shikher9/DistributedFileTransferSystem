package gash.router.server.queueing;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import pipe.work.Work.WorkMessage;

import java.util.concurrent.LinkedBlockingDeque;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import routing.Pipe.CommandMessage;

/*
    Class which deals with rerouting of messages to other servers
 */
public class CommWorker extends Thread {

    private boolean forever = true;
    private LinkedBlockingDeque<QueueItem> pendingQueue;
    protected static Logger logger = LoggerFactory.getLogger("server");

    public CommWorker(LinkedBlockingDeque<QueueItem> pendingQueue) {
        this.pendingQueue = pendingQueue;
    }

    @Override
    public void run() {
        System.out.println("--> starting server worker thread");
        System.out.flush();

        while (forever && !pendingQueue.isEmpty()) {

            try {

                QueueItem item = pendingQueue.take();
                Channel channel = item.getChannel();
                WorkMessage msg = item.getMsg();
                
                if(msg == null) {
                    throw new RuntimeException("No message to send");
                }
                
                if(channel == null) {
                    throw new RuntimeException("Channel not initialized");
                }

                if (channel.isWritable()) {
                    ChannelFuture cf = channel.writeAndFlush(msg);

                    if (cf.isDone() && !cf.isSuccess()) {
                        logger.error("failed to send message to server, will try to send again");
                        pendingQueue.putFirst(item);
                    }

                }

            } catch (InterruptedException e) {
                logger.error("server comm worker interrupted", e);
                break;
            } catch (Exception e) {
                logger.error("exception in server comm worker", e);
                break;
            }
        }
    }
}
