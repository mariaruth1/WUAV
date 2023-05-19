package gui.nodes.dialogs;

import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialogBuilder;
import io.github.palexdev.materialfx.dialogs.MFXStageDialog;
import javafx.scene.Node;
import javafx.scene.control.TextArea;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * A dialogue that allows the user to input text
 * with the initial max length of 256 characters.
 */
public class TextInputDialog extends CustomDialog {
    private TextArea textArea;
    private CompletableFuture<String> result = new CompletableFuture<>();

    public TextInputDialog() {
        this("", "");
    }

    public TextInputDialog(String title, String content) {
        super();
        textArea = new TextArea();
        //textArea.setMaxTextLength(256);
        //TODO add max length

        super.setContent(textArea);
        this.setContent(super.getDialogContent());
        super.getDialogContent().addActions(
                Map.entry(new MFXButton("Confirm"), event -> {
                    result.complete(textArea.getText());
                    this.close();
                }),
                Map.entry(new MFXButton("Cancel"), event -> this.close())
        );
    }

   /* public void setMaxTextLength(int length) {
        textArea.setMaxTextLength(length);
    }*/


    public void setEditorText(String text) {
        textArea.setText(text);
    }

    public String getEditorText() {
        return textArea.getText();
    }

    public CompletableFuture<String> showAndReturnResult() {
        CompletableFuture<String> result = new CompletableFuture<>();
        this.show();
        return result;
    }

    public void setEditable(boolean isEditable) {
        textArea.setEditable(isEditable);
    }

    public void clear() {
        textArea.clear();
    }

    public void setContentText(String text) {
        textArea.setText(text);
    }

    public CompletableFuture<String> getResult() {
        return result;
    }
}
