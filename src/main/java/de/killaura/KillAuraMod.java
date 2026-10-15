package de.killaura;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class KillAuraMod implements ClientModInitializer {
	public static KillAuraConfig CONFIG;
	private static KeyBinding menuKey;
	private static KeyBinding toggleKey;

	@Override
	public void onInitializeClient() {
		CONFIG = KillAuraConfig.load();

		// Menue: auf einer deutschen Tastatur liegt "Ö" auf der Taste, die GLFW als SEMICOLON meldet.
		menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.killaura.menu",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_SEMICOLON,
				KeyBinding.Category.MISC));

		// An/Aus: am Anfang keine Taste - frei waehlbar unter Optionen > Steuerung (oder im Menue).
		toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.killaura.toggle",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_UNKNOWN,
				KeyBinding.Category.MISC));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (menuKey.wasPressed()) {
				if (client.currentScreen == null) {
					client.setScreen(new KillAuraScreen(null));
				}
			}
			while (toggleKey.wasPressed()) {
				CONFIG.enabled = !CONFIG.enabled;
				CONFIG.save();
				client.inGameHud.setOverlayMessage(
						Text.literal(CONFIG.enabled ? "Kill Aura: §aAN" : "Kill Aura: §cAUS"), false);
			}
			KillAura.tick(client);
		});

		HudElementRegistry.addLast(Identifier.of("killaura", "hud"), (context, tickCounter) -> {
			MinecraftClient client = MinecraftClient.getInstance();

			// Weiche Zielhilfe jedes Bild (sieht viel ruhiger aus als nur 20x pro Sekunde)
			KillAura.onFrame(client);

			if (!CONFIG.enabled || !CONFIG.showHud || client.options.hudHidden || client.player == null) return;
			String text;
			int color;
			if (!client.isInSingleplayer()) {
				text = "Kill Aura: nur Einzelspieler";
				color = 0xFFFFAA00;
			} else {
				text = "Kill Aura AN";
				if (CONFIG.autoAttack) text += " | Schlagen " + String.format("%.1f", CONFIG.range);
				if (CONFIG.aimAssist) text += " | Zielhilfe";
				color = 0xFF55FF55;
			}
			context.drawTextWithShadow(client.textRenderer, Text.literal(text), 4, 4, color);
		});
	}
}
