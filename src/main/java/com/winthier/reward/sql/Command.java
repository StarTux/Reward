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
@Table(name = "commands")
@Getter
@Setter
public class Command {
    @Id Integer id;
    @ManyToOne(optional=false) Reward reward;
    @Column(nullable = false, length = 1023) String command;

    public Command() {}

    public Command(Reward reward, String command) {
        setReward(reward);
        setCommand(command);
    }
}
