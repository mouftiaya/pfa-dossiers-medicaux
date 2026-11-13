package ma.clinic.results.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import ma.clinic.results.dto.ResultDtos.ComparisonRow;
import ma.clinic.results.dto.ResultDtos.EvolutionPoint;
import ma.clinic.results.dto.ResultDtos.EvolutionResponse;
import ma.clinic.results.dto.ResultDtos.ParameterDto;
import ma.clinic.results.dto.ResultDtos.ResultDto;
import ma.clinic.results.dto.ResultDtos.ResultRequest;
import ma.clinic.results.dto.ResultDtos.UploadResponse;
import ma.clinic.results.dto.ResultDtos.ValueDto;
import ma.clinic.results.dto.ResultDtos.ValueInput;
import ma.clinic.results.extraction.TextExtractor;
import ma.clinic.results.extraction.ValueParser;
import ma.clinic.results.model.AnalysisParameter;
import ma.clinic.results.model.EntryMode;
import ma.clinic.results.model.LabResult;
import ma.clinic.results.model.LabValue;
import ma.clinic.results.client.NotificationClient;
import ma.clinic.results.client.NotificationClient.LabCriticalEvent;
import ma.clinic.results.model.ReviewStatus;
import ma.clinic.results.model.ValueFlag;
import ma.clinic.results.repository.AnalysisParameterRepository;
import ma.clinic.results.repository.LabResultRepository;
import ma.clinic.results.repository.LabValueRepository;
import ma.clinic.results.storage.FileStorage;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ResultService {

    private final AnalysisParameterRepository parameters;
    private final LabResultRepository results;
    private final LabValueRepository values;
    private final FileStorage storage;
    private final List<TextExtractor> extractors;
    private final ValueParser parser;
    private final NotificationClient notifications;

    public ResultService(AnalysisParameterRepository parameters, LabResultRepository results,
                         LabValueRepository values, FileStorage storage,
                         List<TextExtractor> extractors, ValueParser parser, NotificationClient notifications) {
        this.parameters = parameters;
        this.results = results;
        this.values = values;
        this.storage = storage;
        this.extractors = extractors;
        this.parser = parser;
        this.notifications = notifications;
    }

    // ---------- catalogue ----------

    @Transactional(readOnly = true)
    public List<ParameterDto> listParameters() {
        return parameters.findByActiveTrueOrderByLabel().stream().map(ResultService::toDto).toList();
    }

    // ---------- create ----------

    /** Manual entry: the assistant types the values (no file). */
    @Transactional
    public ResultDto createManual(ResultRequest request, String username) {
        checkNotInFuture(request.resultDate());
        LabResult result = newResult(request.patientId(), request.consultationId(), request.resultDate(),
                EntryMode.MANUAL, username);
        applyValues(result, request.values());
        return toDto(results.save(result));
    }

    /** Upload of a PDF / image, then automatic extraction of the catalogue parameters found in it. */
    @Transactional
    public UploadResponse upload(Long patientId, Long consultationId, LocalDate resultDate,
                                 MultipartFile file, String username) {
        checkNotInFuture(resultDate);
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The file is empty");
        }
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String contentType = FileStorage.detectContentType(data);
        if (contentType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported file: use a PDF, PNG or JPEG");
        }

        LabResult result = newResult(patientId, consultationId, resultDate, EntryMode.MANUAL, username);
        result.setFileName(cleanFileName(file.getOriginalFilename()));
        result.setStoredFileName(storage.store(data, contentType));

        // Try to read the document, keep only the catalogue parameters found in it
        Optional<String> text = Optional.empty();
        for (TextExtractor extractor : extractors) {
            text = extractor.extract(data, contentType);
            if (text.isPresent()) {
                break;
            }
        }
        Map<Long, Double> found = text
                .map(t -> parser.parse(t, parameters.findByActiveTrueOrderByLabel()))
                .orElse(Map.of());

        String message;
        if (!found.isEmpty()) {
            result.setEntryMode(EntryMode.AI_EXTRACTED);
            Map<Long, AnalysisParameter> byId = parameters.findAllById(found.keySet()).stream()
                    .collect(Collectors.toMap(AnalysisParameter::getId, p -> p));
            found.forEach((id, value) -> result.getValues().add(new LabValue(result, byId.get(id), value)));
            message = found.size() + " value(s) extracted. Check them before submitting.";
        } else if (text.isPresent()) {
            message = "The document was read but no known parameter was found. Enter the values manually.";
        } else {
            message = "This document cannot be read automatically (scan or photo). Enter the values manually.";
        }
        LabResult saved = results.save(result);
        return new UploadResponse(toDto(saved), !found.isEmpty(), found.size(), message);
    }

    // ---------- read ----------

    @Transactional(readOnly = true)
    public ResultDto get(Long id) {
        return toDto(find(id));
    }

    @Transactional(readOnly = true)
    public List<ResultDto> list(Long patientId) {
        return results.findByPatientIdOrderByResultDateDescIdDesc(patientId).stream()
                .map(ResultService::toDto).toList();
    }

    public Resource loadFile(Long id) {
        String stored = findFileName(id);
        return storage.load(stored);
    }

    @Transactional(readOnly = true)
    public String fileDisplayName(Long id) {
        LabResult result = find(id);
        return result.getFileName() == null ? "result" : result.getFileName();
    }

    /** Name of the file on disk; its extension tells the real type (set from the file content at upload). */
    public String storedFileName(Long id) {
        return findFileName(id);
    }

    private String findFileName(Long id) {
        LabResult result = results.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Result not found"));
        if (result.getStoredFileName() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This result has no file");
        }
        return result.getStoredFileName();
    }

    // ---------- review workflow ----------

    /** The assistant (or the doctor) corrects or completes the values. Only while pending review. */
    @Transactional
    public ResultDto replaceValues(Long id, List<ValueInput> inputs) {
        LabResult result = find(id);
        requirePending(result);
        result.getValues().clear();
        results.flush(); // delete the old rows before inserting the new ones
        applyValues(result, inputs);
        result.setReviewNote(null);
        return toDto(results.save(result));
    }

    /** The doctor sends the result back with a note; it stays pending review. */
    @Transactional
    public ResultDto requestCorrection(Long id, String note) {
        LabResult result = find(id);
        requirePending(result);
        result.setReviewNote(note);
        return toDto(results.save(result));
    }

    /** The doctor validates the result: from now on it can be used for history, graphs and AI. */
    @Transactional
    public ResultDto validate(Long id, String username) {
        LabResult result = find(id);
        requirePending(result);
        if (result.getValues().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot validate a result without values");
        }
        if (result.getValues().stream().anyMatch(v -> v.getFlag() == ValueFlag.INVALID)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A value is outside the possible range (typing or extraction error): correct it before validating");
        }
        result.setStatus(ReviewStatus.VALIDATED);
        result.setValidatedBy(username);
        result.setValidatedAt(LocalDateTime.now());
        result.setReviewNote(null);
        LabResult saved = results.save(result);
        alertOnCriticalValues(saved);
        return toDto(saved);
    }

    /** A pending result can be deleted (wrong file, wrong patient). A validated one cannot. */
    @Transactional
    public void delete(Long id) {
        LabResult result = find(id);
        requirePending(result);
        results.delete(result);
        storage.delete(result.getStoredFileName());
    }

    // ---------- history, graphs, comparison ----------

    @Transactional(readOnly = true)
    public EvolutionResponse evolution(Long patientId, String parameterCode, LocalDate from, LocalDate to) {
        AnalysisParameter parameter = parameters.findByCodeAndActiveTrue(parameterCode.toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown parameter"));
        LocalDate start = from == null ? LocalDate.of(1900, 1, 1) : from;
        LocalDate end = to == null ? LocalDate.of(9999, 12, 31) : to;
        List<EvolutionPoint> points = values.findValidatedSeries(patientId, parameter.getCode(), start, end).stream()
                .map(v -> new EvolutionPoint(v.getLabResult().getId(), v.getLabResult().getResultDate(),
                        v.getValue(), v.getFlag()))
                .toList();
        return new EvolutionResponse(toDto(parameter), points);
    }

    /** Compares validated results of the same patient, parameter by parameter, oldest to newest. */
    @Transactional(readOnly = true)
    public List<ComparisonRow> compare(List<Long> ids) {
        Set<Long> distinct = new LinkedHashSet<>(ids);
        if (distinct.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select at least two results");
        }
        List<LabResult> selected = new ArrayList<>(results.findAllById(distinct));
        if (selected.size() != distinct.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Result not found");
        }
        if (selected.stream().map(LabResult::getPatientId).distinct().count() > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Results must belong to the same patient");
        }
        if (selected.stream().anyMatch(r -> r.getStatus() != ReviewStatus.VALIDATED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only validated results can be compared");
        }
        selected.sort(Comparator.comparing(LabResult::getResultDate).thenComparing(LabResult::getId));

        Map<Long, List<EvolutionPoint>> pointsByParameter = new LinkedHashMap<>();
        Map<Long, AnalysisParameter> parameterById = new LinkedHashMap<>();
        for (LabResult result : selected) {
            for (LabValue v : result.getValues()) {
                parameterById.putIfAbsent(v.getParameter().getId(), v.getParameter());
                pointsByParameter.computeIfAbsent(v.getParameter().getId(), k -> new ArrayList<>())
                        .add(new EvolutionPoint(result.getId(), result.getResultDate(), v.getValue(), v.getFlag()));
            }
        }
        List<ComparisonRow> rows = new ArrayList<>();
        pointsByParameter.forEach((parameterId, points) -> {
            Double change = points.size() < 2 ? null
                    : points.get(points.size() - 1).value() - points.get(0).value();
            rows.add(new ComparisonRow(toDto(parameterById.get(parameterId)), points, change));
        });
        return rows;
    }

    /** After the commit, tells the notification service about every critical value (best effort, never blocks). */
    private void alertOnCriticalValues(LabResult saved) {
        List<LabCriticalEvent> events = saved.getValues().stream()
                .filter(v -> v.getFlag() == ValueFlag.CRITICAL_LOW || v.getFlag() == ValueFlag.CRITICAL_HIGH)
                .map(v -> new LabCriticalEvent(saved.getPatientId(), saved.getId(), v.getParameter().getLabel(),
                        v.getValue(), v.getParameter().getUnit(), v.getFlag().name()))
                .toList();
        if (events.isEmpty()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                events.forEach(notifications::labCritical);
            }
        });
    }

    // ---------- patient portal (official results of one patient only) ----------

    @Transactional(readOnly = true)
    public List<ResultDto> listValidatedFor(Long patientId) {
        return results.findByPatientIdAndStatusOrderByResultDateDescIdDesc(patientId, ReviewStatus.VALIDATED).stream()
                .map(ResultService::toDto).toList();
    }

    /** 404 (not 403) when the result belongs to someone else or is not validated, so nothing leaks. */
    @Transactional(readOnly = true)
    public ResultDto getValidatedFor(Long id, Long patientId) {
        return toDto(findValidatedFor(id, patientId));
    }

    @Transactional(readOnly = true)
    public void requireValidatedFor(Long id, Long patientId) {
        findValidatedFor(id, patientId);
    }

    private LabResult findValidatedFor(Long id, Long patientId) {
        LabResult result = find(id);
        if (!result.getPatientId().equals(patientId) || result.getStatus() != ReviewStatus.VALIDATED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Result not found");
        }
        return result;
    }

    // ---------- helpers ----------

    private LabResult find(Long id) {
        return results.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Result not found"));
    }

    private void requirePending(LabResult result) {
        if (result.getStatus() != ReviewStatus.PENDING_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This result is already validated");
        }
    }

    private void checkNotInFuture(LocalDate date) {
        if (date.isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The result date cannot be in the future");
        }
    }

    private LabResult newResult(Long patientId, Long consultationId, LocalDate resultDate,
                                EntryMode mode, String username) {
        LabResult result = new LabResult();
        result.setPatientId(patientId);
        result.setConsultationId(consultationId);
        result.setResultDate(resultDate);
        result.setEntryMode(mode);
        result.setStatus(ReviewStatus.PENDING_REVIEW);
        result.setUploadedBy(username);
        return result;
    }

    /** Adds the values to the result; each parameter must exist in the catalogue and appear only once. */
    private void applyValues(LabResult result, List<ValueInput> inputs) {
        Set<Long> ids = inputs.stream().map(ValueInput::parameterId).collect(Collectors.toSet());
        if (ids.size() != inputs.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A parameter appears more than once");
        }
        Map<Long, AnalysisParameter> byId = parameters.findAllById(ids).stream()
                .filter(AnalysisParameter::isActive)
                .collect(Collectors.toMap(AnalysisParameter::getId, p -> p));
        if (byId.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown parameter");
        }
        for (ValueInput input : inputs) {
            result.getValues().add(new LabValue(result, byId.get(input.parameterId()), input.value()));
        }
    }

    private String cleanFileName(String original) {
        if (original == null || original.isBlank()) {
            return "result";
        }
        String name = original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        return name.length() > 200 ? name.substring(0, 200) : name;
    }

    private static ParameterDto toDto(AnalysisParameter p) {
        return new ParameterDto(p.getId(), p.getCode(), p.getLabel(), p.getUnit(), p.getRefMin(), p.getRefMax(),
                p.getValidMin(), p.getValidMax(), p.getCriticalLow(), p.getCriticalHigh());
    }

    private static ResultDto toDto(LabResult r) {
        List<ValueDto> valueDtos = r.getValues().stream()
                .map(v -> new ValueDto(v.getId(), toDto(v.getParameter()), v.getValue(), v.getFlag()))
                .toList();
        return new ResultDto(r.getId(), r.getPatientId(), r.getConsultationId(), r.getResultDate(),
                r.getFileName(), r.getStoredFileName() != null, r.getEntryMode(), r.getStatus(),
                r.getReviewNote(), r.getUploadedBy(), r.getUploadedAt(), r.getValidatedBy(),
                r.getValidatedAt(), valueDtos);
    }
}
