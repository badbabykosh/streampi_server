package StreamPiServer;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Random;
import java.util.ResourceBundle;

public class launchAppConfig extends Application implements Initializable {
    @FXML
    private Label headingLabel;

    @FXML
    private JFXTextField actionNameField;

    @FXML
    private JFXTextField appPathField;

    @FXML
    private JFXTextField iconPathField;

    @FXML
    private ImageView iconPreviewImg;

    @FXML
    private JFXButton deleteButton;

    @FXML
    private JFXButton addButton;


    boolean isImageFileOK = false;

    Image previewImageDefault = new Image(getClass().getResourceAsStream("icons/icon_preview.png"));
    File selectedIconFile;
    File selectedAppFile;
    Image previewIcon;

    @Override
    public void start(Stage primaryStage) throws Exception {

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if(dashboardController.actionConfigType == 2)
        {
            deleteButton.setDisable(false);
            deleteButton.setVisible(true);
            isImageFileOK = true;
            addButton.setText("Apply Changes");
            headingLabel.setText("Modify Application Launcher Action");
            for(int i = 0;i<dashboardController.actions.length;i++)
            {
                String[] eachAction = dashboardController.actions[i];
                System.out.println(eachAction[0]+", "+dashboardController.selectedActionUniqueID);
                if(eachAction[0].equals(dashboardController.selectedActionUniqueID))
                {
                    actionNameField.setText(eachAction[1]);
                    appPathField.setText(eachAction[3]);
                    Image icon = dashboardController.icons.get(eachAction[4]);
                    iconPreviewImg.setImage(icon);
                    break;
                }
            }
        }
    }

