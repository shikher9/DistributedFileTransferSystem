package gash.router.server;

import gash.router.container.RoutingConf;
import gash.router.server.edges.EdgeMonitor;
import gash.router.server.queueing.QueueItem;
import gash.router.server.tasks.TaskList;

import java.util.concurrent.LinkedBlockingDeque;

import routing.Pipe.CommandMessage;

public class ServerState {

    private RoutingConf conf;
    private EdgeMonitor emon;
    private TaskList tasks;
    private LinkedBlockingDeque<QueueItem> pendingQueue;

    public RoutingConf getConf() {
        return conf;
    }

    public void setConf(RoutingConf conf) {
        this.conf = conf;
    }

    public EdgeMonitor getEmon() {
        return emon;
    }

    public void setEmon(EdgeMonitor emon) {
        this.emon = emon;
    }

    public TaskList getTasks() {
        return tasks;
    }

    public void setTasks(TaskList tasks) {
        this.tasks = tasks;
    }

    public LinkedBlockingDeque<QueueItem> getPendingQueue() {
        return pendingQueue;
    }

    public void setPendingQueue(LinkedBlockingDeque<QueueItem> pendingQueue) {
        this.pendingQueue = pendingQueue;
    }

}
