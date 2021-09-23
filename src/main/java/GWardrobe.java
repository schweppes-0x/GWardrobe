import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.parsers.HEntity;
import gearth.extensions.parsers.HEntityType;
import gearth.extensions.parsers.HGender;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;


import java.io.*;
import java.lang.*;
import java.nio.file.Files;
import java.util.*;


@ExtensionInfo(
        Title = "GWardrobe",
        Description = "Save and load your habbo outfits.",
        Version = "1.0",
        Author = "schweppes0x"
)

public class GWardrobe extends ExtensionForm{
    private boolean isEnabled = false;
    private File directory;
    private File outfits;
    private HGender gender = HGender.Unisex;

    private final HashMap<Integer, HEntity> users = new HashMap<>();
    private final HashMap<Integer, String> userFigures = new HashMap<>();
    private final HashMap<Integer, HGender> userGenders= new HashMap<>();

    private int selectedIndex = -1;


    private HashSet<String> loadedFigureStrings = new HashSet<String>();
    private boolean hasHC;

    public ToggleButton wardrobeToggleButton;
    public ListView<String> outlookList;
    public TextField customFigureText;
    public ImageView generalImage;
    public ImageView wardrobeImage;
    public ImageView customFigureImage;
    public ImageView selectedHabboImage;
    public Button otherHabboButton;
    private Image defaultImage = new Image("avatarimage.png");
    private String baseUrl = "https://www.habbo.com/habbo-imaging/avatarimage?size=m&figure=";
    public boolean loaded = false;


    @Override
    protected void initExtension() {
        loadWardrobe();
        this.onConnect((s, i, s1, s2, hClient )->{
            hasHC = false;
            sendToServer(new HPacket("ScrGetUserInfo", HMessage.Direction.TOSERVER, "habbo_club"));
        });

        intercept(HMessage.Direction.TOCLIENT, "ScrSendUserInfo", hMessage -> {
            HPacket packet = hMessage.getPacket();
            if(packet.readString().equals("club_habbo")) {
                hasHC = true;
            }
        }); // Getting HC information

        generalImage.imageProperty().set(getImageByFigureString("hr-hr-3163-45.hd-180-1390.ch-3432-110-1408.lg-3434-110-1408.sh-3435-110-92.ha-3431-110-1408.cc-3360-110"));
        customFigureImage.imageProperty().set(defaultImage);

        intercept(HMessage.Direction.TOSERVER, "GetSelectedBadges", hMessage -> {
            if(!isEnabled)
                return;
            int id = hMessage.getPacket().readInteger();
            selectedIndex = users.values().stream()
                    .filter(hEntity -> hEntity.getId() == id)
                    .map(HEntity::getIndex)
                    .findFirst()
                    .orElse(-1);
            Platform.runLater(()->{
                selectedHabboImage.imageProperty().set(getImageByFigureString(userFigures.get(selectedIndex)));
                otherHabboButton.styleProperty().set("-fx-background-color: #AAFFAA; -fx-border-color: #000000; -fx-border-radius: 5;");
                otherHabboButton.textProperty().set("Add to Wardrobe");
                otherHabboButton.setDisable(false);

            });
        }); // Clicked on another habbo
        intercept(HMessage.Direction.TOCLIENT, "RoomReady", hMessage -> {
            if(!isEnabled)
                return;

            users.clear();
            userFigures.clear();
            userGenders.clear();

            Platform.runLater(()->{
                selectedHabboImage.imageProperty().set(defaultImage);
                otherHabboButton.setDisable(true);

            });
        }); // Loaded room
        intercept(HMessage.Direction.TOCLIENT, "Users", hMessage -> {
            if(!isEnabled)
                return;

            Arrays.stream(HEntity.parse(hMessage.getPacket()))
                    .filter(entity -> entity.getEntityType().equals(HEntityType.HABBO))
                    .forEach(entity -> {
                        users.put(entity.getIndex(), entity);
                        userFigures.put(entity.getIndex(), entity.getFigureId());
                        userGenders.put(entity.getIndex(), entity.getGender());
                    });
        }); // Loaded habbos
        intercept(HMessage.Direction.TOCLIENT, "UserChange", hMessage -> {
            if(!isEnabled)
                return;

            HPacket packet = hMessage.getPacket();
            int index = packet.readInteger();
            userFigures.replace(index, packet.readString());
            userGenders.replace(index, HGender.fromString(packet.readString()));
        }); // Someone came into the current room
        intercept(HMessage.Direction.TOCLIENT, "UserRemove", hMessage -> {

            int index = Integer.parseInt(hMessage.getPacket().readString());
            users.remove(index);
            userFigures.remove(index);
            userGenders.remove(index);
        }); // Someone left the current room
        intercept(HMessage.Direction.TOCLIENT, "FigureUpdate", hMessage -> {
            if(!isEnabled)
                return;

            HPacket packet = hMessage.getPacket();
            String figureString = packet.readString();
            String sex = packet.readString();
            if(wardrobeToggleButton.isSelected()){
                addFigure(figureString,sex);
                System.out.println("[!] - You changed your outfit, I added this to your wardrobe!");
            }
            Platform.runLater(()->{
                generalImage.imageProperty().set(new Image(baseUrl+figureString));
            });
        }); //On changed outfit

    }

    @Override
    protected void onShow() {
        super.onShow();
        isEnabled = true;
    }
    @Override
    protected void onHide() {
        super.onShow();

        users.clear();
        userFigures.clear();
        userGenders.clear();

        Platform.runLater(()->{
            selectedHabboImage.imageProperty().set(defaultImage);
            otherHabboButton.setDisable(true);
            selectedHabboImage.imageProperty().set(defaultImage);
            otherHabboButton.styleProperty().set("-fx-background-color: #FFAAAA; -fx-border-color: #000000; -fx-border-radius: 5;");
            otherHabboButton.textProperty().set("Click on a user");
            otherHabboButton.setDisable(true);
        });

        isEnabled = false;

    }

