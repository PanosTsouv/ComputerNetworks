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
        System.out.println("New thread start running...");
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
                System.out.println("Tracker receive a details request and start handling it.....");
                detailsHandler(splitRequest);
            }else if(splitRequest[0].equals("Notify"))
            {
                System.out.println("Tracker receive a notify request and start handling it.....");
                notifyHandler(splitRequest);
            }
        }while(!splitRequest[0].equals("Logout"));
        closeServerConnectionStreams();
    }

    public void notifyHandler(String[] splitRequest)
    {
        if(splitRequest[1].equals("Success"))
        {
            synchronized(availableFilesWithPeers)
            {
                availableFilesWithPeers.get(splitRequest[5]).add(splitRequest[3]);
                System.out.println("Tracker show availableFilesWithPeers" + availableFilesWithPeers);
            }

            synchronized(registerUsers)
            {
                registerUsers.get(splitRequest[4]).set(1, Integer.toString(Integer.parseInt(registerUsers.get(splitRequest[4]).get(1)) + 1));
                System.out.println("Tracker show registerUsers" + registerUsers);
            }

            synchronized(onlineUsers)
            {
                for(String onPeer : onlineUsers.keySet())
                {
                    if(onlineUsers.get(onPeer).contains(splitRequest[4]))
                    {
                        if(onlineUsers.get(onPeer) != null) onlineUsers.get(onPeer).set(3, Integer.toString(Integer.parseInt(onlineUsers.get(onPeer).get(3)) + 1));
                    }
                }
                System.out.println("Tracker show onlineUsers" + onlineUsers);
            }

        }
        else{
            synchronized(registerUsers)
            {
                registerUsers.get(splitRequest[2]).set(2, Integer.toString(Integer.parseInt(registerUsers.get(splitRequest[2]).get(2)) + 1));
                System.out.println("Tracker show registerUsers" + registerUsers);
            }

            synchronized(onlineUsers)
            {
                for(String onPeer : onlineUsers.keySet())
                {
                    if(onlineUsers.get(onPeer).contains(splitRequest[2]))
                    {
                        if(onlineUsers.get(onPeer) != null) onlineUsers.get(onPeer).set(4, Integer.toString(Integer.parseInt(onlineUsers.get(onPeer).get(4)) + 1));
                    }
                }
                System.out.println("Tracker show onlineUsers" + onlineUsers);
            }
        }
    }

    public void closeServerConnectionStreams()
    {
        if(connection != null)
        {
            try {
                connection.close();
            } catch (Exception e) {
                System.out.println("An I/O error occurs when try to close server connection from server part of Tracker");
                e.printStackTrace();
            }
            if(out != null)
            {
                try {
                    out.close();
                } catch (Exception e) {
                    System.out.println("An I/O error occurs when try to close output stream from server part of Tracker");
                    e.printStackTrace();
                }
            }
            if(in != null)
            {
                try {
                    in.close();
                } catch (Exception e) {
                    System.out.println("An I/O error occurs when try to close input stream from server part of Tracker");
                    e.printStackTrace();
                }
            }
        }
        System.out.println("Tracker disconnects from peer successfully");
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

    public void disconnect()
    {
        if(connectionT2P != null)
        {
            try {
                connectionT2P.close();
            } catch (Exception e) {
                System.out.println("An I/O error occurs when try to close server connection to peer");
                e.printStackTrace();
            }
            if(outT2P != null)
            {
                try {
                    outT2P.close();
                } catch (Exception e) {
                    System.out.println("An I/O error occurs when try to close output stream");
                    e.printStackTrace();
                }
            }
            if(inT2P != null)
            {
                try {
                    inT2P.close();
                } catch (Exception e) {
                    System.out.println("An I/O error occurs when try to close input stream");
                    e.printStackTrace();
                }
            }
        }
        System.out.println("Client Part of Tracker :: Tracker disconnects from peer successfully");
    }

    public void detailsHandler(String[] splitRequest)
    {
        String currentPeerIP = "";
        String currentPeerPort = "";

        String requestFileName = splitRequest[1];
        if(!availableFiles.contains(requestFileName))
        {
            try {
                out.writeObject("Invalid");
                System.out.println("Tracker answer that file " + requestFileName + " is not exist to system");
                return;
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        ArrayList<String> tempUsersWithFile = new ArrayList<>();
        ArrayList<String> tempOnlineUsersWithFile = new ArrayList<>();
        synchronized(availableFilesWithPeers)
        {
            if(availableFilesWithPeers.get(requestFileName) != null)
            {
                tempUsersWithFile.addAll(availableFilesWithPeers.get(requestFileName));
            }
        }
        for(String peerTokenId : tempUsersWithFile)
        {
            if(splitRequest[2].equals(peerTokenId)){
                continue;
            }
            synchronized(onlineUsers)
            {
                if(onlineUsers.get(peerTokenId) == null)
                {
                    synchronized(availableFilesWithPeers)
                    {
                        availableFilesWithPeers.get(requestFileName).remove(peerTokenId);
                        continue;
                    }
                }
                currentPeerIP = onlineUsers.get(peerTokenId).get(0);
                currentPeerPort = onlineUsers.get(peerTokenId).get(1);
            }
            if(connect(currentPeerIP, Integer.parseInt(currentPeerPort), peerTokenId))
            {
                try {
                    outT2P.writeObject("Tracker");
                    System.out.println("Client part of tracker :: initialize a request as Tracker to ckeck if peer is online");
                    outT2P.flush();
                    System.out.println("Client part of tracker :: send a request as Tracker to check if peer is online");
                } catch (IOException e) {
                    System.out.println("Client part of tracker :: An I/O error occurs when tracker use the output stream to send a message to peer");
                    e.printStackTrace();
                }

                String peerAnswer = "";
                try {
                    peerAnswer = (String)inT2P.readObject();
                } catch (ClassNotFoundException e) {
                    System.out.println("An ClassNotFoundException error occurs while tracker tries to receive message that peer is active");
                    e.printStackTrace();
                } catch (IOException e) {
                    System.out.println("An I/O error occurs while tracker tries to receive message that peer is active");
                    e.printStackTrace();
                }
                if(peerAnswer.equals("Active"))
                {
                    System.out.println("Peer with tokenId " + peerTokenId + " is active");
                    tempOnlineUsersWithFile.add(peerTokenId);
                    disconnect();
                    continue;
                }
            }
            synchronized(onlineUsers)
            {
                onlineUsers.remove(peerTokenId);
            }
            synchronized(availableFilesWithPeers)
            {
                availableFilesWithPeers.get(requestFileName).remove(peerTokenId);
            }
        }
        String detailsAnswer = "";
        synchronized(onlineUsers)
        {
            for(String peer: tempOnlineUsersWithFile){
                detailsAnswer += onlineUsers.get(peer).get(0) + "," + onlineUsers.get(peer).get(1) + "," + onlineUsers.get(peer).get(2) 
                + "," + onlineUsers.get(peer).get(3) + "," + onlineUsers.get(peer).get(4) + /* TODO */"/";
            }
        }
        try {
            if(detailsAnswer.equals(""))
            {
                out.writeObject("Fail");
                System.out.println("Tracker answer that there are not online peer with file " + requestFileName);
            }
            else
            {
                detailsAnswer = detailsAnswer.substring(0,detailsAnswer.length() - 1);
                out.writeObject(detailsAnswer);
                System.out.println("Tracker answer that there are online peers with file " + requestFileName 
                + " and send their information " + detailsAnswer);
            }
        } catch (IOException e) {
            
            e.printStackTrace();
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
            registerUserInfo.add("");
            registerUserInfo.add("");
            System.out.println("Tracker create a list with peer's info");

            synchronized(registerUsers)
            {
                registerUsers.put(userName, registerUserInfo);
                System.out.println("Tracker add peer " + userName + "=" + registerUsers.get(userName) + " to register list");
                System.out.println(registerUsers);
            }
        }
    }

    public void logInHandler(String[] splitRequest)
    {
        String userName = splitRequest[1];
        String userPass = splitRequest[2];
        String userPasswordInRegisterList = "";
        String countDownloads = "";
        String countFailures = "";
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

        int tokenId = 0;
        do{
            Random rand = new Random();
            tokenId = rand.nextInt(1000);
        }while(onlineUsers.containsKey(Integer.toString(tokenId)));

        try {
            out.writeObject(Integer.toString(tokenId));
            out.flush();
            System.out.println("Tracker gives tokenId " + tokenId + " to peer with name " + userName);
        } catch (IOException e) {
            System.out.println("An I/O error occurs while tracker tries to send tokenID for login request");
            e.printStackTrace();
        }

        informRequest(userName, countDownloads, countFailures, tokenId);
    }

    private void informRequest(String userName, String countDownloads, String countFailures, int tokenId)
    {
        String userInform = "";
        String peerIp = "";
        String peerPort = "";
        try {
            userInform = (String)in.readObject();
            peerIp = userInform.split(",")[2];
            peerPort = userInform.split(",")[3];
            System.out.println("Tracker receive Ip-Port from peer " + userName);

            String[] userInformArray = userInform.split(",");
            String allFilesAsString = userInform.replace(userInformArray[0] + "," + userInformArray[1] + "," + userInformArray[2] + "," + userInformArray[3] + ",", "");
            System.out.println(allFilesAsString);
            String files = "";
            for(String file : availableFiles){
                if(allFilesAsString.contains(file))
                {
                    files += file + ",";
                    allFilesAsString = allFilesAsString.replace(file + ",", "");
                }
            }
            if(!files.equals(""))
            {
                files.subSequence(0, files.length()-1);
            }

            if(userInform.split(",")[0].equals("inform"))
            {
                userInform = userInformArray[0] + "," + userInformArray[1] + "," + userInformArray[2] + "," + userInformArray[3] + "," + files;
                informHandler(peerIp, peerPort, userName, countDownloads, countFailures, userInform, tokenId);
            }
            else
            {
                String userinformWithFiles = userInform;
                userInform = userInformArray[0] + "," + userInformArray[1] + "," + userInformArray[2] + "," + userInformArray[3] + "," + allFilesAsString.replace(",", "/");
                seederInformHandler(peerIp, peerPort, userName, countDownloads, countFailures, userInform, tokenId, userinformWithFiles);
            }
        } catch (ClassNotFoundException e) {
            System.out.println("A ClassNotFoundException error occurs while tracker tries to receive Ip-Port from peer " + userName);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("An I/O error occurs while tracker tries to receive Ip-Port from peer " + userName);
            e.printStackTrace();
        }
    }

    private void informHandler(String peerIp, String peerPort, String userName, String countDownloads, String countFailures, String userInform, int tokenId)
    {
        updateOnlineUserList(peerIp, peerPort, userName, countDownloads, countFailures, Integer.toString(tokenId),userInform.split(","));
        updateFileLists(userInform.split(","), Integer.toString(tokenId));
    }

    private void seederInformHandler(String peerIp, String peerPort, String userName, String countDownloads, String countFailures, String userInform, int tokenId, String userinformWithFiles)
    {
        updateOnlineUserList(peerIp, peerPort, userName, countDownloads, countFailures, Integer.toString(tokenId),userInform.split(","));
        updateFileLists(userinformWithFiles.split(","), Integer.toString(tokenId));
    }

    private void updateFileLists(String[] splitRequest, String tokenId)
    {
        Boolean fileExists = false;
        for(int i = 4; i < splitRequest.length; i++)
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
                        availableFilesWithPeers.get(splitRequest[i]).add(tokenId);
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

    public void updateOnlineUserList(String peerIp, String peerPort, String userName, String countDownloads, String countFailures, String tokenId, String[] userInform)
    {
        ArrayList<String> onlineUserInfo = new ArrayList<>();
        onlineUserInfo.add(peerIp);
        onlineUserInfo.add(peerPort);
        onlineUserInfo.add(userName);
        onlineUserInfo.add(countDownloads);
        onlineUserInfo.add(countFailures);

        if(userInform[1].equals("true"))
        {
            synchronized(registerUsers)
            {
                onlineUserInfo.add(5, registerUsers.get(userName).get(3));
                onlineUserInfo.add(6, registerUsers.get(userName).get(4));
            }
        }
        else{
            if(userInform[0].equals("inform"))
            {
                onlineUserInfo.add("");
                onlineUserInfo.add("");
            }
            else
            {
                String fileName = "";
                String[] tempParts = userInform[4].split("/");
                String seederBit = "";
                for(String part : tempParts)
                {
                    if(part.substring(part.length() - 1, part.length()).equals("0"))
                    {
                        fileName = part.substring(0, part.length() - 2);
                        seederBit += fileName + "=1,";
                    }
                }
                onlineUserInfo.add(userInform[4]);
                onlineUserInfo.add(seederBit.substring(0, seederBit.length() - 1));

                synchronized(registerUsers)
                {
                    registerUsers.get(userName).set(3, userInform[4]);
                    registerUsers.get(userName).set(4, seederBit.substring(0, seederBit.length() - 1));
                }
            }
        }

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
            System.out.println("Tracker send list with available files " + availableFiles);
        } catch (IOException e) {
            System.out.println("An I/O error occurs while tracker tries to send list with avalables files for list request");
            e.printStackTrace();
        }
    }
}
