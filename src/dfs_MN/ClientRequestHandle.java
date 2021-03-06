package dfs_MN;

import dfs_api.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/* Program specific import */

/**
 * Created by Abhishek on 4/24/2016.
 */
//This is a Client Thread, which is created for EVERY client request handled
public class ClientRequestHandle implements Runnable
{
	private Socket client_socket;
	private String uuid;
	private ObjectInputStream ois;
	private ObjectOutputStream oos;
	private ClientRequestPacket req_packet;

	public ClientRequestHandle(Socket soc,String uuid)
	{
		this.client_socket = soc;
		this.uuid = uuid;
	}
	@Override
	public void run()
	{
		try
		{
			ois = new ObjectInputStream(client_socket.getInputStream());
			req_packet = (ClientRequestPacket)ois.readObject();

			if(req_packet == null)
				System.out.println("Handle NULL case");

			System.out.println("Request for ID : " + req_packet.client_uuid);

			handle_command(req_packet);
		}
		catch (IOException e)
		{
			System.out.println("FIRST");
			e.printStackTrace();
		}
		catch (ClassNotFoundException ex)
		{
			System.out.println("Second");
			ex.printStackTrace();
		}
		finally
		{
			MN_Server_PM.remove_active_request(uuid);
			try {
				ois.close();
				client_socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void handle_command(ClientRequestPacket req_packet)
	{
		ClientResponsePacket res_packet = null;
		String[] arg = null;

		switch (req_packet.command)
		{
			case DFS_CONSTANTS.REGISTER:
				ClientWrapper cw = new ClientWrapper(req_packet.client_uuid);
				DFS_Globals.global_client_list.put(req_packet.client_uuid,cw);
				res_packet = new ClientResponsePacket();
				res_packet.response_code = DFS_CONSTANTS.OK;

				/* Set the Secondary ip */
				if(DFS_Globals.synhronization_start)
				{
					System.out.println("Secondary IP is : " + DFS_Globals.sec_mn_ip_addr);
					arg = new String[DFS_CONSTANTS.ONE];
					arg[DFS_CONSTANTS.ZERO] = DFS_Globals.sec_mn_ip_addr;
				}
				res_packet.arguments = arg;
				break;

			case DFS_CONSTANTS.LOGIN:
				/* Validate the username requested */
				if(validate_login(req_packet))
				{
					/* Send Positive Feedback */
					res_packet = new ClientResponsePacket();
					res_packet.response_code = DFS_CONSTANTS.OK;
				}
				else
				{
					/* Send Negative Feedback */
					res_packet = new ClientResponsePacket();
					res_packet.response_code = DFS_CONSTANTS.AUTH_FAILED;
				}

				/* Set the Secondary ip */
				if(DFS_Globals.synhronization_start)
				{
					System.out.println("Secondary IP is : " + DFS_Globals.sec_mn_ip_addr);
					arg = new String[DFS_CONSTANTS.ONE];
					arg[DFS_CONSTANTS.ZERO] = DFS_Globals.sec_mn_ip_addr;
				}
				res_packet.arguments = arg;
				break;

			case DFS_CONSTANTS.MKDIR:
				res_packet = CommandHandler.commandMKDIR(req_packet);
				break;

			case DFS_CONSTANTS.LS:
				res_packet = CommandHandler.commandLS(req_packet);
				break;

			case DFS_CONSTANTS.GET:
				res_packet = CommandHandler.commandGET(req_packet);
				System.out.println("MN NAME : " + res_packet.file_name);
				System.out.println("MN Size : " + res_packet.file_size);
				break;

			case DFS_CONSTANTS.PUT:
				res_packet = CommandHandler.commandPUT(req_packet);
				break;

			case DFS_CONSTANTS.UPDATE:
				System.out.print("Anas : Command Update");
				res_packet = CommandHandler.commandPUTData(req_packet);
				break;

			default:
				System.out.println("Invalid Command");
				res_packet = new ClientResponsePacket();
				res_packet.response_code = DFS_CONSTANTS.INVALID_CMD;
		}
		send_response(res_packet);
	}

	public boolean validate_login(ClientRequestPacket req_packet)
	{
		ClientWrapper temp_cw;
		if((temp_cw = DFS_Globals.global_client_list.get(req_packet.client_uuid)) != null)
		{
			if(temp_cw.ID.equals(req_packet.client_uuid))
			{
				/* Send the client positive response */
				return true;
			}
			else
			{
				/* Send the client negative reponse */
				return false;
			}
		}
		else
		{
			/* Send the client negative response */
			return false;
		}
	}

	public void send_response(ClientResponsePacket res_packet)
	{
		try
		{
			System.out.println("Sending the Response with code : " + res_packet.response_code);
			oos = new ObjectOutputStream(client_socket.getOutputStream());
			oos.writeObject(res_packet);
			oos.flush();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
