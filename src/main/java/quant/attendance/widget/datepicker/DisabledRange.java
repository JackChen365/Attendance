package quant.attendance.widget.datepicker;

import java.time.LocalDate;

class DisabledRange {

    private final LocalDate initialDate;
    private final LocalDate endDate;

    public DisabledRange(LocalDate initialDate, LocalDate endDate){
        this.initialDate=initialDate;
        this.endDate = endDate;
    }

    public LocalDate getInitialDate() { return initialDate; }
    public LocalDate getEndDate() { return endDate; }

}