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
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.lang.*;
import java.nio.file.Files;
import java.util.*;


@ExtensionInfo(
        Title = "GWardrobe",
        Description = "Save and load your habbo outfits.",
        Version = "1.3",
        Author = "schweppes0x"
)

public class GWardrobe extends ExtensionForm{

    private boolean isEnabled = false;
    private File directory;
    private File outfitsFile;

    private final HashMap<Integer, HEntity> users = new HashMap<>();
    private final HashMap<Integer, String> userFigures = new HashMap<>();
    private final HashMap<Integer, HGender> userGenders= new HashMap<>();

    private int selectedIndex = -1;

    private final HashSet<String> loadedFigureStrings = new HashSet<String>();
    private JSONArray currentOutfits = new JSONArray(); // JSON ARRAY


    public ToggleButton wardrobeToggleButton;
    public ListView<String> outlookList;
    public TextField customFigureText;
    public ImageView generalImage;
    public ImageView wardrobeImage;
    public ImageView customFigureImage;
    public ImageView selectedHabboImage;
    public Tab copyOthersTab;
    public Button otherHabboButton;
    public CheckBox aotChk;
    public CheckBox wearChk;

    public ChoiceBox<Character> genderChoiceBox;

    public TextField generalOutfitName;
    public TextField customOutfitName;
    public TextField otherOutfitName;


    private final Image defaultImage = new Image("defaultImage.png");
    private Image dinoImage;
    private final String baseUrl = "https://www.habbo.com/habbo-imaging/avatarimage?size=m&figure=";

