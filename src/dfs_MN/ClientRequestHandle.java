package dfs_MN;

import java.net.Socket;

/* Program specific import */

public class ClientRequestHandle implements Runnable{
	private Socket client_socket;
	private String uuid;
	
	public ClientRequestHandle(Socket soc,String uuid) {
		this.client_socket = soc;
		this.uuid = uuid;
	}
	@Override
	public void run() {				
		try {
			System.out.println("Sleeping : " + uuid);
			Thread.sleep(5000);
			Main_Node_Server.remove_active_request(uuid);
			System.out.println("removed " + uuid);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}		
	}

}
