package gui.controller;

import gui.SceneManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.util.ResourceBundle;

public class MenuController implements Initializable {
    @FXML
    private GridPane gridPane;
    private Node documentView, employeeView, currentScene;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        FXMLLoader documentLoader = new FXMLLoader(getClass().getResource(SceneManager.DOCUMENT_SCENE));
        FXMLLoader employeeLoader = new FXMLLoader(getClass().getResource(SceneManager.EMPLOYEE_SCENE));
        try {
            documentView = documentLoader.load();
            employeeView = employeeLoader.load();
            currentScene = employeeView;
            gridPane.add(currentScene, 2, 0, 1,gridPane.getRowCount());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void switchScene(Node scene) {
       if (currentScene != scene) {
              gridPane.getChildren().remove(currentScene);
              gridPane.add(scene, 2, 0, 1, gridPane.getRowCount());
              currentScene = scene;
       }
    }

    public void btnDocumentAction() {
        switchScene(documentView);
    }

    public void btnEmployeesAction() {
        switchScene(employeeView);
    }
}
