package com.example.backend.sqlserver1.model;

import jakarta.persistence.*;

@Entity
@Table(name = "ENT")
public class Ent {

    @Id
    @Column(name = "ENTCOD", nullable = false)
    private int ENTCOD;
    @Column(name = "ENTNOM", nullable = false)
    private String ENTNOM;
    @Column(name = "ENTNIF", nullable = false)
    private String ENTNIF;

    public Ent() {}

    public int getENTCOD() {return ENTCOD;}
    public void setENTCOD(int ENTCOD) {this.ENTCOD = ENTCOD;}

    public String getENTNOM() {return ENTNOM;}
    public void setENTNOM(String ENTNOM) {this.ENTNOM = ENTNOM;}

    public String getENTNIF() {return ENTNIF;}
    public void setENTNIF(String ENTNIF) {this.ENTNIF = ENTNIF;}
}
