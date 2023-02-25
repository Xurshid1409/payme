package uz.payme.controller;

import lombok.RequiredArgsConstructor;
import net.minidev.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.payme.json.PaycomRequestForm;
import uz.payme.service.PaycomService;

@RequestMapping("/api/merchant")
@RestController
@RequiredArgsConstructor
public class PaymeController {

    private final PaycomService ipaycomService;

    @PostMapping
    ResponseEntity<JSONObject> post(@RequestBody PaycomRequestForm requestForm,
                                    @RequestHeader(defaultValue = "Authorization") String authorization) {
        return ResponseEntity.ok(ipaycomService.payWithPaycom(requestForm, authorization));
    }
}