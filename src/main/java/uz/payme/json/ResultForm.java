package uz.payme.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import uz.payme.entity.OrderTransaction;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
//@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResultForm {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long id;
    @JsonProperty(value = "cancel_time")
    private Long cancelTime;
    @JsonProperty(value = "create_time")
    private Long createTime;
    @JsonProperty(value = "perform_time")
    private Long performTime;
    private Integer reason;
    private Integer state;
    private String transaction;


    //CREATE TRANACTION DA KETADI BIZDAN
    public ResultForm(Long createTime, Integer state, String transaction) {
        this.createTime = createTime;
        this.state = state;
        this.transaction = transaction;
    }

    public ResultForm(Long createTime, Long performTime, Long cancelTime, Integer reason, Integer state, String transaction) {
        this.createTime = createTime;
        this.performTime = performTime;
        this.cancelTime = cancelTime;
        this.reason = reason;
        this.state = state;
        this.transaction = transaction;
    }

    //Perform transaction response
    public ResultForm(OrderTransaction orderTransaction){
        this.createTime = orderTransaction.getTransactionCreationTime().getTime();
        this.performTime = orderTransaction.getPerformTime().getTime();
        this.reason = orderTransaction.getReason();
        this.state = orderTransaction.getState();
        this.transaction = orderTransaction.getId().toString();
    }
}
