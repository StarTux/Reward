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
import javax.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "items")
@Getter
@Setter
public class Item {
    @Id Integer id;
    @Version Integer version;
    @ManyToOne Reward reward;
    @NotNull Integer lineNumber;
    @Length(max = 255) String line;

    public Item() {}
    
    public Item(Reward reward, Integer lineNumber, String line) {
        setReward(reward);
        setLineNumber(lineNumber);
        setLine(line);
    }
}
