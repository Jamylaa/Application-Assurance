package tn.vermeg.gestionproduit.services.chatbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tn.vermeg.gestionproduit.dto.SegmentedPromptDTO;

import java.util.Locale;
// Découpe le prompt avant extraction regex : zone pack vs zone garanties.
 // Recherche en minuscules sur le texte d'origine (mêmes longueurs → indices substring valides).

@Service
public class PromptSegmentationService {

    private static final Logger logger = LoggerFactory.getLogger(PromptSegmentationService.class);

    /** Marqueurs en minuscules ; ordre du plus spécifique au plus large. */
    private static final String[] GARANTIE_SECTION_MARKERS = {
        "les garanties à ajouter sont les suivantes",
        "les garanties a ajouter sont les suivantes",
        "les garanties à ajouter sont",
        "les garanties a ajouter sont",
        "les garanties suivantes",
        "liste des garanties",
        "garanties suivantes",
        "garanties à inclure",
        "garanties a inclure",
        "garanties incluses",
        "avec les garanties suivantes",
        "incluant les garanties",
        "inclure les garanties",
        "les garanties ci-dessous",
        "--- garanties",
        "garanties :"
    };

    public SegmentedPromptDTO segment(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return new SegmentedPromptDTO("", "", "", "");
        }
        String lower = prompt.toLowerCase(Locale.FRENCH);
        for (String marker : GARANTIE_SECTION_MARKERS) {
            int idx = lower.indexOf(marker);
            if (idx >= 0) {
                int end = idx + marker.length();
                String packPart = prompt.substring(0, idx).trim();
                String garPart = prompt.substring(end).trim();
                logger.info("Prompt segmenté (marqueur connu) — pack: {} car., garanties: {} car.",
                        packPart.length(), garPart.length());
                return new SegmentedPromptDTO(prompt, packPart, garPart, marker);
            }
        }
        return new SegmentedPromptDTO(prompt, prompt.trim(), "", "");
    }
}
