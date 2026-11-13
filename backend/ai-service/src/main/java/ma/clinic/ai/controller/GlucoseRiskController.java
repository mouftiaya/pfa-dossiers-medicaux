package ma.clinic.ai.controller;

import ma.clinic.ai.glucose.GlucoseRiskService;
import ma.clinic.ai.glucose.GlucoseRiskService.RunReport;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Manual trigger of the glucose trend analysis (doctor or assistant). The normal run is scheduled. */
@RestController
@RequestMapping("/api/ai/glucose-risk")
public class GlucoseRiskController {

    private final GlucoseRiskService service;

    public GlucoseRiskController(GlucoseRiskService service) {
        this.service = service;
    }

    @PostMapping("/run")
    public RunReport run() {
        return service.run();
    }
}