    @FXML
    void addButtonClicked(ActionEvent event) {
        StringBuilder errors = new StringBuilder("Please correct and resolve the following errors :\n");
        boolean isError = false;

        if(actionNameField.getText().length()==0)
        {
            errors.append("Invalid Action Name. Can't be left empty!\n");
            isError = true;
        }

        if(appPathField.getText().length() == 0)
        {
            errors.append("Invalid App Path Name. Can't be left empty!\n");
            isError = true;
        }

        if(!isImageFileOK)
        {
            errors.append("Invalid Action Icon\n");
            isError = true;
        }

        if(isError)
        {
            Main.dc.showErrorAlert("Error!",errors.toString());
            return;
        }

        //Good to go!

        new Thread(new Task<Void>() {
            @Override
            protected Void call() {
                try
                {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            Main.dc.showProgress("Updating actions... ");
                        }
                    });

                    if(dashboardController.actionConfigType == 1)
                    {
                        String newFileName = "icon_"+r.nextInt(30000);
                        File newFile = new File(selectedIconFile.getPath().replace(selectedIconFile.getName(),"")+"/"+newFileName);
                        Files.copy(selectedIconFile.toPath(), newFile.toPath());
                        //send icon to client ...
                        FileInputStream fs = new FileInputStream(newFile.getAbsolutePath());
                        byte[] imageB = fs.readAllBytes();
                        fs.close();
                        String base64EncryptedIcon = Base64.getEncoder().encodeToString(imageB);

                        String iconName = newFile.getName();

                        Main.dc.writeToOS("update_icon::"+iconName+"::"+base64EncryptedIcon+"::");
                        newFile.delete();

                        //first update local actions....
                        String[][] oldActions = new String[Main.dc.actions.length+1][8];

                        for(i = 0;i<Main.dc.actions.length;i++)
                        {
                            oldActions[i] = Main.dc.actions[i];
                        }

                        oldActions[i][0] = generateRandomID();
                        oldActions[i][1] = actionNameField.getText();
                        oldActions[i][2] = "launch_app";
                        String toWrite = r.nextInt((15000 - 1) + 1) + 1+"";
                        oldActions[i][3] = appPathField.getText();
                        //oldActions[i][4] = selectedIconFile.getName();
                        oldActions[i][4] = newFileName;
                        oldActions[i][5] = Main.dc.selectedRow+"";
                        oldActions[i][6] = Main.dc.selectedCol+"";
                        oldActions[i][7] = dashboardController.currentLayer+"";

                        dashboardController.maxLayers++;
                        Main.dc.actions = oldActions;


                        Main.dc.icons.put(oldActions[i][4],previewIcon);


                        ImageView icon = new ImageView();
                        icon.setImage(Main.dc.icons.get(Main.dc.actions[i][4]));
                        icon.setFitHeight(100);
                        icon.setFitWidth(100);


                        HBox row = (HBox) Main.dc.controlVBox.getChildren().get(Main.dc.selectedRow);

                        Pane ac = (Pane) row.getChildren().get(Main.dc.selectedCol);
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                ac.getStyleClass().remove("action_box_highlight");
                                ac.getChildren().add(icon);
                                //actionPane.setStyle("-fx-effect: dropshadow(three-pass-box, "+actions[i][4]+", 5, 0, 0, 0);-fx-background-color:#212121");
                                ac.setId("allocatedaction_"+oldActions[i][5]+"_"+oldActions[i][6]+"_"+oldActions[i][0]);
                            }
                        });

                    }
                    else if(dashboardController.actionConfigType == 2)
                    {
                        String newFileName = "icon_"+r.nextInt(30000);
                        if(iconPathField.getText().length()>0)
                        {
                            File newFile = new File(selectedIconFile.getPath().replace(selectedIconFile.getName(),"")+"/"+newFileName);
                            Files.copy(selectedIconFile.toPath(), newFile.toPath());
                            //send icon to client ...
                            FileInputStream fs = new FileInputStream(newFile.getAbsolutePath());
                            byte[] imageB = fs.readAllBytes();
                            String base64EncryptedIcon = Base64.getEncoder().encodeToString(imageB);

                            String iconName = newFile.getName();

                            Main.dc.writeToOS("update_icon::"+iconName+"::"+base64EncryptedIcon+"::");
                        }

                        //first update local actions....
                        String[][] oldActions = dashboardController.actions;

                        for(i = 0;i<dashboardController.actions.length;i++)
                        {
                            if(dashboardController.actions[i][0].equals(dashboardController.selectedActionUniqueID))
                            {
                                oldActions[i][0] = generateRandomID();
                                oldActions[i][1] = actionNameField.getText();
                                oldActions[i][2] = "launch_app";
                                oldActions[i][3] = appPathField.getText();
                                //oldActions[i][4] = selectedIconFile.getName();
                                if(iconPathField.getText().length()>0)
                                {
                                    oldActions[i][4] = newFileName;
                                }
                                oldActions[i][5] = dashboardController.selectedRow+"";
                                oldActions[i][6] = dashboardController.selectedCol+"";
                                oldActions[i][7] = dashboardController.currentLayer+"";
                                System.out.println(oldActions[i][7]+"XXASDSD");
                                System.out.println("YAAY");
                                break;
                            }
                        }

                        dashboardController.actions = oldActions;


                        if(iconPathField.getText().length()>0)
                        {
                            dashboardController.icons.put(oldActions[i][4],previewIcon);
                        }

                        ImageView icon = new ImageView();
                        icon.setImage(dashboardController.icons.get(dashboardController.actions[i][4]));
                        icon.setFitHeight(100);
                        icon.setFitWidth(100);


                        HBox row = (HBox) Main.dc.controlVBox.getChildren().get(dashboardController.selectedRow);

                        Pane ac = (Pane) row.getChildren().get(dashboardController.selectedCol);
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                ac.getStyleClass().remove("action_box_highlight");
                                ac.getChildren().clear();
                                ac.getChildren().add(icon);
                                //actionPane.setStyle("-fx-effect: dropshadow(three-pass-box, "+actions[i][4]+", 5, 0, 0, 0);-fx-background-color:#212121");
                                ac.setId("allocatedaction_"+oldActions[i][5]+"_"+oldActions[i][6]+"_"+oldActions[i][0]);
                            }
                        });


                    }

                    Thread.sleep(1000);

                    String towrite = "actions_update::"+dashboardController.actions.length+"::";

                    for(String[] eachAction : dashboardController.actions)
                    {
                        //FileInputStream fs = new FileInputStream("actions/icons/"+eachAction[3]);
                        //byte[] imageB = fs.readAllBytes();
                        //fs.close();
                        //String base64Image = Base64.getEncoder().encodeToString(imageB);
                        towrite+=eachAction[0]+"__"+eachAction[1]+"__"+eachAction[2]+"__"+eachAction[3]+"__"+eachAction[4]+"__"+eachAction[5]+"__"+eachAction[6]+"__"+eachAction[7]+"::";
                    }

                    System.out.println(towrite);
                    Main.dc.writeToOS(towrite);



                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            Main.dc.hideProgress();
                            Main.dc.hideNewActionConfigDialog();
                        }
                    });
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                return null;
            }
        }).start();
    }

    @FXML
    void appPathBrowseButtonClicked(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("EXE","*.exe"));
        selectedAppFile = fileChooser.showOpenDialog(Main.ps);

        appPathField.setText(selectedAppFile.getAbsolutePath());
        if(selectedAppFile == null)
        {
            appPathField.setText("");
        }
    }

    @FXML
    public void deleteButtonClicked()
    {
        new Thread(new Task<Void>() {
            @Override
            protected Void call() {
                try
                {
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            Main.dc.showProgress("Removing requested Action ");
                        }
                    });

                    for(String[] eachAction : dashboardController.actions)
                    {
                        if(eachAction[0].equals(dashboardController.selectedActionUniqueID))
                        {
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    HBox row = (HBox) Main.dc.controlVBox.getChildren().get(Integer.parseInt(eachAction[5]));
                                    Pane ac = (Pane) row.getChildren().get(Integer.parseInt(eachAction[6]));
                                    ac.getChildren().clear();
                                    ac.setId("freeAction_"+eachAction[5]+"_"+eachAction[6]);
                                    ac.getStyleClass().add("action_box");
                                }
                            });
                            System.out.println("Removed from Server...");
                            Main.dc.writeToOS("delete_action::"+dashboardController.selectedActionUniqueID+"::"+eachAction[4]);
                            System.out.println("Removed from Client!");
                            break;
                        }
                    }

                    int x2 = 0;
                    String[][] newActions = new String[dashboardController.actions.length-1][8];
                    for(int x1 = 0;x1<dashboardController.actions.length;x1++)
                    {
                        String[] eachAction = dashboardController.actions[x1];
                        if(eachAction[0].equals(dashboardController.selectedActionUniqueID))
                            continue;
                        else
                        {
                            newActions[x2][0] = eachAction[0];
                            newActions[x2][1] = eachAction[1];
                            newActions[x2][2] = eachAction[2];
                            newActions[x2][3] = eachAction[3];
                            newActions[x2][4] = eachAction[4];
                            newActions[x2][5] = eachAction[5];
                            newActions[x2][6] = eachAction[6];
                            newActions[x2][7] = eachAction[7];
                            x2++;
                        }
                    }

                    dashboardController.actions = newActions;


                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            Main.dc.hideProgress();
                            Main.dc.hideNewActionConfigDialog();
                        }
                    });
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
                return null;
            }
        }).start();
    }

    @FXML
    public void cancelButtonClicked()
    {
        Main.dc.newActionConfigDialog.close();
    }

    @FXML
    void iconPathBrowseButtonClicked(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("PNG","*.png"), new FileChooser.ExtensionFilter("GIF","*.gif"));
        selectedIconFile = fileChooser.showOpenDialog(Main.ps);

        if(selectedIconFile == null)
        {
            isImageFileOK = false;
            iconPathField.setText("");
            iconPreviewImg.setImage(previewImageDefault);
        }

        try
        {
            previewIcon = new Image(selectedIconFile.toURI().toString());
            iconPreviewImg.setImage(previewIcon);
            iconPathField.setText(selectedIconFile.getAbsolutePath());
            isImageFileOK = true;
        }
        catch (Exception e)
        {
            isImageFileOK = false;
            e.printStackTrace();
            Main.dc.showErrorAlert("Uh Oh!","It seems that the Icon you selected is invalid!");
            iconPreviewImg.setImage(previewImageDefault);
            iconPathField.setText("");
        }
    }

    @FXML
    void openElgatoStreamDeckKeyCreator(ActionEvent event) {
        getHostServices().showDocument("https://www.elgato.com/en/gaming/keycreator");
    }

    int i;
    Random r= new Random();

    public String generateRandomID() {
        return "action_"+r.nextInt((15000 - 1) + 1) + 1;
    }
}
