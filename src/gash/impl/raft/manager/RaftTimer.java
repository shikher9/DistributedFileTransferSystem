package gash.impl.raft.manager;

import gash.impl.raft.manager.NodeData.RaftStatus;
import gash.router.server.ServerInfo;

import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RaftTimer implements Runnable {

    protected static Logger logger = LoggerFactory.getLogger("RaftTimer");
    protected static AtomicReference<RaftTimer> instance = new AtomicReference<RaftTimer>();
    private static ServerInfo _siInfo = null;
    private Integer syncPt = 1;
    private static RaftTimer _raftTimer = null;

    public static RaftTimer initManager() {
        instance.compareAndSet(null, new RaftTimer());
        return instance.get();
    }

    private RaftTimer() {

    }

    public void SetServerInfo(ServerInfo siInfo) {
        _siInfo = siInfo;
    }

    public ServerInfo GetSetServerInfo() {
        return _siInfo;
    }

    public static RaftTimer getInstance() {
        // TODO throw exception if not initialized!
        if (_raftTimer == null) {
            _raftTimer = new RaftTimer();
            _raftTimer.initManager();
            //_raftTimer = instance.get();

        }
        return _raftTimer;
    }

    private static long lastBeatTime = System.currentTimeMillis();

    public long getLastBeatTime() {
        return lastBeatTime;
    }

    public static void setLastBeatTime(long lastBeatTime1) {
        lastBeatTime = lastBeatTime1;
    }

    @Override
    public void run() {

        //generate a random timeout 
        long randomTimeOut = getRandomTimeOut();

        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {

                e.printStackTrace();
            }
            logger.info("random time." + getRandomTimeOut());
            logger.info("current time. last beat" + System.currentTimeMillis());
            long difference = System.currentTimeMillis() - lastBeatTime;
            logger.info("Difference:" + difference);
            // If the follower does not receive heartbeat from leader node within the timeout interval, then start election
            if ((System.currentTimeMillis() - lastBeatTime) > getRandomTimeOut()) {

                lastBeatTime = System.currentTimeMillis();
                RaftElectionManager raftManager = RaftElectionManager.getInstance();
                raftManager.SetServerInfo(_siInfo);

                NodeDataManager mgr = NodeDataManager.getInstance();
                if (mgr.getNodeData().getNodeStatus() != RaftStatus.LEADER) {
                    logger.info("Timer has elapsed. This Follower node did not receive heartbeat from leader node. Starting election now..");
                    raftManager.startElection();
                }

            }

        }

    }

    //Generate random timeout
    public int getRandomTimeOut() {

        Random rgen = new Random();

        int rtimeout = rgen.nextInt(8000);
        rtimeout = rtimeout > 8000 ? rtimeout : (rtimeout + 7500);

        return rtimeout;
    }

}
