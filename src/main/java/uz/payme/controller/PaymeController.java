package uz.payme.controller;

import lombok.RequiredArgsConstructor;
import net.minidev.json.JSONObject;
import org.springframework.web.bind.annotation.*;
import uz.payme.json.PaycomRequestForm;
import uz.payme.service.PaycomService;

@RequestMapping("/api/merchant")
@RestController
@RequiredArgsConstructor
public class PaymeController {

    private final PaycomService ipaycomService;

    @PostMapping
    JSONObject post(@RequestBody PaycomRequestForm requestForm,
                    @RequestHeader("Authorization") String authorization) {
        return ipaycomService.payWithPaycom(requestForm, authorization);
    }
}