import gearth.extensions.ExtensionForm;
import gearth.extensions.ExtensionFormCreator;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;


public class ExtensionLauncher extends ExtensionFormCreator {

    public ExtensionForm createForm(Stage primaryStage) throws Exception {

        FXMLLoader loader = new FXMLLoader(getClass().getResource("mainform.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("schweppes0x");
        primaryStage.setScene(new Scene(root));

        primaryStage.getIcons().add(new Image(Objects.requireNonNull(this.getClass().getResource("icon.png")).openStream()));

        primaryStage.setResizable(false);

        return loader.getController();

    }

    public static void main(String[] args) {
        runExtensionForm(args, ExtensionLauncher.class);
    }

}


