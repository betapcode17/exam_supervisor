package model.bean;

import java.io.Serializable;

public class RoomHistory implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Integer id;
    private String maGV;
    private String phongThi;
    private Integer shift;

    public RoomHistory() {
    }

    public RoomHistory(String maGV, String phongThi, Integer shift) {
        this.maGV = maGV;
        this.phongThi = phongThi;
        this.shift = shift;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMaGV() {
        return maGV;
    }

    public void setMaGV(String maGV) {
        this.maGV = maGV;
    }

    public String getPhongThi() {
        return phongThi;
    }

    public void setPhongThi(String phongThi) {
        this.phongThi = phongThi;
    }

    public Integer getShift() {
        return shift;
    }

    public void setShift(Integer shift) {
        this.shift = shift;
    }

    @Override
    public String toString() {
        return "RoomHistory{" +
                "id=" + id +
                ", maGV='" + maGV + '\'' +
                ", phongThi='" + phongThi + '\'' +
                ", shift=" + shift +
                '}';
    }
}
