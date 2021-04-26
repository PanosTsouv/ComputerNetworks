import java.io.File;
public class FileIO{
    
    public static String readPeerFiles(){
        File folder = new File("Project/peer1/shared_directory");
        File[] listOfFiles = folder.listFiles();
        String fileNames = "";
        for (File file : listOfFiles) {
            if (file.isFile()) {
                fileNames = fileNames + file.getName().replace(".txt","") + ",";
            }
        }
        fileNames = fileNames.substring(0,fileNames.length() - 1);
        return fileNames;
    }
}