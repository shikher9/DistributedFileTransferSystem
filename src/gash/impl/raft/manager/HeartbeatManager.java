/*
 * copyright 2014, gash
 * 
 * Gash licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package gash.impl.raft.manager;

import gash.impl.raft.manager.HeartbeatData.BeatStatus;
import gash.impl.raft.manager.NodeData.RaftStatus;
import gash.router.container.RoutingConf;
import gash.router.server.ServerOpUtil;
import gash.router.server.edges.EdgeList;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pipe.work.Work.AcknowledgementPayload;
import pipe.work.Work.AcknowledgementPayload.ResponseAction;
import pipe.work.Work.LogList;
import pipe.work.Work.RaftHeader;
import pipe.work.Work.RaftMessage;
import pipe.work.Work.RaftMessagePayload.Builder;
import pipe.work.Work.RaftMessagePayload.RaftAction;
import pipe.work.Work.RaftMessagePayloadOrBuilder;
import pipe.work.Work.WorkMessage;

import com.google.protobuf.GeneratedMessage;

/**
 * A server can contain multiple, separate interfaces that represent different
 * connections within an overlay network. Therefore, we will need to manage
 * these edges (e.g., heart beats) separately.
 * 
 * Essentially, there should be one HeartbeatManager per node (assuming a node
 * does not support more than one overlay network - which it could...).
 * Secondly, the HB manager holds the network connections of the graph that the
 * other managers rely upon.
 * 
 * - HB and many readers).
 * 
 * @author gash
 * 
 */
public class HeartbeatManager extends Thread {
	protected static Logger logger = LoggerFactory.getLogger("heartbeat");
	protected static AtomicReference<HeartbeatManager> instance = new AtomicReference<HeartbeatManager>();

	// frequency that heartbeats are checked
	static final int sHeartRate = 5000; // msec

	private static RoutingConf conf;

	//ManagementQueue mqueue;
	boolean forever = true;

																																																																																																																																								
	// the in/out queues for sending heartbeat messages
	ConcurrentHashMap<Channel, HeartbeatData> outgoingHB = new ConcurrentHashMap<Channel, HeartbeatData>();
	ConcurrentHashMap<Integer, HeartbeatData> incomingHB = new ConcurrentHashMap<Integer, HeartbeatData>();

	public static HeartbeatManager initManager(RoutingConf conf) {
		logger.info("\nTODO HB QUEUES SHOULD BE SHARED!\n");
		HeartbeatManager.conf = conf;
		instance.compareAndSet(null, new HeartbeatManager());
		return instance.get();
	}

	public static HeartbeatManager getInstance() {
		return instance.get();
	}

	/**
	 * initialize the heartbeatMgr for this server
	 * 
	 * @param nodeId
	 *            The server's (this) ID
	 */
	protected HeartbeatManager() {
	}

	/**
	 * create/register expected connections that this node will make. These
	 * edges are connections this node is responsible for monitoring.
	 * 
	 * @deprecated not sure why this is needed
	 * @param edges
	 */
	//public void initNetwork(AdjacentConf edges) {
	//}

	/**
	 * update information on a node we monitor
	 * 
	 * @param nodeId
	 */
	public void processRequest(WorkMessage mgmt) {
		if (mgmt == null)
			return;

		HeartbeatData hd = incomingHB.get(mgmt.getRaftMessage().getRaftHeader().getOriginatorNodeId());
		if (hd == null) {
			// credentials as this would allow anyone to connect to our network.
			logger.error("Unknown heartbeat received from node ", mgmt.getRaftMessage().getRaftHeader().getOriginatorNodeId());
			return;
		} else {
			logger.info("Heartbeat received from " + mgmt.getRaftMessage().getRaftHeader().getOriginatorNodeId());
			
			//LogListManager.getInstance().replicateLogs(mgmt.getRaftMessagePayload().getLogList());

			hd.setFailures(0);
			long now = System.currentTimeMillis();
			hd.setLastBeat(now);
			RaftTimer.setLastBeatTime(now);
			NodeDataManager.getInstance().getNodeData().setLastBeatReceivedFromLeader(now);
		}
	}

	/**
	 * This is called by the HeartbeatPusher when this node requests a
	 * connection to a node, this is called to register interest in creating a
	 * connection/edge.
	 * 
	 * @param node
	 */
	protected void addAdjacentNode(HeartbeatData node) {
		if (node == null || node.getHost() == null || node.getMgmtport() == null) {
			logger.error("HeartbeatManager registration of edge failed, missing data");
			return;
		}

		if (!incomingHB.containsKey(node.getNodeId())) {
			logger.info("Expects to connect to node " + node.getNodeId() + " (" + node.getHost() + ", "
					+ node.getMgmtport() + ")");

			// ensure if we reuse node instances that it is not dirty.
			node.clearAll();
			node.setInitTime(System.currentTimeMillis());
			node.setStatus(BeatStatus.Init);
			incomingHB.put(node.getNodeId(), node);
		}
	}

	/**
	 * add an INCOMING endpoint (receive HB from). This is called when this node
	 * actually establishes the connection to the node. Prior to this call, the
	 * system will register an inactive/pending node through addIncomingNode().
	 * 
	 * @param nodeId
	 * @param ch
	 * @param sa
	 */
	public void addAdjacentNodeChannel(int nodeId, Channel ch, SocketAddress sa) {
		HeartbeatData hd = incomingHB.get(nodeId);
		if (hd != null) {
			hd.setConnection(ch, sa, nodeId);
			hd.setStatus(BeatStatus.Active);

			// when the channel closes, remove it from the incomingHB list
			ch.closeFuture().addListener(new CloseHeartListener(hd));
		} else {
			logger.error("Received a HB ack from an unknown node, node ID = ", nodeId);
		}
	}

