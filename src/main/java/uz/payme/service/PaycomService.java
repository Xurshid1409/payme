package uz.payme.service;

import com.thetransactioncompany.jsonrpc2.JSONRPC2Error;
import com.thetransactioncompany.jsonrpc2.JSONRPC2Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uz.payme.entity.Client;
import uz.payme.entity.Order;
import uz.payme.entity.OrderTransaction;
import uz.payme.entity.Payment;
import uz.payme.entity.enums.TransactionState;
import uz.payme.json.*;
import uz.payme.repository.ClientRepository;
import uz.payme.repository.OrderRepository;
import uz.payme.repository.OrderTransactionRepository;
import uz.payme.repository.PaymentRepository;

import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaycomService implements IPaycomService {
    private final ClientRepository clientRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrderTransactionRepository orderTransactionRepository;
    private final Long TIME_EXPIRED_PAYCOM_ORDER = 43_200_000L;

    @Override
    public JSONObject payWithPaycom(PaycomRequestForm requestForm, String authorization) {

        Params params = requestForm.getParams();
        JSONRPC2Response response = new JSONRPC2Response(params.getId());

        if (authorization == null || checkPaycomUserAuth(authorization, response)) {
            return response.toJSONObject();
        }

        //PAYCOM QAYSI METHODDA KELAYOTGANLIGIGA QARAB ISH BAJARAMIZ
        switch (requestForm.getMethod()) {
            case "CheckPerformTransaction" -> checkPerformTransaction(requestForm, response, authorization);
            case "CreateTransaction" -> createTransaction(requestForm, response, authorization);
            case "PerformTransaction" -> performTransaction(requestForm, response, authorization);
            case "CancelTransaction" -> cancelTransaction(requestForm, response, authorization);
            case "CheckTransaction" -> checkTransaction(requestForm, response, authorization);
            case "GetStatement" -> getStatement(requestForm, response, authorization);
        }
        return response.toJSONObject();
    }

    /**
     * ORDER HAMMA JIHATDAN TO'G'RILIGINI TEKSHIRAMIZ
     * https://developer.help.paycom.uz/ru/metody-merchant-api/checkperformtransaction
     *
     * @param requestForm @RequestBody
     * @param response    JSONRPC2Response
     * @return boolena
     */
    public boolean checkPerformTransaction(PaycomRequestForm requestForm, JSONRPC2Response response, String authorization) {

//        if (!checkPaycomUserAuth(authorization, response)) {
//            return false;
//        }

        //PAYCOMDAN ACOUNT FIELDI KELMASA
        if (requestForm.getParams().getAccount() == null) {
            response.setError(new JSONRPC2Error(
                    -31050,
                    "Account field not found",
                    "account"
            ));
            return false;
        }

        //PAYCOMDAN ACOUNT FIELDI KELMASA
        if (requestForm.getParams().getAccount().getOrder() == null) {
            response.setError(new JSONRPC2Error(
                    -31050,
                    "Order not found",
                    "order"
            ));
            return false;
        }

        //PAYCOMDAN AMOUNT FIELDI NULL YOKI 0 KELSA
        if (requestForm.getParams().getAmount() == null || requestForm.getParams().getAmount() == 0) {
            response.setError(new JSONRPC2Error(
                    -31001,
                    "Amount error or null",
                    "amount"));
            return false;
        }

        //PAYCOM DAN KELGAN ORDER ID ORQALI ORDERNI OLAMIZ
        Optional<Order> optionalOrder = orderRepository.findById(requestForm.getParams().getAccount().getOrder());

        //AGAR ORDER BO'LSA
        if (optionalOrder.isEmpty()) {
            response.setError(new JSONRPC2Error(
                    -31050,
                    "Order not found",
                    "order"));
            return false;
        }

        //ORDER SUM BILAN PAYCOMDAN KELGAN SUM TENGLIGI TEKSHIRILYAPTI
        Order order = optionalOrder.get();
        if (!Objects.equals(order.getOrderSum(), requestForm.getParams().getAmount())) {
            response.setError(new JSONRPC2Error(
                    -31001,
                    "Wrong amount",
                    "amount"));
            return false;
        }

        //ORDER CANCEL BO'LGAN YOKI YO'QLIGINI TEKSHRIAMIZ
        if (order.isCancelled()) {
            response.setError(new JSONRPC2Error(
                    -31099,
                    "Order already cancelled",
                    "order"));
            return false;
        }

        //ORDER ALLAQACHON YAKUNLANAGAN BO'LSA
        if (order.isPaid()) {
            response.setError(new JSONRPC2Error(
                    -31099,
                    "Order already finished",
                    "order"));
            return false;
        }

        response.setResult(new CheckPerformTransactionAllowResponse(
                new AdditionalInfo(order.getId(), order.getOrderSum()),
                true));
        return true;
    }

    /**
     * YANGI TRANSACTION OCHISH UCHUN YOKI ESKISI BO'LSA UNI MUDDATI O'TMAGANLIGINI TEKSHIRAMIZ
     * https://developer.help.paycom.uz/ru/metody-merchant-api/createtransaction
     *
     * @param requestForm @RequestBody
     * @param response    JSONRPC2Response
     */
    private void createTransaction(PaycomRequestForm requestForm, JSONRPC2Response response, String authorization) {

        Optional<OrderTransaction> transactionOptional = orderTransactionRepository.findByOrderId(requestForm.getParams().getAccount().getOrder());
        if (transactionOptional.isPresent()) {
            OrderTransaction orderTransaction = transactionOptional.get();
            if (!Objects.equals(orderTransaction.getTransactionId(), requestForm.getParams().getId())) {
                response.setError(new JSONRPC2Error(
                        -31050,
                        "Unable to complete operation",
                        "transaction"));
                return;
            }
        }

        //PAYCOM DAN KELGAN ID BO'YICHA TRASACTION OLYAPMIZ
        Optional<OrderTransaction> transactionId = orderTransactionRepository.findByTransactionId(requestForm.getParams().getId());
        OrderTransaction orderTransaction;

        //AGAR OrderTransaction AVVAL YARATILGAN BO'LSA
        if (transactionId.isPresent()) {
            orderTransaction = transactionId.get();
            //OrderTransaction STATE IN PROGRESS DA BO'LMASA XATO QAYTARAMIZ
            if (!orderTransaction.getState().equals(TransactionState.STATE_IN_PROGRESS.getCode())) {
                response.setError(new JSONRPC2Error(
                        -31008,
                        "Unable to complete operation",
                        "transaction"));
                return;
            }
            //OrderTransaction YARATILGAN VAQTI 12 SOATDAN  KO'P BO'LSA XATO QAYTARAMIZ. MUDDATI O'TGAN ORDER
            if (System.currentTimeMillis() - orderTransaction.getTransactionCreationTime().getTime() > TIME_EXPIRED_PAYCOM_ORDER) {
                response.setError(new JSONRPC2Error(
                        -31008,
                        "Unable to complete operation",
                        "transaction"));

                //ORDER_TRANSACTION NI O'ZGARTIRIB SAQLAB QO'YAMIZ
                orderTransaction.setReason(4);
                orderTransaction.setState(TransactionState.STATE_CANCELED.getCode());
                orderTransactionRepository.save(orderTransaction);
                return;
            }
            response.setResult(new ResultForm(
                    orderTransaction.getTransactionCreationTime().getTime(),
                    orderTransaction.getState(),
                    orderTransaction.getId().toString()));
        }

        //OrderTransaction YARATILMAGAN BO'LSA
        else {
            //ORDER HAMMA JIHATDAN TO'G'RILIGINI TEKSHIRAMIZ
            boolean checkPerformTransaction = checkPerformTransaction(requestForm, response, authorization);

//            if (!checkPaycomUserAuth(authorization, response)) {
//                return;
//            }

            //AGAR ORDER XATO BO'LSA XATONI YUBORAMIZ
            if (!checkPerformTransaction) {
                return;
            }
            //YANGI OrderTransaction
            orderTransaction = new OrderTransaction(
                    requestForm.getParams().getId(),
                    new Timestamp(requestForm.getParams().getTime()),
                    TransactionState.STATE_IN_PROGRESS.getCode(),
                    requestForm.getParams().getAccount().getOrder());
            orderTransactionRepository.save(orderTransaction);

        }

        //AVVAL SAQLANGAN MUDDATI O'TMAGAN OrderTransaction YOKI YANGI SAQLANGAN OrderTransaction NING MA'LUMOTLARI QAYTARILYAPTI
        response.setResult(new ResultForm(
                orderTransaction.getTransactionCreationTime().getTime(),
                orderTransaction.getState(),
                orderTransaction.getId().toString()));

    }

    /**
     * TO'LOVNI AMALGA OSHIRADIGAN METHOD
     * https://developer.help.paycom.uz/ru/metody-merchant-api/performtransaction
     *
     * @param requestForm @RequestBody
     * @param response    JSONRPC2Response
     */

    private void performTransaction(PaycomRequestForm requestForm, JSONRPC2Response response, String authorization) {

//        if (authorization == null || !checkPaycomUserAuth(authorization, response)) {
//            return;
//        }

        //PAYCOM DAN KELGAN ID BO'YICHA OrderTransaction NI QIDIRAMIZ
        Optional<OrderTransaction> optionalOrderTransaction = orderTransactionRepository.findByTransactionId(requestForm.getParams().getId());
        if (optionalOrderTransaction.isEmpty()) {
            response.setError(new JSONRPC2Error(
                    -31003,
                    "Order transaction not found",
                    "transaction"));
            return;
        }

        OrderTransaction orderTransaction = optionalOrderTransaction.get();
        //OrderTransaction NING STATE IN_PROGRESS(1) BO'LSA
        if (orderTransaction.getState().equals(TransactionState.STATE_IN_PROGRESS.getCode())) {
            //OrderTransaction YARATILGAN VAQTI 12 SOATDAN  KO'P BO'LSA XATO QAYTARAMIZ. MUDDATI O'TGAN ORDER
            if (System.currentTimeMillis() - orderTransaction.getTransactionCreationTime().getTime() > TIME_EXPIRED_PAYCOM_ORDER) {
                response.setError(new JSONRPC2Error(
                        -31008,
                        "Unable to complete operation",
                        "transaction"));

                //ORDER_TRANSACTION NI O'ZGARTIRIB SAQLAB QO'YAMIZ
                orderTransaction.setReason(4);
                orderTransaction.setState(TransactionState.STATE_CANCELED.getCode());
                orderTransactionRepository.save(orderTransaction);
                return;
            }
            orderTransaction.setState(TransactionState.STATE_DONE.getCode());
            orderTransaction.setPerformTime(new Timestamp(System.currentTimeMillis()));
            OrderTransaction save = orderTransactionRepository.save(orderTransaction);
            //TO'LOV BO'LDI
            Order order = orderRepository.findById(orderTransaction.getOrderId()).get();
            Client client = clientRepository.findById(order.getClient().getId()).get();
            Payment payment = new Payment(
                    client,
                    (double) order.getOrderSum(),
                    new Timestamp(System.currentTimeMillis()),
                    orderTransaction.getId(),
                    orderTransaction.getTransactionId());
            paymentRepository.save(payment);
            order.setPaid(true);
            orderRepository.save(order);
            response.setResult(new ResultForm(save));
            return;
        }

        //OrderTransaction GA TO'LOV QILINIB YAKUNIGA YETGAN BO'LSA
        if (orderTransaction.getState().equals(TransactionState.STATE_DONE.getCode())) {
            response.setResult(new ResultForm(orderTransaction));
            return;
        }

        //OrderTransaction NING STATE DONE(2) BO'LMASA XATOLIK BERAMIZ
        response.setError(new JSONRPC2Error(
                -31008,
                "Unable to complete operation",
                "transaction"));
    }

    /**
     * TRANSACTION NI BEKOR QILISH UCHUN METHOD
     * https://developer.help.paycom.uz/ru/metody-merchant-api/canceltransaction
     *
     * @param requestForm @RequestBody
     * @param response    JSONRPC2Response
     */
    private void cancelTransaction(PaycomRequestForm requestForm, JSONRPC2Response response, String authorization) {

//        if (!checkPaycomUserAuth(authorization, response)) {
//            return;
//        }

        Optional<OrderTransaction> transactionId = orderTransactionRepository.findByTransactionId(requestForm.getParams().getId());
        if (transactionId.isEmpty()) {
            response.setError(new JSONRPC2Error(
                    -31003,
                    "Order transaction not found",
                    "transaction"));
            return;
        }
        OrderTransaction orderTransaction = transactionId.get();
        if (orderTransaction.getState().equals(TransactionState.STATE_IN_PROGRESS.getCode())) {
            orderTransaction.setState(TransactionState.STATE_CANCELED.getCode());
            orderTransaction.setReason(requestForm.getParams().getReason());
            orderTransaction.setCancelTime(new Timestamp(System.currentTimeMillis()));
            OrderTransaction orderTransactionSave = orderTransactionRepository.save(orderTransaction);
            response.setResult(new ResultForm(
                    orderTransactionSave.getTransactionCreationTime().getTime(),
                    orderTransactionSave.getPerformTime() != null ? orderTransaction.getPerformTime().getTime() : 0,
                    orderTransactionSave.getCancelTime() != null ? orderTransaction.getCancelTime().getTime() : 0,
                    orderTransactionSave.getReason(),
                    orderTransactionSave.getState(),
                    orderTransactionSave.getId().toString()));
            return;
        }

        if (orderTransaction.getState().equals(TransactionState.STATE_DONE.getCode())) {
            Order order = orderRepository.findById(orderTransaction.getOrderId()).get();
//            Order order = orderTransaction.getOrder();
            if (!order.isDelivered()) {
                Optional<Payment> optionalPayment = paymentRepository.findFirstByOrderTransactionIdOrderByPayDateDesc(orderTransaction.getId());
                if (optionalPayment.isPresent()) {
                    Payment payment = optionalPayment.get();
                    payment.setCancelled(true);
                    paymentRepository.save(payment);
                    orderTransaction.setState(TransactionState.STATE_POST_CANCELED.getCode());
                    orderTransaction.setReason(requestForm.getParams().getReason());
                    orderTransaction.setCancelTime(new Timestamp(System.currentTimeMillis()));
                    orderTransactionRepository.save(orderTransaction);
                    response.setResult(new ResultForm(
                            orderTransaction.getTransactionCreationTime().getTime(),
                            orderTransaction.getPerformTime() != null ? orderTransaction.getPerformTime().getTime() : 0,
                            orderTransaction.getCancelTime() != null ? orderTransaction.getCancelTime().getTime() : 0,
                            orderTransaction.getReason(),
                            orderTransaction.getState(),
                            orderTransaction.getId().toString()));
                    return;
                } else {
                    response.setError(new JSONRPC2Error(
                            -31007,
                            "Unable to cancel transaction",
                            "transaction"));
                    return;
                }
            } else {
                response.setError(new JSONRPC2Error(
                        -31007,
                        "Order already delivered",
                        "order"));
                return;
            }
        }
        response.setResult(new ResultForm(
                orderTransaction.getTransactionCreationTime().getTime(),
                orderTransaction.getPerformTime() != null ? orderTransaction.getPerformTime().getTime() : 0,
                orderTransaction.getCancelTime() != null ? orderTransaction.getCancelTime().getTime() : 0,
                orderTransaction.getReason(),
                orderTransaction.getState(),
                orderTransaction.getId().toString()));
    }

    /**
     * TRANSACTION HOLATINI BILISH UCHUN METHOD
     * https://developer.help.paycom.uz/ru/metody-merchant-api/checktransaction
     *
     * @param requestForm @RequestBody
     * @param response    JSONRPC2Response
     */
    private void checkTransaction(PaycomRequestForm requestForm, JSONRPC2Response response, String authorization) {

//        if (!checkPaycomUserAuth(authorization, response)) {
//            return;
//        }

        Optional<OrderTransaction> transactionId = orderTransactionRepository.findByTransactionId(requestForm.getParams().getId());
        if (transactionId.isEmpty()) {
            response.setError(new JSONRPC2Error(
                    -31003,
                    "Order transaction not found",
                    "transaction"));
            return;
        }
        OrderTransaction orderTransaction = transactionId.get();
        response.setResult(new ResultForm(
                orderTransaction.getTransactionCreationTime().getTime(),
                orderTransaction.getPerformTime() != null ? orderTransaction.getPerformTime().getTime() : 0,
                orderTransaction.getCancelTime() != null ? orderTransaction.getCancelTime().getTime() : 0,
                orderTransaction.getReason(),
                orderTransaction.getState(),
                orderTransaction.getId().toString()));
    }

    /**
     * PAYCOM TOMONIDAN MUVAFFAQIYATLI BAJRILGAN BARCHA OrderTransaction LARNI QAYTARAMIZ
     * https://developer.help.paycom.uz/ru/metody-merchant-api/getstatement
     *
     * @param requestForm @RequestBody
     * @param response    JSONRPC2Response
     */

    private void getStatement(PaycomRequestForm requestForm, JSONRPC2Response response, String authorization) {

//        if (!checkPaycomUserAuth(authorization, response)) {
//            return;
//        }

        //DB DAN PAYCOM BERGAN VAQT OALIG'IDA TRANSACTION STATE DONE(2) BO'LGAN OrderTransaction LAR OLINADI
        List<OrderTransaction> orderTransactionList = orderTransactionRepository.findAllByStateAndTransactionCreationTimeBetween(TransactionState.STATE_DONE.getCode(),
                new Timestamp(requestForm.getParams().getFrom()), new Timestamp(requestForm.getParams().getTo()));
        List<Transaction> transactions = new ArrayList<>();

        //OrderTransaction LARDAN Transaction OBJECTIGA MAP QILINADI
        for (OrderTransaction orderTransaction : orderTransactionList) {
            Transaction transaction = new Transaction(
                    orderTransaction.getTransactionId(),
                    new Account(orderTransaction.getOrderId()),
                    orderTransaction.getOrder().getOrderSum(),
                    0L,
                    orderTransaction.getTransactionCreationTime().getTime(),
                    orderTransaction.getPerformTime().getTime(),
                    null,
                    orderTransaction.getState(),
                    orderTransaction.getTransactionCreationTime().getTime(),
                    orderTransaction.getId().toString());
            transactions.add(transaction);
        }
        //PAYCOMGA Transaction LISTI YUBORILADI
        response.setResult(transactions);
    }

    /**
     * PAYCOM DAN KELGAN BASIC AUTHNI TEKSHIRAMIZ
     *
     * @param basicAuth String
     * @param response  JSONRPC2Response
     * @return boolean
     */
    private boolean checkPaycomUserAuth(String basicAuth, JSONRPC2Response response) {

        basicAuth = basicAuth.substring("Basic".length()).trim();
        byte[] decode = Base64.getDecoder().decode(basicAuth);
        basicAuth = new String(decode, Charset.defaultCharset());
        String[] split = basicAuth.split(":", 2);
        Optional<Client> optionalClient = clientRepository.findByPhoneNumber("Paycom");
        if (optionalClient.isPresent()) {
            Client client = optionalClient.get();
            if (passwordEncoder.matches(split[1], client.getPassword())) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(client, null, new ArrayList<>());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        response.setError(new JSONRPC2Error(-32504,
                "Error authentication",
                "auth"));
        return false;
    }
}
