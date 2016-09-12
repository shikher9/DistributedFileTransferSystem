package gash.jdbc.persistance;

import java.sql.Connection;
import java.util.List;

public interface IDataStore {
	
	public <T> T GetConnection(String host, String userId,String password);
	@SuppressWarnings("rawtypes")
	public boolean SaveData(ReplicationDTO dto);
	public List<ReplicationDTO> GetData(ReplicationDTO dto);
	
}


