/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gash.server.message.work.handle;

import io.netty.channel.Channel;
import pipe.work.Work.WorkMessage;

/**
 *
 * @author shikher
 */
public interface Handler {
    
    // for handling request
    void handleRequest(WorkMessage msg, Channel channel);
    
    // for setting successor
    void setSuccessor(Handler next);
}