    private boolean wardrobeFileExists() {
        try{
            directory = new File(new File(GWardrobe.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParent() + "/Wardrobe");
            //check if dir exists
            if(directory.exists()){
                System.out.println("[\uD83D\uDDF8] - Directory exists!");
                System.out.println(directory.getPath());
            }else {

                System.out.println("[!] - Dir does not exist. Creating one...");
                Files.createDirectories(directory.toPath());
                System.out.println("[!] - Directory created!");
            }

            //check if file exists
            outfits = new File(directory, "Outfits.txt");
            if(!outfits.exists()){
                System.out.println("[!] - File does not exist! Creating one..");
                outfits.createNewFile();
            }else {
                System.out.println("[\uD83D\uDDF8] - File exists!");
            }

            if(!outfits.exists())
                return false;

            readWardrobeFile();
            return true;
        }

        catch (Exception e){
            System.out.println("[!] - Something went wrong.");
            System.out.println(e);
            return false;
        }

    }

    private void writeToFile(String text){
        if(outfits==null || !outfits.exists()){
            System.out.println("[!] - Error, Outfits.txt is null");
            return;
        }
        System.out.println("[\uD83D\uDDF8] - Trying to write to file.." );

        try
        {
            FileWriter fileWriter = new FileWriter(outfits, true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

            bufferedWriter.append(text);
            bufferedWriter.newLine();
            bufferedWriter.close();

        }
        catch(IOException ex) {
            System.out.println("Error writing to file");}
        }

    private void readWardrobeFile(){
        if(outfits==null || !outfits.exists())
            return;
        System.out.println("[\uD83D\uDDF8] - Trying to read file..");
        try {
            Scanner myReader = new Scanner(outfits);
            while (myReader.hasNextLine()) {
                String read = myReader.nextLine();
                if(read.isEmpty())
                    continue;
                loadedFigureStrings.add(read);
            }
            myReader.close();
            System.out.println("[\uD83D\uDDF8] - Done with writing to file. Total lines: " + loadedFigureStrings.size());
            try{
                //remove duplicates
                overwriteUniqueListToFile();
            }
            catch (Exception e){
            }
        }
        catch (FileNotFoundException e) {
            System.out.println("[!] - An error occurred while trying to write.");
            e.printStackTrace();
        }
    }

    private void overwriteUniqueListToFile() {
        try{
            System.out.println("[!] - Trying to Delete and Create file..");
            System.out.println("[!] Deleted: "+ outfits.delete());
            System.out.println("[!] Created: "+ outfits.createNewFile());

            System.out.println("[!] - Total lines to add: "+ loadedFigureStrings.size());
            for (String string : loadedFigureStrings) {
                writeToFile(string);
            }
        }
        catch (Exception e){
            System.out.println("[!] - Error while trying to overwrite file.");
        }
    }


    private void addFigure(String figureString, String sex){
        String toAdd = String.format("%s:%s", figureString,sex);
        if(loadedFigureStrings.add(toAdd)){
            writeToFile(toAdd);
            updateListView();
        }
    }


    private void loadWardrobe() {
        wardrobeFileExists(); /// check if file exist
        updateListView(); // load ListView table
    }

    private void updateListView() {

        Platform.runLater(()->{
            outlookList.getItems().clear();
            for (String s:loadedFigureStrings) {
                outlookList.getItems().add(s);
            }
        });
    }

    private void SetOutfit(String figureString, String sex){
        sendToServer(new HPacket("{out:UpdateFigureData}{s:\""+sex+"\"}{s:\""+figureString+"\"}"))
    }

    private Image getImageByFigureString(String figureString){
        return new Image(baseUrl+figureString);
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

    public void resetFile(ActionEvent actionEvent) {
        outfits.delete();
        try {
            outfits.createNewFile();
        }
        catch (Exception e){

        }
    }

    public void OnSetOutfitClicked(ActionEvent actionEvent) {
        String[] figureStringAndSex = GetSelectedOutfit(null).split(":");
        SetOutfit(figureStringAndSex[0], figureStringAndSex[1]);
    }

    public void addCustomFigure(ActionEvent actionEvent) {
        //use format shown below:
        //ch-3279-1408.sh-3027-110-64.cc-3075-110.hr-3163-45.lg-3058-64.ha-3620-0.hd-180-1390.ca-3187-96:M
        String[] figureStringAndSex = customFigureText.getText().split(":");
        addFigure(figureStringAndSex[0], figureStringAndSex[1]);
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
        overwriteUniqueListToFile();
        updateListView();
        Platform.runLater(()->{
            wardrobeImage.setImage(new Image("avatarimage.png"));
        });
    }

    public void addSelectedHabboOutfit(ActionEvent actionEvent) {
        if(selectedIndex == -1) {
            System.out.println("[x] - selectedIndex is unvalid");
            return;
        }

        SetOutfit(userFigures.get(selectedIndex), userGenders.get(selectedIndex).toString());
        addFigure(userFigures.get(selectedIndex), userGenders.get(selectedIndex).toString());

        updateListView();
    }

    public String GetSelectedOutfit(MouseEvent mouseEvent) {
        Platform.runLater(()->{
            wardrobeImage.setImage(getImageByFigureString(outlookList.getSelectionModel().getSelectedItem().split(":")[0]));
        });
        return outlookList.getSelectionModel().getSelectedItem();
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

}
