package org.example.dto;

import java.util.List;

public class VisualizeResponse {
    private String compound;
    private String source;
    private String format;
    private String data;
    private Long cid;
    private List<CompoundInfoDto> isomers;

    public VisualizeResponse() {
    }

    public VisualizeResponse(String compound, String source, String format, String data, Long cid, List<CompoundInfoDto> isomers) {
        this.compound = compound;
        this.source = source;
        this.format = format;
        this.data = data;
        this.cid = cid;
        this.isomers = isomers;
    }

    public String getCompound() {
        return compound;
    }

    public String getSource() {
        return source;
    }

    public String getFormat() {
        return format;
    }

    public String getData() {
        return data;
    }

    public Long getCid() {
        return cid;
    }

    public List<CompoundInfoDto> getIsomers() {
        return isomers;
    }
}
