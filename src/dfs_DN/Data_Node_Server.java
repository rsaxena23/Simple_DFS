package dfs_DN;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by abhishek on 4/26/16.
 */

/* Program specific imports */
import dfs_MN.ClientRequestHandle;
import dfs_api.*;

public class Data_Node_Server
{
    private static ExecutorService workers;

    /* List which maintains the current active client REQUESTS in the Server */
    private static ConcurrentHashMap<String,RequestProcessor> active_request_list;

    private static ServerSocket request; /* Server Socket which listens for client request */
    private static InetAddress hostAddress;
    private static RequestProcessor curr_req;
    private static String new_uuid;

    private static StorageNode storageNode;

    //args[0] => Master Node IP, args[1] => Capacity
    public static void main(String[] args)
    {
        try
        {
            if(args.length != 2)
            {
                System.out.println("Please call with arguments as: <Master Node IP> <Data Node Capacity>");
                System.exit(0);
            }
			/* Wait for a connection so that it can be served in a thread */
            setUpDN(args); /* Set up the server */

            while(DFS_Globals.is_DN_on)
            {
                /* Generate a random UUID for Every new Client Request to be used as a Key in the HashMap */
                new_uuid = UUID.randomUUID().toString();

                curr_req = new RequestProcessor(request.accept(),new_uuid); /* Listen to request and assign the request to a worker thread */

                active_request_list.put(new_uuid,curr_req); /* Add the client to the end of the list */

                workers.submit(curr_req);
            }
        }
        catch(IOException ioe)
        {
            ioe.printStackTrace();
        }
        finally
        {
            cleanUpDN();
        }
    }

    public static void cleanUpDN()
    {
        try
        {
            request.close(); /* Close the server socket connection */
            workers.shutdown();
            workers.awaitTermination(1, TimeUnit.MINUTES);
			/* Close all the open connection */
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public static boolean remove_active_request(String uuid)
    {
        return (active_request_list.remove(uuid) != null);

    }

    public static void setUpDN(String[] args) throws IOException
    {
        //Registering Data Node with Master Node. Exit if Fails
        if(!NotifyMasterNode(args))
        {
            System.out.println("Data Node cannot register with Master Node");
            System.exit(0);
        }

        hostAddress = InetAddress.getLocalHost();  /* Get the host address */
        request = new ServerSocket(DFS_CONSTANTS.DN_LISTEN_PORT,DFS_CONSTANTS.REQUEST_BACK_LOG/*,hostAddress*/);
        active_request_list = new ConcurrentHashMap<String, RequestProcessor>();
        workers = Executors.newFixedThreadPool(DFS_CONSTANTS.NUM_OF_WORKERS);
        DFS_Globals.client_data = new HashMap();

        if((DFS_Globals.server_addr = System.getenv(DFS_CONSTANTS.DFS_SERVER_ADDR)) == null)
        {
            System.out.println("Please set the environment variable for Server Address");
            System.exit(DFS_CONSTANTS.SUCCESS);
        }
    }

    private static boolean NotifyMasterNode(String[] args) {

        //Creating Packet Transfer Object with the Main Node IP and Misc Listen Port
        PacketTransfer packetTransfer = new PacketTransfer(args[0],DFS_CONSTANTS.MN_MISC_LISTEN_PORT);

        Packet responsePacket;

        Packet clientRequestPacket = new Packet();
        clientRequestPacket.command = DFS_CONSTANTS.ADD_DN;

        try {
            ArrayList<StorageNode> storageNodes = new ArrayList<>();
            StorageNode tempNode = new StorageNode(InetAddress.getLocalHost().getHostAddress(),UUID.randomUUID().toString(),Integer.parseInt(args[1]));
            storageNodes.add(tempNode);

            //REGISTER WITH MASTER NODE
            packetTransfer.sendPacket(clientRequestPacket);

            responsePacket = packetTransfer.receivePacket();

            if (responsePacket == null || responsePacket.response_code!= DFS_CONSTANTS.OK)
                return false;
        }
        catch (UnknownHostException e) {
            e.printStackTrace();
            return false;
        }
        return  true;
    }
}
