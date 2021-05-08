import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
public class FileIO{
    
    public static String readPeerFiles(String sharedDirectoryPath){
        File folder = new File(sharedDirectoryPath);
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
            
            File myObj = new File("fileDownloadList.txt");
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

    public static File returnRequestedFile(String filename, String sharedDirectoryPath){
        File file = new File(sharedDirectoryPath + "/" + filename + ".txt");
        if(!file.exists()) {
            return null;
        }else{
            return file;
        }
    }

    public static void splitTxt(int sizeOfChunk, String path)
    {
        String fileName = path.substring(0, path.lastIndexOf(".") + 1);
        File file = new File(path);
        FileInputStream fis = null;
        BufferedOutputStream bos = null;
        try {
            fis = new FileInputStream(file);
            byte[] buf = new byte[sizeOfChunk * 1024];

            int count = 0;
            for (int readNum; (readNum = fis.read(buf)) != -1;) {
                File temp = new File(fileName + count + ".txt");
                bos = new BufferedOutputStream(new FileOutputStream(temp));
                bos.write(buf);
                bos.flush();
                count++;
            }

            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}