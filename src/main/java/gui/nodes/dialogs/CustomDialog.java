package gui.nodes.dialogs;

import io.github.palexdev.materialfx.dialogs.MFXGenericDialog;
import io.github.palexdev.materialfx.dialogs.MFXGenericDialogBuilder;
import io.github.palexdev.materialfx.dialogs.MFXStageDialog;
import javafx.scene.Node;

/**
 * A custom dialog that can be used to create custom dialogs.
 * It is a wrapper for the way MaterialFX dialogs are created.
 */
public abstract class CustomDialog extends MFXStageDialog {
    private MFXGenericDialog dialogContent;

    public CustomDialog() {
        this("", "");
    };

    public CustomDialog(String title, String content) {
        super();
        dialogContent = MFXGenericDialogBuilder.build()
                .setOnMinimize(event -> this.setIconified(true))
                .setOnAlwaysOnTop(event -> this.setAlwaysOnTop(true))
                .setOnClose(event -> this.close())
                .setHeaderText(title)
                .setContentText(content)
                .makeScrollable(true)
                .get();
        dialogContent.getStylesheets().add("/css/Dialogs.css");
    }

    public abstract void clear();

    public void setContentText(String contentText) {
        dialogContent.setContentText(contentText);
    };

    public void setTitleText(String title) {
        dialogContent.setHeaderText(title);
    }

    public void setContent(Node content) {
        dialogContent.setContent(content);
    }

    protected MFXGenericDialog getDialogContent() {
        return dialogContent;
    }
}
