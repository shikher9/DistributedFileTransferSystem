package gash.jdbc.persistance;

import java.util.List;

import com.couchbase.client.java.document.JsonDocument;

public class CouchbaseDataStore implements IDataStore {

	@SuppressWarnings("unchecked")
	@Override
	public <T> T GetConnection(String host, String userId,String password) {
		// TODO Auto-generated method stub
		return (T) CouchbaseDAO.GetConnection(host, userId, password);
		//return null;
		
	}
	
	@Override
	public boolean SaveData(ReplicationDTO dto) {
	     CouchbaseDAO _dao = new CouchbaseDAO();
	     _dao.CreateDocument(dto.GetBucketId(), dto.GetKey(), dto.GetValues());
		 return true;
	}

	@Override
	public List<ReplicationDTO>  GetData(ReplicationDTO dto) {
		// TODO Auto-generated method stub
		 CouchbaseDAO _dao = new CouchbaseDAO();
		 //JsonDocument doc = _dao.GetDocument(dto.GetBucketId());
		 return null;
	}

}
