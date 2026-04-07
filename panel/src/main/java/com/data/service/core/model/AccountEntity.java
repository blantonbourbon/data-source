package com.data.service.core.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;

@Entity
@Immutable
@Subselect("SELECT MIN(t.id) as id, t.counterparty as account_name, SUM(t.amount) as balance FROM trade t GROUP BY t.counterparty")

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountEntity {

    @Id

    private Long id;

    private String accountName;

    private Double balance;

}
