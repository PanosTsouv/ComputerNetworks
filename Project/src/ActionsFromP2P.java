import java.net.Socket;

public class ActionsFromP2P extends Thread{

    Socket connection;

    public ActionsFromP2P(Socket connection){
        this.connection = connection;
        
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        super.run();
    }
}
