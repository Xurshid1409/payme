package uz.payme.json;

import lombok.*;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaycomRequestForm {

    private String method;
    private Params params;
    private Integer id;
}
