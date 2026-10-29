package za.co.sbsa.sbsa_fraud.web;

import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.sbsa.sbsa_fraud.service.TransactionService;
import za.co.sbsa.sbsa_fraud.web.dto.TransactionDetailsResponse;
import za.co.sbsa.sbsa_fraud.web.dto.TransactionRequest;
import za.co.sbsa.sbsa_fraud.web.dto.TransactionResponse;
// add to the imports
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST endpoints for submitting and looking up transactions.
 * Errors are turned into JSON by {@link GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name="Transactions",description ="Submit transaction and look them up")
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * Submits a transaction and returns the fraud decision in the same response (US-01).
     *
     * @return 201 Created, with a Location header for the new transaction
     */
    @PostMapping
    @Operation(summary="Look up a transaction", description="Validates and fraud-screens the transaction, then returns 201 with the decision.")
    public ResponseEntity<TransactionResponse> submit(@Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.submit(request);
        return ResponseEntity
                .created(URI.create("/api/transactions/" + response.transactionRef()))
                .body(response);
    }

    /**
     * Looks up a transaction and its alerts (US-07).
     *
     * @return 200 with the details, or 404 if the reference is unknown
     */
    @GetMapping("/{transactionRef}")
    @Operation(summary ="Look up a transaction", description = "Returns the transaction and any fraud alerts, or 404 if the reference is unknown.")
    public TransactionDetailsResponse get(@PathVariable("transactionRef") String transactionRef) {
        return transactionService.findByReference(transactionRef);
    }
}
