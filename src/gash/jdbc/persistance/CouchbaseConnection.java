package gash.jdbc.persistance;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.couchbase.client.ClusterManager;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;


public class CouchbaseConnection {

		  private static CouchbaseConnection _connectionManager = null;
          private static Cluster cluster;
		  public static CouchbaseConnection getInstance() {
			
			if(_connectionManager == null)
			{
				_connectionManager = new CouchbaseConnection();
			}
		  	return _connectionManager;
		  }
		  
		  public static Bucket GetConnection()
		  {
			  cluster = CouchbaseCluster.create();
			  Bucket bucket = cluster.openBucket("Mem2");
			  if (!bucket.exists("Mem2"))
				 {
					
				 }
			  return bucket;
		  
		  }
		  
		  public static void DisposeConnection()
		  {
			  if(cluster!=null)
			  {
			    cluster.disconnect();
			    cluster=null;
			  }
		  }
}

	
