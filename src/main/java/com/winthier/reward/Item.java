package com.winthier.reward;

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

@Entity
@Table(name = "items")
public class Item {
    @Id
    private Integer id;

    @Version
    private Integer version;

    @ManyToOne
    private Reward reward;

    @NotNull
    private Integer lineNumber;

    @Length(max = 255)
    private String line;

    public Item() {}
    public Item(Reward reward, Integer lineNumber, String line) {
        setReward(reward);
        setLineNumber(lineNumber);
        setLine(line);
    }

    public Integer getId() { return this.id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getVersion() { return this.version; }
    public void setVersion(Integer version) { this.version = version; }

    public Reward getReward() { return this.reward; }
    public void setReward(Reward reward) { this.reward = reward; }

    public Integer getLineNumber() { return this.lineNumber; }
    public void setLineNumber(Integer lineNumber) { this.lineNumber = lineNumber; }

    public String getLine() { return this.line; }
    public void setLine(String line) { this.line = line; }
}
