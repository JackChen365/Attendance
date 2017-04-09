package quant.attendance.model;

import java.util.ArrayList;

/**
 * Created by cz on 4/23/16.
 */
public class Employee {
    public int id;
    public int index;
    public String department;
    public String name;
    public int startHour;
    public int startMiunte;
    public int endHour;
    public int endMiunte;
    public final ArrayList<Integer> weekDays;//1,3,5
    public int weekStart;//1-3
    public int weekEnd;

    public Employee() {
        weekDays = new ArrayList<>();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Employee)) {
            return false;
        }
        Employee e = (Employee) obj;
        return name.equals(e.name);
    }
}
