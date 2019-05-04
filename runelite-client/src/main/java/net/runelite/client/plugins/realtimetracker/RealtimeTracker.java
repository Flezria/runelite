package net.runelite.client.plugins.realtimetracker;

import com.google.gson.Gson;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.realtimetracker.models.Xp;
import net.runelite.client.plugins.realtimetracker.models.PlayerData;
import net.runelite.http.api.RuneLiteAPI;
import okhttp3.*;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;


@PluginDescriptor(
        name = "OSRS Realtime Tracker",
        description = "XP Tracking in realtime",
        loadWhenOutdated = true,
        enabledByDefault = false
)
@Slf4j
public class RealtimeTracker extends Plugin {
    @Inject
    private Client client;

    @Inject
    private Gson gson;

    @Inject
    private RealtimeTrackerConfig config;

    private PlayerData playerData;

    private boolean initDataCapture;

    private long lastAmountOfXp;

    private int amountOfTicksRealtimeData;
    private int amountOfTicksDayData;
    private int amountOfTicksHistoricData;

    @Provides
    RealtimeTrackerConfig provideConfig(ConfigManager configManager) { return configManager.getConfig(RealtimeTrackerConfig.class); }

    @Override
    protected void startUp() {
        playerData = new PlayerData();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged)
    {
        GameState state = gameStateChanged.getGameState();
        if (state == GameState.LOGGED_IN)
        {
            System.out.println("LOGGED_IN");
            System.out.println("APIKEY: " + config.apiKey());
            initDataCapture = true;
        }
        else if (state == GameState.LOGIN_SCREEN)
        {
            if (client.getLocalPlayer().getName() == null)
            {
                return;
            }

            initDataCapture = true;
        }
    }


    @Subscribe
    public void onGameTick(GameTick gameTick) {
        if(config.apiKey().equals("")) {
            return;
        }

        if(initDataCapture) {
            if(playerData.getRsn() == null) {
                playerData.setRsn(client.getLocalPlayer().getName());
            }

            for(int skillXp : client.getSkillExperiences()) {
                lastAmountOfXp = lastAmountOfXp + skillXp;
            }

            playerData.setXp(getSkills());
            updateXp(gson.toJson(playerData), "daydata");
            updateXp(gson.toJson(playerData), "historicdata");

            initDataCapture = false;
        }

        amountOfTicksRealtimeData++;
        amountOfTicksDayData++;
        amountOfTicksHistoricData++;

        if(amountOfTicksRealtimeData == 15) {
            int currentSumOfXp = 0;
            for(int xp : client.getSkillExperiences()) {
                currentSumOfXp = currentSumOfXp + xp;
            }

            if(currentSumOfXp <= lastAmountOfXp) {
                amountOfTicksRealtimeData = 0;
                return;
            }

            lastAmountOfXp = currentSumOfXp;

            if(playerData.getRsn() == null) {
                playerData.setRsn(client.getLocalPlayer().getName());
            }

            playerData.setXp(getSkills());

            realtimeXp(gson.toJson(playerData));

            amountOfTicksRealtimeData = 0;
        }

        if(amountOfTicksDayData == 1000) {
            if(playerData.getRsn() == null) {
                playerData.setRsn(client.getLocalPlayer().getName());
            }

            playerData.setXp(getSkills());

            updateXp(gson.toJson(playerData), "daydata");

            amountOfTicksDayData = 0;
        }

        if(amountOfTicksHistoricData == 6000) {
            if(playerData.getRsn() == null) {
                playerData.setRsn(client.getLocalPlayer().getName());
            }

            playerData.setXp(getSkills());

            log.debug("Historic data");
            //updateXp(gson.toJson(playerData), "historicdata");

            amountOfTicksHistoricData = 0;
        }
    }

    private List<Xp> getSkills() {
        List<Xp> allXp = new ArrayList<>();
        allXp.add(addSkill(Skill.ATTACK));
        allXp.add(addSkill(Skill.DEFENCE));
        allXp.add(addSkill(Skill.STRENGTH));
        allXp.add(addSkill(Skill.HITPOINTS));
        allXp.add(addSkill(Skill.RANGED));
        allXp.add(addSkill(Skill.PRAYER));
        allXp.add(addSkill(Skill.MAGIC));
        allXp.add(addSkill(Skill.COOKING));
        allXp.add(addSkill(Skill.WOODCUTTING));
        allXp.add(addSkill(Skill.FLETCHING));
        allXp.add(addSkill(Skill.FISHING));
        allXp.add(addSkill(Skill.FIREMAKING));
        allXp.add(addSkill(Skill.CRAFTING));
        allXp.add(addSkill(Skill.SMITHING));
        allXp.add(addSkill(Skill.MINING));
        allXp.add(addSkill(Skill.HERBLORE));
        allXp.add(addSkill(Skill.AGILITY));
        allXp.add(addSkill(Skill.THIEVING));
        allXp.add(addSkill(Skill.SLAYER));
        allXp.add(addSkill(Skill.FARMING));
        allXp.add(addSkill(Skill.RUNECRAFT));
        allXp.add(addSkill(Skill.HUNTER));
        allXp.add(addSkill(Skill.CONSTRUCTION));

        return allXp;
    }

    private Xp addSkill(Skill skill) {
        Xp xp = new Xp();
        xp.setSkill(skill);
        xp.setXp(client.getSkillExperience(skill));

        return xp;
    }

    private void realtimeXp(String jsonData) {
        final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        OkHttpClient httpClient = RuneLiteAPI.CLIENT;

        HttpUrl httpUrl = new HttpUrl.Builder()
                .scheme("http")
                .host("localhost").port(8080)
                .addPathSegment("api")
                .addPathSegment("restricted")
                .addPathSegment("socket")
                .addPathSegment("update")
                .build();

        RequestBody body = RequestBody.create(JSON, jsonData);

        Request request = new Request.Builder()
                .header("User-Agent", "RuneLite")
                .header("Authorization", "Bearer " + config.apiKey())
                .url(httpUrl)
                .method("POST", body)
                .build();

        httpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.error("Error updating XP", e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {
                log.debug("XP Update success");
                response.close();
            }
        });
    }

    private void updateXp(String jsonData, String placeData) {
        final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        OkHttpClient httpClient = RuneLiteAPI.CLIENT;

        HttpUrl httpUrl = new HttpUrl.Builder()
                .scheme("http")
                .host("localhost").port(8080)
                .addPathSegment("api")
                .addPathSegment("restricted")
                .addPathSegment(placeData)
                .build();

        RequestBody body = RequestBody.create(JSON, jsonData);

        Request request = new Request.Builder()
                .header("User-Agent", "RuneLite")
                .header("Authorization", "Bearer " + config.apiKey())
                .url(httpUrl)
                .method("POST", body)
                .build();

        httpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.error("Error updating XP", e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {
                log.debug("XP Update success");
                response.close();
            }
        });


    }

}
