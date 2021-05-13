import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.BufferedReader;


public class PeerNode {
    private final int CHUNK_SIZE = 512;
    private boolean hasLoginAgain = false;
    private int peerServerPort = 0;
    private String trackerIP = "";
    private int trackerPort = 0;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    Socket requestSocket = null;
    private ArrayList<String> onlinePeersFromDetailsResponse;

    private ObjectInputStream inP2P;
    private ObjectOutputStream outP2P;
    Socket requestSocketP2P = null;

    private String userName = "";
    private String userPass = "";
    private String tokenId = "";
    private String selectedFileName = "";
    private String sharedDirectoryPath = "";

    
    public PeerNode(int peerServerPort, int trackerPort, String trackerIP, String userName, String userPass)
    {
        this.peerServerPort = peerServerPort;
        this.trackerIP = trackerIP;
        this.trackerPort = trackerPort;

        this.userName = userName;
        this.userPass = userPass;
    }

    public void connect(String connectionIp, int connectionPort)
    {
        try
        {
            requestSocket = new Socket(connectionIp, connectionPort);
            out = new ObjectOutputStream(requestSocket.getOutputStream());
            in = new ObjectInputStream(requestSocket.getInputStream());
            System.out.println("Client Part of peer :: Peer is successfully connected with server with IP: " + connectionIp + " Port: " + connectionPort);
        } 
        catch(UnknownHostException unknownHost) 
        {
            System.err.println("You are trying to connect to an unknown host!");
        } 
        catch (IOException ioException) 
        {
            System.err.println("You are trying to connect to an offline server.Check the server IP and port");
            System.exit(1);
        }
    }

    public void connectP2P(String connectionIp, int connectionPort)
    {
        try
        {
            requestSocketP2P = new Socket();
            requestSocketP2P.connect(new InetSocketAddress(connectionIp, connectionPort), 2000);
            outP2P = new ObjectOutputStream(requestSocketP2P.getOutputStream());
            inP2P = new ObjectInputStream(requestSocketP2P.getInputStream());
            System.out.println("Client Part of peer :: Peer is successfully connected with peer's server with IP: " + connectionIp + " Port: " + connectionPort);
        } 
        catch(UnknownHostException unknownHost) 
        {
            System.err.println("You are trying to connect to an unknown peer host!");
        } 
        catch (IOException ioException) 
        {
            System.err.println("You are trying to connect to an offline server.Check the peer's server IP and port");
        }
    }

    public boolean checkActive(String peerIP, int peerPort)
    {
        String answerFromAnotherPeer = "";
        connectP2P(peerIP, peerPort);
        try {
            if(outP2P != null){outP2P.writeObject("Peer");}
            System.out.println("Client Part of Peer :: Initialize a stream with this an check_active request");
            if(outP2P != null){outP2P.flush();}
            System.out.println("Client Part of Peer :: Send an check_active request");
        } catch (IOException e) {
            System.out.println("An I/O error occurs when peer use output stream to check another peer's activity");
            e.printStackTrace();
            return false;
        }

        try {
            if(inP2P != null){answerFromAnotherPeer = (String)inP2P.readObject();}
        } catch (ClassNotFoundException e) {
            System.out.println("An ClassNotFoundException error occurs while peer was waiting active answer response fror another peer");
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            System.out.println("An I/O error occurs while peer was waiting active answer response fror another peer");
            e.printStackTrace();
            return false;
        }

        if(answerFromAnotherPeer.equals("Active")){
            System.out.println("Client Part of Peer :: Receive an active answer that target peer is active");
            return true;
        }
        System.out.println("Client Part of Peer :: Receive answer that target peer is not active");
        return false;
    }