    @Override
    protected void initExtension() {
        genderChoiceBox.getItems().add('F');
        genderChoiceBox.getItems().add('M');

        dinoImage = getImageByFigureString("hr-3163-45.hd-180-1390.ch-3432-110-1408.lg-3434-110-1408.sh-3435-110-92.ha-3431-110-1408.cc-3360-110");

        generalImage.imageProperty().set(dinoImage);
        customFigureImage.imageProperty().set(defaultImage);

        loadWardrobe();

        intercept(HMessage.Direction.TOSERVER, "GetSelectedBadges", hMessage -> {
            if(!isEnabled)
                return;

            if(!copyOthersTab.isSelected())
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
                otherHabboButton.textProperty().set("Copy to Wardrobe");
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
            if(!isEnabled)
                return;

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
            String gender = packet.readString();

            if(wardrobeToggleButton.isSelected()){
                String outfitName = generalOutfitName.getText();
                addOutfit(outfitName,figureString, gender);
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
                System.out.println("[!] - Directory exists!");
                System.out.println(directory.getPath());
            }else {

                System.out.println("[!] - Dir does not exist. Creating one...");
                Files.createDirectories(directory.toPath());
                System.out.println("[!] - Directory created!");
            }

            //check if file exists
            outfitsFile = new File(directory, "outfits.json");
            if(!outfitsFile.exists()){
                System.out.println("[!] - File does not exist! Creating one..");
                outfitsFile.createNewFile();
                insertJSONArray();
            }else {
                System.out.println("[!] - File exists!");
            }

            if(!outfitsFile.exists())
                return false;

            loadOutfits();
            return true;
        }

        catch (Exception e){
            System.out.println("[!] - Something went wrong.");
            System.out.println(e);
            return false;
        }

    }


    private boolean loadOutfits(){
        currentOutfits = new JSONArray();
        JSONParser parser = new JSONParser();
        try{
            currentOutfits = (JSONArray)parser.parse(new FileReader(outfitsFile));
            System.out.println("[!] - Loaded outfits. "+ currentOutfits.size());
        } catch (Exception e) {
            System.out.println("Smoething went wrong");
            return false;
        }
        updateListView();
        return true;
    }

    private boolean writeToJSONFile(JSONObject object){
        if(outfitsFile ==null || !outfitsFile.exists()){
            System.out.println("[!] - Error, outfits.json file does not exist.");
            return false;
        }

        System.out.println("[!] - Trying to write to file.." );
        JSONParser parser = new JSONParser();
        try{
            Object rootObject = parser.parse(new FileReader(outfitsFile));
            JSONArray outfitsFromFile = (JSONArray) rootObject;

            outfitsFromFile.add(object);

            FileWriter fileWriter = new FileWriter(outfitsFile);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(outfitsFromFile.toJSONString());
            bufferedWriter.flush();
            bufferedWriter.close();

            return true;
        }
        catch (Exception e){
            System.out.println("Error writing to file");
        }

        return false;
    }

    private boolean addOutfit(String name, String figureString, String gender){
        if(name.isEmpty() || name == "" || name == null){
            name = "No name";
        }
        gender = gender.toUpperCase();

        JSONObject toAdd = new JSONObject();
        toAdd.put("Name", name);
        toAdd.put("FigureString", figureString);
        toAdd.put("Gender", gender);

        return addOutfit(toAdd);
    }

    private boolean addOutfit(JSONObject outfit){
        if(alreadyExists(outfit.get("FigureString").toString()))
            return false;

        if(currentOutfits.add(outfit)){
            writeToJSONFile(outfit);
            updateListView();
        }
        return true;

    }

    private boolean deleteJSONObject(JSONObject toRemove){
        if(currentOutfits.remove(toRemove)) {
            if(currentOutfits.size() < 1){
                deleteAllOutfits();
                return true;
            }
            if (overWriteFile()) {
                updateListView();
                System.out.println("[!] - Removed succesfully");
                return true;
            }
        }

        return false;
    }

    private boolean alreadyExists(String figureString) {
        for (Object outfit:currentOutfits) {
            JSONObject obj = (JSONObject) outfit;
            if(obj.get("FigureString").equals(figureString)){
                System.out.printf("[!] - Duplicate found");
                return true;
            }
        }

        return false;
    }

    private boolean overWriteFile() {
        JSONArray currentTempOutfits = new JSONArray();

        for (Object object: currentOutfits) {
            currentTempOutfits.add((JSONObject)object);
        }

        if(!deleteAllOutfits())
            return false;

        System.out.println("Overwriting all " +currentTempOutfits.size()+ " figures");
        for (Object outfit: currentTempOutfits) {
            if(!writeToJSONFile((JSONObject) outfit))
                return false;
        }
        return true;
    }

    @FXML
    private boolean deleteAllOutfits(){
        currentOutfits.clear();
        try{
            if(outfitsFile ==null || !outfitsFile.exists()){
                System.out.println("[!] - Error, outfits.json file does not exist.");
                return false;
            }

            try{
                FileWriter fileWriter = new FileWriter(outfitsFile);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                bufferedWriter.write("[]");
                bufferedWriter.flush();
                bufferedWriter.close();
                updateListView();
                return true;
            }
            catch (Exception e){
                System.out.println("Error writing to file");
            }

            return false;

        }
        catch (Exception e){

        }

        /*System.out.println("file is deleted: " + outfitsFile.delete());
        try {
            boolean success = outfitsFile.createNewFile();
            if(success){
                System.out.println("Created new file");
                if(insertJSONArray()){
                    return loadOutfits();
                }
            }else {
                System.out.println("COULLD NOT CREATE FILE");
            }
        }

        catch (IOException e) {
            return false;
        }*/
        return false;
    }

    private boolean insertJSONArray() {
        try{
            FileWriter fileWriter = new FileWriter(outfitsFile,false);
            fileWriter.write("[]");
            fileWriter.flush();
            fileWriter.close();
            System.out.println("[!] - Created initial JSON Array");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    private void loadWardrobe() {
        wardrobeFileExists(); /// check if file exist
    }

    private void updateListView() {

        Platform.runLater(()->{
            outlookList.getItems().clear();
            System.out.println("Total LIST items: " +outlookList.getItems().size());
            System.out.println("Total OUTFIT items: " +currentOutfits.size());
            for (Object outfitObject :currentOutfits) {

                JSONObject outfit = (JSONObject) outfitObject;

                String outfitName = outfit.get("Name").toString();
                //String figureString = outfit.get("FigureString").toString();
                //String gender = outfit.get("Gender").toString();

                //outlookList.getItems().add(outfitName+"\t\t\t"+ figureString +"\t\t\t" + gender);
                outlookList.getItems().add(outfitName);
            }
        });
    }


    private void SetOutfit(JSONObject outfit){
        String figureString = outfit.get("FigureString").toString();
        String gender = outfit.get("Gender").toString();

        SetOutfit(figureString,gender);
    }

    private void SetOutfit(String figureString, String gender){
        sendToServer(new HPacket("UpdateFigureData", HMessage.Direction.TOSERVER, gender, figureString));
    }

    private Image getImageByFigureString(String figureString){
        return new Image(baseUrl+figureString);
    }

    private Image getImageByOutfitObject(JSONObject outfit){
        return getImageByFigureString(outfit.get("FigureString").toString());
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
        JSONObject selectedOutfit = GetSelectedOutfit(null);

        if(selectedOutfit.isEmpty() || selectedOutfit == null)
            return;

        SetOutfit(selectedOutfit);
    }

    public void addCustomFigure(ActionEvent actionEvent) {

        String name = customOutfitName.getText();
        String figureString = customFigureText.getText();
        String gender = genderChoiceBox.getSelectionModel().getSelectedItem().toString();

        addOutfit(name, figureString, gender);
    }

    public void TurnOffGeneral(Event event) {
        if(!wardrobeToggleButton.isSelected())
            return;
        wardrobeToggleButton.setSelected(false);
        toggleWardrobeButton(null);
    }

    public void deleteSelected(ActionEvent actionEvent) {
        JSONObject toRemove = GetSelectedOutfit(null);

        if(toRemove==null || toRemove.isEmpty())
            return;


        deleteJSONObject(toRemove);

        Platform.runLater(()->{
            wardrobeImage.setImage(new Image("defaultImage.png"));
        });
        System.out.println(currentOutfits.size());
    }


    /////////////////////////////////////////////////////////////////////////////////////////////////

    public void addSelectedHabboOutfit(ActionEvent actionEvent) {
        if(selectedIndex == -1) {
            System.out.println("[x] - selectedIndex is unvalid");
            return;
        }

        String outfitName = otherOutfitName.getText();
        String figureString = userFigures.get(selectedIndex);
        String gender = userGenders.get(selectedIndex).toString();

        if(wearChk.isSelected())
            SetOutfit(figureString, gender);

        addOutfit(outfitName,figureString, gender);
    }


    public JSONObject GetSelectedOutfit(MouseEvent mouseEvent) {
        if(outlookList.getItems().size() <= 0 || currentOutfits.size() <= 0)
            return null;

        int index = outlookList.getSelectionModel().getSelectedIndex();
        JSONObject selectedOutfit = (JSONObject) currentOutfits.get(index);

        Platform.runLater(()->{
            wardrobeImage.setImage(getImageByOutfitObject(selectedOutfit));
        });
        return selectedOutfit;
    }

    public void changeCustomFigure(KeyEvent keyEvent) {
        if(customFigureText.getText().isEmpty()){
            customFigureImage.imageProperty().set(defaultImage);
        }
        Platform.runLater(()->{
            customFigureImage.imageProperty().set(getImageByFigureString(customFigureText.getText()));

            if(customFigureText.getText().equals("")){
                customFigureText.styleProperty().set("-fx-background-color: #FFAAAA; -fx-border-color: #000000");
            }else {
                customFigureText.styleProperty().set("-fx-background-color: #AAFFAA; -fx-border-color: #000000");
            }
        });

    }

    public void turnOffHabboSelect(Event event) {
        Platform.runLater(()->{
            otherHabboButton.styleProperty().set("-fx-background-color: #FFAAAA; -fx-border-color: #000000; -fx-border-radius:5");
            otherHabboButton.setDisable(true);
            otherHabboButton.textProperty().set("Click on a user");
        });
    }

    public void toggleAOT(ActionEvent actionEvent) {
        primaryStage.setAlwaysOnTop(aotChk.isSelected());
    }
}
