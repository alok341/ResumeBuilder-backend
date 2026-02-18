package com.alok.resumebuilder.service;

import com.alok.resumebuilder.Document.Payment;
import com.alok.resumebuilder.Document.User;
import com.alok.resumebuilder.Dto.AuthResponse;
import com.alok.resumebuilder.repository.PaymentRepository;
import com.alok.resumebuilder.repository.UserRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static com.alok.resumebuilder.util.AppConstants.PREMIUM;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AuthService authService;
    private final UserRepository userRepository;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;
    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    public Payment createOrder(String name, String planType) throws RazorpayException {
        log.info("Inside PaymentService : createOrder() {}, {}", name, planType);
        AuthResponse response = authService.getProfile(name);
        RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
        int amount = 99900;
        String currency = "INR";
        String receipt = PREMIUM+"_"+ UUID.randomUUID().toString().substring(0,8);

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amount);
        orderRequest.put("currency", currency);
        orderRequest.put("receipt", receipt);

        Order razprpayOrder = razorpayClient.orders.create(orderRequest);
        Payment newPayment = Payment.builder()
                .userId(response.getId())
                .razorpayOrderId(razprpayOrder.get("id"))
                .amount(amount)
                .currency(currency)
                .planType(planType)
                .status("created")
                .receipt(receipt)
                .build();
        log.info("Razorpay order created successfully for user {} with order id {}", name, razprpayOrder.get("id"));

        return paymentRepository.save(newPayment);
    }

    public boolean verifyPayment(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) throws RazorpayException {
        try{
            JSONObject attribute = new JSONObject();
            attribute.put("razorpay_order_id", razorpayOrderId);
            attribute.put("razorpay_payment_id", razorpayPaymentId);
            attribute.put("razorpay_signature", razorpaySignature);
            boolean isValidSignature = Utils.verifyPaymentSignature(attribute, razorpayKeySecret);
            log.info("Payment signature verification result for order id {} : {}", razorpayOrderId, isValidSignature);
            if (isValidSignature) {
                // Update payment record in database
                Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                        .orElseThrow(() -> new RuntimeException("Payment record not found for order id " + razorpayOrderId));
                payment.setRazorpayPaymentId(razorpayPaymentId);
                payment.setRazorpaySignature(razorpaySignature);
                payment.setStatus("paid");
                paymentRepository.save(payment);

                // Update user's subscription plan
                updateUserSubscriptionPlan(payment.getUserId(), payment.getPlanType());
                log.info("Payment record updated to paid for order id {}", razorpayOrderId);
                return  true;
            }
            return false;

        }
        catch (Exception e){
            log.error("Error verifying payment for order id {} : {}", razorpayOrderId, e.getMessage());
            return false;
        }
    }

    private void updateUserSubscriptionPlan(String userId, String planType) {
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new RuntimeException("User not found with id " + userId));
        user.setSubscriptionPlan(planType);
        userRepository.save(user);
        log.info("User subscription plan updated to {} for user id {}", planType, userId);

    }

    public List<Payment> getUserPayments(String name) {
        log.info("Inside PaymentService : getUserPayments() {}", name);
        AuthResponse response = authService.getProfile(name);
        List<Payment> payments = paymentRepository.findByUserIdOrderByCreatedAtDesc(response.getId());
        return payments;
    }

    public Payment getPaymentByOrderId(String orderId) {
        log.info("Inside PaymentService : getPaymentByOrderId() {}", orderId);
        Payment payment = paymentRepository.findByRazorpayOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for order id " + orderId));
        return payment;
    }
}
