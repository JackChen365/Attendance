package quant.attendance.widget;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.InputEvent;
import javafx.util.StringConverter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeSpinner extends Spinner<LocalTime> {
    // Mode represents the unit that is currently being edited.
    // For convenience expose methods for incrementing and decrementing that
    // unit, and for selecting the appropriate portion in a spinner's editor
    enum Mode {
        HOURS {
           @Override
           LocalTime increment(LocalTime time, int steps) {
               return time.plusHours(steps);
           }
           @Override
           void select(TimeSpinner spinner) {
               int index = spinner.getEditor().getText().indexOf(':');
               spinner.getEditor().selectRange(0, index);
           }
        },
        MINUTES {
            @Override
            LocalTime increment(LocalTime time, int steps) {
                return time.plusMinutes(steps);
            }
            @Override
            void select(TimeSpinner spinner) {
                int hrIndex = spinner.getEditor().getText().indexOf(':');
                int minIndex = spinner.getEditor().getText().indexOf(':', hrIndex + 1);
                spinner.getEditor().selectRange(hrIndex+1, minIndex);
            }
        },
        SECONDS {
            @Override
            LocalTime increment(LocalTime time, int steps) {
                return time.plusSeconds(steps);
            }
            @Override
            void select(TimeSpinner spinner) {
                int index = spinner.getEditor().getText().lastIndexOf(':');
                spinner.getEditor().selectRange(index+1, spinner.getEditor().getText().length());
            }
        };
        abstract LocalTime increment(LocalTime time, int steps);
        abstract void select(TimeSpinner spinner);
        LocalTime decrement(LocalTime time, int steps) {
            return increment(time, -steps);
        }
    }

    // Property containing the current editing mode:

    private final ObjectProperty<Mode> mode = new SimpleObjectProperty<>(Mode.HOURS) ;

    public ObjectProperty<Mode> modeProperty() {
        return mode;
    }

    public final Mode getMode() {
        return modeProperty().get();
    }

    public final void setMode(Mode mode) {
        modeProperty().set(mode);
    }


    public TimeSpinner(LocalTime time) {
        setEditable(true);
        // Create a StringConverter for converting between the text in the
        // editor and the actual value:

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        StringConverter<LocalTime> localTimeConverter = new StringConverter<LocalTime>() {

            @Override
            public String toString(LocalTime time) {
                return formatter.format(time);
            }

            @Override
            public LocalTime fromString(String string) {
                String[] tokens = string.split(":");
                int hours = getIntField(tokens, 0);
                int minutes = getIntField(tokens, 1) ;
                int seconds = getIntField(tokens, 2);
                int totalSeconds = (hours * 60 + minutes) * 60 + seconds ;
                return LocalTime.of((totalSeconds / 3600) % 24, (totalSeconds / 60) % 60, seconds % 60);
            }

            private int getIntField(String[] tokens, int index) {
                if (tokens.length <= index || tokens[index].isEmpty()) {
                    return 0 ;
                }
                return Integer.parseInt(tokens[index]);
            }

        };

        // The textFormatter both manages the text <-> LocalTime conversion,
        // and vetoes any edits that are not valid. We just make sure we have
        // two colons and only digits in between:

        TextFormatter<LocalTime> textFormatter = new TextFormatter<>(localTimeConverter, LocalTime.now(), c -> {
            String newText = c.getControlNewText();
            if (newText.matches("[0-9]{0,2}:[0-9]{0,2}:[0-9]{0,2}")) {
                return c ;
            }
            return null ;
        });

        // The spinner value factory defines increment and decrement by
        // delegating to the current editing mode:

        SpinnerValueFactory<LocalTime> valueFactory = new SpinnerValueFactory<LocalTime>() {
            {

                setConverter(localTimeConverter);
                setValue(time);
            }

            @Override
            public void decrement(int steps) {
                setValue(mode.get().decrement(getValue(), steps));
                mode.get().select(TimeSpinner.this);
            }

            @Override
            public void increment(int steps) {
                setValue(mode.get().increment(getValue(), steps));
                mode.get().select(TimeSpinner.this);
            }

        };

        this.setValueFactory(valueFactory);
        this.getEditor().setTextFormatter(textFormatter);

        // Update the mode when the user interacts with the editor.
        // This is a bit of a hack, e.g. calling spinner.getEditor().positionCaret()
        // could result in incorrect state. Directly observing the caretPostion
        // didn't work well though; getting that to work properly might be
        // a better approach in the long run.
        this.getEditor().addEventHandler(InputEvent.ANY, e -> {
            int caretPos = this.getEditor().getCaretPosition();
            int hrIndex = this.getEditor().getText().indexOf(':');
            int minIndex = this.getEditor().getText().indexOf(':', hrIndex + 1);
            if (caretPos <= hrIndex) {
                mode.set( Mode.HOURS );
            } else if (caretPos <= minIndex) {
                mode.set( Mode.MINUTES );
            } else {
                mode.set( Mode.SECONDS );
            }
        });

        // When the mode changes, select the new portion:
        mode.addListener((obs, oldMode, newMode) -> newMode.select(this));

    }

    public void setEditError(boolean error){
        TextField editor = getEditor();
        String textColor=error?"#ff0000":"#000000";
        String borderColor=error?"#ff0000":"#BCBCBC";
        editor.setStyle("-fx-text-fill: "+textColor+";-fx-border-color: "+borderColor+";-fx-border-width: 1;");
    }


    public TimeSpinner() {
        this(LocalTime.now());
    }

    public String getText(){
        return getEditor().getText();
    }

    public LocalTime getLocalTime(){
        LocalTime localTime=null;
        Pattern pattern = Pattern.compile("(\\d{1,2}):(\\d{1,2}):(\\d{1,2})");
        Matcher matcher = pattern.matcher(getEditor().getText());
        if(matcher.find()) {
            localTime = LocalTime.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
        }
        return localTime;
    }

    public boolean validatorNumber(int value,int start,int end){
        return start<=value&&value<end;
    }

    public long getTimeMillis(){
        int intValue=-1;
        Pattern pattern = Pattern.compile("(\\d{1,2}):(\\d{1,2}):(\\d{1,2})");
        Matcher matcher = pattern.matcher(getEditor().getText());
        if(matcher.find()) {
            int hour=Integer.parseInt(matcher.group(1));
            int minute=Integer.parseInt(matcher.group(2));
            int second=Integer.parseInt(matcher.group(3));
            if(validatorNumber(hour,0,24)&&validatorNumber(minute,0,60)&&validatorNumber(second,0,60)){
                LocalTime localTime = LocalTime.of(hour,minute,second);
                intValue=localTime.toSecondOfDay()*1000;
            }
        }
        return intValue;
    }
}