package model.bean;

import java.io.Serializable;

public class Assignment implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Integer id;
    private Integer shift;
    private String phongThi;
    private String maGV1;
    private String maGV2;
    // optional: keep source row identifiers to preserve uniqueness (tt from Excel)
    private Integer tt1;
    private Integer tt2;

    public Assignment() {
    }

    public Assignment(Integer shift, String phongThi, String maGV1, String maGV2) {
        this.shift = shift;
        this.phongThi = phongThi;
        this.maGV1 = maGV1;
        this.maGV2 = maGV2;
    }

    public Assignment(Integer shift, String phongThi, String maGV1, String maGV2, Integer tt1, Integer tt2) {
        this.shift = shift;
        this.phongThi = phongThi;
        this.maGV1 = maGV1;
        this.maGV2 = maGV2;
        this.tt1 = tt1;
        this.tt2 = tt2;
    }

    public Integer getTt1() {
        return tt1;
    }

    public void setTt1(Integer tt1) {
        this.tt1 = tt1;
    }

    public Integer getTt2() {
        return tt2;
    }

    public void setTt2(Integer tt2) {
        this.tt2 = tt2;
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

    public String getPhongThi() {
        return phongThi;
    }

    public void setPhongThi(String phongThi) {
        this.phongThi = phongThi;
    }

    public String getMaGV1() {
        return maGV1;
    }

    public void setMaGV1(String maGV1) {
        this.maGV1 = maGV1;
    }

    public String getMaGV2() {
        return maGV2;
    }

    public void setMaGV2(String maGV2) {
        this.maGV2 = maGV2;
    }

    @Override
    public String toString() {
        return "Assignment{" +
                "id=" + id +
                ", shift=" + shift +
                ", phongThi='" + phongThi + '\'' +
                ", maGV1='" + maGV1 + '\'' +
                ", maGV2='" + maGV2 + '\'' +
                '}';
    }
}
