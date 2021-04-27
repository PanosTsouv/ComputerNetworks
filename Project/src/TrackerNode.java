import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;
import java.util.ArrayList;

public class TrackerNode {
    private int serverPort = 0;
    private ConcurrentHashMap<String, ArrayList<String>> registerUsers = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, ArrayList<String>> onlineUsers = new ConcurrentHashMap<>();
    private ArrayList<String> availableFiles = new ArrayList<>();
    private ConcurrentHashMap<String, ArrayList<String>> availableFilesWithPeers = new ConcurrentHashMap<>();
    
    public TrackerNode(int serverPort){
        this.serverPort = serverPort;
        availableFiles = FileIO.readListFile();
        System.out.println("Availables files are: " + availableFiles);
    }

    public void openServer(){
        ServerSocket providerSocket = null;
        Socket connection = null;
        try
        {
            providerSocket = new ServerSocket(this.serverPort);
            while(true)
            {
                connection = providerSocket.accept();
                System.out.println("\nA peer is connected successfully");
                ActionsFromT2P newPeerConnection = new ActionsFromT2P(connection, registerUsers, onlineUsers, availableFiles, availableFilesWithPeers);
                newPeerConnection.start();
                System.out.println("A new thread is created by server to handle peer");
            }
        }
        catch (IOException e) 
        {
            System.out.println("An I/O error occures during initialization of server socket");
            e.printStackTrace();
        } 
        finally 
        {
            try 
            {
                System.out.println("A peer disconnect from server successfully");
                providerSocket.close();
            } 
            catch (IOException ioException) 
            {
                ioException.printStackTrace();
            }
        }
    }
}
