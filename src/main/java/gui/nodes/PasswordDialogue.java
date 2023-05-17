package gui.nodes;

import be.User;
import gui.model.UserModel;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXPasswordField;
import io.github.palexdev.materialfx.controls.MFXTextField;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialogBuilder;
import io.github.palexdev.materialfx.dialogs.MFXStageDialog;
import io.github.palexdev.materialfx.dialogs.MFXStageDialogBuilder;
import io.github.palexdev.materialfx.enums.ScrimPriority;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Window;
import utils.HashPasswordHelper;
import utils.enums.UserRole;

import java.util.HashMap;
import java.util.Map;

public class PasswordDialogue extends MFXStageDialog {
    //TODO generify this class
    private GridPane passwordGridPane;
    private MFXGenericDialog dialogContent;
    private HashMap<PasswordType, MFXPasswordField> passwordFields;
    private MFXButton btnConfirm, btnCancel;
    private byte[] newPassword, newSalt;
    private int row = 0;
    private boolean passwordChanged;
    private HashPasswordHelper hashPasswordHelper;

    private User userToUpdate;
    private Window owner;
    private Pane ownerNode;

    enum PasswordType {
        OLD("Current password"),
        NEW("New password"),
        CONFIRM("Confirm password");

        private String text;
        PasswordType(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }

    public PasswordDialogue(Window owner, Pane ownerNode, User userToUpdate) {
        super();
        this.owner = owner;
        this.ownerNode = ownerNode;
        this.userToUpdate = userToUpdate;
        hashPasswordHelper = new HashPasswordHelper();
        passwordFields = new HashMap<>();

        setUpGridPane();
        dialogContent = MFXGenericDialogBuilder.build()
                .setContent(passwordGridPane)
                .get();
        setUpDialogueWindow();
        addButtons();
    }

    private void setUpDialogueWindow() {
        super.setContent(dialogContent);
        super.initOwner(owner);
        super.initModality(Modality.APPLICATION_MODAL);
        super.setDraggable(true);
        super.setTitle("Change password");
        super.setOwnerNode(ownerNode);
        super.setScrimPriority(ScrimPriority.WINDOW);
        super.setScrimOwner(true);
    }

    private void addButtons() {
        btnConfirm = new MFXButton("Confirm");
        btnConfirm.setDisable(true);
        btnCancel = new MFXButton("Cancel");
        dialogContent.addActions(
                Map.entry(btnConfirm, event -> {
                    passwordChanged = true;
                    byte[][] passwordHashAndSalt = hashPasswordHelper.hashPassword(passwordFields.get(PasswordType.NEW).getText());
                    newPassword = passwordHashAndSalt[0];
                    newSalt = passwordHashAndSalt[1];
                    this.close();
                }),
                Map.entry(btnCancel, event -> this.close())
        );
    }

    private void setUpGridPane() {
        passwordGridPane = new GridPane();
        passwordGridPane.setHgap(10);
        passwordGridPane.setVgap(10);

        // If the user is not an admin, add the current password field, otherwise start at the new password field
        int startIndex = userToUpdate == UserModel.getLoggedInUser() ? 0 : 1;
        for (int i = startIndex; i < PasswordType.values().length; i++) {
            PasswordType passwordType = PasswordType.values()[i];
            MFXPasswordField textField = new MFXPasswordField();
            textField.setFloatingText(passwordType.getText());
            textField.maxWidthProperty().bind(passwordGridPane.widthProperty().subtract(20));
            textField.textProperty().addListener(passwordListener);
            passwordFields.put(passwordType, textField);
            passwordGridPane.add(textField, 1, row++);
            GridPane.setHalignment(textField, javafx.geometry.HPos.CENTER);
        }
    }

    // Add a listener to the text fields to check if the passwords match
    private final ChangeListener<String> passwordListener = (observable, oldValue, newValue) -> {
        if (passwordFields.get(PasswordType.NEW).getText().isEmpty() || passwordFields.get(PasswordType.CONFIRM).getText().isEmpty()) {
            btnConfirm.setDisable(true);
            System.out.println("Password fields are empty");
            return;
        }

        // If the new password and confirm password fields match, check if the old password field matches the current password
        if (passwordFields.get(PasswordType.NEW).getText().equals(passwordFields.get(PasswordType.CONFIRM).getText())) {
            if (UserModel.getLoggedInUser().getUserRole() != UserRole.ADMINISTRATOR) {
                if (hashPasswordHelper.hashPassword(passwordFields.get(PasswordType.OLD).getText(), userToUpdate.getSalt()) != userToUpdate.getPassword()) {
                    btnConfirm.setDisable(true);
                } else {
                    btnConfirm.setDisable(false);
                }
            } else {
                btnConfirm.setDisable(false);
            }
        }
    };

    public byte[] getNewPassword() {
        return newPassword;
    }

    public byte[] getNewSalt() {
        return newSalt;
    }

    public boolean isPasswordChanged() {
        return passwordChanged;
    }
}