package quant.attendance.widget.drag

import javafx.event.EventHandler
import javafx.scene.input.DragEvent
import javafx.scene.input.Dragboard
/**
 * Created by cz on 2017/2/24.
 */
class DragDroppedEvent implements EventHandler<DragEvent> {
    private final DragTextField textField;

    public DragDroppedEvent(DragTextField textField){
        this.textField = textField;
    }

    public void handle(DragEvent event) {
        Dragboard dragBoard = event.getDragboard();
        if (dragBoard&&dragBoard.hasFiles()){
            try {
                File file = dragBoard.files[0];
                if (file) {
                    textField.setText(file.getAbsolutePath())
                    textField?.closure(file)
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
