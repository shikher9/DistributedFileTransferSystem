package gash.jdbc.persistance;

import java.util.ArrayList;
import java.util.List;

public class CouchbaseTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		IDBFactory _factory = new  CouchbaseConnectionFactory();
	    IDataStore store =	_factory.GetDataStore();
	    ReplicationDTO _dto = new ReplicationDTO();
	    _dto.SetUserId(101);
	    _dto.SetFileName("test1009");
	    store.GetConnection("jdbc:h2:tcp://localhost/~/test", "test", "test");
	    store.SaveData(_dto);
	    store.GetData(_dto);

	}

}
