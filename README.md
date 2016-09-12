# Distributed File Transfer System

A distributed file transfer system using netty. This project just provides a basic framework and supports scalablity up to some extent. This basic setup using asynchrous messages and uses just a single thread for managing queue of incoming messages. This limits the scalablity which can be solved by creating multiple threads and distributing the work. This approach would take care of the increased number of messages in the queue during increased load.


# Technology and Algorithms

The project uses netty for file transfer , Raft for distributing the state machine and synchronization among machines in a cluster , and CouchDB and H2 Database Engine for storing data. All the source is in Java, both the client and the server and uses Ant build system.
