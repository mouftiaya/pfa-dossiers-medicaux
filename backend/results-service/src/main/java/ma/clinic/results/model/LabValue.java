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

    /** NORMAL / LOW / HIGH computed from the catalogue's reference range. */
    public static ValueFlag flagFor(AnalysisParameter parameter, double value) {
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
