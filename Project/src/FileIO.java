import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileIO{
    
    public static List<String> readPeerFiles(){
        File folder = new File("Project/peer1/shared_directory");
        File[] listOfFiles = folder.listFiles();
        ArrayList<String> fileNames = new ArrayList<>();
        for (File file : listOfFiles) {
            if (file.isFile()) {
                fileNames.add(file.getName());
            }
        }
        return fileNames;
    }
}