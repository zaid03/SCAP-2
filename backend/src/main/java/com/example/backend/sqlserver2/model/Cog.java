package com.example.backend.sqlserver2.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;

@Entity
@IdClass(CogId.class)
@Table(name = "COG", schema = "dbo")
public class Cog {
    @Id
    private Integer ENT;

    @Id
    private String EJE;

    @Id
    private Integer CONCOD;

    @Id
    private String CGECOD;

    private Double COGIMP;

    private String COGOPD;

    private Double COGAIP;

    @ManyToOne
    @JoinColumns({
        @JoinColumn(name = "ENT", referencedColumnName = "ENT", insertable = false, updatable = false),
        @JoinColumn(name = "EJE", referencedColumnName = "EJE", insertable = false, updatable = false),
        @JoinColumn(name = "CGECOD", referencedColumnName = "CGECOD", insertable = false, updatable = false)
    })
    private Cge cge;

    public Cge getCge() { return cge; }
    
    public Integer getENT() {return ENT;}
    public void setENT(Integer ENT) {this.ENT = ENT;}

    public String getEJE() {return EJE;}
    public void setEJE(String EJE) {this.EJE = EJE;}

    public Integer getCONCOD() {return CONCOD;}
    public void setCONCOD(Integer CONCOD) {this.CONCOD = CONCOD;}

    public String getCGECOD() {return CGECOD;}
    public void setCGECOD(String CGECOD) {this.CGECOD = CGECOD;}

    public Double getCOGIMP() {return COGIMP;}
    public void setCOGIMP(Double COGIMP) {this.COGIMP = COGIMP;}

    public String getCOGOPD() {return COGOPD;}
    public void setCOGOPD(String COGOPD) {this.COGOPD = COGOPD;}

    public Double getCOGAIP() {return COGAIP;}
    public void setCOGAIP(Double COGAIP) {this.COGAIP = COGAIP;}
}
