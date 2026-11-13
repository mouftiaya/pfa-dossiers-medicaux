package ma.clinic.results.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** One measured value of a catalogue parameter inside a LabResult. */
@Entity
@Table(name = "lab_values")
public class LabValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lab_result_id")
    private LabResult labResult;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "parameter_id")
    private AnalysisParameter parameter;

    @Column(name = "measured_value", nullable = false)
    private Double value;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ValueFlag flag;

    protected LabValue() {
    }

    public LabValue(LabResult labResult, AnalysisParameter parameter, Double value) {
        this.labResult = labResult;
        this.parameter = parameter;
        this.value = value;
        this.flag = flagFor(parameter, value);
    }

    /**
     * INVALID (impossible value) first, then CRITICAL_*, then LOW / HIGH from the reference range, else NORMAL.
     * Every limit is optional: a parameter without it simply never gets that flag.
     */
    public static ValueFlag flagFor(AnalysisParameter parameter, double value) {
        if ((parameter.getValidMin() != null && value < parameter.getValidMin())
                || (parameter.getValidMax() != null && value > parameter.getValidMax())) {
            return ValueFlag.INVALID;
        }
        if (parameter.getCriticalLow() != null && value < parameter.getCriticalLow()) {
            return ValueFlag.CRITICAL_LOW;
        }
        if (parameter.getCriticalHigh() != null && value > parameter.getCriticalHigh()) {
            return ValueFlag.CRITICAL_HIGH;
        }
        if (parameter.getRefMin() != null && value < parameter.getRefMin()) {
            return ValueFlag.LOW;
        }
        if (parameter.getRefMax() != null && value > parameter.getRefMax()) {
            return ValueFlag.HIGH;
        }
        return ValueFlag.NORMAL;
    }

    public Long getId() {
        return id;
    }

    public LabResult getLabResult() {
        return labResult;
    }

    public AnalysisParameter getParameter() {
        return parameter;
    }

    public Double getValue() {
        return value;
    }

    public ValueFlag getFlag() {
        return flag;
    }
}
