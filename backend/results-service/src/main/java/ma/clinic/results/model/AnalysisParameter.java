package ma.clinic.results.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Catalogue of the analysis parameters the system knows (glycemia, HbA1c, ...).
 * Only these parameters are extracted from an uploaded document; everything else is ignored.
 * The reference range (refMin / refMax) used to flag a value comes from here, not from the paper.
 */
@Entity
@Table(name = "analysis_parameters")
public class AnalysisParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false, length = 20)
    private String unit;

    private Double refMin;

    private Double refMax;

    /** Other ways the parameter is written on a paper, separated by ";" (used to find it in the text). */
    @Column(length = 500)
    private String aliases;

    @Column(nullable = false)
    private boolean active = true;

    protected AnalysisParameter() {
    }

    public AnalysisParameter(String code, String label, String unit, Double refMin, Double refMax, String aliases) {
        this.code = code;
        this.label = label;
        this.unit = unit;
        this.refMin = refMin;
        this.refMax = refMax;
        this.aliases = aliases;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public String getUnit() {
        return unit;
    }

    public Double getRefMin() {
        return refMin;
    }

    public Double getRefMax() {
        return refMax;
    }

    public String getAliases() {
        return aliases;
    }

    public boolean isActive() {
        return active;
    }
}
