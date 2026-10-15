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

	@Override
	public void onInitializeClient() {
		CONFIG = KillAuraConfig.load();

		// Auf einer deutschen Tastatur liegt "Ö" auf der Taste, die GLFW als SEMICOLON meldet.
		// Kann jederzeit unter Optionen > Steuerung geaendert werden.
		menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.killaura.menu",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_SEMICOLON,
				KeyBinding.Category.MISC));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (menuKey.wasPressed()) {
				if (client.currentScreen == null) {
					client.setScreen(new KillAuraScreen(null));
				}
			}
			KillAura.tick(client);
		});

		HudElementRegistry.addLast(Identifier.of("killaura", "hud"), (context, tickCounter) -> {
			MinecraftClient client = MinecraftClient.getInstance();
			if (!CONFIG.enabled || !CONFIG.showHud || client.options.hudHidden || client.player == null) return;
			if (!client.isInSingleplayer()) {
				context.drawTextWithShadow(client.textRenderer,
						Text.literal("Kill Aura: nur Einzelspieler"), 4, 4, 0xFFFFAA00);
			} else {
				context.drawTextWithShadow(client.textRenderer,
						Text.literal("Kill Aura AN (" + String.format("%.1f", CONFIG.range) + ")"), 4, 4, 0xFF55FF55);
			}
		});
	}
}
