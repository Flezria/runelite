package net.runelite.client.plugins.realtimetracker.models;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PlayerData {
    private String rsn;
    private List<Xp> xp;
}
