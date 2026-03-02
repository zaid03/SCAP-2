package com.example.backend.dto;

import java.time.LocalDateTime;

public class GbsWithCgeDto {
    private String cgecod;
    private String cgedes;
    private String cgecic;
    private String gbsref;
    private String gbsope;
    private String gbsorg;
    private String gbsfun;
    private String gbseco;
    private LocalDateTime gbsfop;
    private Double gbsimp;
    private Double gbsibg;
    private Double gbsius;
    private Double gbsico;
    private Double gbsiut;
    private Double gbsict;
    private Double gbs413;

    public GbsWithCgeDto(String cgecod, String cgedes, String cgecic, String gbsref, String gbsope, String gbsorg, String gbsfun, String gbseco,LocalDateTime gbsfop, Double gbsimp, Double gbsibg, Double gbsius, Double gbsico, Double gbsiut, Double gbsict, Double gbs413) {
        this.cgecod = cgecod;
        this.cgedes = cgedes;
        this.cgecic = cgecic;
        this.gbsref = gbsref;
        this.gbsope = gbsope;
        this.gbsorg = gbsorg;
        this.gbsfun = gbsfun;
        this.gbseco = gbseco;
        this.gbsfop = gbsfop;
        this.gbsimp = gbsimp;
        this.gbsibg = gbsibg;
        this.gbsius = gbsius;
        this.gbsico = gbsico;
        this.gbsiut = gbsiut;
        this.gbsict = gbsict;
        this.gbs413 = gbs413;
    }

    public String getCgecod() { return cgecod; }
    public void setCgecod(String cgecod) { this.cgecod = cgecod; }

    public String getCgedes() { return cgedes; }
    public void setCgedes(String cgedes) { this.cgedes = cgedes; }

    public String getCgecic() { return cgecic; }
    public void setCgecic(String cgecic) { this.cgecic = cgecic; }

    public String getGbsref() { return gbsref; }
    public void setGbsref(String gbsref) { this.gbsref = gbsref; }

    public String getGbsope() { return gbsope; }
    public void setGbsope(String gbsope) { this.gbsope = gbsope; }

    public String getGbsorg() { return gbsorg; }
    public void setGbsorg(String gbsorg) { this.gbsorg = gbsorg; }

    public String getGbsfun() { return gbsfun; }
    public void setGbsfun(String gbsfun) { this.gbsfun = gbsfun; }

    public String getGbseco() { return gbseco; }
    public void setGbseco(String gbseco) { this.gbseco = gbseco; }

    public LocalDateTime getGbsfop() { return gbsfop; }
    public void setGbsfop(LocalDateTime gbsfop) { this.gbsfop = gbsfop; }

    public Double getGbsimp() { return gbsimp; }
    public void setGbsimp(Double gbsimp) { this.gbsimp = gbsimp; }

    public Double getGbsibg() { return gbsibg; }
    public void setGbsibg(Double gbsibg) { this.gbsibg = gbsibg; }

    public Double getGbsius() { return gbsius; }
    public void setGbsius(Double gbsius) { this.gbsius = gbsius; }

    public Double getGbsico() { return gbsico; }
    public void setGbsico(Double gbsico) { this.gbsico = gbsico; }

    public Double getGbsiut() { return gbsiut; }
    public void setGbsiut(Double gbsiut) { this.gbsiut = gbsiut; }

    public Double getGbsict() { return gbsict; }
    public void setGbsict(Double gbsict) { this.gbsict = gbsict; }

    public Double getGbs413() { return gbs413; }
    public void setGbs413(Double gbs413) { this.gbs413 = gbs413; }
}
