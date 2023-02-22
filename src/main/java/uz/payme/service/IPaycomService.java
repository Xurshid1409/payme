package uz.payme.service;


import net.minidev.json.JSONObject;
import org.springframework.stereotype.Service;
import uz.payme.json.PaycomRequestForm;

@Service
public interface IPaycomService {

    JSONObject payWithPaycom(PaycomRequestForm requestForm);


}
