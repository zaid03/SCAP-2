package com.example.backend.dto;

import java.time.LocalDateTime;

public class FacWithTerDto {
    private Integer ent;
    private String eje;
    private Integer facnum;
    private Integer tercod;
    private String cgecod;
    private String facobs;
    private Double facimp;
    private Double faciec;
    private Double facidi;
    private String factdc;
    private String facann;
    private String facfac;
    private String facdoc;
    private LocalDateTime facdat;    
    private LocalDateTime facfco;      
    private String facado;           
    private String factxt;
    private LocalDateTime facfre;
    private String conctp;
    private String concpr;
    private String conccr;
    private Integer facoct;
    private String facfpg;
    private String facopg;             
    private String factpg;
    private Double facdto;
    private String ternom;
    private String ternif;

    public FacWithTerDto(Integer ent, String eje, Integer facnum, Integer tercod, String cgecod, 
                         String facobs, Double facimp, Double faciec, Double facidi, String factdc, 
                         String facann, String facfac, String facdoc, LocalDateTime facdat, 
                         LocalDateTime facfco, String facado, String factxt, LocalDateTime facfre, 
                         String conctp, String concpr, String conccr, Integer facoct, String facfpg, 
                         String facopg, String factpg, Double facdto, String ternom, String ternif) {
        this.ent = ent;
        this.eje = eje;
        this.facnum = facnum;
        this.tercod = tercod;
        this.cgecod = cgecod;
        this.facobs = facobs;
        this.facimp = facimp;
        this.faciec = faciec;
        this.facidi = facidi;
        this.factdc = factdc;
        this.facann = facann;
        this.facfac = facfac;
        this.facdoc = facdoc;
        this.facdat = facdat;
        this.facfco = facfco;
        this.facado = facado;
        this.factxt = factxt;
        this.facfre = facfre;
        this.conctp = conctp;
        this.concpr = concpr;
        this.conccr = conccr;
        this.facoct = facoct;
        this.facfpg = facfpg;
        this.facopg = facopg;
        this.factpg = factpg;
        this.facdto = facdto;
        this.ternom = ternom;
        this.ternif = ternif;
    }

    // Getters
    public Integer getEnt() { return ent; }
    public String getEje() { return eje; }
    public Integer getFacnum() { return facnum; }
    public Integer getTercod() { return tercod; }
    public String getCgecod() { return cgecod; }
    public String getFacobs() { return facobs; }
    public Double getFacimp() { return facimp; }
    public Double getFaciec() { return faciec; }
    public Double getFacidi() { return facidi; }
    public String getFactdc() { return factdc; }
    public String getFacann() { return facann; }
    public String getFacfac() { return facfac; }
    public String getFacdoc() { return facdoc; }
    public LocalDateTime getFacdat() { return facdat; }
    public LocalDateTime getFacfco() { return facfco; }
    public String getFacado() { return facado; }
    public String getFactxt() { return factxt; }
    public LocalDateTime getFacfre() { return facfre; }
    public String getConctp() { return conctp; }
    public String getConcpr() { return concpr; }
    public String getConccr() { return conccr; }
    public Integer getFacoct() { return facoct; }
    public String getFacfpg() { return facfpg; }
    public String getFacopg() { return facopg; }
    public String getFactpg() { return factpg; }
    public Double getFacdto() { return facdto; }
    public String getTernom() { return ternom; }
    public String getTernif() { return ternif; }

    // Setters
    public void setEnt(Integer ent) { this.ent = ent; }
    public void setEje(String eje) { this.eje = eje; }
    public void setFacnum(Integer facnum) { this.facnum = facnum; }
    public void setTercod(Integer tercod) { this.tercod = tercod; }
    public void setCgecod(String cgecod) { this.cgecod = cgecod; }
    public void setFacobs(String facobs) { this.facobs = facobs; }
    public void setFacimp(Double facimp) { this.facimp = facimp; }
    public void setFaciec(Double faciec) { this.faciec = faciec; }
    public void setFacidi(Double facidi) { this.facidi = facidi; }
    public void setFactdc(String factdc) { this.factdc = factdc; }
    public void setFacann(String facann) { this.facann = facann; }
    public void setFacfac(String facfac) { this.facfac = facfac; }
    public void setFacdoc(String facdoc) { this.facdoc = facdoc; }
    public void setFacdat(LocalDateTime facdat) { this.facdat = facdat; }
    public void setFacfco(LocalDateTime facfco) { this.facfco = facfco; }
    public void setFacado(String facado) { this.facado = facado; }
    public void setFactxt(String factxt) { this.factxt = factxt; }
    public void setFacfre(LocalDateTime facfre) { this.facfre = facfre; }
    public void setConctp(String conctp) { this.conctp = conctp; }
    public void setConcpr(String concpr) { this.concpr = concpr; }
    public void setConccr(String conccr) { this.conccr = conccr; }
    public void setFacoct(Integer facoct) { this.facoct = facoct; }
    public void setFacfpg(String facfpg) { this.facfpg = facfpg; }
    public void setFacopg(String facopg) { this.facopg = facopg; }
    public void setFactpg(String factpg) { this.factpg = factpg; }
    public void setFacdto(Double facdto) { this.facdto = facdto; }
    public void setTernom(String ternom) { this.ternom = ternom; }
    public void setTernif(String ternif) { this.ternif = ternif; }
}