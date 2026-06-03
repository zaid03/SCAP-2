package com.example.backend.sqlserver2.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@IdClass(MatId.class)
@Table(name = "MAT", schema = "dbo")
public class Mat {
    @Id
    private Integer ENT;

    @Id
    private Integer MAGCOD;

    @Id
    private Integer MTACOD;

    public Integer getENT() {return ENT;}
    public void setENT(Integer ENT) {this.ENT = ENT;}

    public Integer getMAGCOD() {return MAGCOD;}
    public void setMAGCOD(Integer MAGCOD) {this.MAGCOD = MAGCOD;}

    public Integer getMTACOD() {return MTACOD;}
    public void setMTACOD(Integer MTACOD) {this.MTACOD = MTACOD;}
}
