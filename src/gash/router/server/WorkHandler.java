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

import gash.server.message.work.handle.FileRequestHandler;
import gash.server.message.work.handle.ChunkHandler;
import gash.server.message.work.handle.HeartBeatHandler;
import gash.server.message.work.handle.FailureHandler;
import gash.server.message.work.handle.PingHandler;
import gash.server.message.work.handle.Handler;
import gash.server.message.work.handle.RaftMessageHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import pipe.common.Common.Failure;
import pipe.work.Work.WorkMessage;

/**
 *
 * @author gash
 *
 */
public class WorkHandler extends SimpleChannelInboundHandler<WorkMessage> {
    
    protected static Logger logger = LoggerFactory.getLogger("work handler");
    public ServerState state;
    protected boolean debug = false;
    protected Handler pingHandler;
    protected Handler chunkHandler;
    protected Handler fileRequestHandler;
    protected Handler heartBeatHandler;
    protected Handler failureHandler;
    protected Handler raftMessageHandler;
    private ServerInfo si;
    
    public WorkHandler(ServerState state, ServerInfo si) {
        if (state != null) {
            this.state = state;
        }
        
        this.si = si;

        //creating handlers
        pingHandler = new PingHandler(state,si);
        heartBeatHandler = new HeartBeatHandler(state,si);
        chunkHandler = new ChunkHandler(state,si);
        fileRequestHandler = new FileRequestHandler(state,si);
        failureHandler = new FailureHandler(state,si);
        raftMessageHandler = new RaftMessageHandler(state, si);
        
        pingHandler.setSuccessor(heartBeatHandler);
        heartBeatHandler.setSuccessor(chunkHandler);
        chunkHandler.setSuccessor(fileRequestHandler);
        fileRequestHandler.setSuccessor(raftMessageHandler);
        raftMessageHandler.setSuccessor(failureHandler);
    }

    /**
     * override this method to provide processing behavior. T
     *
     * @param msg
     */
    public void handleMessage(WorkMessage msg, Channel channel) {
        if (msg == null) {
            logger.info("ERROR: Unexpected content - " + msg);
            System.out.println("ERROR: Unexpected content - " + msg);
            return;
        }
        
        if (debug) {
            PrintUtil.printWork(msg);
        }

        try {
            pingHandler.handleRequest(msg, channel);
            //raftMessageHandler.handleRequest(msg, channel);
        } catch (Exception e) {
            
            logger.error("Exception while reading message ", e);
            Failure.Builder eb = Failure.newBuilder();
            eb.setId(state.getConf().getNodeId()); // set current id
            eb.setRefId(msg.getHeader().getNodeId());
            eb.setMessage(e.getMessage());
            WorkMessage.Builder rb = WorkMessage.newBuilder(msg);
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
    protected void channelRead0(ChannelHandlerContext ctx, WorkMessage msg) throws Exception {
        handleMessage(msg, ctx.channel());
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Unexpected exception from downstream.", cause);
        ctx.close();
    }
    
}
