package model.bean;

import java.io.Serializable;

public class PairHistory implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Integer id;
    private String maGV1;
    private String maGV2;
    private Integer shift;

    public PairHistory() {
    }

    public PairHistory(String maGV1, String maGV2, Integer shift) {
        this.maGV1 = maGV1;
        this.maGV2 = maGV2;
        this.shift = shift;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public Integer getShift() {
        return shift;
    }

    public void setShift(Integer shift) {
        this.shift = shift;
    }

    @Override
    public String toString() {
        return "PairHistory{" +
                "id=" + id +
                ", maGV1='" + maGV1 + '\'' +
                ", maGV2='" + maGV2 + '\'' +
                ", shift=" + shift +
                '}';
    }
}
