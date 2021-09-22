import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;


import javax.swing.*;
import java.io.*;
import java.lang.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;


@ExtensionInfo(
        Title = "GWardrobe",
        Description = "Save and load your habbo outfits.",
        Version = "1.0",
        Author = "schweppes0x"
)

public class GWardrobe extends ExtensionForm{
    private File outfitFile;

    private HashSet<String> loadedFigureStrings = new HashSet<String>();

    public ToggleButton wardrobeToggleButton;
    public ListView<String> outlookList;
    public TextField customFigureText;
    public ImageView generalImage;
    public ImageView wardrobeImage;
    public ImageView customFigureImage;
    public boolean loaded = false;
    private Image defaultImage = new Image("avatarimage.png");
    private String baseUrl = "https://www.habbo.com/habbo-imaging/avatarimage?size=m&figure=";

    @Override
    protected void initExtension() {
        generalImage.imageProperty().set(defaultImage);
        customFigureImage.imageProperty().set(defaultImage);
        //LoadWardrobe();

        intercept(HMessage.Direction.TOCLIENT, "FigureUpdate", hMessage -> {
            HPacket packet = hMessage.getPacket();
            String figureString = packet.readString();
            String sex = packet.readString();
            if(wardrobeToggleButton.isSelected()){
                AddFigure(figureString,sex);
                System.out.println("[!] - You changed your outfit, I added this to your wardrobe!");
            }
            Platform.runLater(()->{
                generalImage.imageProperty().set(new Image(baseUrl+figureString));
            });
        }); //On changed outfit

        intercept(HMessage.Direction.TOCLIENT, "Users", hMessage -> {
            if(generalImage.getImage() == defaultImage){
                HPacket packet = hMessage.getPacket();
                if (loaded)
                    return;
                try{
                    int x = packet.readInteger();
                    int userID = packet.readInteger();
                    String habboUsername = packet.readString();
                    String y = packet.readString();
                    String figure = packet.readString();
                    Platform.runLater(()->{
                        generalImage.imageProperty().set(new Image(baseUrl+figure));
                        loaded = true;
                    });
                }catch (Exception e){

                }
            }
        } ); //On room entry

    }

    private boolean WardrobeFileExists() {
        try{/*
            directory = new File(GWardrobe.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()+File.separator + "Wardrobe/");
            System.out.println(directory.getPath());

            //check if dir exists
            if(directory.exists()){
                System.out.println("[\uD83D\uDDF8] - Directory exists!");
            }else {

                System.out.println("[!] - Dir does not exist. Creating one...");
                Files.createDirectories(directory.toPath());
                System.out.println("[!] - Directory created!");
            }

            //check if file exists
            outfits = new File(directory.getPath(), "Outfits.txt");
            if(!outfits.exists()){
                System.out.println("[!] - File does not exist! Creating one..");
                outfits.createNewFile();
            }else {
                System.out.println("[\uD83D\uDDF8} - File exists!");
            }*/
            if(!outfitFile.exists())
                return false;

            ReadWardrobeFile();
            return true;
        }

        catch (Exception e){
            System.out.println("[!] - Something went wrong.");
            System.out.println(e);
            return false;
        }


    }

