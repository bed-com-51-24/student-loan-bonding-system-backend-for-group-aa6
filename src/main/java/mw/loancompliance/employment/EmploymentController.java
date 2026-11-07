package mw.loancompliance.employment;

import jakarta.validation.Valid;
import mw.loancompliance.employment.dto.EmploymentRequest;
import mw.loancompliance.employment.dto.EmploymentResponse;
import mw.loancompliance.employment.dto.UpdateVerificationStatusRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employments")
public class EmploymentController {

    private final EmploymentService employmentService;

    public EmploymentController(EmploymentService employmentService) {
        this.employmentService = employmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmploymentResponse create(@Valid @RequestBody EmploymentRequest request) {
        return EmploymentResponse.from(employmentService.create(request));
    }

    @GetMapping
    public List<EmploymentResponse> getAll() {
        return employmentService.getAll().stream().map(EmploymentResponse::from).toList();
    }

    @GetMapping("/{id}")
    public EmploymentResponse getById(@PathVariable Long id) {
        return EmploymentResponse.from(employmentService.getById(id));
    }

    @PutMapping("/{id}")
    public EmploymentResponse update(@PathVariable Long id,
                                     @Valid @RequestBody EmploymentRequest request) {
        return EmploymentResponse.from(employmentService.update(id, request));
    }

    @PatchMapping("/{id}/verification-status")
    public EmploymentResponse updateVerificationStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVerificationStatusRequest request) {
        return EmploymentResponse.from(
                employmentService.updateVerificationStatus(id, request.verificationStatus()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        employmentService.delete(id);
    }
}
