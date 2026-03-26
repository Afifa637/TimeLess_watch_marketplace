package com.timeless.app.controller;

import com.timeless.app.dto.request.PaymentRequest;
import com.timeless.app.dto.response.PaymentResponse;
import com.timeless.app.security.UserPrincipal;
import com.timeless.app.service.PaymentService;
import com.timeless.app.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Prototype simulated payments. No real gateway or card data is used.")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Initiate a prototype payment and simulate instant approval")
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Payment simulated successfully",
            content = @Content(
                examples = @ExampleObject(value = "{\"orderId\":1,\"method\":\"CREDIT_CARD\"}")
            )
        )
    })
    public ResponseEntity<PaymentResponse> initiatePayment(@Valid @RequestBody PaymentRequest request) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.initiatePayment(request, user.getId()));
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get payment by order id")
    public ResponseEntity<PaymentResponse> getPaymentByOrder(@PathVariable Long orderId) {
        UserPrincipal user = SecurityUtils.getCurrentUser();
        return ResponseEntity.ok(paymentService.getPaymentByOrder(orderId, user.getId(), user.getRole()));
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Refund a payment as admin")
    public ResponseEntity<PaymentResponse> refundPayment(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.refundPayment(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all payments as admin")
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }
}
