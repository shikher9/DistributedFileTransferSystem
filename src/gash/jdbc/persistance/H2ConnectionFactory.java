package gash.jdbc.persistance;

public class H2ConnectionFactory implements IDBFactory {

	@Override
	public IDataStore GetDataStore() {
		// TODO Auto-generated method stub
		return new H2DataStore();
	}


}
