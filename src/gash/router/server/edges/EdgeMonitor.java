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
package gash.router.server.edges;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gash.router.container.RoutingConf.RoutingEntry;
import gash.router.server.ServerInfo;
import gash.router.server.ServerOpUtil;
import gash.router.server.ServerState;
import io.netty.channel.ChannelFuture;
import java.net.ConnectException;
import pipe.common.Common.Header;
import pipe.work.Work.Heartbeat;
import pipe.work.Work.WorkMessage;
import pipe.work.Work.WorkState;

public class EdgeMonitor implements EdgeListener, Runnable {

	protected static Logger logger = LoggerFactory.getLogger("edge");
	private ChannelFuture futureChannel;
	private EdgeList outboundEdges;
	private EdgeList inboundEdges;
	private long dt = 2000;
	private ServerState state;
	private boolean forever = true;
	boolean runOnce = false;
	private ServerInfo si;

	public EdgeMonitor(ServerState state, ServerInfo si) {
		if (state == null) {
			throw new RuntimeException("state is null");
		}

		this.outboundEdges = new EdgeList();
		this.inboundEdges = new EdgeList();
		this.state = state;
		this.state.setEmon(this);
		this.si = si;

		if (state.getConf().getRouting() != null) {
			int connCount = 0;
			for (RoutingEntry e : state.getConf().getRouting()) {
				outboundEdges.addNode(e.getId(), e.getHost(), e.getPort());
				connCount++;
			}
			ServerOpUtil.SetTotalConnections(connCount);
		}

		// cannot go below 2 sec
		if (state.getConf().getHeartbeatDt() > this.dt) {
			this.dt = state.getConf().getHeartbeatDt();
		}

		// set serverinfo edgelist
		si.setConnectedEdges(outboundEdges);
	}

	public void createInboundIfNew(int ref, String host, int port) {
		inboundEdges.createIfNew(ref, host, port);
	}

	private WorkMessage createHB(EdgeInfo ei) {
		WorkState.Builder sb = WorkState.newBuilder();
		sb.setEnqueued(-1);
		sb.setProcessed(-1);

		Heartbeat.Builder bb = Heartbeat.newBuilder();
		bb.setState(sb);

		Header.Builder hb = Header.newBuilder();
		hb.setNodeId(state.getConf().getNodeId());
		hb.setDestination(ei.getRef());
		hb.setTime(System.currentTimeMillis());

		WorkMessage.Builder wb = WorkMessage.newBuilder();
		wb.setHeader(hb);
		wb.setBeat(bb);
		wb.setSecret(si.getSecret());

		return wb.build();
	}

	public void shutdown() {
		forever = false;
	}

	@Override
	public void run() {
		while (forever) {
			try {
				for (EdgeInfo ei : this.outboundEdges.map.values()) {

					if (ei.isActive() && ServerOpUtil.isChannelWritable(ei.getChannel())) {

						WorkMessage wm = createHB(ei);
						ei.getChannel().writeAndFlush(wm);
						logger.info(
								"Sending heartbeat to node " + ei.getRef() + " " + ei.getHost() + " " + ei.getPort());
					} else {
						// create a client to the node
						logger.info(
								"Trying to connect to node " + ei.getRef() + " " + ei.getHost() + " " + ei.getPort());
						connectToNode(ei);
					}
				}

				Thread.sleep(dt);
			} catch (InterruptedException e) {
				logger.error("Exception, edge monitor interrupted ", e);
			} catch (Exception e) {
				logger.error("Exception ", e);
			}
		}
	}

	@Override
	public synchronized void onAdd(EdgeInfo ei) {
		// add edge info
	}

	@Override
	public synchronized void onRemove(EdgeInfo ei) {
		// remove edge info
	}

	protected void connectToNode(EdgeInfo ei) {

		try {

			// try to connect
			futureChannel = ServerOpUtil.connect(ei, state, si);

			// check if the connection is established
			if (ServerOpUtil.checkConnectionEstablished(futureChannel)) {
				ei.setActive(true);
				ei.setChannel(futureChannel.channel());
				logger.info("Connected to node " + ei.getRef() + " " + ei.getHost() + " " + ei.getPort());
			} else {
				throw new RuntimeException("Not able to establish connection to server");
			}

		} catch (Exception ex) {

			if (!(ex instanceof ConnectException)) {
				logger.error("Exception while creating a handler for connecting to a server", ex);
			}
		}

	}

}
