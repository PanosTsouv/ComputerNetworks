import java.util.Random;

public class PeerMain {
    public static void main(String[] args) {

        Random r = new Random();
        PeerNode myPeer = new PeerNode(r.nextInt(4000-1000) + 1000, 5000, "192.168.1.5", "", "");
        myPeer.setSharedDirectoryPath(myPeer.askPeerInput("Enter path of shared directory"));

        new Thread(new Runnable(){
            @Override
            public void run() {
                myPeer.openServer();
            }
        }).start();
        
        FileIO.readListFile();

        boolean registerFlag = false;
        boolean loginFlag = false;
        boolean detailsFlag = false;
        while(true)
        {
            System.out.println("\nStatus: Register -> " + registerFlag + " , Login -> " + loginFlag);
            String choice = myPeer.askPeerInput
            ("Press 1 -> Register\nPress 2 -> Login\nPress 3 -> list\nPress 4 -> details\nPress 5 -> checkActive \nPress 6 -> simpleDownload\nPress 7 -> Logout");

            if(choice.equals("1"))
            {
                if(registerFlag == true)
                {
                    System.out.println("You have already register to tracker");
                    continue;
                }
                while(registerFlag==false){
                    myPeer.connect(myPeer.getTrackerIP(), myPeer.getTrackerPort());
                    myPeer.setUserName(myPeer.askPeerInput("Enter username").replaceAll("[ ']", ""));
                    myPeer.setUserPass(myPeer.askPeerInput("Enter password").replaceAll("[ ']", ""));
                    registerFlag = myPeer.request("Register");
                    myPeer.disconnect();
                } 
            }
            else if(choice.equals("2"))
            {
                if(registerFlag == false)
                {
                    System.out.println("You should register to tracker before log in");
                    continue;
                }
                if(loginFlag == true)
                {
                    System.out.println("You have already log in to tracker");
                    continue;
                }
                myPeer.connect(myPeer.getTrackerIP(), myPeer.getTrackerPort());
                while(loginFlag==false){
                    myPeer.setUserName(myPeer.askPeerInput("Enter username").replaceAll("[ ']", ""));
                    myPeer.setUserPass(myPeer.askPeerInput("Enter password").replaceAll("[ ']", ""));
                    loginFlag = myPeer.request("Login");
                }
            }
            else if(choice.equals("3"))
            {
                if(loginFlag==false){
                    System.out.println("You have to log in to tracker first");
                    continue;
                }
                myPeer.request("List");
            }
            else if(choice.equals("4"))
            {
                if(loginFlag==false){
                    System.out.println("You have to log in to tracker first");
                    continue;
                }
                myPeer.setSelectedFileName(myPeer.askPeerInput("Select a file"));
                myPeer.request("Details");
                
            }
            else if(choice.equals("5"))
            {
                if(loginFlag == false)
                {
                    System.out.println("You can not checkActive without login first");
                    continue;
                }
                myPeer.checkActive(myPeer.askPeerInput("Enter peer's IP").replaceAll("[ ']", ""), Integer.parseInt(myPeer.askPeerInput("Enter peer's Port").replaceAll("[ ']", "")));
                detailsFlag = true;
            }
            else if(choice.equals("6"))
            {
                if(detailsFlag==true){
                    myPeer.simpleDownload();
                    detailsFlag=false;
                }else{
                    System.out.println("You should press details first");
                }
                
            }
            else if(choice.equals("7"))
            {
                if(loginFlag == false)
                {
                    System.out.println("You can not logout without login first");
                    continue;
                }
                if(myPeer.request("Logout"))
                {
                    loginFlag = false;
                }
            }
        }
    }
}
