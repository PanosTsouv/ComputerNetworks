import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class ActionsFromT2P extends Thread{

    private Socket connection;
    private ConcurrentHashMap<String, ArrayList<String>> registerUsers;
    private ConcurrentHashMap<String, ArrayList<String>> onlineUsers;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    
    public ActionsFromT2P(Socket connection, ConcurrentHashMap<String, ArrayList<String>> registerUsers,
                                ConcurrentHashMap<String, ArrayList<String>> onlineUsers)
    {
        this.connection = connection;
        this.registerUsers = registerUsers;
        this.onlineUsers = onlineUsers;
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
                System.out.println("Tracker receive a request -> " + peerRequest);
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
            }
        }while(!splitRequest[0].equals("Logout"));
    }

    public void logOutHandler(String[] splitRequest){
        synchronized(onlineUsers){
            onlineUsers.remove(splitRequest[1]);
            System.out.println("Tracker remove peer with token " + splitRequest[1] + "=" + onlineUsers.get(splitRequest[1]) " from online peers list");
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

        try {
            String userIpPort = (String)in.readObject();
            peerIp = userIpPort.split(",")[0];
            peerPort = userIpPort.split(",")[1];
            System.out.println("Tracker receive Ip-Port from peer " + userName);
        } catch (ClassNotFoundException e) {
            System.out.println("A ClassNotFoundException error occurs while tracker tries to receive Ip-Port from peer " + userName);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("An I/O error occurs while tracker tries to receive Ip-Port from peer " + userName);
            e.printStackTrace();
        }

        updateOnlineUserList(peerIp, peerPort, userName, countDownloads, countFailures, Integer.toString(tokenId));
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
            System.out.println("Tracker add peer with tokenId " + tokenId + "=" + onlineUsers.get(userName) + " to online peers list");
        }
    }
}
