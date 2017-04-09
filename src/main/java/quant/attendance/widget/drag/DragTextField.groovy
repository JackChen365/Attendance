package quant.attendance.widget.drag

import com.jfoenix.controls.JFXTextField

/**
 * Created by cz on 2017/2/24.
 */
class DragTextField extends JFXTextField{
    def closure
    public DragTextField() {
        setOnDragOver(new DragOverEvent(this));
        setOnDragDropped(new DragDroppedEvent(this));
    }

    public DragTextField(String text) {
        super(text);
        setOnDragOver(new DragOverEvent(this));
        setOnDragDropped(new DragDroppedEvent(this));
    }

    def setDragListener(closure){
        this.closure=closure
    }
}
