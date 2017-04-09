package quant.attendance.widget.drag;

import com.jfoenix.controls.JFXTextField;
import javafx.event.EventHandler;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;

public class DragOverEvent implements EventHandler<DragEvent> {

    private JFXTextField textField;

    public DragOverEvent(JFXTextField textField){
        this.textField = textField;
    }

    public void handle(DragEvent event) {
        if (event.getGestureSource() != textField){
            event.acceptTransferModes(TransferMode.ANY);
        }
    }
}
