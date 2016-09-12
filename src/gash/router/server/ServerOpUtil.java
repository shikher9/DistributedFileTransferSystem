package gash.router.server;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;

import file.Filemessage.Chunk;
import file.Filemessage.ChunkHeader;
import gash.router.container.RoutingConf;
import gash.router.server.edges.EdgeInfo;
import gash.router.server.edges.EdgeList;
import gash.router.server.queueing.QueueItem;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import pipe.common.Common;
import pipe.common.Common.Header;
import pipe.work.Work.Heartbeat;
import pipe.work.Work.WorkMessage;
import pipe.work.Work.WorkState;
import routing.Pipe.CommandMessage;

/**
 * Utility class for creating heartbeat and ping
 *
 * @author shikher
 */
public class ServerOpUtil {

    private static int totalConnections;
    protected static Logger logger = LoggerFactory.getLogger("edge");
	/*
	 * Create heartbeat work message
     */
    public static synchronized WorkMessage createHB(EdgeInfo ei, ServerState state, ServerInfo si, int enqueued, int processed) {
        WorkState.Builder sb = WorkState.newBuilder();
        sb.setEnqueued(enqueued);
        sb.setProcessed(processed);

		Heartbeat.Builder bb = Heartbeat.newBuilder();
		bb.setState(sb);

		Common.Header.Builder hb = Common.Header.newBuilder();
		hb.setNodeId(state.getConf().getNodeId());
		hb.setDestination(ei.getRef());
		hb.setTime(System.currentTimeMillis());

		WorkMessage.Builder wb = WorkMessage.newBuilder();
		wb.setHeader(hb);
		wb.setBeat(bb);
		wb.setSecret(si.getSecret());

		return wb.build();
	}
	
	public static int GetTotalConnectionCount()
	{
		return totalConnections;
	}
	public static void SetTotalConnections(int connectionCount)
	{
		totalConnections = connectionCount;
	}


	/*
	 * creates ping work message
     */
    public static synchronized WorkMessage createPing(EdgeInfo ei, ServerState state, ServerInfo si) {

		Header.Builder hb = Header.newBuilder();
		hb.setNodeId(state.getConf().getNodeId());
		hb.setDestination(ei.getRef());
		hb.setTime(System.currentTimeMillis());

		WorkMessage.Builder wb = WorkMessage.newBuilder();
		wb.setHeader(hb);
		wb.setPing(true);
		wb.setSecret(123456789);

		return wb.build();
	}



	/*
	 * creates raft create chunk command message, used when sending files from
	 * server to client
     */
    public static synchronized CommandMessage createChunkMessage(long userID, String fileName, File file, byte[] data,
            RoutingConf conf) {

		ChunkHeader.Builder chb = ChunkHeader.newBuilder();
		chb.setUserID(userID);
		chb.setFileName(fileName);
		chb.setFileSize(file.length());
		chb.setChunkID(-1);
		chb.setMaxhops(-1);

		Chunk.Builder cb = Chunk.newBuilder();
		cb.setChunkHeader(chb);
		cb.setChunkData(ByteString.copyFrom(data)); // set data here

		Common.Header.Builder hb = Common.Header.newBuilder();
		hb.setNodeId(conf.getNodeId());
		hb.setTime(System.currentTimeMillis());
		hb.setDestination(-1);

		CommandMessage.Builder rb = CommandMessage.newBuilder();
		rb.setHeader(hb);
		rb.setChunk(cb);

		return rb.build();
	}

	/*
	 * utility method for creating a connection to a server
     */
    public static synchronized ChannelFuture connect(EdgeInfo ei, ServerState state, ServerInfo si) {

		ChannelFuture futureChannel;

		Bootstrap bootstrap = new Bootstrap();
		NioEventLoopGroup nioEventLoopGroup = new NioEventLoopGroup();

		bootstrap.group(nioEventLoopGroup);
		bootstrap.channel(NioSocketChannel.class);
		bootstrap.option(ChannelOption.TCP_NODELAY, true);
		bootstrap.option(ChannelOption.SO_KEEPALIVE, true); // (4)
		bootstrap.handler(new WorkInit(state, false, si));
		futureChannel = bootstrap.connect(ei.getHost(), ei.getPort()).syncUninterruptibly();

		return futureChannel;
	}

	/*
	 * Utility method to check if connection is established for the particular
	 * channelFuture
     */
    public static synchronized boolean checkConnectionEstablished(ChannelFuture futureChannel) {

		if (futureChannel != null && futureChannel.isSuccess() && futureChannel.channel().isWritable()) {
			return true;
		}

		return false;
	}
	
	public synchronized static int broadcast(WorkMessage mgmt) {
		if (mgmt == null)
			return 0;
		EdgeList lstEdge =  ServerInfo.getConnectedEdges();
		logger.info("The management connections are "+ServerInfo.GetTotalConnections());
		int _totalActiveConnections = 0;
		for (EdgeInfo edge: lstEdge.map.values())
		{
			//WorkMessage wm = createHB(edge,mgmt);
			Channel ch = null;
			ChannelFuture cf=null;
			try
			{
					ch = edge.getChannel();
					if(ch.isActive())
					{
						 _totalActiveConnections++;
				         cf = ch.writeAndFlush(mgmt);
				    
					}
			      
			}
			catch(Exception ex)
			{
				//System.out.println(ex.getMessage());
			}
			finally
			{
				System.out.println("Broadcast Status to: " + edge.getHost()+ "is"+cf);
			
			}
		}
		  if(_totalActiveConnections > 0)
	        {
	        	SetTotalConnections(_totalActiveConnections);
	        }
		  
		  return _totalActiveConnections;
		
	}
	
