package net.runelite.client.plugins.realtimetracker.models;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Skill;

@Getter
@Setter
public class Xp {
    private Skill skill;
    private int xp;
}