	/**
	 * send a OUTGOING heartbeatMgr to a node. This is called when a
	 * client/server makes a request to receive heartbeats.
	 * 
	 * @param nodeId
	 * @param ch
	 * @param sa
	 */
	public void addOutgoingChannel(int nodeId, String host, int mgmtport, Channel ch, SocketAddress sa) {
		if (!outgoingHB.containsKey(ch)) {
			HeartbeatData heart = new HeartbeatData(nodeId, host, null, mgmtport);
			heart.setConnection(ch, sa, nodeId);
			outgoingHB.put(ch, heart);

			// when the channel closes, remove it from the outgoingHB
			ch.closeFuture().addListener(new CloseHeartListener(heart));
		} else {
			logger.error("Received a HB connection unknown to the server, node ID = ", nodeId);
		}
	}

	public void release() {
		forever = true;
	}

	private WorkMessage generateHB() {

		RaftHeader.Builder rh = RaftHeader.newBuilder();
		rh.setOriginatorNodeId(HeartbeatManager.conf.getNodeId());
		rh.setTime(System.currentTimeMillis());

		Builder rp = pipe.work.Work.RaftMessagePayload.newBuilder();
		rp.setAction(RaftAction.HEARTBEAT);
		
		//embed leader logs
		LogList.Builder logListbuilder = LogList.newBuilder();
		//logListbuilder = LogListManager.getInstance().embedLogs();
		//rp.setLogList(logListbuilder.build());

		WorkMessage.Builder b = WorkMessage.newBuilder();
		
		RaftMessage.Builder rfmessage = RaftMessage.newBuilder();
		
		rfmessage.setRaftHeader(rh.build());
		rfmessage.setRaftMessagePayload(rp.build());
        b.setRaftMessage(rfmessage);
		return b.build();
	}

	@Override
	public void run() {
		logger.info("starting HB manager");
		int count = 0;
		NodeDataManager nodeDataManager = NodeDataManager.getInstance();

		try{
			while (forever) {
				try {
					Thread.sleep(sHeartRate);

					// ignore until we have edges with other nodes
					if (outgoingHB.size() > 0) {

						// send my status (heartbeatMgr)
						GeneratedMessage msg = null;
						if(nodeDataManager.getNodeData().getNodeStatus() == RaftStatus.LEADER)
						{
							count++;
							for (HeartbeatData hd : outgoingHB.values()) {
								// if failed sends exceed threshold, stop sending
								if (hd.getFailuresOnSend() > HeartbeatData.sFailureToSendThresholdDefault) {
									continue;
								}

								// only generate the message if needed
								if (msg == null)
									msg = generateHB();

								try {
									if (logger.isDebugEnabled())
										logger.debug("sending heartbeat to " + hd.getNodeId());
									hd.getChannel().writeAndFlush(msg);
									hd.setLastBeatSent(System.currentTimeMillis());
									hd.setFailuresOnSend(0);
									if (logger.isDebugEnabled())
										logger.debug("beat (" + HeartbeatManager.conf.getNodeId() + ") sent to "
												+ hd.getNodeId() + " at " + hd.getHost());
								} catch (Exception e) {
									hd.incrementFailuresOnSend();
									logger.error("Failed " + hd.getFailures() + " times to send HB for " + hd.getNodeId()
											+ " at " + hd.getHost(), e);
								}
							}

							if(outgoingHB.size() == 2 && count == 2){
								logger.info("Network is set and node is connected to adjacent nodes..");
							}

						}
					} else
						; // logger.info("No nodes to send HB");
				} catch (InterruptedException ie) {
					break;
				} catch (Exception e) {
					logger.error("Unexpected management communcation failure", e);
					break;
				}
			}
		}catch(NullPointerException e)
		{
			logger.info("Null Pointer");
		}

		if (!forever)
			logger.info("management outbound queue closing");
		else
			logger.info("unexpected closing of HB manager");

	}

	public class CloseHeartListener implements ChannelFutureListener {
		private HeartbeatData heart;

		public CloseHeartListener(HeartbeatData heart) {
			this.heart = heart;
		}

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			if (outgoingHB.containsValue(heart)) {
				logger.warn("HB outgoing channel closing for node '" + heart.getNodeId() + "' at " + heart.getHost());
				outgoingHB.remove(future.channel());
			} else if (incomingHB.containsValue(heart)) {
				logger.warn("HB incoming channel closing for node '" + heart.getNodeId() + "' at " + heart.getHost());
				incomingHB.remove(future.channel());
			}
		}
	}

	public void generateAckAppend(int nodeId){

		RaftHeader.Builder rh = RaftHeader.newBuilder();
		rh.setOriginatorNodeId(HeartbeatManager.conf.getNodeId());
		rh.setTime(System.currentTimeMillis());
	
		AcknowledgementPayload.Builder ackBuilder = AcknowledgementPayload.newBuilder();
		ackBuilder.setAction(ResponseAction.ACK_APPEND);
		
		WorkMessage.Builder b = WorkMessage.newBuilder();
		
        RaftMessage.Builder rfmessage = RaftMessage.newBuilder();
			
			rfmessage.setRaftHeader(rh.build());
			rfmessage.setAckPayload(ackBuilder.build());

			b.setRaftMessage(rfmessage);
	
		WorkMessage msg = b.build();
		
		ServerOpUtil.GetConnectionChannel(nodeId).writeAndFlush(msg);
		
	}
}