	   public static Channel GetConnectionChannel(int nodeId)
	   {
		  ServerInfo svcInfo = new ServerInfo();
		  EdgeList lstEdge =  svcInfo.getConnectedEdges();
	      EdgeInfo info =	lstEdge.map.get(nodeId);
	       return info.getChannel();
	   }

	/*
	 * Utility method to check if channel is writable or not
     */
    public static synchronized boolean isChannelWritable(Channel channel) {
        if (channel == null) {
            return false;
        }

		if (channel.isActive() && channel.isWritable()) {
			return true;
		}

		return false;
	}

    public static synchronized void uploadFileToFollowers(File file,
            long userID,
            String fileName,
            int maxHops,
            ServerInfo si) {

        EdgeList list = si.getConnectedEdges();

        for (EdgeInfo ei : list.getMap().values()) {

            Channel channel = ei.getChannel();
            int destination = ei.getRef();

            if (isChannelWritable(channel)) {
                uploadFileToNode(file, userID, fileName, maxHops, destination, channel, si);
            }
        }
    }

    public static synchronized void uploadFileToLeader(File file,
            long userID,
            String fileName,
            int maxHops,
            ServerInfo si) {

        int leaderNodeId = si.getLeaderNodeId();
        EdgeList list = si.getConnectedEdges();

        EdgeInfo ei = list.getNode(leaderNodeId);

        Channel channel = ei.getChannel();

        if (channel.isWritable()) {
            uploadFileToNode(file, userID, fileName, maxHops, leaderNodeId, channel, si);
        }
    }

    public static synchronized void uploadFileToNode(File file,
            long userID,
            String fileName,
            int maxHops,
            int destination,
            Channel channel,
            ServerInfo si
            ) {

        byte[] bufferarray;
        BufferedInputStream bis;
        FileInputStream fis;
        int secret = si.getSecret();
        int nodeId = si.getConf().getNodeId();

        try {
            fis = new FileInputStream(file);
            bis = new BufferedInputStream(fis);
            long fsize = file.length();
            int lastNumberOfBytes = 0;
            long totalMul = 0;

            if (fsize < 500000) {

                bufferarray = new byte[(int) fsize];
                bis.read(bufferarray);

                WorkMessage msg = getUploadWorkMsg(file, userID, fileName, maxHops, bufferarray, destination,secret, nodeId);

                //enqueue this message for sending
                QueueItem item = new QueueItem(msg, channel);
                si.getPendingQueue().put(item);
            } else {

                lastNumberOfBytes = (int) fsize % 500000;
                totalMul = fsize / 500000;
                bufferarray = new byte[500000];

                for (int i = 0; i < totalMul; i++) {
                    bis.read(bufferarray);
                    WorkMessage msg = getUploadWorkMsg(file, userID, fileName, maxHops, bufferarray, destination,secret,nodeId);

                    //enqueue this message for sending
                    QueueItem item = new QueueItem(msg, channel);
                    si.getPendingQueue().put(item);
                }

                bufferarray = new byte[lastNumberOfBytes];
                bis.read(bufferarray);
                WorkMessage msg = getUploadWorkMsg(file, userID, fileName, maxHops, bufferarray, destination,secret,nodeId);

                //enqueue this message for sending
                QueueItem item = new QueueItem(msg, channel);
                si.getPendingQueue().put(item);
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

    private static synchronized WorkMessage getUploadWorkMsg(File file,
            long userID,
            String fileName,
            int maxHops,
            byte[] data,
            int destination,
            int secret,
            int nodeId) {

        ChunkHeader.Builder chb = ChunkHeader.newBuilder();
        chb.setUserID(userID);
        chb.setFileName(fileName);
        chb.setFileSize(file.length());
        chb.setChunkID(1);
        chb.setMaxhops(maxHops);

        Chunk.Builder cb = Chunk.newBuilder();
        cb.setChunkHeader(chb);
        cb.setChunkData(ByteString.copyFrom(data)); // set data here

        Header.Builder hb = Header.newBuilder();
        hb.setNodeId(nodeId);
        hb.setTime(System.currentTimeMillis());
        hb.setDestination(destination);

        WorkMessage.Builder rb = WorkMessage.newBuilder();
        rb.setHeader(hb);
        rb.setSecret(secret);
        rb.setChunk(cb);

        return rb.build();
    }
    
    /*
     * converts command message to work message
     */
    public static synchronized WorkMessage convertCMtoWM(CommandMessage msg, ServerInfo si) {

    	
    	 WorkMessage.Builder wb = WorkMessage.newBuilder();
    	 
    	 Header.Builder hb = Header.newBuilder();
    	 hb.setNodeId(msg.getHeader().getNodeId());
    	 hb.setTime(System.currentTimeMillis());
    	 hb.setMaxHops(msg.getHeader().getMaxHops());
    	 hb.setDestination(si.getLeaderNodeId());
    	 
         wb.setHeader(hb);
         wb.setSecret(si.getSecret());
         wb.setChunk(msg.getChunk());
         
         return wb.build();
    }

}