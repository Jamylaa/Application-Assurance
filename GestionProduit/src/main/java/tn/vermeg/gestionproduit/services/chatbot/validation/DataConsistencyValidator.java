package tn.vermeg.gestionproduit.services.chatbot.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tn.vermeg.gestionproduit.dto.ChatbotGarantieRequestDTO;
import tn.vermeg.gestionproduit.entities.Garantie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DataConsistencyValidator {
    private static final Logger logger = LoggerFactory.getLogger(DataConsistencyValidator.class);

    public class ConsistencyReport {
        private boolean consistent;
        private List<String> discrepancies;
        private Map<String, FieldTrace> fieldTraces;

        public ConsistencyReport() {
            this.consistent = true;
            this.discrepancies = new ArrayList<>();
            this.fieldTraces = new HashMap<>();
        }

        public void addDiscrepancy(String field, String extracted, String recorded, String severity) {
            this.consistent = false;
            this.discrepancies.add(String.format("[%s] %s: extrait='%s' vs enregistré='%s'", severity, field, extracted, recorded));
        }

        public void addFieldTrace(String fieldName, FieldTrace trace) {
            this.fieldTraces.put(fieldName, trace);
        }

        public boolean isConsistent() { return consistent; }
        public List<String> getDiscrepancies() { return discrepancies; }
        public Map<String, FieldTrace> getFieldTraces() { return fieldTraces; }
    }

    public class FieldTrace {
        private String fieldName;
        private Object extractedValue;
        private Object recordedValue;
        private boolean match;
        private String source;

        public FieldTrace(String fieldName, Object extractedValue, Object recordedValue, String source) {
            this.fieldName = fieldName;
            this.extractedValue = extractedValue;
            this.recordedValue = recordedValue;
            this.source = source;
            this.match = valuesMatch(extractedValue, recordedValue);
        }

        private boolean valuesMatch(Object extracted, Object recorded) {
            if (extracted == null && recorded == null) return true;
            if (extracted == null || recorded == null) return false;
            return extracted.toString().equals(recorded.toString());
        }

        public String getFieldName() { return fieldName; }
        public Object getExtractedValue() { return extractedValue; }
        public Object getRecordedValue() { return recordedValue; }
        public boolean isMatch() { return match; }
        public String getSource() { return source; }
    }

    /**
     * Valide la correspondance entre les données extraites (DTO) et les données enregistrées (Entity)
     */
    public ConsistencyReport validateGarantieConsistency(ChatbotGarantieRequestDTO dto, Garantie entity) {
        logger.info("=== VALIDATION DE CONSISTANCE DES DONNÉES ===");
        logger.info("DTO: {}", dto);
        logger.info("Entity: {}", entity);

        ConsistencyReport report = new ConsistencyReport();

        // Validation de chaque champ critique
        validateField(report, "nomGarantie", dto.getNomGarantie(), entity.getNomGarantie(), "EXTRACTION");
        validateField(report, "domaine", dto.getDomaine() != null ? dto.getDomaine().name() : null, 
                     entity.getDomaine() != null ? entity.getDomaine().name() : null, "EXTRACTION");
        validateField(report, "typeMontant", dto.getTypeMontant(), 
                     entity.getTypeMontant() != null ? entity.getTypeMontant().name() : null, "EXTRACTION");
        validateNumericField(report, "tauxRemboursement", dto.getTauxRemboursement(), entity.getTauxRemboursement(), "EXTRACTION");
        validateNumericField(report, "plafondAnnuel", dto.getPlafondAnnuel(), entity.getPlafondAnnuel(), "EXTRACTION");
        validateNumericField(report, "plafondMensuel", dto.getPlafondMensuel(), entity.getPlafondMensuel(), "EXTRACTION");
        validateNumericField(report, "plafondParActe", dto.getPlafondParActe(), entity.getPlafondParActe(), "EXTRACTION");
        validateNumericField(report, "franchise", dto.getFranchise(), entity.getFranchise(), "EXTRACTION");
        validateNumericField(report, "coutMoyenParSinistre", dto.getCoutMoyenParSinistre(), entity.getCoutMoyenParSinistre(), "EXTRACTION");
        validateIntegerField(report, "dureeMinContrat", dto.getDureeMinContrat(), entity.getDureeMinContrat(), "EXTRACTION");
        validateIntegerField(report, "dureeMaxContrat", dto.getDureeMaxContrat(), entity.getDureeMaxContrat(), "EXTRACTION");
        validateBooleanField(report, "resiliableAnnuellement", dto.getResiliableAnnuellement(), entity.isResiliableAnnuellement(), "EXTRACTION");
        validateField(report, "description", dto.getDescription(), entity.getDescription(), "EXTRACTION");
        validateField(report, "statut", dto.getStatut(), entity.getStatut() != null ? entity.getStatut().name() : null, "EXTRACTION");

        // Log détaillé du rapport
        logger.info("=== RAPPORT DE VALIDATION ===");
        logger.info("Consistance globale: {}", report.isConsistent() ? "OK" : "ECHEC");
        
        if (!report.isConsistent()) {
            logger.error("Discrepances détectées:");
            report.getDiscrepancies().forEach(discrepancy -> logger.error("  - {}", discrepancy));
        }

        logger.info("=== TRACE DES CHAMPS ===");
        report.getFieldTraces().forEach((fieldName, trace) -> {
            logger.info("  {}: extrait={}, enregistré={}, match={}, source={}", 
                fieldName, trace.getExtractedValue(), trace.getRecordedValue(), 
                trace.isMatch(), trace.getSource());
        });

        return report;
    }

    private void validateField(ConsistencyReport report, String fieldName, Object extracted, Object recorded, String source) {
        FieldTrace trace = new FieldTrace(fieldName, extracted, recorded, source);
        report.addFieldTrace(fieldName, trace);
        
        if (!trace.isMatch()) {
            String severity = determineSeverity(fieldName);
            report.addDiscrepancy(fieldName, 
                extracted != null ? extracted.toString() : "null",
                recorded != null ? recorded.toString() : "null",
                severity);
        }
    }

    private void validateNumericField(ConsistencyReport report, String fieldName, Double extracted, double recorded, String source) {
        Object extractedObj = extracted != null ? extracted : null;
        FieldTrace trace = new FieldTrace(fieldName, extractedObj, recorded, source);
        report.addFieldTrace(fieldName, trace);
        
        if (!trace.isMatch()) {
            String severity = determineSeverity(fieldName);
            report.addDiscrepancy(fieldName,
                extractedObj != null ? extractedObj.toString() : "null",
                String.valueOf(recorded),
                severity);
        }
    }

    private void validateIntegerField(ConsistencyReport report, String fieldName, Integer extracted, int recorded, String source) {
        Object extractedObj = extracted != null ? extracted : null;
        FieldTrace trace = new FieldTrace(fieldName, extractedObj, recorded, source);
        report.addFieldTrace(fieldName, trace);
        
        if (!trace.isMatch()) {
            String severity = determineSeverity(fieldName);
            report.addDiscrepancy(fieldName,
                extractedObj != null ? extractedObj.toString() : "null",
                String.valueOf(recorded),
                severity);
        }
    }

    private void validateBooleanField(ConsistencyReport report, String fieldName, Boolean extracted, boolean recorded, String source) {
        Object extractedObj = extracted != null ? extracted : null;
        FieldTrace trace = new FieldTrace(fieldName, extractedObj, recorded, source);
        report.addFieldTrace(fieldName, trace);
        
        if (!trace.isMatch()) {
            String severity = determineSeverity(fieldName);
            report.addDiscrepancy(fieldName,
                extractedObj != null ? extractedObj.toString() : "null",
                String.valueOf(recorded),
                severity);
        }
    }

    private String determineSeverity(String fieldName) {
        // Champs critiques qui doivent correspondre exactement
        if (fieldName.equals("typeMontant") || fieldName.equals("domaine") || fieldName.equals("statut")) {
            return "CRITICAL";
        }
        // Champs importants mais tolérants
        if (fieldName.equals("dureeMaxContrat") || fieldName.equals("dureeMinContrat") || 
            fieldName.equals("plafondAnnuel") || fieldName.equals("coutMoyenParSinistre")) {
            return "HIGH";
        }
        return "MEDIUM";
    }

    /**
     * Génère un rapport JSON détaillé pour le frontend
     */
    public Map<String, Object> generateValidationReport(ConsistencyReport report) {
        Map<String, Object> validationReport = new HashMap<>();
        validationReport.put("consistent", report.isConsistent());
        validationReport.put("discrepancies", report.getDiscrepancies());
        
        Map<String, Map<String, Object>> fieldDetails = new HashMap<>();
        report.getFieldTraces().forEach((fieldName, trace) -> {
            Map<String, Object> fieldInfo = new HashMap<>();
            fieldInfo.put("extracted", trace.getExtractedValue());
            fieldInfo.put("recorded", trace.getRecordedValue());
            fieldInfo.put("match", trace.isMatch());
            fieldInfo.put("source", trace.getSource());
            fieldDetails.put(fieldName, fieldInfo);
        });
        validationReport.put("fieldTraces", fieldDetails);
        
        return validationReport;
    }
}
