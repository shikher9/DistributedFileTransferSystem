/**
 * Copyright 2016 Gash.
 *
 * This file and intellectual content is protected under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package gash.router.app;

import gash.router.client.CommConnection;
import gash.router.client.CommListener;
import gash.router.client.MessageClient;
import java.io.File;
import routing.Pipe.CommandMessage;

public class DemoApp implements CommListener {

	private MessageClient mc;

	public DemoApp(MessageClient mc) {
		init(mc);
	}

	private void init(MessageClient mc) {
		this.mc = mc;
		this.mc.addListener(this);
	}

	@Override
	public String getListenerID() {
		return "demo";
	}

	@Override
	public void onMessage(CommandMessage msg) {
		System.out.println("---> " + msg);
	}

	/**
	 * sample application (client) use of our messaging service
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		String host = "127.0.0.1";
		int port = 8001;

		try {
			MessageClient mc = new MessageClient(host, port);
			DemoApp da = new DemoApp(mc);

			File file1 = new File("samplefile");
			// File file2 = new File("samplemovie2.mp4");
			// File file3 = new File("sampledoc3");

			da.uploadFile(file1, 11, 10, -1);
			// da.uploadFile(file2, 12, 10, -1);
			// da.uploadFile(file3, 13, 10, -1);

			// da.downloadFile("samplemovie1.mp4", 11);

			System.out.println("Client will stop after 10 minutes");
			Thread.sleep(10 * 60000L);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			CommConnection.getInstance().release();
		}
	}

	public void sendPing() {
		mc.ping();
	}

	public void sendMessage(String message) {
		mc.message(message);
	}

	public void uploadFile(File file, long userID, int maxHops, int destination) {
		mc.uploadFile(file, userID, maxHops, destination);
	}

	public void downloadFile(String fileName, long userID) {
		mc.downloadFile(fileName, userID);
	}

}
