package velmir.weathershower;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WeatherDisplayMod implements ClientModInitializer {
    public static final String MOD_ID = "weathershower";
    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Identifier DAY_TEXTURE = Identifier.of(MOD_ID, "textures/gui/weather/day.png");
    public static final Identifier NIGHT_TEXTURE = Identifier.of(MOD_ID, "textures/gui/weather/night.png");
    public static final Identifier RAIN_TEXTURE = Identifier.of(MOD_ID, "textures/gui/weather/rain.png");
    public static final Identifier STORM_TEXTURE = Identifier.of(MOD_ID, "textures/gui/weather/storm.png");
    public static final Identifier MORNING_TEXTURE = Identifier.of(MOD_ID, "textures/gui/weather/morning.png");
    public static final Identifier EVENING_TEXTURE = Identifier.of(MOD_ID, "textures/gui/weather/evening.png");
    public static final Identifier SLEEP_TEXTURE = Identifier.of(MOD_ID, "textures/gui/sleep_icon.png");
    public static final Identifier FALLBACK_TEXTURE = Identifier.ofVanilla("textures/gui/icons.png");

    private static WeatherType currentWeather = WeatherType.DAY;
    private static int updateTimer = 0;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Weather Display Mod");
        checkTextures();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world != null && client.player != null) {
                updateTimer++;
                if (updateTimer >= 20) {
                    updateWeather(client.world);
                    updateTimer = 0;
                }
            }
        });

        HudRenderCallback.EVENT.register(this::renderWeatherHud);
    }

    private void checkTextures() {
        for (WeatherType weather : WeatherType.values()) {
            LOGGER.info("Checking texture: {}", weather.getTexture().toString());
        }
        LOGGER.info("Checking sleep texture: {}", SLEEP_TEXTURE.toString());
    }

    private void updateWeather(World world) {
        long timeOfDay = world.getTimeOfDay() % 24000;
        boolean isRaining = world.isRaining();
        boolean isThundering = world.isThundering();

        LOGGER.info("Time of day: {}", timeOfDay);

        WeatherType newWeather;

        if (isThundering) {
            newWeather = WeatherType.STORM;
        } else if (isRaining) {
            newWeather = WeatherType.RAIN;
        } else if (timeOfDay >= 0 && timeOfDay < 3000) {
            newWeather = WeatherType.MORNING;
        } else if (timeOfDay >= 3000 && timeOfDay < 11500) {
            newWeather = WeatherType.DAY;
        } else if (timeOfDay >= 11500 && timeOfDay < 13000) {
            newWeather = WeatherType.EVENING;
        } else {
            newWeather = WeatherType.NIGHT;
        }

        if (currentWeather != newWeather) {
            currentWeather = newWeather;
            LOGGER.debug("Weather changed to: {}", currentWeather.getTranslationKey());
        }
    }

    private boolean canSleep(World world) {
        if (world == null) return false;
        long timeOfDay = world.getTimeOfDay() % 24000;
        boolean canSleepTime = (timeOfDay >= 12510 && timeOfDay <= 23500) || world.isThundering();


        return canSleepTime;
    }

    private void renderWeatherHud(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;

        int screenWidth = client.getWindow().getScaledWidth();
        int iconSize = 32;
        int x = screenWidth - iconSize - 10;
        int y = 10;


        context.drawTexture(
                RenderLayer::getGuiTextured,
                currentWeather.getTexture(),
                x, y,
                0, 0,
                iconSize, iconSize,
                iconSize, iconSize
        );

        // Відображення тексту погоди
        Text weatherText = Text.translatable(currentWeather.getTranslationKey());
        int textWidth = client.textRenderer.getWidth(weatherText);
        int textX = x + (iconSize - textWidth) / 2;
        int textY = y + iconSize + 5;

        context.drawText(client.textRenderer, weatherText, textX + 1, textY + 1, 0x000000, false);
        context.drawText(client.textRenderer, weatherText, textX, textY, 0xFFFFFF, false);

        // Перевірка та відображення іконки сну
        if (canSleep(client.world)) {
            renderSleepIcon(context, client);
        }
    }

    private void renderSleepIcon(DrawContext context, MinecraftClient client) {
        int screenWidth = client.getWindow().getScaledWidth();
        int iconSize = 32;
        int weatherIconX = screenWidth - iconSize - 10;
        int weatherIconY = 10;

        // Отримуємо позицію тексту погоди
        Text weatherText = Text.translatable(currentWeather.getTranslationKey());
        int textWidth = client.textRenderer.getWidth(weatherText);
        int textX = weatherIconX + (iconSize - textWidth) / 2;
        int textY = weatherIconY + iconSize + 5;

        // Налаштування іконки сну - під текстом погоди по центру
        int sleepIconSize = 16;
        int sleepX = weatherIconX + (iconSize - sleepIconSize) / 2; // Центруємо відносно іконки погоди
        int sleepY = textY + client.textRenderer.fontHeight + 3; // Під текстом погоди з відступом


        try {
            // Варіант 1: Якщо текстура sleep_icon.png має розмір 16x16
            context.drawTexture(
                    RenderLayer::getGuiTextured,
                    SLEEP_TEXTURE,
                    sleepX, sleepY,
                    0, 0,
                    sleepIconSize, sleepIconSize,
                    16, 16  // Змінено з 64x64 на 16x16
            );
        } catch (Exception e) {
            // Варіант 2: Якщо основна текстура не працює, використовуємо fallback
            LOGGER.warn("Failed to render sleep icon, using fallback: {}", e.getMessage());

            // Малюємо простий квадрат як fallback
            context.fill(sleepX, sleepY, sleepX + sleepIconSize, sleepY + sleepIconSize, 0xFFFFFFFF);
            context.fill(sleepX + 1, sleepY + 1, sleepX + sleepIconSize - 1, sleepY + sleepIconSize - 1, 0xFF000080);
        }
    }

    public enum WeatherType {
        DAY(DAY_TEXTURE, "weathershower.weather.day", 0xFFFFD700),
        NIGHT(NIGHT_TEXTURE, "weathershower.weather.night", 0xFF191970),
        RAIN(RAIN_TEXTURE, "weathershower.weather.rain", 0xFF4682B4),
        STORM(STORM_TEXTURE, "weathershower.weather.storm", 0xFF2F4F4F),
        MORNING(MORNING_TEXTURE, "weathershower.weather.morning", 0xFFFFB6C1),
        EVENING(EVENING_TEXTURE, "weathershower.weather.evening", 0xFFFF8C00);

        private final Identifier texture;
        private final String translationKey;
        private final int fallbackColor;

        WeatherType(Identifier texture, String translationKey, int fallbackColor) {
            this.texture = texture;
            this.translationKey = translationKey;
            this.fallbackColor = fallbackColor;
        }

        public Identifier getTexture() {
            return texture;
        }

        public String getTranslationKey() {
            return translationKey;
        }

        public int getFallbackColor() {
            return fallbackColor;
        }
    }
}