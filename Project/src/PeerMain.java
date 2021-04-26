public class PeerMain {
    public static void main(String[] args) {

        PeerNode myPeer = new PeerNode(5002, 5000, "192.168.1.5", "", "");

        

        // new Thread(new Runnable(){
        //     @Override
        //     public void run() {
        //         myPeer.openServer();
        //     }
        // }).start();

        // disconnect();
        //         connect(trackerIP, trackerPort);
        //         setUserName(askPeerInput("Enter username"));
        //         register();
        boolean registerFlag = false;
        while(true)
        {
            
            boolean loginFlag = false;
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
                    registerFlag = myPeer.register();
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
            }
            else if(choice.equals("3"))
            {
                //TO DO
            }
            else if(choice.equals("4"))
            {
                //TO DO
            }
            else if(choice.equals("5"))
            {
                //TO DO
            }
            else if(choice.equals("6"))
            {
                //TO DO
            }
            else if(choice.equals("7"))
            {
                //TO DO
            }
        }
    }
}
