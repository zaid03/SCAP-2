package com.example.backend.sqlserver2.model;

import java.io.Serializable;
import java.util.Objects;

public class CfgId implements Serializable {
    private Integer ENT;
    private String EJE;

    public CfgId() {}

    public Integer getENT() {return ENT;}
    public void setENT(Integer ENT) {this.ENT = ENT;}

    public String getEJE() {return EJE;}
    public void setEJE(String EJE) {this.EJE = EJE;}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CfgId cfgId = (CfgId) o;
        return Objects.equals(ENT, cfgId.ENT) &&
               Objects.equals(EJE, cfgId.EJE);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ENT, EJE);
    }
}