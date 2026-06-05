package tn.vermeg.gestionproduit.dto;

public final class SegmentedPromptDTO {

    private final String fullPrompt;
    private final String packSection;
    private final String garantiesSection;
    private final String markerMatched;

    public SegmentedPromptDTO(String fullPrompt, String packSection, String garantiesSection, String markerMatched) {
        this.fullPrompt = fullPrompt != null ? fullPrompt : "";
        this.packSection = packSection != null ? packSection : "";
        this.garantiesSection = garantiesSection != null ? garantiesSection : "";
        this.markerMatched = markerMatched != null ? markerMatched : "";
    }
    public String getFullPrompt() {
        return fullPrompt;
    }
    public String getPackSection() {
        return packSection;
    }
    public String getGarantiesSection() {
        return garantiesSection;
    }
    public String getMarkerMatched() {
        return markerMatched;
    }
    public boolean isSplit() {
        return !garantiesSection.isBlank();
    }
}