import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.UnknownHostException;
import java.io.Console;


public class PeerNode {
    private int peerServerPort = 0;
    private String trackerIP = "";
    private int trackerPort = 0;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    Socket requestSocket = null;

    private String userName = "";
    private String userPass = "";

    
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
            if(out != null)
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

    public boolean request(String mode)
    {
        String request = mode + "," + userName + "," + userPass;
        String answerMessage = "";
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

            answerMessage = (String)in.readObject();
            System.out.println("Client Part of Peer :: Receive answer from server for " + mode + " request");
            if(mode.equals("Register")){
                return handleRegisterResponse(answerMessage);
            }else{
                return handleLoginResponse(answerMessage);
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
        
        if(answerMessage.equals("Wrong pass"))
        {
            System.out.println("Client Part of Peer :: Receive answer from server and login request contains wrong password");
            return false;
        }else if(answerMessage.equals("Wrong user"))
        {
            System.out.println("Client Part of Peer :: Receive answer from server and login request contains wrong user name");
            return false;
        }
        else
        {
            System.out.println("Client Part of Peer :: Receive a token id from server -> " + answerMessage);
            return true;
        }
    }

    public boolean sendPeerIpAndPort(){
        String request = "192.168.1.5," + peerServerPort;
        try {
            out.writeObject(request);
            System.out.println("Client Part of Peer :: Initialize a stream with this request -> " + request);
            out.flush();
            System.out.println("Client Part of Peer :: Request '" + request + "' is sent successfully");
        } catch (IOException e) {
            System.out.println("An I/O error occurs when using the output stream for sending ip and port");
            e.printStackTrace();
            return false;
        }
        return true;
    }
    

    

    public String askPeerInput(String screenMessage)
    {
        Console console = System.console();
        System.out.println(screenMessage);

        String temp = console.readLine();
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
                System.out.println("Peer Server Part :: A peer is connected successfully");
                ActionsFromP2P newPeerConnection = new ActionsFromP2P(connection);
                new Thread(newPeerConnection).start();
                System.out.println("Peer Server Part :: A new thread is created to handle another peer");
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
                System.out.println("Peer Server Part :: A peer disconnect from server successfully");
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











}
