package gash.jdbc.persistance;

import java.util.List;

public class H2DataStore implements IDataStore {
    
	H2DAO _h2dao = null;
	public H2DataStore()
	{
		_h2dao = new H2DAO();
	}
	
	@Override
	public <T> T GetConnection(String host, String userId,String password) {
		// TODO Auto-generated method stub
		
		return (T) _h2dao.GetConnection(host,userId,password);
		
	}
	
	@Override
	public boolean SaveData(ReplicationDTO dto) {
		// TODO Auto-generated method stub
		boolean isSuccess= false;
		try {
			isSuccess = _h2dao.SetLogEntries(dto);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return isSuccess;
	}

	@Override
	public  List<ReplicationDTO> GetData(ReplicationDTO dto) {
		// TODO Auto-generated method stub
		 List<ReplicationDTO> lstdto = null;
		try {
			lstdto =  _h2dao.GetLogEntry(dto);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return lstdto;
	}

	

}
