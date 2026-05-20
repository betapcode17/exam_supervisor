package model.bean;

import java.io.Serializable;
import java.util.List;

public class ScheduleInput implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private List<Invigilator> invigilators;
    private List<Room> rooms;
    private Integer numberOfRooms;
    private Integer numberOfInvigilators;
    private Integer shift;

    public ScheduleInput() {
    }

    public ScheduleInput(List<Invigilator> invigilators, List<Room> rooms, 
                         Integer numberOfRooms, Integer numberOfInvigilators, Integer shift) {
        this.invigilators = invigilators;
        this.rooms = rooms;
        this.numberOfRooms = numberOfRooms;
        this.numberOfInvigilators = numberOfInvigilators;
        this.shift = shift;
    }

    public List<Invigilator> getInvigilators() {
        return invigilators;
    }

    public void setInvigilators(List<Invigilator> invigilators) {
        this.invigilators = invigilators;
    }

    public List<Room> getRooms() {
        return rooms;
    }

    public void setRooms(List<Room> rooms) {
        this.rooms = rooms;
    }

    public Integer getNumberOfRooms() {
        return numberOfRooms;
    }

    public void setNumberOfRooms(Integer numberOfRooms) {
        this.numberOfRooms = numberOfRooms;
    }

    public Integer getNumberOfInvigilators() {
        return numberOfInvigilators;
    }

    public void setNumberOfInvigilators(Integer numberOfInvigilators) {
        this.numberOfInvigilators = numberOfInvigilators;
    }

    public Integer getShift() {
        return shift;
    }

    public void setShift(Integer shift) {
        this.shift = shift;
    }

    @Override
    public String toString() {
        return "ScheduleInput{" +
                "invigilators=" + invigilators.size() +
                ", rooms=" + rooms.size() +
                ", numberOfRooms=" + numberOfRooms +
                ", numberOfInvigilators=" + numberOfInvigilators +
                ", shift=" + shift +
                '}';
    }
}
