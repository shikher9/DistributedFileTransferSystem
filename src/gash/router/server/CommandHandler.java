/**
 * Copyright 2016 Gash.
 *
 * This file and intellectual content is protected under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package gash.router.server;

import gash.server.message.command.handle.ChunkHandler;
import gash.server.message.command.handle.FileRequestHandler;
import gash.server.message.command.handle.MessageHandler;
import gash.server.message.command.handle.PingHandler;
import gash.server.message.command.handle.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.router.container.RoutingConf;
import gash.router.container.RoutingConf.RoutingEntry;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import pipe.common.Common.Failure;
import routing.Pipe.CommandMessage;

/**
 *
 * @author gash
 *
 */
public class CommandHandler extends SimpleChannelInboundHandler<CommandMessage> {

    protected static Logger logger = LoggerFactory.getLogger("cmd handler");
    protected RoutingConf conf;
    protected Handler pingHandler;
    protected Handler messageHandler;
    protected Handler chunkHandler;
    protected Handler fileRequestHandler;
    private boolean interClusterDestination = false;
    private ServerInfo si;

    public CommandHandler(RoutingConf conf, ServerInfo si) {
        if (conf != null) {
            this.conf = conf;
            this.si = si;
        }

        //creating handlers
        pingHandler = new PingHandler(conf,si);
        messageHandler = new MessageHandler(conf,si);
        chunkHandler = new ChunkHandler(conf,si);
        fileRequestHandler = new FileRequestHandler(conf,si);

        pingHandler.setSuccessor(messageHandler);
        messageHandler.setSuccessor(chunkHandler);
        chunkHandler.setSuccessor(fileRequestHandler);

    }

    /**
     * override this method to provide processing behavior. This implementation
     * mimics the routing we see in annotating classes to support a RESTful-like
     * behavior (e.g., jax-rs).
     *
     * @param msg
     * @param channel
     */
    public void handleMessage(CommandMessage msg, Channel channel) {
        if (msg == null) {
            logger.info("ERROR: Unexpected content - " + msg);
            System.out.println("ERROR: Unexpected content - " + msg);
            return;
        }

        PrintUtil.printCommand(msg);

        try {

            int destination = msg.getHeader().getDestination();

            //to check whether destination belongs to this cluster
            for (RoutingEntry re : conf.getRouting()) {
                if (re.getId() == destination) {
                    interClusterDestination = true;
                    break;
                }
            }

           // if destination belongs to this cluster ,
            // the leader of this cluster should handle
            if (destination == -1 || interClusterDestination) {
                pingHandler.handleRequest(msg, channel);
            } else {
                // the message should go to other node in a different cluster
                
            }

        } catch (Exception e) {

            logger.error("Exception while reading message ", e);
            Failure.Builder eb = Failure.newBuilder();
            eb.setId(conf.getNodeId());
            eb.setRefId(msg.getHeader().getNodeId());
            eb.setMessage(e.getMessage());
            CommandMessage.Builder rb = CommandMessage.newBuilder(msg);
            rb.setErr(eb);
            channel.write(rb.build());
        }

        System.out.flush();
    }

    /**
     * a message was received from the server. Here we dispatch the message to
     * the client's thread pool to minimize the time it takes to process other
     * messages.
     *
     * @param ctx The channel the message was received from
     * @param msg The message
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CommandMessage msg) throws Exception {
        handleMessage(msg, ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Unexpected exception from downstream.", cause);
        ctx.close();
    }

}
