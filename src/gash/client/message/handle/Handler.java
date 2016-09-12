/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gash.client.message.handle;

import gash.server.message.command.handle.*;
import io.netty.channel.Channel;
import routing.Pipe.CommandMessage;

/**
 *
 * @author shikher
 */
public interface Handler {
    
    // for handling request
    void handleRequest(CommandMessage msg, Channel channel);
    
    
    // for setting successor
    void setSuccessor(Handler next);
}
