import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;

public class Cacher {
    public static final String dir;
    public static File outfitsFile;
    public static final String outfitsFileName = "outfits.json";

    static {
        String tryDir;
        try {
            tryDir = new File(GWardrobe.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParent() + "/Wardrobe";
        } catch (URISyntaxException e) {
            e.printStackTrace();
            tryDir = null;
        }
        dir = tryDir;

        if(dir!=null){
            File directory = new File(dir);
            if(!directory.exists())
                directory.mkdirs();
        }

        try {
            outfitsFile = new File(dir, outfitsFileName);
            if(!outfitsFile.exists()){
                FileWriter fileWriter = new FileWriter(outfitsFile);
                fileWriter.write("[]");
                fileWriter.flush();
            }
        }
        catch (IOException e) {
            System.out.println("[!] - Something went wrong with initializing file.");
            System.out.println(e);
        }
    }

    public static boolean updateCache(String content) {
        File parent_dir = new File(dir);
        parent_dir.mkdirs();

        try (FileWriter file = new FileWriter(new File(dir, outfitsFileName))) {
            file.write(content);
            file.flush();
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    public static JSONArray getJSONArrayFromFile() {
        if(!outfitsFile.exists()) return new JSONArray();

        try {
            return (JSONArray) new JSONParser().parse(new String(Files.readAllBytes(outfitsFile.toPath())));
        } catch (Exception e) {
            System.out.println("[!] - JSONArray from file couldn't be collected.");
        }
        return null;
    }
}