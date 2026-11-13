package ma.clinic.glucose.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ma.clinic.glucose.client.NotificationClient;
import ma.clinic.glucose.dto.GlucoseDtos.DemoResultDto;
import ma.clinic.glucose.dto.GlucoseDtos.MeasurementDto;
import ma.clinic.glucose.dto.GlucoseDtos.MeasurementRequest;
import ma.clinic.glucose.dto.GlucoseDtos.StatsDto;
import ma.clinic.glucose.dto.GlucoseDtos.SummaryDto;
import ma.clinic.glucose.dto.GlucoseDtos.TargetDto;
import ma.clinic.glucose.dto.GlucoseDtos.TargetRequest;
import ma.clinic.glucose.dto.GlucoseDtos.WindowDto;
import ma.clinic.glucose.logic.DemoHistoryGenerator;
import ma.clinic.glucose.logic.DemoHistoryGenerator.Generated;
import ma.clinic.glucose.logic.FlagCalculator;
import ma.clinic.glucose.logic.MeasurementRules;
import ma.clinic.glucose.logic.StatsCalculator;
import ma.clinic.glucose.logic.StatsCalculator.Aggregate;
import ma.clinic.glucose.logic.StatsCalculator.Reading;
import ma.clinic.glucose.logic.StatsPeriod;
import ma.clinic.glucose.logic.Thresholds;
import ma.clinic.glucose.logic.WindowCalculator;
import ma.clinic.glucose.model.GlucoseMeasurement;
import ma.clinic.glucose.model.GlucoseTarget;
import ma.clinic.glucose.repository.GlucoseMeasurementRepository;
import ma.clinic.glucose.repository.GlucoseTargetRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Measurements (CRUD with ownership check), targets, summary and statistics. All values are in mg/dL. */
@Service
public class GlucoseService {

    private static final int SUMMARY_DAYS = 30;

    private final GlucoseMeasurementRepository measurements;
    private final GlucoseTargetRepository targets;
    private final NotificationClient notifications;

    public GlucoseService(GlucoseMeasurementRepository measurements, GlucoseTargetRepository targets,
                          NotificationClient notifications) {
        this.measurements = measurements;
        this.targets = targets;
        this.notifications = notifications;
    }

    // ---------------------------------------------------------------- measurements

