import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;
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

    public static ArrayList<String> readListFile(){
        ArrayList<String> fileList = new ArrayList<>();
        try {
            
            File myObj = new File("Project/src/fileDownloadList.txt");
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                fileList.add(myReader.nextLine());
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        return fileList;
    }
}