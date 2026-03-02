package com.example.backend.dto;

import java.time.LocalDateTime;

public class ContratoDto {
    private Integer concod;
    private String conlot;
    private String condes;
    private LocalDateTime confin;
    private LocalDateTime conffi;
    private Integer conblo;
    private Integer tercod;
    private String ternom;

    public ContratoDto(Integer concod, String conlot, String condes, LocalDateTime confin, LocalDateTime conffi, Integer conblo, Integer tercod, String ternom) {
        this.concod = concod;
        this.conlot = conlot;
        this.condes = condes;
        this.confin = confin;
        this.conffi = conffi;
        this.conblo = conblo;
        this.tercod = tercod;
        this.ternom = ternom;
    }

    public Integer getConcod() { return concod; }
    public String getConlot() { return conlot; }
    public String getCondes() { return condes; }
    public LocalDateTime getConfin() { return confin; }
    public LocalDateTime getConffi() { return conffi; }
    public Integer getConblo() { return conblo; }
    public Integer getTercod() { return tercod; }
    public String getTernom() { return ternom; }
}