    public List<MeasurementDto> list(Long patientId, LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from must not be after to");
        }
        LocalDateTime start = (from == null ? LocalDate.of(1900, 1, 1) : from).atStartOfDay();
        LocalDateTime end = (to == null ? LocalDate.of(2200, 1, 1) : to.plusDays(1)).atStartOfDay();
        Thresholds t = thresholds(patientId);
        List<MeasurementDto> result = new ArrayList<>();
        for (GlucoseMeasurement m : measurements.findInRange(patientId, start, end)) {
            result.add(toDto(m, t));
        }
        return result;
    }

    public MeasurementDto create(Long patientId, MeasurementRequest request) {
        validate(request);
        GlucoseMeasurement m = new GlucoseMeasurement();
        m.setPatientId(patientId);
        apply(m, request);
        GlucoseMeasurement saved = measurements.save(m);
        Thresholds t = thresholds(patientId);

        // Best effort, never fails the request (the client swallows every exception).
        List<GlucoseMeasurement> before = measurements
                .findTop4ByPatientIdAndMeasuredAtBeforeOrderByMeasuredAtDesc(patientId, saved.getMeasuredAt());
        notifications.notifyMeasurementCreated(saved, t, before);

        return toDto(saved, t);
    }

    public MeasurementDto update(Long patientId, Long id, MeasurementRequest request) {
        GlucoseMeasurement m = findOwn(patientId, id);
        validate(request);
        apply(m, request);
        return toDto(measurements.save(m), thresholds(patientId));
    }

    public void delete(Long patientId, Long id) {
        measurements.delete(findOwn(patientId, id));
    }

    private GlucoseMeasurement findOwn(Long patientId, Long id) {
        // Another patient's id gives the same 404 as an unknown id.
        return measurements.findByIdAndPatientId(id, patientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Measurement not found"));
    }

    private void validate(MeasurementRequest request) {
        String error = MeasurementRules.validationError(request.value(), request.measuredAt(),
                request.context(), LocalDateTime.now());
        if (error != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
        }
    }

    private void apply(GlucoseMeasurement m, MeasurementRequest request) {
        m.setValue(request.value());
        m.setMeasuredAt(request.measuredAt());
        m.setContext(request.context());
        String note = request.note();
        m.setNote(note == null || note.isBlank() ? null : note.trim());
    }

    private MeasurementDto toDto(GlucoseMeasurement m, Thresholds t) {
        return new MeasurementDto(m.getId(), m.getValue(), m.getMeasuredAt(), m.getContext(), m.getNote(),
                FlagCalculator.compute(m.getValue(), t));
    }

    // ---------------------------------------------------------------- targets

    public TargetDto getTarget(Long patientId) {
        return toTargetDto(patientId, targets.findByPatientId(patientId));
    }

    public TargetDto putTarget(Long patientId, TargetRequest request, String doctor) {
        Thresholds t = new Thresholds(request.criticalLow(), request.lowThreshold(), request.targetMin(),
                request.targetMax(), request.highThreshold(), request.criticalHigh(), request.absenceDays());
        String error = t.validationError();
        if (error != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, error);
        }
        GlucoseTarget target = targets.findByPatientId(patientId).orElseGet(() -> {
            GlucoseTarget created = new GlucoseTarget();
            created.setPatientId(patientId);
            return created;
        });
        target.setCriticalLow(t.criticalLow());
        target.setLowThreshold(t.lowThreshold());
        target.setTargetMin(t.targetMin());
        target.setTargetMax(t.targetMax());
        target.setHighThreshold(t.highThreshold());
        target.setCriticalHigh(t.criticalHigh());
        target.setAbsenceDays(t.absenceDays());
        target.setValidated(true);
        target.setDefinedBy(doctor);
        target.setUpdatedAt(LocalDateTime.now());
        return toTargetDto(patientId, Optional.of(targets.save(target)));
    }

    /** The patient's thresholds, or the defaults when no target is stored. */
    public Thresholds thresholds(Long patientId) {
        return targets.findByPatientId(patientId).map(GlucoseService::toThresholds).orElse(Thresholds.DEFAULTS);
    }

    static Thresholds toThresholds(GlucoseTarget g) {
        return new Thresholds(g.getCriticalLow(), g.getLowThreshold(), g.getTargetMin(), g.getTargetMax(),
                g.getHighThreshold(), g.getCriticalHigh(), g.getAbsenceDays());
    }

    private TargetDto toTargetDto(Long patientId, Optional<GlucoseTarget> stored) {
        if (stored.isEmpty()) {
            Thresholds d = Thresholds.DEFAULTS;
            return new TargetDto(patientId, d.targetMin(), d.targetMax(), d.lowThreshold(), d.highThreshold(),
                    d.criticalLow(), d.criticalHigh(), d.absenceDays(), false, false, null, null);
        }
        GlucoseTarget g = stored.get();
        return new TargetDto(patientId, g.getTargetMin(), g.getTargetMax(), g.getLowThreshold(),
                g.getHighThreshold(), g.getCriticalLow(), g.getCriticalHigh(), g.getAbsenceDays(), true,
                g.isValidated(), g.getDefinedBy(), g.getUpdatedAt());
    }

    // ---------------------------------------------------------------- stats and summary

    public StatsDto stats(Long patientId, StatsPeriod period, LocalDate anchorDate) {
        LocalDate anchor = anchorDate == null ? LocalDate.now() : anchorDate;
        Optional<GlucoseTarget> stored = targets.findByPatientId(patientId);
        Thresholds t = stored.map(GlucoseService::toThresholds).orElse(Thresholds.DEFAULTS);
        List<Reading> readings = readings(patientId, anchor, period.days());
        return StatsCalculator.compute(period, anchor, readings, t, toTargetDto(patientId, stored));
    }

    public SummaryDto summary(Long patientId) {
        Optional<GlucoseTarget> stored = targets.findByPatientId(patientId);
        Thresholds t = stored.map(GlucoseService::toThresholds).orElse(Thresholds.DEFAULTS);
        List<Reading> readings = readings(patientId, LocalDate.now(), SUMMARY_DAYS);
        Aggregate agg = StatsCalculator.aggregate(readings, t);
        return new SummaryDto(patientId, SUMMARY_DAYS, agg.count(), agg.average(), agg.percentInTarget(),
                agg.highCount(), agg.lowCount(), measurements.findLastMeasuredAt(patientId),
                toTargetDto(patientId, stored));
    }

    // ---------------------------------------------------------------- long-window analysis (AI service)

    /** Patients who have at least one measurement. */
    public List<Long> patientIdsWithData() {
        return measurements.findDistinctPatientIds();
    }

    /** Aggregated history of the last {@code days} days (no individual value), split in weeks. */
    public WindowDto window(Long patientId, int days) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(days - 1L);
        return WindowCalculator.compute(patientId, from, to, readings(patientId, to, days), thresholds(patientId));
    }

    // ---------------------------------------------------------------- demo data (testing only)

    /**
     * Replaces the patient's measurements of the last {@code days} days with generated ones, so that the trend
     * analysis can be tried without waiting weeks. Only the calling patient's own data is touched.
     */
    @Transactional
    public DemoResultDto generateDemoHistory(Long patientId, DemoHistoryGenerator.Profile profile, int days) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.toLocalDate().minusDays(days - 1L).atStartOfDay();
        measurements.deleteAll(measurements.findInRange(patientId, start, now.toLocalDate().plusDays(1).atStartOfDay()));
        List<GlucoseMeasurement> created = new ArrayList<>();
        for (Generated g : DemoHistoryGenerator.generate(profile, days, now)) {
            GlucoseMeasurement m = new GlucoseMeasurement();
            m.setPatientId(patientId);
            m.setValue(g.value());
            m.setMeasuredAt(g.at());
            m.setContext(g.context());
            m.setNote("Donnée de démonstration");
            created.add(m);
        }
        measurements.saveAll(created);
        return new DemoResultDto(profile.name(), days, created.size());
    }

    /** Readings of the {@code days} days ending on the anchor day (included). */
    private List<Reading> readings(Long patientId, LocalDate anchor, int days) {
        LocalDateTime start = anchor.minusDays(days - 1L).atStartOfDay();
        LocalDateTime end = anchor.plusDays(1).atStartOfDay();
        List<Reading> result = new ArrayList<>();
        for (GlucoseMeasurement m : measurements.findInRange(patientId, start, end)) {
            result.add(new Reading(m.getValue(), m.getMeasuredAt()));
        }
        return result;
    }
}