    public void simpleDownload()
    {
        String currentPeerIp = "";
        String currentPeerPort = "";
        String currentPeerDownloads = "";
        String currentPeerFailures = "";
        double doubleCurrentScore = 0;
        ConcurrentHashMap<String, Double> scorePerPeer = new ConcurrentHashMap<>();
        
        for(String peerInfo : onlinePeersFromDetailsResponse)
        {
            currentPeerIp = peerInfo.split(",")[0];
            currentPeerPort = peerInfo.split(",")[1];
            currentPeerDownloads = peerInfo.split(",")[3];
            currentPeerFailures = peerInfo.split(",")[4];
            long start = Calendar.getInstance().getTimeInMillis();
            checkActive(currentPeerIp,Integer.parseInt(currentPeerPort));
            disconnectP2P();
            long end = Calendar.getInstance().getTimeInMillis();
            long time = end - start;
            System.out.println("time : " + time);
            doubleCurrentScore = time * Math.pow(0.9, Double.parseDouble(currentPeerDownloads)) * Math.pow(1.2, Double.parseDouble(currentPeerFailures));
            scorePerPeer.put(peerInfo, doubleCurrentScore);
        }
        List<Map.Entry<String, Double> > list = new LinkedList<Map.Entry<String, Double> >(scorePerPeer.entrySet());
  
        // Sort the list
        Collections.sort(list, new Comparator<Map.Entry<String, Double> >() {
            public int compare(Map.Entry<String, Double> o1, 
                               Map.Entry<String, Double> o2)
            {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });
        System.out.println(scorePerPeer);
        System.out.println(list);
        boolean flag = false;

        while(!flag && list.size() > 0)
        {
            flag = receiveFile(list.get(0).getKey().split(",")[0] ,Integer.parseInt(list.get(0).getKey().split(",")[1]), selectedFileName);
            disconnectP2P();
            notify(flag, list.get(0).getKey());
            list.remove(0);
        }

        if(!flag)
        {
            System.out.println("File " + selectedFileName + " could not be downloaded");
        }
    }

    private void notify(boolean isFileDownloaded, String peerInfoSourceOfFile)
    {
        if(isFileDownloaded)
        {
            try {
                out.writeObject("Notify,Success," + userName + "," + tokenId + "," + peerInfoSourceOfFile.split(",")[2] + "," + selectedFileName);
                System.out.println("Client Part of Peer :: Initialize an output stream with 'successful download' message");
                out.flush();
                System.out.println("Client Part of Peer :: Send a 'successful download' message to Tracker");
            } catch (IOException e) {
            System.out.println("Client Part of Peer :: An I/O error occurs when peer tries to send a 'successful download' message to Tracker");
                e.printStackTrace();
            }
        }
        else
        {
            try {
                out.writeObject("Notify,Fail," + peerInfoSourceOfFile.split(",")[2]);
                System.out.println("Client Part of Peer :: Initialize an output stream with 'invalid download' message");
                out.flush();
                System.out.println("Client Part of Peer :: Send a 'invalid download' message");
            } catch (IOException e) {
            System.out.println("Client Part of Peer :: An I/O error occurs when peer tries to send a 'invalid download' message to Tracker");
                e.printStackTrace();
            }
        }
    }

    public boolean receiveFile(String peerIP, int peerPort,String file)
    {
        connectP2P(peerIP, peerPort);
        byte[] answerFromAnotherPeer = null;
        int length = 0;
        try {
            outP2P.writeObject(file);
            System.out.println("Client Part of Peer :: Initialize a stream with this file request");
            outP2P.flush();
            System.out.println("Client Part of Peer :: Send an file request");
        } catch (IOException e) {
            System.out.println("An I/O error occurs when peer use output stream to request file for download");
            e.printStackTrace();  
            return false;  
        }
        try {
            length = (int)inP2P.readObject();
            answerFromAnotherPeer = (byte[])inP2P.readObject();
            if(length == answerFromAnotherPeer.length && length != 0){
                Files.write(Paths.get(sharedDirectoryPath + "/" + file + ".txt"), answerFromAnotherPeer);
                System.out.println("Client Part of Peer :: Receive the file " + file + " successfully with length " + answerFromAnotherPeer.length + " of " + length);
                return true;
            }
            else if(length != answerFromAnotherPeer.length)
            {
                System.out.println("Client Part of Peer :: Valid length of file " + file + " is " + length + "but client peer receive " + answerFromAnotherPeer.length + ".Download failed");
                return false;
            }else if(length == 0)
            {
                System.out.println("Client Part of Peer :: File was empty.Download failed");
                return false;
            }
        } catch (IOException | ClassNotFoundException e){
            System.out.println("Client Part of Peer :: An I/O|ClassNotFoundException error occurs when using the input stream for receiving requested file");
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public void disconnect()
    {
        if(requestSocket != null)
        {
            try {
                requestSocket.close();
            } catch (Exception e) {
                System.out.println("An I/O error occurs when try to close peer connection");
                e.printStackTrace();
            }
            if(out != null)
            {
                try {
                    out.close();
                } catch (Exception e) {
                    System.out.println("An I/O error occurs when try to close output stream");
                    e.printStackTrace();
                }
            }
            if(in != null)
            {
                try {
                    in.close();
                } catch (Exception e) {
                    System.out.println("An I/O error occurs when try to close input stream");
                    e.printStackTrace();
                }
            }
        }
        System.out.println("Client Part of Peer :: Peer disconnects from tracker successfully");
    }

    public void disconnectP2P()
    {
        if(requestSocketP2P != null)
        {
            try {
                requestSocketP2P.close();
            } catch (Exception e) {
                System.out.println("An I/O error occurs when try to close peer connection from another peer's server");
                e.printStackTrace();
            }
            if(outP2P != null)
            {
                try {
                    outP2P.close();
                } catch (Exception e) {
                    System.out.println("An I/O error occurs when try to close output stream from another peer's server");
                    e.printStackTrace();
                }
            }
            if(inP2P != null)
            {
                try {
                    inP2P.close();
                } catch (Exception e) {
                    System.out.println("An I/O error occurs when try to close input stream from another peer's server");
                    e.printStackTrace();
                }
            }
        }
        System.out.println("Client Part of Peer :: Peer disconnects from another peer's server successfully");
    }

    @SuppressWarnings("unchecked")
    public boolean request(String mode)
    {
            String request = "";
            String answerMessage = "";
            ArrayList<String> answerMessageList;
            if(mode.equals("Logout"))
            {
                request = mode + "," + this.tokenId;
            }else if(mode.equals("List")){
                request = mode;
            }else if(mode.equals("Details")){
                request = mode + "," + this.selectedFileName + "," + tokenId;
            }else{
                request = mode + "," + userName + "," + userPass;
            }
            
            try {
                out.writeObject(request);
                System.out.println("Client Part of Peer :: Initialize a stream with this request -> " + request);
                out.flush();
                System.out.println("Client Part of Peer :: Request '" + request + "' is sent successfully");
            } catch (IOException e) {
                System.out.println("An I/O error occurs when using the output stream for " + mode + " request");
                e.printStackTrace();
                return false;
            }

        try {
            
            System.out.println("Client Part of Peer ::Waiting to receive answer from server for " + mode + " request");
            if(mode.equals("Register"))
            {
                answerMessage = (String)in.readObject();
                return handleRegisterResponse(answerMessage);
            }else if(mode.equals("Login"))
            {
                answerMessage = (String)in.readObject();
                return handleLoginResponse(answerMessage);
            }else if(mode.equals("Logout"))
            {
                answerMessage = (String)in.readObject();
                return handleLogoutResponse(answerMessage);
            }else if(mode.equals("List")){
                answerMessageList = (ArrayList<String>)in.readObject();
                return handleListResponse(answerMessageList);
            }else{
                answerMessage = (String)in.readObject();
                return handleDetailsResponse(answerMessage);
            }
        } catch (IOException e) {
            System.out.println("An I/O error occurs when using the input stream for receiving answer of" + mode + " request");
            e.printStackTrace();
            return false;
        } catch (ClassNotFoundException e) {
            System.out.println("An 'ClassNotFoundException' error occurs when using the input stream for receiving answer of " + mode + " request");
            e.printStackTrace();
            return false;
        }
    }

    public boolean handleRegisterResponse(String answerMessage){
        if(answerMessage.equals("Success"))
        {
            System.out.println("Client Part of Peer :: Receive answer from server and register request was handled successfully");
            return true;
        }
        else if(answerMessage.equals("Fail"))
        {
            System.out.println("Client Part of Peer :: Receive answer from server and register request contains an existing user name");
            return false;
        }
        else
        {
            System.out.println("Client Part of Peer :: Receive unknown answer for register request -> " + answerMessage);
            return false;
        }
    }

    public boolean handleLoginResponse(String answerMessage){
        
        if(answerMessage.equals("Wrong password"))
        {
            System.out.println("Client Part of Peer :: Receive answer from server and login request contains wrong password");
            return false;
        }else if(answerMessage.equals("Wrong username"))
        {
            System.out.println("Client Part of Peer :: Receive answer from server and login request contains wrong user name");
            return false;
        }
        else
        {
            System.out.println("Client Part of Peer :: Receive a token id from server -> " + answerMessage);
            tokenId = answerMessage;
            if(!FileIO.readPeerFiles(sharedDirectoryPath).equals("") && !hasLoginAgain){
                seederInform();
            }
            else
            {
                inform("inform");
            }
            hasLoginAgain = true;
            return true;
        }
    }

    public boolean handleLogoutResponse(String answerMessage){
        if(answerMessage.equals("Success"))
        {
            System.out.println("Client Part of Peer :: Receive answer from server and logout request was handled successfully");
            disconnect();
            return true;
        }else{
            System.out.println("Client Part of Peer :: Receive answer from server and logout request was failed");
            return false;
        }
    }
    public boolean handleListResponse(ArrayList<String> answerMessage){
        if(answerMessage.isEmpty())
        {
            System.out.println("Client Part of Peer :: Receive answer from server and list of files is empty");
            return false;
        }else{
            System.out.println("Client Part of Peer :: Receive answer from server and the available files are:\n" + answerMessage);
            return true;
        }
    }
    public boolean handleDetailsResponse(String answerMessage){
        if(answerMessage.equals("Invalid"))
        {
            System.out.println("Client Part of Peer :: Receive answer from server and the request file does not exist to system");
            return false;
        }
        else if(answerMessage.equals("Fail"))
        {
            System.out.println("Client Part of Peer :: Receive answer from server and there is no peer connected with this file");
            return false;
        }else{
            System.out.println("Client Part of Peer :: Receive answer from server and the available peers for this file are:\n" + answerMessage);
            onlinePeersFromDetailsResponse = new ArrayList<>();
            for(String peerInfo : answerMessage.split("/"))
            {
                onlinePeersFromDetailsResponse.add(peerInfo);
            }
            return true;
        }
    }

    public void inform(String type){
        String inform = "";
        if(type.equals("seeder"))
            inform = type + "," + Boolean.toString(hasLoginAgain) + "," + getTrackerIP() + "," + peerServerPort 
            + "," + FileIO.readPeerFiles(this.sharedDirectoryPath);
        else{
            inform = type + "," + Boolean.toString(hasLoginAgain) + "," + getTrackerIP() + "," + peerServerPort 
            + "," + FileIO.readPeerFiles(this.sharedDirectoryPath);
        }
        try {
            out.writeObject(inform);
            System.out.println("Client Part of Peer :: Initialize a stream with this information -> " + inform);
            out.flush();
            System.out.println("Client Part of Peer :: Information '" + inform + "' is sent successfully");
        } catch (IOException e) {
            System.out.println("An I/O error occurs when using the output stream for sending information");
            e.printStackTrace();
        }
    }

    public void seederInform(){
        inform("seeder");
    }

    public void partition(){
        String[] slitPeerFiles = FileIO.readPeerFiles(sharedDirectoryPath).split(",");
        for(String file : slitPeerFiles){
            FileIO.splitTxt(CHUNK_SIZE, sharedDirectoryPath + "/" + file + ".txt");
        }
    }
    
    public String askPeerInput(String screenMessage)
    {
        System.out.println(screenMessage);
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
 
        // Reading data using readLine
        String temp = "";
        try {
            temp = reader.readLine();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println("Application receive peer's input '" + temp + "'");
        return temp;
    }

    public void openServer()
    {
        ServerSocket providerSocket = null;
        Socket connection = null;
        try
        {
            providerSocket = new ServerSocket(this.peerServerPort);
            while(true)
            {
                connection = providerSocket.accept();
                System.out.println("\nPeer Server Part :: A user is connected successfully");
                ActionsFromP2P newPeerConnection = new ActionsFromP2P(connection, getSharedDirectoryPath());
                new Thread(newPeerConnection).start();
                System.out.println("Peer Server Part :: A new thread is created to handle another user");
            }
        }
        catch (IOException e) 
        {
            System.out.println("Peer Server Part :: An I/O error occures during initialization of server socket");
            e.printStackTrace();
        } 
        finally 
        {
            try 
            {
                System.out.println("Peer Server Part :: Server is closed");
                System.out.println("Peer Server Part :: Remind message --> MENU is still open");
                providerSocket.close();
            } 
            catch (IOException ioException) 
            {
                ioException.printStackTrace();
            }
        }
    }

    public String getTrackerIP()
    {
        return this.trackerIP;
    }

    public int getTrackerPort()
    {
        return this.trackerPort;
    }

    public String getUsername()
    {
        return this.userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getUserPass()
    {
        return this.userPass;
    }

    public void setUserPass(String userPass)
    {
        this.userPass = userPass;
    }

    public String getSelectedFileName(){
        return this.selectedFileName;
    }

    public void setSelectedFileName(String selectedFileName){ 
        if(FileIO.readPeerFiles(sharedDirectoryPath).contains(selectedFileName)){
            System.out.println("This file is already in your directory");
            setSelectedFileName(askPeerInput("Select a file"));
        }else{
            this.selectedFileName = selectedFileName;
        }
    }

    public String getSharedDirectoryPath()
    {
        return this.sharedDirectoryPath;
    }

    public void setSharedDirectoryPath(String sharedDirectoryPath)
    {
        this.sharedDirectoryPath = sharedDirectoryPath;
    }

    public void setTrackerIP(String trackerIP)
    {
        this.trackerIP = trackerIP;
    }
}
