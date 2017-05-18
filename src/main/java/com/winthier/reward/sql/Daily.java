package com.winthier.reward.sql;

import java.util.Date;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "dailies",
       uniqueConstraints = @UniqueConstraint(columnNames = {"uuid", "name"}))
@Getter
@Setter
public class Daily {
    @Id Integer id;
    @Version Date version;
    @Column(nullable = false) UUID uuid;
    @Column(nullable = false, length = 255) String name;
    @Column(nullable = false) Date day;

    public Daily() {}

    public Daily(UUID uuid, String name, Date day) {
        setUuid(uuid);
        setName(name);
        setDay(day);
    }
}
