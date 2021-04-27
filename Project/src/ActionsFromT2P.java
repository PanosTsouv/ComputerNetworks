import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class ActionsFromT2P extends Thread{

    private Socket connection;
    private ConcurrentHashMap<String, ArrayList<String>> registerUsers;
    private ConcurrentHashMap<String, ArrayList<String>> onlineUsers;
    private ConcurrentHashMap<String, ArrayList<String>> availableFilesWithPeers;
    private ArrayList<String> availableFiles;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    private Socket connectionT2P;
    private ObjectInputStream inT2P;
    private ObjectOutputStream outT2P;
    
    public ActionsFromT2P(Socket connection, ConcurrentHashMap<String, ArrayList<String>> registerUsers,
                                ConcurrentHashMap<String, ArrayList<String>> onlineUsers, ArrayList<String> availableFiles,
                                ConcurrentHashMap<String, ArrayList<String>> availableFilesWithPeers)
    {
        this.connection = connection;
        this.registerUsers = registerUsers;
        this.onlineUsers = onlineUsers;
        this.availableFiles = availableFiles;
        this.availableFilesWithPeers = availableFilesWithPeers;
        try {
            this.in = new ObjectInputStream(this.connection.getInputStream());
            this.out = new ObjectOutputStream(this.connection.getOutputStream());
        } catch (IOException e) {
            System.out.println("Error occures during initialization of in-out streams of connection");
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        System.out.println("\nNew thread start running...");
        String peerRequest = "";
        String[] splitRequest = null;

        do{
            try {
                peerRequest = (String)this.in.readObject();
                System.out.println("\nTracker receive a request -> " + peerRequest);
            } catch (ClassNotFoundException e) {
                System.out.println("A ClassNotFoundException error occurs while tracker tries to receive new request");
                e.printStackTrace();
                break;
            } catch (IOException e) {
                System.out.println("An I/O error occurs while tracker tries to receive new request");
                e.printStackTrace();
                break;
            }
            splitRequest = peerRequest.split(",");
            System.out.println("Tracker receive a request of type -> " + splitRequest[0]);

            if(splitRequest[0].equals("Register"))
            {
                System.out.println("Tracker receive a register request and start handling it.....");
                registerhandler(splitRequest);
                break;
            }else if(splitRequest[0].equals("Login")){
                System.out.println("Tracker receive a login request and start handling it.....");
                logInHandler(splitRequest);
            }else if(splitRequest[0].equals("Logout"))
            {
                System.out.println("Tracker receive a logout request and start handling it.....");
                logOutHandler(splitRequest);
            }else if(splitRequest[0].equals("List"))
            {
                System.out.println("Tracker receive a list request and start handling it.....");
                listHandler();
            }else if(splitRequest[0].equals("Details"))
            {
                System.out.println("Tracker receive a list request and start handling it.....");
                detailsHandler(splitRequest);
            }
        }while(!splitRequest[0].equals("Logout"));
    }

    public boolean connect(String connectionIp, int connectionPort, String tokenId)
    {
        try
        {
            connectionT2P = new Socket(connectionIp, connectionPort);
            outT2P = new ObjectOutputStream(connectionT2P.getOutputStream());
            inT2P = new ObjectInputStream(connectionT2P.getInputStream());
            System.out.println("Client Part of tracker :: Tracker is successfully connected with server part of peer with IP: " + connectionIp + " Port: " + connectionPort + " TokenId: " + tokenId);
            return true;
        } 
        catch(UnknownHostException unknownHost) 
        {
            System.err.println("You are trying to connect to an unknown host!");
            return false;
        } 
        catch (IOException ioException) 
        {
            System.err.println("You are trying to connect to an offline server.Check the server IP and port");
            return false;
        }
    }

    public void detailsHandler(String[] splitRequest)
    {
        String currentPeerIP = "";
        String currentPeerPort = "";

        String requestFileName = splitRequest[1];
        ArrayList<String> tempUsersWithFile = new ArrayList<>();
        synchronized(availableFilesWithPeers)
        {
            tempUsersWithFile.addAll(availableFilesWithPeers.get(requestFileName));
        }
        for(String peerTokenId : tempUsersWithFile)
        {
            synchronized(onlineUsers)
            {
                if(onlineUsers.get(peerTokenId) != null)
                {
                    currentPeerIP = onlineUsers.get(peerTokenId).get(0);
                    currentPeerPort = onlineUsers.get(peerTokenId).get(1);
                    if(connect(currentPeerIP, Integer.parseInt(currentPeerPort), peerTokenId))
                    {
                        try {
                            outT2P.writeObject("Tracker");
                            System.out.println("Client part of tracker :: initialize a request as Tracker to ckeck if peer is online");
                            outT2P.flush();
                            System.out.println("Client part of tracker :: send a request as Tracker to ckeck if peer is online");
                        } catch (IOException e) {
                            System.out.println("Client part of tracker :: An I/O error occurs when tracker use the output stream to send a message to peer");
                            e.printStackTrace();
                        }

                        String peerAnswer = "";
                        try {
                            peerAnswer = (String)inT2P.readObject();
                        } catch (ClassNotFoundException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        if(!peerAnswer.equals("Active"))
                        {
                            System.out.println("Peer with tokenId " + peerTokenId + " is active");
                        }
                        else
                        {
                            onlineUsers.remove(peerTokenId);
                            synchronized(availableFilesWithPeers)
                            {
                                availableFilesWithPeers.get(requestFileName).remove(peerTokenId);
                            }
                        }
                    }
                    else
                    {
                        onlineUsers.remove(peerTokenId);
                        synchronized(availableFilesWithPeers)
                        {
                            availableFilesWithPeers.get(requestFileName).remove(peerTokenId);
                        }
                    }
                }
                else{
                    synchronized(availableFilesWithPeers)
                    {
                        availableFilesWithPeers.get(requestFileName).remove(peerTokenId);
                    }
                }
            }
        }
        synchronized(availableFilesWithPeers)
        {
            tempUsersWithFile.clear();
            tempUsersWithFile.addAll(availableFilesWithPeers.get(requestFileName));
        }
    }

    public void logOutHandler(String[] splitRequest){
        synchronized(onlineUsers){
            onlineUsers.remove(splitRequest[1]);
            System.out.println("Tracker remove peer with token " + splitRequest[1] + " from online peers list");
            System.out.println("Online peer list now contains : " + onlineUsers.keySet());
        }
        try {
            out.writeObject("Success");
            out.flush();
            System.out.println("Tracker answer that peer's logout request is handled susccessfully");
        } catch (IOException e) {
            System.out.println("An I/O error occurs while tracker tries to send a positive answer for logout request");
            e.printStackTrace();
        }
    }

    public void registerhandler(String[] splitRequest)
    {
        String userName = splitRequest[1];
        String userPass = splitRequest[2];

        if(registerUsers.get(userName) != null)
        {
            try {
                out.writeObject("Fail");
                out.flush();
                System.out.println("Tracker answer that peer's name aready exists in register list");
            } catch (IOException e) {
                System.out.println("An I/O error occurs while tracker tries to send a negative answer for register request");
                e.printStackTrace();
            }
        }
        else if(registerUsers.get(userName) == null)
        {
            try {
                out.writeObject("Success");
                out.flush();
                System.out.println("Tracker answer that peer's register request is handled susccessfully");
            } catch (IOException e) {
                System.out.println("An I/O error occurs while tracker tries to send a positive answer for register request");
                e.printStackTrace();
            }


            ArrayList<String> registerUserInfo = new ArrayList<>();
            registerUserInfo.add(userPass);
            registerUserInfo.add("0");
            registerUserInfo.add("0");
            System.out.println("Tracker create a list with peer's info");

            synchronized(registerUsers)
            {
                registerUsers.put(userName, registerUserInfo);
                System.out.println("Tracker add peer " + userName + "=" + registerUsers.get(userName) + " to register list");
            }
        }
    }

    public void logInHandler(String[] splitRequest)
    {
        String userName = splitRequest[1];
        String userPass = splitRequest[2];
        String countDownloads = "";
        String countFailures = "";
        String userPasswordInRegisterList = "";
        String peerIp = "";
        String peerPort = "";
        ArrayList<String> userInfo;

        synchronized(registerUsers)
        {
            userInfo = registerUsers.get(userName);
            if(userInfo != null)
            {
                userPasswordInRegisterList = userInfo.get(0);
                countDownloads = userInfo.get(1);
                countFailures = userInfo.get(2);
            }
        }

        if(!userAuthentication(userName, userPass, userPasswordInRegisterList))
        {
            return;
        }

        Random rand = new Random();
        int tokenId = rand.nextInt(1000);

        try {
            out.writeObject(Integer.toString(tokenId));
            out.flush();
            System.out.println("Tracker gives tokenId " + tokenId + " to peer with name " + userName);
        } catch (IOException e) {
            System.out.println("An I/O error occurs while tracker tries to send tokenID for login request");
            e.printStackTrace();
        }
        String userInform = "";
        try {
            userInform = (String)in.readObject();
            peerIp = userInform.split(",")[0];
            peerPort = userInform.split(",")[1];
            System.out.println("Tracker receive Ip-Port from peer " + userName);
        } catch (ClassNotFoundException e) {
            System.out.println("A ClassNotFoundException error occurs while tracker tries to receive Ip-Port from peer " + userName);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("An I/O error occurs while tracker tries to receive Ip-Port from peer " + userName);
            e.printStackTrace();
        }

        updateOnlineUserList(peerIp, peerPort, userName, countDownloads, countFailures, Integer.toString(tokenId));
        updateFileLists(userInform.split(","), Integer.toString(tokenId));
    }

    private void updateFileLists(String[] splitRequest, String tokenId)
    {
        Boolean fileExists = false;
        for(int i = 2; i < splitRequest.length; i++)
        {
            synchronized(availableFiles)
            {
                fileExists = availableFiles.contains(splitRequest[i]);
            }
            if(fileExists)
            {
                synchronized(availableFilesWithPeers)
                {
                    if(availableFilesWithPeers.get(splitRequest[i]) == null)
                    {
                        ArrayList<String> temp = new ArrayList<>();
                        temp.add(tokenId);
                        availableFilesWithPeers.put(splitRequest[i], temp);
                    }
                    else
                    {
                        ArrayList<String> temp = availableFilesWithPeers.get(splitRequest[i]);
                        synchronized(temp)
                        {
                            temp.add(tokenId);
                        }
                    }
                }
            }
        }
        synchronized(availableFilesWithPeers)
        {
            System.out.println("Tracker update availableFilesWithPeers successfully " + availableFilesWithPeers);
        }
    }

    public boolean userAuthentication(String userName, String userPass, String userPasswordInRegisterList)
    {
        if(userPasswordInRegisterList.equals(""))
        {
            System.out.println("User with name " + userName + " does not exist");
            try {
                out.writeObject("Wrong username");
                out.flush();
                System.out.println("Tracker send that username " + userName + " does not exist to peer successfully");
            } catch (IOException e) {
                System.out.println("An I/O error occurs while tracker tries to send an error message (Wrong username) for login request");
                e.printStackTrace();
            } 
            return false;
        }
        if(!userPass.equals(userPasswordInRegisterList))
        {
            System.out.println("User with name " + userName + " give wrong password");
            try {
                out.writeObject("Wrong password");
                out.flush();
                System.out.println("Tracker send that password of username " + userName + " is wrong to peer successfully");
            } catch (IOException e) {
                System.out.println("An I/O error occurs while tracker tries to send an error message (Wrong password) for login request");
                e.printStackTrace();
            } 
            return false;
        }
        System.out.println("Tracker send that authedication for username " + userName + " is successfully");
        return true;
    }

    public void updateOnlineUserList(String peerIp, String peerPort, String userName, String countDownloads, String countFailures, String tokenId)
    {
        ArrayList<String> onlineUserInfo = new ArrayList<>();
        onlineUserInfo.add(peerIp);
        onlineUserInfo.add(peerPort);
        onlineUserInfo.add(userName);
        onlineUserInfo.add(countDownloads);
        onlineUserInfo.add(countFailures);

        System.out.println("Tracker create a list with online peer's info");

        synchronized(onlineUsers)
        {
            onlineUsers.put(tokenId, onlineUserInfo);
            System.out.println("Tracker add peer with tokenId " + tokenId + "=" + onlineUsers.get(tokenId) + " to online peers list");
        }
    }

    public void listHandler()
    {
        try {
            out.writeObject(availableFiles);
            out.flush();
            System.out.println("Tracker send list with avalables files " + availableFiles);
        } catch (IOException e) {
            System.out.println("An I/O error occurs while tracker tries to send list with avalables files for list request");
            e.printStackTrace();
        }
    }
}
