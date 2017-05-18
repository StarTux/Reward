package com.winthier.reward.sql;

import java.sql.Date;
import java.util.List;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "currencies")
@Getter
@Setter
public class Currency {
    @Id Integer id;
    @ManyToOne(optional=false) Reward reward;
    @Column(nullable = false, length = 255) String name;
    @Column(nullable = false) Integer amount;

    public Currency() {}

    public Currency(Reward reward, String name, Integer amount) {
        setReward(reward);
        setName(name);
        setAmount(amount);
    }
}
