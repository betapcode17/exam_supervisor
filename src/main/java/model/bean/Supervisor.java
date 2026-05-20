package model.bean;

import java.io.Serializable;

public class Supervisor implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Integer id;
    private Integer shift;
    private String maGV;
    private String fromRoom;
    private String toRoom;

    public Supervisor() {
    }

    public Supervisor(Integer shift, String maGV, String fromRoom, String toRoom) {
        this.shift = shift;
        this.maGV = maGV;
        this.fromRoom = fromRoom;
        this.toRoom = toRoom;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getShift() {
        return shift;
    }

    public void setShift(Integer shift) {
        this.shift = shift;
    }

    public String getMaGV() {
        return maGV;
    }

    public void setMaGV(String maGV) {
        this.maGV = maGV;
    }

    public String getFromRoom() {
        return fromRoom;
    }

    public void setFromRoom(String fromRoom) {
        this.fromRoom = fromRoom;
    }

    public String getToRoom() {
        return toRoom;
    }

    public void setToRoom(String toRoom) {
        this.toRoom = toRoom;
    }

    @Override
    public String toString() {
        return "Supervisor{" +
                "id=" + id +
                ", shift=" + shift +
                ", maGV='" + maGV + '\'' +
                ", fromRoom='" + fromRoom + '\'' +
                ", toRoom='" + toRoom + '\'' +
                '}';
    }
}
