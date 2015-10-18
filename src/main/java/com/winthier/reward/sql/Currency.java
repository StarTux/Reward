package com.winthier.reward.sql;

import com.avaje.ebean.annotation.CreatedTimestamp;
import com.avaje.ebean.validation.Length;
import com.avaje.ebean.validation.NotEmpty;
import com.avaje.ebean.validation.NotNull;
import java.sql.Date;
import java.util.List;
import java.util.UUID;
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
    @NotNull @Length(max = 255) String name;
    @NotNull Integer amount;

    public Currency() {}
    
    public Currency(Reward reward, String name, Integer amount) {
        setReward(reward);
        setName(name);
        setAmount(amount);
    }
}
