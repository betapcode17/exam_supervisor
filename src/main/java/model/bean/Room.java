package model.bean;

import java.io.Serializable;

public class Room implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer stt;

    private String phongThi;

    private String diaDiem;

    public Room() {
    }

    public Room(Integer stt, String phongThi, String diaDiem) {

        this.stt = stt;

        this.phongThi = phongThi;

        this.diaDiem = diaDiem;
    }

    public Room(String phongThi, String diaDiem) {

        this(null, phongThi, diaDiem);
    }

    public Integer getStt() {
        return stt;
    }

    public void setStt(Integer stt) {
        this.stt = stt;
    }

    public String getPhongThi() {
        return phongThi;
    }

    public void setPhongThi(String phongThi) {
        this.phongThi = phongThi;
    }

    public String getDiaDiem() {
        return diaDiem;
    }

    public void setDiaDiem(String diaDiem) {
        this.diaDiem = diaDiem;
    }

    @Override
    public String toString() {

        return "Room{" +
                "stt=" + stt +
                ", phongThi='" + phongThi + '\'' +
                ", diaDiem='" + diaDiem + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof Room)) {
            return false;
        }

        Room other = (Room) obj;

        return this.stt != null
                && this.stt.equals(other.stt);
    }

    @Override
    public int hashCode() {

        return stt != null
                ? stt.hashCode()
                : 0;
    }
}