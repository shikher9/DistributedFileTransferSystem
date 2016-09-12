package gash.server.message.work.handle;

import file.Filemessage.Chunk;
import file.Filemessage.ChunkHeader;
import gash.impl.raft.manager.FileTransferInfo;
import gash.impl.raft.manager.NodeDataManager;
import gash.impl.raft.manager.NodeData.RaftStatus;
import gash.router.server.ServerInfo;
import gash.router.server.ServerState;
import gash.router.server.edges.EdgeInfo;
import io.netty.channel.Channel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pipe.work.Work.WorkMessage;

public class ChunkHandler implements Handler {

	private Handler successor = null;
	protected ServerState state;
	private ServerInfo si;
	private File filesFolder;
	protected static Logger logger = LoggerFactory.getLogger("work-handler");
	private final HashMap<Long, HashMap<String, ChunkInfo>> ucimap = new HashMap<Long, HashMap<String, ChunkInfo>>();

	public ChunkHandler(ServerState state, ServerInfo si) {
		this.state = state;
		this.si = si;
	}

	@Override
	public void handleRequest(WorkMessage msg, Channel channel) {
		if (msg.hasChunk()) {
			try {
				createDownloadDirectory();
				processChunk(msg, channel);
			} catch (IOException ex) {
				logger.error("IO Exception ", ex);
			}
		} else if (successor != null) {
			successor.handleRequest(msg, channel);
		}
	}

	@Override
	public void setSuccessor(Handler next) {
		this.successor = next;
	}

	private void createDownloadDirectory() {

		filesFolder = new File("Files" + state.getConf().getNodeId());

		if (!filesFolder.exists()) {

			if (!filesFolder.mkdir()) {
				throw new RuntimeException("Unable to create files folder");
			}

		}
	}

	private void processChunk(WorkMessage msg, Channel channel)
			throws IOException {

		Chunk chunk = msg.getChunk();

		ChunkHeader header = chunk.getChunkHeader();
		String fileName = header.getFileName();
		long fileSize = header.getFileSize();
		long userID = header.getUserID();
		long chunkID = header.getChunkID();
		long currentBytesWritten = 0;
		int maxHops = header.getMaxhops();
		byte[] data = chunk.getChunkData().toByteArray();
		FileOutputStream fileOutputStream = null;

		try {
			if (ucimap.containsKey(userID)) {
				HashMap<String, ChunkInfo> ncimap = ucimap.get(userID);

				if (ncimap.containsKey(fileName)) {
					ChunkInfo ci = ncimap.get(fileName);

					fileOutputStream = ci.getFileOutputStream();
					fileOutputStream.write(data);

					ci.setCurrentBytesWritten(ci.getCurrentBytesWritten()
							+ data.length);
				} else {

					// write data
					String fileStoreName = String.valueOf(userID) + "-"
							+ fileName;
					fileOutputStream = new FileOutputStream(new File(
							filesFolder, fileStoreName));
					fileOutputStream.write(data);

					currentBytesWritten = data.length;
					ChunkInfo chunkInfo = new ChunkInfo(fileSize,
							currentBytesWritten, chunkID, fileOutputStream);
					ncimap.put(fileName, chunkInfo);
				}
			} else {

				// write data
				String fileStoreName = String.valueOf(userID) + "-" + fileName;
				fileOutputStream = new FileOutputStream(new File(filesFolder,
						fileStoreName));
				fileOutputStream.write(data);

				currentBytesWritten = data.length;

				HashMap<String, ChunkInfo> ncimap = new HashMap<String, ChunkInfo>();
				ChunkInfo chunkInfo = new ChunkInfo(fileSize,
						currentBytesWritten, chunkID, fileOutputStream);
				ncimap.put(fileName, chunkInfo);
				ucimap.put(userID, ncimap);
			}

		} catch (FileNotFoundException ex) {
			logger.error("File not found : ", ex);
		} finally {

			HashMap<String, ChunkInfo> ncimap = ucimap.get(userID);
			ChunkInfo chunkInfo = ncimap.get(fileName);
			long totalBytes = chunkInfo.getCurrentBytesWritten();

			if (totalBytes == fileSize) {

				System.out.println("Total Bytes : " + totalBytes);
				System.out.println("File Size : " + fileSize);

				FileOutputStream fos = chunkInfo.getFileOutputStream();
				fos.flush();
				fos.close();

				// update hashmaps
				ncimap.remove(fileName);

				// send file to followers
				if (NodeDataManager.getNodeData().getNodeStatus() == RaftStatus.LEADER) {
					// send uploaded file to other servers(followers) for
					// replication
					String fileStoreName = String.valueOf(userID) + "-"
							+ fileName;
					File fileToTransfer = new File(filesFolder, fileStoreName);
					FileTransferInfo fileTransferInfo = new FileTransferInfo(
							fileToTransfer, userID, fileName, maxHops, si);
					si.addFileToSendList(fileTransferInfo);
				}

			}
		}
	}

	private static class ChunkInfo {

		private long fileSize;
		private long currentBytesWritten;
		private long chunkID;
		private FileOutputStream fileOutputStream;

		public ChunkInfo(long fileSize, long currentBytesWritten, long chunkID,
				FileOutputStream fileOutputStream) {
			this.fileSize = fileSize;
			this.currentBytesWritten = currentBytesWritten;
			this.chunkID = chunkID;
			this.fileOutputStream = fileOutputStream;
		}

		public void setFileSize(long fileSize) {
			this.fileSize = fileSize;
		}

		public void setCurrentBytesWritten(long currentBytesWritten) {
			this.currentBytesWritten = currentBytesWritten;
		}

		public void setChunkID(long chunkID) {
			this.chunkID = chunkID;
		}

		public void setFileOutputStream(FileOutputStream fileOutputStream) {
			this.fileOutputStream = fileOutputStream;
		}

		public long getFileSize() {
			return fileSize;
		}

		public long getCurrentBytesWritten() {
			return currentBytesWritten;
		}

		public long getChunkID() {
			return chunkID;
		}

		public FileOutputStream getFileOutputStream() {
			return fileOutputStream;
		}

	}

}
