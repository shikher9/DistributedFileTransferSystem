package gash.jdbc.persistance;

import java.sql.Date;
import java.util.List;

public class ReplicationDTO<T> {

	private String _fileName;
	private int _userId;
	private String _fileUrl;
	private Date _createdate;
	private String _bucketId;
	private String _key;
	private List<T> _values;
	private String _dbHost;
	private String _dbUserId;
	private String _dbPassword;
	
	public void SetFileName(String fileName)
	{
		_fileName = fileName;
	}
	public String GetFileName()
	{
		return _fileName;
	}
	public void SetUserId(int userId)
	{
		_userId = userId;
	}
	public int  GetUserId()
	{
		return _userId;
	}
	public void SetFileUrl(String fileUrl)
	{
		_fileUrl = fileUrl;
	}
	public String  GetFileUrl()
	{
		return _fileUrl;
	}
	public void SetCreateDate(Date createdate)
	{
		_createdate = createdate;
	}
	public Date GetCreateDate()
	{
		return _createdate;
	}
	public void SetBucketId(String bucketId)
	{
		_bucketId = bucketId;
	}
	public String GetBucketId()
	{
		return _bucketId;
	}
	public void SetKey(String key)
	{
		_key = key;
	}
	public String GetKey()
	{
		return _key;
	}
	public void SetValues(List<T> values)
	{
		_values = values;
	}
	public List<T> GetValues()
	{
		return _values;
	}
	
}
