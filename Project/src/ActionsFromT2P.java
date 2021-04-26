import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
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
        System.out.println("New thread start running...");
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
            }
        }while(!splitRequest[0].equals("Logout"));
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
                System.out.println("Tracker answer that peer's request is handled susccessfully");
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
        
    }
}
