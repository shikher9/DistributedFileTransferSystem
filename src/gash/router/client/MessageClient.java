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
package gash.router.client;

import com.google.protobuf.ByteString;
import file.Filemessage.FileRequest;
import file.Filemessage.Chunk;
import file.Filemessage.ChunkHeader;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pipe.common.Common.Header;
import routing.Pipe.CommandMessage;

/**
 * front-end (proxy) to our service - functional-based
 *
 * @author gash
 *
 */
public class MessageClient {

    protected static Logger logger = LoggerFactory.getLogger("client");
    private byte[] bufferarray;
    private BufferedInputStream bis;
    private FileInputStream fis;
    private Bootstrap bootstrap;
    private NioEventLoopGroup nioEventLoopGroup;
    private ChannelFuture futureChannel;
    private String host;
    private int port;

    // track requests
    private long curID = 0;

    public MessageClient(String host, int port) {
        init(host, port);
        this.host = host;
        this.port = port;
    }

    private void init(String host, int port) {
        CommConnection.initConnection(host, port);
    }

    public void addListener(CommListener listener) {
        CommConnection.getInstance().addListener(listener);
    }

    public synchronized void connect() {

        bootstrap = new Bootstrap();
        nioEventLoopGroup = new NioEventLoopGroup();

        bootstrap.group(nioEventLoopGroup);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.handler(new CommInit(false));
        futureChannel = bootstrap.connect(host, port).syncUninterruptibly();
    }

    // for sending ping
    public synchronized void ping() {
        Header.Builder hb = Header.newBuilder();
        hb.setNodeId(999);
        hb.setTime(System.currentTimeMillis());
        hb.setDestination(-1);

        CommandMessage.Builder rb = CommandMessage.newBuilder();
        rb.setHeader(hb);
        rb.setPing(true);

        try {
            CommConnection.getInstance().enqueue(rb.build());
        } catch (Exception ex) {
            logger.info("Exception while sending ping - " + ex.getMessage());
        }
    }

    // for sending string message
    public synchronized void message(String message) {
        // construct the message to send
        Header.Builder hb = Header.newBuilder();
        hb.setNodeId(999);
        hb.setTime(System.currentTimeMillis());
        hb.setDestination(-1);

        CommandMessage.Builder rb = CommandMessage.newBuilder();
        rb.setHeader(hb);
        rb.setMessage(message);

        try {
            CommConnection.getInstance().enqueue(rb.build());
        } catch (Exception ex) {
            logger.info("Exception while sending message - " + ex.getMessage());
        }
    }

    // for uploading file
    public synchronized void uploadFile(File file, long userID, int maxHops, int destination) {

        try {
            fis = new FileInputStream(file);
            bis = new BufferedInputStream(fis);
            long fsize = file.length();
            int lastNumberOfBytes = 0;
            long totalMul = 0;

            if (futureChannel == null) {
                connect();
            }

            if (fsize < 500000) {

                bufferarray = new byte[(int) fsize];
                bis.read(bufferarray);
                CommConnection.getInstance().enqueue(getUploadCommMsgBuilder(file, userID, maxHops, bufferarray, destination).build());

            } else {

                lastNumberOfBytes = (int)fsize % 500000;
                totalMul = fsize / 500000;
                bufferarray = new byte[500000];

                for (int i = 0; i < totalMul; i++) {
                    bis.read(bufferarray);
                    CommConnection.getInstance().enqueue(getUploadCommMsgBuilder(file, userID, maxHops, bufferarray, destination).build());
                }

                bufferarray = new byte[lastNumberOfBytes];
                bis.read(bufferarray);
                CommConnection.getInstance().enqueue(getUploadCommMsgBuilder(file, userID, maxHops, bufferarray, destination).build());

            }

            bufferarray = null;
            fis.close();
            bis.close();
        } catch (FileNotFoundException ex) {
            logger.error("File not found", ex);
        } catch (IOException ex) {
            logger.error("IO Exception", ex);
        } catch (Exception ex) {
            logger.info("Exception while file upload - " + ex.getMessage());
        }
    }

    private synchronized CommandMessage.Builder getUploadCommMsgBuilder(File file,
            long userID,
            int maxHops,
            byte[] data,
            int destination) {

        ChunkHeader.Builder chb = ChunkHeader.newBuilder();
        chb.setUserID(userID);
        chb.setFileName(file.getName());
        chb.setFileSize(file.length());
        chb.setChunkID(1);
        chb.setMaxhops(maxHops);

        Chunk.Builder cb = Chunk.newBuilder();
        cb.setChunkHeader(chb);
        cb.setChunkData(ByteString.copyFrom(data)); // set data here

        Header.Builder hb = Header.newBuilder();
        hb.setNodeId(999);
        hb.setTime(System.currentTimeMillis());
        hb.setDestination(destination);

        CommandMessage.Builder rb = CommandMessage.newBuilder();
        rb.setHeader(hb);
        rb.setChunk(cb);

        return rb;
    }

    // for downloading file
    public synchronized void downloadFile(String fileName, long userID) {
        FileRequest.Builder fb = FileRequest.newBuilder();
        fb.setUserID(userID);
        fb.setFileName(fileName);

        Header.Builder hb = Header.newBuilder();
        hb.setNodeId(999);
        hb.setTime(System.currentTimeMillis());
        hb.setDestination(-1);

        CommandMessage.Builder rb = CommandMessage.newBuilder();
        rb.setHeader(hb);
        rb.setFilerequest(fb);
        

        try {
            CommConnection.getInstance().enqueue(rb.build());
        } catch (Exception ex) {
            logger.info("Exception while sending message - " + ex.getMessage());
        }
    }

    public void release() {
        CommConnection.getInstance().release();
    }

    /**
     * Since the service/server is asynchronous we need a unique ID to associate
     * our requests with the server's reply
     *
     * @return
     */
    private synchronized long nextId() {
        return ++curID;
    }
}
