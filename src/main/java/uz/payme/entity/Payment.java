package uz.payme.entity;

import lombok.*;
import jakarta.persistence.*;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Client client;

    //USHBU TO'LOVDAN QANCHA TISHGANI
    @Column(nullable = false)
    private Double paySum;

    private Timestamp payDate = new Timestamp(System.currentTimeMillis());

    private Long orderTransactionId;
    private String transactionId;
    private Boolean cancelled = false;

    public Payment(Client client, Double paySum, Timestamp payDate, Long orderTransactionId, String transactionId) {
        this.client = client;
        this.paySum = paySum;
        this.payDate = payDate;
        this.orderTransactionId = orderTransactionId;
        this.transactionId = transactionId;
    }
}
