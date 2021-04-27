import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ActionsFromP2P extends Thread{

    Socket connection;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public ActionsFromP2P(Socket connection){
        this.connection = connection;
        try {
            this.in = new ObjectInputStream(this.connection.getInputStream());
            this.out = new ObjectOutputStream(this.connection.getOutputStream());
        } catch (IOException e) {
            System.out.println("Error occures during initialization of in-out streams of connection of peer server");
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        System.out.println("\nNew thread start running...");
        String request = "";
        String answer = "";
        
        try {
            request = (String)this.in.readObject();
            System.out.println("\nPeer server receive a request -> " + request);
        } catch (ClassNotFoundException e) {
            System.out.println("A ClassNotFoundException error occurs while peer server tries to receive new request");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("An I/O error occurs while peer server tries to receive new request");
            e.printStackTrace();
        }
        if(request.equals("Tracker")){
            try {
                answer = "Active";
                out.writeObject(answer);
                System.out.println("Client Part of Peer :: Initialize a stream with this answer -> " + answer);
                out.flush();
                System.out.println("Client Part of Peer :: Answer '" + answer + "' is sent successfully");
            } catch (IOException e) {
                System.out.println("An I/O error occurs when using the output stream for answer");
                e.printStackTrace();
            }
        }
    }
}
