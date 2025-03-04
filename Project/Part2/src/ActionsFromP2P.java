import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ActionsFromP2P extends Thread{

    Socket connection;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String sharedDirectoryPath = "";

    public ActionsFromP2P(Socket connection, String sharedDirectoryPath){
        this.connection = connection;
        this.sharedDirectoryPath = sharedDirectoryPath;
        try {
            this.in = new ObjectInputStream(this.connection.getInputStream());
            this.out = new ObjectOutputStream(this.connection.getOutputStream());
        } catch (IOException e) {
            System.out.println("Server Part of Peer :: Error occures during initialization of in-out streams of connection of peer server");
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        System.out.println("Server Part of Peer :: New thread start running...");
        String request = "";
        String answer = "";
        
        try {
            request = (String)this.in.readObject();
            System.out.println("\nServer Part of Peer :: Peer server receive a request -> " + request);
        } catch (ClassNotFoundException e) {
            System.out.println("Server Part of Peer :: A ClassNotFoundException error occurs while peer server tries to receive new request");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Server Part of Peer :: An I/O error occurs while peer server tries to receive new request");
            e.printStackTrace();
        }
        if(request.equals("Tracker") || request.equals("Peer")){
            try {
                answer = "Active";
                out.writeObject(answer);
                System.out.println("Server Part of Peer :: Initialize a stream with this answer -> " + answer);
                out.flush();
                System.out.println("Server Part of Peer :: Answer '" + answer + "' is sent successfully");
            } catch (IOException e) {
                System.out.println("Server Part of Peer :: An I/O error occurs when using the output stream for active answer");
                e.printStackTrace();
            }
        }else{
            if(FileIO.returnRequestedFile(request, sharedDirectoryPath)!=null){
                try {
                    byte[] requestFile = Files.readAllBytes(Paths.get(sharedDirectoryPath + "/" + request + ".txt"));
                    out.writeObject(requestFile.length);
                    out.writeObject(requestFile);
                    System.out.println("Server Part of Peer :: Initialize a stream with this file -> " + request);
                    out.flush();
                    System.out.println("Server Part of Peer :: Send file successfully -> " + request);
                } catch (IOException e) {
                    System.out.println("Server Part of Peer :: An I/O error occurs when using the output stream for sending requested file");
                    e.printStackTrace();
                }
            }
            else{
                System.out.println("Server part of peer:: Given file doesnt exist in directory");
                try {
                    out.writeObject(0);
                    out.flush();
                    out.writeObject(new byte[0]);
                    out.flush();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                //TODO
            }
        }
        closeConnectionStreams();
        System.out.println("Server Part of Peer :: Remind message --> MENU is still open");
    }

    public void closeConnectionStreams()
    {
        if(connection != null)
        {
            try {
                connection.close();
            } catch (Exception e) {
                System.out.println("An I/O error occurs when try to close user connection from server part of peer");
                e.printStackTrace();
            }
            if(out != null)
            {
                try {
                    out.close();
                } catch (Exception e) {
                    System.out.println("An I/O error occurs when try to close output stream from server part of peer");
                    e.printStackTrace();
                }
            }
            if(in != null)
            {
                try {
                    in.close();
                } catch (Exception e) {
                    System.out.println("An I/O error occurs when try to close input stream from server part of peer");
                    e.printStackTrace();
                }
            }
        }
        System.out.println("Server Part of Peer :: Peer's server disconnects from user successfully");
    }
}
