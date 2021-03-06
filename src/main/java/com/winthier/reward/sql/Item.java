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
@Table(name = "items")
@Getter
@Setter
public class Item {
    @Id Integer id;
    @ManyToOne Reward reward;
    @Column(nullable = false) Integer lineNumber;
    @Column(length = 255) String line;

    public Item() {}

    public Item(Reward reward, Integer lineNumber, String line) {
        setReward(reward);
        setLineNumber(lineNumber);
        setLine(line);
    }
}