    private void WriteToFile(String text){
        if(outfitFile==null || !outfitFile.exists())
            return;

        try
        {
            FileWriter fileWriter = new FileWriter(outfitFile, true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            bufferedWriter.append(text);
            bufferedWriter.newLine();
            bufferedWriter.close();

        }
        catch(IOException ex) {
            System.out.println("Error writing to file");}
        }

    private void ReadWardrobeFile(){
        if(outfitFile==null || !outfitFile.exists())
            return;
        System.out.println("[\uD83D\uDDF8] - Trying to read file..");
        try {
            Scanner myReader = new Scanner(outfitFile);
            while (myReader.hasNextLine()) {
                String read = myReader.nextLine();
                if(read.isEmpty())
                    continue;
                loadedFigureStrings.add(read);
            }
            myReader.close();

            try{
                RewriteDataToFile();
            }
            catch (Exception e){
            }
        }
        catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    private void RewriteDataToFile() {
        try{
            outfitFile.delete();
            outfitFile.createNewFile();
            for (String string : loadedFigureStrings) {
                WriteToFile(string);
            }
        }
        catch (Exception e){

        }
    }

    public void clear(ActionEvent actionEvent) {
        outfitFile.delete();
        try {
            outfitFile.createNewFile();
        }
        catch (Exception e){

        }
    }

    private void AddFigure(String figureString, String sex){
        String toAdd = String.format("%s:%s", figureString,sex);
        if(loadedFigureStrings.add(toAdd))
            WriteToFile(toAdd);
        Update();
    }

    private void Update(){
        LoadFigures();
    }

    private void LoadWardrobe() {
        WardrobeFileExists(); /// check if file exist
        LoadFigures(); // load ListView table
    }

    private void LoadFigures() {

        Platform.runLater(()->{
            outlookList.getItems().clear();
            for (String s:loadedFigureStrings) {
                outlookList.getItems().add(s);
            }
        });
    }

    public String GetSelectedOutfit(MouseEvent mouseEvent) {
        Platform.runLater(()->{
            wardrobeImage.setImage(getImageByFigureString(outlookList.getSelectionModel().getSelectedItem().split(":")[0]));
        });
        return outlookList.getSelectionModel().getSelectedItem();
    }

    public void toggleWardrobeButton(ActionEvent actionEvent) {
        Platform.runLater(()->{
            if(wardrobeToggleButton.isSelected()){
                wardrobeToggleButton.textProperty().set("Turned ON");
                wardrobeToggleButton.styleProperty().set("-fx-background-color: #AAFFAA; -fx-border-color: #000000; -fx-border-radius: 5;");
            }else {
                wardrobeToggleButton.textProperty().set("Turned OFF");
                wardrobeToggleButton.styleProperty().set("-fx-background-color: #FFAAAA; -fx-border-color: #000000; -fx-border-radius: 5;");
            }
        });
    }

    public void OnSetOutfitClicked(ActionEvent actionEvent) {
        String[] figureStringAndSex = GetSelectedOutfit(null).split(":");
        SetOutfit(figureStringAndSex[0], figureStringAndSex[1]);
    }

    public void addCustomFigure(ActionEvent actionEvent) {
        //ch-3279-1408.sh-3027-110-64.cc-3075-110.hr-3163-45.lg-3058-64.ha-3620-0.hd-180-1390.ca-3187-96:M
        String[] figureStringAndSex = customFigureText.getText().split(":");
        AddFigure(figureStringAndSex[0], figureStringAndSex[1]);
    }

    private Image getImageByFigureString(String figureString){
        return new Image(baseUrl+figureString);
    }

    private void SetOutfit(String figureString, String sex){
        System.out.println(sendToServer(new HPacket("{out:UpdateFigureData}{s:\""+sex+"\"}{s:\""+figureString+"\"}")));
    }

    public void TurnOffGeneral(Event event) {
        if(!wardrobeToggleButton.isSelected())
            return;
        wardrobeToggleButton.setSelected(false);
        toggleWardrobeButton(null);
    }

    public void deleteSelected(ActionEvent actionEvent) {
        String toRemove = GetSelectedOutfit(null);
        loadedFigureStrings.remove(toRemove);
        RewriteDataToFile();
        LoadFigures();
        Platform.runLater(()->{
            wardrobeImage.setImage(new Image("avatarimage.png"));
        });
    }

    public void addSelectedHabboOutfit(ActionEvent actionEvent) {
    }

    public void changeCustomFigure(KeyEvent keyEvent) {
        if(customFigureText.getText().isEmpty()){
            customFigureImage.imageProperty().set(defaultImage);
        }
        Platform.runLater(()->{
            customFigureImage.imageProperty().set(getImageByFigureString(customFigureText.getText().split(":")[0]));

            if(customFigureText.getText().equals("")){
                customFigureText.styleProperty().set("-fx-background-color: #FFAAAA; -fx-border-color: #000000");
            }else {
                customFigureText.styleProperty().set("-fx-background-color: #AAFFAA; -fx-border-color: #000000");
            }
        });

    }

    public void OnOpenFile(ActionEvent actionEvent) {
        JFileChooser fileChooser = new JFileChooser();
        int returnVal = fileChooser.showOpenDialog(null);
        outfitFile = fileChooser.getSelectedFile();
        try{
            if(outfitFile.exists())
                LoadWardrobe();
        }
        catch (Exception e){

        }
        return;
    }
}
