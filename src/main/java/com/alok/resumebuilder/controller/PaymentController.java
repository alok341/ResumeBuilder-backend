package com.alok.resumebuilder.controller;

import com.alok.resumebuilder.Document.Payment;
import com.alok.resumebuilder.service.PaymentService;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.alok.resumebuilder.util.AppConstants.*;

@RestController
@RequiredArgsConstructor
@RequestMapping(PAYMENT)
@Slf4j
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping(CREATE_ORDER)
    public ResponseEntity<?> createOrder(@RequestBody Map<String,String> request, Authentication authentication) throws RazorpayException {
        log.info("Inside PaymentController : createOrder() {}", request);
        String planType = request.get("planType");
        if(!PREMIUM.equalsIgnoreCase(planType)){
            return ResponseEntity.badRequest().body(Map.of("message","Invalid plan type"));
        }
        Payment payment = paymentService.createOrder(authentication.getName(), planType);
        Map<String,Object> response = Map.of(
                "orderId", payment.getRazorpayOrderId(),
                "amount", payment.getAmount(),
                "currency", payment.getCurrency(),
                "receipt", payment.getReceipt()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping(VERIFY)
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String,String> request) throws RazorpayException {
        log.info("Inside PaymentController : verifyPayment() {}", request);
        String razorpayOrderId = request.get("razorpay_order_Id");
        String razorpayPaymentId = request.get("razorpay_payment_Id");
        String razorpaySignature = request.get("razorpay_signature");
        if(Objects.isNull(razorpayOrderId) || Objects.isNull(razorpayPaymentId) || Objects.isNull(razorpaySignature)) {
            return ResponseEntity.badRequest().body(Map.of("message","Missing required payment details"));
        }
        boolean isPaymentValid = paymentService.verifyPayment(razorpayOrderId, razorpayPaymentId, razorpaySignature);
        if(!isPaymentValid) {
            return ResponseEntity.badRequest().body(Map.of("message","Payment verification failed"));
        }
        else {
            return ResponseEntity.ok(Map.of("message", "Payment verified successfully",
                    "status", "success"));
        }
    }

    @GetMapping(HISTORY)
    public ResponseEntity<?> getPaymentHistory(Authentication authentication) {
        log.info("Inside PaymentController : getPaymentHistory() {}", authentication.getName());
        List<Payment> payments = paymentService.getUserPayments(authentication.getName());

        return ResponseEntity.ok(payments);
    }
    @GetMapping(ORDER_DETAILS)
    public ResponseEntity<?> getOrderDetails(@PathVariable String orderId) {
        log.info("Inside PaymentController : getOrderDetails() {}", orderId);
        Payment payment = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(payment);
    }
}
