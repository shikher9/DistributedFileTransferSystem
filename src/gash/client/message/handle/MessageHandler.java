package gash.client.message.handle;

import io.netty.channel.Channel;
import routing.Pipe.CommandMessage;

public class MessageHandler implements Handler {

    private Handler successor = null;

    @Override
    public void handleRequest(CommandMessage msg, Channel channel) {
        if (msg.hasMessage()) {
            //TODO logic for handling message

            String message = msg.getMessage();
            System.out.println("Message from Server : " + message);

        } else if (successor != null) {
            successor.handleRequest(msg, channel);
        }
    }

    @Override
    public void setSuccessor(Handler next) {
        this.successor = next;
    }

}
