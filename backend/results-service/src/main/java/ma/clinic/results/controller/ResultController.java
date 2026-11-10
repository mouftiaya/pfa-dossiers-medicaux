package ma.clinic.results.controller;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import ma.clinic.results.dto.ResultDtos.ComparisonRow;
import ma.clinic.results.dto.ResultDtos.CorrectionRequest;
import ma.clinic.results.dto.ResultDtos.EvolutionResponse;
import ma.clinic.results.dto.ResultDtos.ParameterDto;
import ma.clinic.results.dto.ResultDtos.ResultDto;
import ma.clinic.results.dto.ResultDtos.ResultRequest;
import ma.clinic.results.dto.ResultDtos.UploadResponse;
import ma.clinic.results.dto.ResultDtos.ValuesRequest;
import ma.clinic.results.service.ResultService;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/results")
public class ResultController {

    private final ResultService service;

    public ResultController(ResultService service) {
        this.service = service;
    }

    /** Catalogue of the known parameters (for the manual-entry form). */
    @GetMapping("/parameters")
    public List<ParameterDto> parameters() {
        return service.listParameters();
    }

    /** Results of one patient, most recent first. */
    @GetMapping
    public List<ResultDto> list(@RequestParam Long patientId) {
        return service.list(patientId);
    }

    @GetMapping("/{id}")
    public ResultDto get(@PathVariable Long id) {
        return service.get(id);
    }

    /** Manual entry (no file). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResultDto create(@Valid @RequestBody ResultRequest request, Authentication authentication) {
        return service.createManual(request, authentication.getName());
    }

    /** Upload of a PDF / image: the known parameters are extracted automatically when the document is readable. */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public UploadResponse upload(@RequestParam Long patientId,
                                 @RequestParam(required = false) Long consultationId,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate resultDate,
                                 @RequestParam("file") MultipartFile file,
                                 Authentication authentication) {
        return service.upload(patientId, consultationId, resultDate, file, authentication.getName());
    }

    /** Correction of the values (assistant or doctor) while the result is pending review. */
    @PutMapping("/{id}/values")
    public ResultDto replaceValues(@PathVariable Long id, @Valid @RequestBody ValuesRequest request) {
        return service.replaceValues(id, request.values());
    }

    /** Doctor only: sends the result back for correction. */
    @PostMapping("/{id}/request-correction")
    public ResultDto requestCorrection(@PathVariable Long id, @Valid @RequestBody CorrectionRequest request) {
        return service.requestCorrection(id, request.note());
    }

    /** Doctor only: validates the result. */
    @PostMapping("/{id}/validate")
    public ResultDto validate(@PathVariable Long id, Authentication authentication) {
        return service.validate(id, authentication.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    /** The original document (PDF / image). */
    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> file(@PathVariable Long id) {
        Resource resource = service.loadFile(id);
        String name = service.fileDisplayName(id);
        // The real type comes from the stored file's extension, not from the user's file name
        String stored = service.storedFileName(id);
        MediaType type = stored.endsWith(".pdf") ? MediaType.APPLICATION_PDF
                : stored.endsWith(".png") ? MediaType.IMAGE_PNG
                : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(name).build().toString())
                .body(resource);
    }

    /** Validated values of one parameter over time (for the HbA1c / glycemia graph). */
    @GetMapping("/evolution")
    public EvolutionResponse evolution(@RequestParam Long patientId,
                                       @RequestParam String parameter,
                                       @RequestParam(required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam(required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.evolution(patientId, parameter, from, to);
    }

    /** Comparison of several validated results, e.g. /api/results/compare?ids=3,7 */
    @GetMapping("/compare")
    public List<ComparisonRow> compare(@RequestParam List<Long> ids) {
        return service.compare(ids);
    }
}
