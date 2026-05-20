package model.bean;

import java.io.Serializable;
import java.util.Date;

public class Invigilator implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Integer tt;
    private String maGV;
    private String hoTen;
    private Date ngaySinh;
    private String donViCongTac;

    public Invigilator() {
    }

    public Invigilator(Integer tt, String maGV, String hoTen, Date ngaySinh, String donViCongTac) {
        this.tt = tt;
        this.maGV = maGV;
        this.hoTen = hoTen;
        this.ngaySinh = ngaySinh;
        this.donViCongTac = donViCongTac;
    }

    public Invigilator(String maGV, String hoTen, Date ngaySinh, String donViCongTac) {
        this(null, maGV, hoTen, ngaySinh, donViCongTac);
    }

    public Integer getTt() {
        return tt;
    }

    public void setTt(Integer tt) {
        this.tt = tt;
    }

    public String getMaGV() {
        return maGV;
    }

    public void setMaGV(String maGV) {
        this.maGV = maGV;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public Date getNgaySinh() {
        return ngaySinh;
    }

    public void setNgaySinh(Date ngaySinh) {
        this.ngaySinh = ngaySinh;
    }

    public String getDonViCongTac() {
        return donViCongTac;
    }

    public void setDonViCongTac(String donViCongTac) {
        this.donViCongTac = donViCongTac;
    }

    @Override
    public String toString() {
        return "Invigilator{" +
                "tt=" + tt +
                ", maGV='" + maGV + '\'' +
                ", hoTen='" + hoTen + '\'' +
                ", ngaySinh=" + ngaySinh +
                ", donViCongTac='" + donViCongTac + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Invigilator)) return false;
        Invigilator other = (Invigilator) obj;
        return this.tt != null && this.tt.equals(other.tt);
    }

    @Override
    public int hashCode() {
        return tt != null ? tt.hashCode() : 0;
    }
}
