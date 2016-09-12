package gash.server.message.command.handle;

import com.google.protobuf.ByteString;
import file.Filemessage.Chunk;
import file.Filemessage.ChunkHeader;
import gash.router.container.RoutingConf;
import gash.router.server.ServerInfo;
import gash.router.server.ServerOpUtil;
import io.netty.channel.Channel;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pipe.common.Common;
import routing.Pipe.CommandMessage;

public class FileRequestHandler implements Handler {

    private RoutingConf conf;
    private Handler successor = null;
    private byte[] bufferarray;
    private BufferedInputStream bis;
    private FileInputStream fis;
    protected Logger logger = LoggerFactory.getLogger("command-handler");
    private ServerInfo si;

    public FileRequestHandler(RoutingConf conf, ServerInfo si) {
        this.conf = conf;
        this.si = si;
    }

    @Override
    public void handleRequest(CommandMessage msg, Channel channel) {
        if (msg.hasFilerequest()) {
            //TODO logic for handling file request
            processChunk(msg, channel);

        } else if (successor != null) {
            successor.handleRequest(msg, channel);
        }
    }

    @Override
    public void setSuccessor(Handler next) {
        this.successor = next;
    }

    private void processChunk(CommandMessage msg, Channel channel) {

        long userID = msg.getFilerequest().getUserID();
        String fileName = msg.getFilerequest().getFileName();
        File file = new File("Files", userID + "-" + fileName);

        try {
            fis = new FileInputStream(file);
            bis = new BufferedInputStream(fis);
            long fsize = file.length();
            int lastNumberOfBytes = 0;
            long totalMul = 0;

            if (fsize < 500000) {

                bufferarray = new byte[(int) fsize];
                bis.read(bufferarray);
                channel.writeAndFlush(ServerOpUtil.createChunkMessage(userID, fileName, file, bufferarray, conf));

            } else {

                lastNumberOfBytes = (int) fsize % 500000;
                totalMul = fsize / 500000;
                bufferarray = new byte[500000];

                for (int i = 0; i < totalMul; i++) {
                    bis.read(bufferarray);
                    channel.writeAndFlush(ServerOpUtil.createChunkMessage(userID, fileName, file, bufferarray, conf));
                }

                bufferarray = new byte[lastNumberOfBytes];
                bis.read(bufferarray);
                channel.writeAndFlush(ServerOpUtil.createChunkMessage(userID, fileName, file, bufferarray, conf));
            }

            bufferarray = null;
            fis.close();
            bis.close();
        } catch (FileNotFoundException ex) {
            logger.error("File not found", ex);
        } catch (IOException ex) {
            logger.error("IO Exception", ex);
        } catch (Exception ex) {
            logger.info("Exception while file transfer - " + ex.getMessage());
        }

    }

}
