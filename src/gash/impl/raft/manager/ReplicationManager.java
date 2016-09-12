package gash.impl.raft.manager;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.router.server.ServerInfo;
import gash.router.server.ServerOpUtil;
import gash.router.server.edges.EdgeInfo;
import gash.router.server.edges.EdgeList;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import pipe.work.Work.WorkMessage;

public class ReplicationManager {

	protected static Logger logger = LoggerFactory
			.getLogger("replicationmanager");

	public synchronized static void ReplicateFileToFollowers(int leaderNodeId,
			WorkMessage mgmt, ServerInfo si) {
		
		for(EdgeInfo ei : si.getConnectedEdges().map.values()) {
			
			//whether edge is active
			Channel channel = ei.getChannel();
			
			for(FileTransferInfo fileTransferInfo : ei.GetFilesToTransfer()) {
				if(ServerOpUtil.isChannelWritable(channel)) {
					File file = fileTransferInfo.getFile();
					long userID = fileTransferInfo.getUserId();
					String fileName = fileTransferInfo.getFileName();
					int maxHops = fileTransferInfo.getMaxHops();
					ServerInfo sinfo = fileTransferInfo.getSi();
					ServerOpUtil.uploadFileToNode(file, userID, fileName, maxHops, ei.getRef(), channel, sinfo);
					ei.GetFilesToTransfer().remove(fileTransferInfo);
				} 
			}
			
		}

		//update database of followers

	}
}
