package gash.jdbc.persistance;

public class CouchbaseConnectionFactory implements IDBFactory {


	public IDataStore GetDataStore() {
		// TODO Auto-generated method stub
		return new CouchbaseDataStore();
	}
	


	

}
