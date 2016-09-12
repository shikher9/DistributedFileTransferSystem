package gash.jdbc.persistance;
import java.sql.Connection;
import java.util.List;




import com.couchbase.client.ClusterManager;
import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;



public class CouchbaseDAO {
	
	
    private static CouchbaseConnection _cnn=null;
	public static <T> T GetConnection(String host, String userId, String password)
	{
		_cnn = CouchbaseConnection.getInstance();
		 return (T) _cnn;
	}
	
	public <T> boolean CreateDocument(String bucketId, String Key, List<T> Value)
	{
		CouchbaseConnection cnn = null;
		cnn = CouchbaseConnection.getInstance();
		Bucket bucket =  cnn.GetConnection();
		JsonObject content = JsonObject.create().put(Key,Value);
		JsonDocument inserted = bucket.upsert(JsonDocument.create(bucketId, content));
		
		return true;
	}
	

    public JsonDocument GetDocument(String bucketId)
    {
    	JsonDocument jso = null;
		try
		{
			
			Cluster cluster = CouchbaseCluster.create();
	        Bucket bucket = cluster.openBucket("loggentries");
	        if (bucket.exists(bucketId))
	        {
			    jso= bucket.get(bucketId);
				System.out.println(jso);
	        }
	        else
	        {
	        	throw new Exception("Bucket do not exist with id"+bucketId);
	        }
			
		}
		catch(Exception ex)
		{
			
		}
		
		return jso;
    }
		
	
}


