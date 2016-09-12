package gash.impl.raft.manager;

import gash.router.server.ServerInfo;

import java.io.File;

public class FileTransferInfo {

	private File file;
	private long userId;
	private String fileName;
	private int maxHops;
	private ServerInfo si;
	
	
	
	
	
	
	public FileTransferInfo(File file, long userId, String fileName,
			int maxHops, ServerInfo si) {
		super();
		this.file = file;
		this.userId = userId;
		this.fileName = fileName;
		this.maxHops = maxHops;
		this.si = si;
	}
	
	
	
	public long getUserId() {
		return userId;
	}



	public void setUserId(long userId) {
		this.userId = userId;
	}



	public File getFile() {
		return file;
	}
	public void setFile(File file) {
		this.file = file;
	}
	
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public int getMaxHops() {
		return maxHops;
	}
	public void setMaxHops(int maxHops) {
		this.maxHops = maxHops;
	}
	public ServerInfo getSi() {
		return si;
	}
	public void setSi(ServerInfo si) {
		this.si = si;
	}


//	@Override
//	public boolean equals(Object obj) {
//		// TODO Auto-generated method stub
//		
//		FileTransferInfo fti = (FileTransferInfo)obj;
//		
//		if(fti.getUserId() == this.userId &&
//				fti.getFileName() == this.fileName &&
//				fti.getMaxHops() == this.maxHops &&
//				fti.getFile() == this.file) {
//			return true;
//		}
//			
//			
//		return false;
//	}
	
}
