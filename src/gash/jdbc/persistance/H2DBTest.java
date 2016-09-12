package gash.jdbc.persistance;

import java.sql.ResultSet;
import java.util.List;

public class H2DBTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
		   
			IDBFactory _factory = new H2ConnectionFactory();
		    IDataStore store =	_factory.GetDataStore();
		    ReplicationDTO _dto = new ReplicationDTO();
		    _dto.SetUserId(101);
		    _dto.SetFileName("test1009");
		    store.GetConnection("jdbc:h2:tcp://localhost/~/test", "test", "test");
		    store.SaveData(_dto);
		    store.GetData(_dto);
			 
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
