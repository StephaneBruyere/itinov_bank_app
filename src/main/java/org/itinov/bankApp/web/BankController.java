package org.itinov.bankApp.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.itinov.bankApp.dto.*;
import org.itinov.bankApp.dto.TransferRequest;
import org.itinov.bankApp.mapper.BankAPIMapper;
import org.itinov.bankApp.service.BankService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for bank account operations.
 * All endpoints require the user to be authenticated and have a keycloak 'customer' role.
 */
@PreAuthorize("isAuthenticated() and hasRole('customer')")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/accounts")
public class BankController {

    private final BankService bankService;
    private final BankAPIMapper mapper;

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get all accounts for a customer")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of accounts returned"),
        @ApiResponse(responseCode = "403", description = "Forbidden - not your account"),
        @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<List<AccountDTO>> getAccounts(@PathVariable Long customerId) {
        List<AccountDTO> accounts = mapper.toAccountDTOs(bankService.getAccountsByCustomer(customerId));
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{accountId}/transactions")
    @Operation(summary = "Get all transactions for an account")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of transactions returned"),
        @ApiResponse(responseCode = "403", description = "Forbidden - not your account"),
    })
    public ResponseEntity<List<TransactionDTO>> getTransactions(@PathVariable Long accountId) {
        List<TransactionDTO> transactions = mapper.toTransactionDTOs(bankService.getTransactionsByAccount(accountId));
        return ResponseEntity.ok(transactions);
    }

    @PostMapping("/{accountId}/deposit")
    @Operation(summary = "Deposit money into an account")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Deposit successful"),
        @ApiResponse(responseCode = "403", description = "Forbidden - not your account"),
        @ApiResponse(responseCode = "404", description = "Account not found"),
        @ApiResponse(responseCode = "400", description = "Invalid deposit request")
    })
    public ResponseEntity<TransactionDTO> deposit(@PathVariable Long accountId,
                                                  @Valid @RequestBody DepositRequest request) {
        String performedBy = resolvePerformedBy();
        TransactionDTO tx = mapper.toDTO(bankService.deposit(accountId, request.amount(), performedBy));
        return ResponseEntity.status(HttpStatus.CREATED).body(tx);
    }

    @PostMapping("/{accountId}/withdraw")
    @Operation(summary = "Withdraw money from an account")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Withdrawal successful"),
        @ApiResponse(responseCode = "403", description = "Forbidden - not your account"),
        @ApiResponse(responseCode = "404", description = "Account not found"),
        @ApiResponse(responseCode = "400", description = "Invalid withdrawal request")
    })
    public ResponseEntity<TransactionDTO> withdraw(@PathVariable Long accountId,
                                                   @Valid @RequestBody WithdrawRequest request) {
        String performedBy = resolvePerformedBy();
        TransactionDTO tx = mapper.toDTO(bankService.withdraw(accountId, request.amount(), performedBy));
        return ResponseEntity.status(HttpStatus.CREATED).body(tx);
    }

    @PostMapping("/{accountId}/transfer")
    @Operation(summary = "Transfer money between two accounts")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Transfer successful"),
        @ApiResponse(responseCode = "403", description = "Forbidden - not your account"),
        @ApiResponse(responseCode = "404", description = "One of the accounts not found"),
        @ApiResponse(responseCode = "400", description = "Invalid transfer request")
    })
    public ResponseEntity<List<TransactionDTO>> transfer(@PathVariable Long accountId,
                                                         @Valid @RequestBody TransferRequest request) {
        String performedBy = resolvePerformedBy();
        List<TransactionDTO> tx = mapper
            .toTransactionDTOs(bankService.transfer(accountId, request.toAccountId(), request.amount(), performedBy)
            );
        return ResponseEntity.status(HttpStatus.CREATED).body(tx);
    }

    /**
     * Resolve the username of the authenticated user from the security context.
     * If the authentication is a JWT, it tries to get the 'preferred_username' claim.
     * Otherwise, it falls back to the principal name.
     *
     * @return the username of the authenticated user, or "unknown" if not available
     */
    private String resolvePerformedBy() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            String preferred = jwtAuth.getToken().getClaimAsString("preferred_username");
            return preferred != null ? preferred : jwtAuth.getName();
        }
        return authentication != null ? authentication.getName() : "unknown";
    }

}
