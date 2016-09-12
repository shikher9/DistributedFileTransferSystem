package gash.jdbc.persistance;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.annotation.Nullable;

import org.h2.jdbcx.JdbcDataSource;

import pipe.work.Work.LogEntry;
import pipe.work.Work.LogEntry.Builder;
import pipe.work.Work.Operation;

public class H2DAO extends gash.jdbc.persistance.JDBCBase{

	public H2DAO() {
		super("jdbc:h2:tcp://localhost/~/test", "test", "test");
		// TODO Auto-generated constructor stub
	}
	
	private static Connection _cnn=null;
	
	public static Connection GetConnection(String host, String userId, String password)
	{
		JdbcDataSource _ds = null;
		
		//RaftDAO _raftdao = new RaftDAO();
		_ds = new JdbcDataSource();
		String hostUrl = host!=null ? host: "jdbc:h2:tcp://localhost/~/test";
		_ds.setURL(hostUrl);
		
		String dbUser = userId!=null ? userId : "test";
		_ds.setUser(dbUser);
		
		String dbpassword = password!=null ? password : "test";
		_ds.setPassword(dbpassword);
		
		try {
			_cnn = _ds.getConnection();
			  return _cnn;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 return _cnn;
	}

	protected void addDatabaseCreateStatementsToBatch(Statement statement)
			throws Exception {
		// TODO Auto-generated method stub
		
	}


    public synchronized boolean SetLogEntries(ReplicationDTO dto) throws Exception {
        try {
                 
        	   /*   Operation op =  le.getOperation();
        	      final int userId = Integer.parseInt(op.getCmd());
        	      final String fn =  op.getFileName(); */
        	     final int userId = dto.GetUserId();
  	             final String fn =  dto.GetFileName();
  	             
        	      if(_cnn == null)
        	      {
        	    	  throw new Exception("Database connection not initialized");
        	    	    //_cnn = GetConnection();
        	      
        	      }
        	      
        	      //create table if not exists

        	      withStatement(_cnn, "CREATE TABLE IF NOT EXISTS NODE_LOGENTRIES(ID bigint auto_increment ,USERID INT, SERVERID INT, FILENAME VARCHAR(255), FILEPATH VARCHAR(255), CREATEDATE DATE)", new StatementBlock() {
                      @SuppressWarnings("deprecation")
						public void use(PreparedStatement statement) throws Exception {
   
                          //statement.setInt(1, userId);
                          //statement.setInt(2, 1);
                          //statement.setString(3, fn);
                          //DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
                          //Date date = new Date(2016, 4, 6);
                          //Calendar cal = Calendar.getInstance();
                          //statement.setDate(4, date);
                           statement.executeUpdate();
                          //checkState(rowsUpdated == 1, "commit_index: too many rows:%s)", rowsUpdated);
                      }
                  });  
        	      
        	      
                   boolean doUpdate = withStatement(_cnn, "SELECT COUNT(*) FROM LOGENTRIES WHERE FILENAME=? and userId=?", new StatementWithReturnBlock<Boolean>() {
                       @Override
                       public Boolean use(PreparedStatement statement) throws Exception {
                           statement.setString(1, fn);
                           statement.setInt(2, userId);
                           return withResultSet(statement, new ResultSetBlock<Boolean>() {
                               @Override
                               public Boolean use(ResultSet resultSet) throws Exception {
                                   resultSet.next(); // COUNT(*) should always return a value

                                   int count = resultSet.getInt(1);
                                     return count != 1;
                               }
                           });
                       }
                   });
                   
                   if(doUpdate)
                   {
                	withStatement(_cnn, "INSERT INTO LOGENTRIES (USERID,SERVERID,FILENAME,CREATEDATE) VALUES (?,?,?,?)", new StatementBlock() {
                        @SuppressWarnings("deprecation")
						@Override
                        public void use(PreparedStatement statement) throws Exception {
     
                            statement.setInt(1, userId);
                            statement.setInt(2, 1);
                            statement.setString(3, fn);
                            //DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
                            Date date = new Date(2016, 4, 6);
                            //Calendar cal = Calendar.getInstance();
                            statement.setDate(4, date);
                            int rowsUpdated = statement.executeUpdate();
                            //checkState(rowsUpdated == 1, "commit_index: too many rows:%s)", rowsUpdated);
                        }
                    });
                   }
               
            
        } catch (Exception e) {
            throw e;
        }
        return true;
    }
    
    @SuppressWarnings("rawtypes")
	public synchronized List<ReplicationDTO> GetLogEntry(ReplicationDTO dto) throws Exception {

    	List<ReplicationDTO> lstReplicationDTO = new ArrayList<ReplicationDTO>();
    	  if(_cnn == null)
	      {
	    	  throw new Exception("Database connection not initialized");
	    	    //_cnn = GetConnection();
	      
	      }
    	 PreparedStatement ps;
    	 ResultSet rs;
    	 List lst = new ArrayList<ReplicationDTO>();
    	 try {
    		 ps = _cnn.prepareStatement("SELECT ID,FILENAME FROM LOGENTRIES WHERE USERID = ?");
    		 ps.setInt(1, dto.GetUserId());
    		 rs = ps.executeQuery();
    		 
    		 while (rs.next()) 
    			 {
    			 //System.out.println(rs.getString("name"));
    			 ReplicationDTO _dto = new ReplicationDTO();
    			 _dto.SetFileName(rs.getString("FILENAME"));
    			 lstReplicationDTO.add(_dto);
    			 }
    		 //rs.close();

         } catch (Exception e) {
             throw new Exception("Failed getting log entry", e);
         }
    	 finally
    	 {
    		 _cnn.close();
    		
    	 }

    	 return lstReplicationDTO;
    }

  
	
}
