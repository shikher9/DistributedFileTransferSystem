package gash.client.message.handle;

import file.Filemessage.Chunk;
import io.netty.channel.Channel;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import routing.Pipe.CommandMessage;

public class ChunkHandler implements Handler {

    private Handler successor = null;
    long currentBytesWritten = 0;
    protected Logger logger = LoggerFactory.getLogger("client-handler");
    private HashMap<String, FileOutputStream> fosmap = new HashMap<String, FileOutputStream>();
    private HashMap<String, Long> cbwmap = new HashMap<String, Long>();

    @Override
    public void handleRequest(CommandMessage msg, Channel channel) {
        if (msg.hasChunk()) {
            //TODO logic for handling chunk

            Chunk chunk = msg.getChunk();

            String fileName = chunk.getChunkHeader().getFileName();
            byte[] data = chunk.getChunkData().toByteArray();
            long fileSize = chunk.getChunkHeader().getFileSize();

            try {
                if (fosmap.containsKey(fileName)) {

                    FileOutputStream fileOutputStream = fosmap.get(fileName);
                    fileOutputStream.write(data);
                    fileOutputStream.flush();

                    currentBytesWritten = cbwmap.get(fileName) + data.length;
                    cbwmap.put(fileName, currentBytesWritten);

                } else {
                    FileOutputStream fileOutputStream = new FileOutputStream(new File(fileName));
                    fileOutputStream.write(data);
                    fileOutputStream.flush();
                    currentBytesWritten = data.length;

                    cbwmap.put(fileName, currentBytesWritten);
                    fosmap.put(fileName, fileOutputStream);
                }
            } catch (FileNotFoundException ex) {
                logger.error("File not found ", ex);
            } catch (IOException ex) {
                logger.error("IOException ", ex);
            } finally {
                if (currentBytesWritten == fileSize) {
                    logger.info("File download complete : " + fileName);

                    FileOutputStream fileOutputStream = fosmap.get(fileName);
                    try {
                        fileOutputStream.close();
                    } catch (IOException ex) {
                        logger.error("IOException while closing file", ex);
                    }
                }
            }

        } else if (successor != null) {
            successor.handleRequest(msg, channel);
        }
    }

    @Override
    public void setSuccessor(Handler next) {
        this.successor = next;
    }

}
