package gash.jdbc.persistance;

import java.util.List;

public class DBHelper {
	
	IDBFactory _factory = null;
	IDataStore _store = null;
	DBHelper(IDBFactory factory)
	{
		_factory = factory;
	}
	
	public boolean SaveData(ReplicationDTO dto)
	{
		_store = _factory.GetDataStore();
		 return _store.SaveData(dto);
	}
	
	public List<ReplicationDTO> GetData(ReplicationDTO dto)
	{
		_store = _factory.GetDataStore();
		 return _store.GetData(dto);
	}

}
