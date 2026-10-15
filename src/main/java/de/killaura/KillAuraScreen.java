package de.killaura;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.Supplier;

/** Das Einstellungsmenue (Taste Oe). */
public class KillAuraScreen extends Screen {
	private final Screen parent;

	public KillAuraScreen(Screen parent) {
		super(Text.literal("Kill Aura – Einstellungen"));
		this.parent = parent;
	}

	private int colA, colB, colC, headerY;

	@Override
	protected void init() {
		KillAuraConfig cfg = KillAuraMod.CONFIG;
		int w = 125, h = 20, gap = 22;
		colA = this.width / 2 - w - w / 2 - 6;
		colB = this.width / 2 - w / 2;
		colC = this.width / 2 + w / 2 + 6;
		int y = 32;

		// Grosser An/Aus-Schalter oben
		addDrawableChild(toggle(colA, y, colC + w - colA, h, "Kill Aura",
				() -> cfg.enabled, v -> cfg.enabled = v));
		headerY = y + gap + 4;
		y = headerY + 12;

		// Spalte 1: Ziele
		addDrawableChild(toggle(colA, y, w, h, "Feindliche", () -> cfg.hostile, v -> cfg.hostile = v));
		addDrawableChild(toggle(colA, y + gap, w, h, "Neutrale", () -> cfg.neutral, v -> cfg.neutral = v));
		addDrawableChild(toggle(colA, y + gap * 2, w, h, "Tiere", () -> cfg.animals, v -> cfg.animals = v));
		addDrawableChild(toggle(colA, y + gap * 3, w, h, "Dorfbewohner", () -> cfg.villagers, v -> cfg.villagers = v));
		addDrawableChild(toggle(colA, y + gap * 4, w, h, "Haustiere", () -> cfg.pets, v -> cfg.pets = v));
		addDrawableChild(toggle(colA, y + gap * 5, w, h, "Spieler", () -> cfg.players, v -> cfg.players = v));
		addDrawableChild(cycle(colA, y + gap * 6, w, h, "Ziel",
				() -> cfg.lowestHealthFirst ? "Wenig Leben" : "Nächstes",
				() -> cfg.lowestHealthFirst = !cfg.lowestHealthFirst));

		// Spalte 2: Automatisch schlagen
		addDrawableChild(toggle(colB, y, w, h, "Auto-Schlagen", () -> cfg.autoAttack, v -> cfg.autoAttack = v));
		addDrawableChild(new ValueSlider(colB, y + gap, w, h, 2.0, 6.0,
				() -> cfg.range,
				v -> cfg.range = Math.round(v * 10.0) / 10.0,
				v -> Text.literal(String.format("Reichweite: %.1f", v))));
		addDrawableChild(toggle(colB, y + gap * 2, w, h, "Voller Schlag", () -> cfg.waitCooldown, v -> cfg.waitCooldown = v));
		addDrawableChild(new ValueSlider(colB, y + gap * 3, w, h, 0, 20,
				() -> cfg.extraDelay,
				v -> cfg.extraDelay = (int) Math.round(v),
				v -> Text.literal("Pause: " + Math.round(v) + " Ticks")));
		addDrawableChild(toggle(colB, y + gap * 4, w, h, "Sichtlinie", () -> cfg.lineOfSight, v -> cfg.lineOfSight = v));
		addDrawableChild(toggle(colB, y + gap * 5, w, h, "Beim Schlag drehen", () -> cfg.rotate, v -> cfg.rotate = v));

		// Spalte 3: Zielhilfe (Visier folgt, du schlaegst selbst)
		addDrawableChild(toggle(colC, y, w, h, "Zielhilfe", () -> cfg.aimAssist, v -> cfg.aimAssist = v));
		addDrawableChild(new ValueSlider(colC, y + gap, w, h, 3.0, 10.0,
				() -> cfg.aimRange,
				v -> cfg.aimRange = Math.round(v * 2.0) / 2.0,
				v -> Text.literal(String.format("Weite: %.1f", Math.round(v * 2.0) / 2.0))));
		addDrawableChild(new ValueSlider(colC, y + gap * 2, w, h, 1.0, 10.0,
				() -> cfg.aimSpeed,
				v -> cfg.aimSpeed = Math.round(v),
				v -> Text.literal("Tempo: " + Math.round(v))));
		addDrawableChild(toggle(colC, y + gap * 3, w, h, "HUD-Anzeige", () -> cfg.showHud, v -> cfg.showHud = v));
		addDrawableChild(ButtonWidget.builder(Text.literal("Tasten ändern…"), b -> {
			KillAuraMod.CONFIG.save();
			if (this.client != null) this.client.setScreen(new KeybindsScreen(this, this.client.options));
		}).dimensions(colC, y + gap * 5, w, h).build());

		// Fertig
		addDrawableChild(ButtonWidget.builder(Text.literal("Fertig"), b -> close())
				.dimensions(this.width / 2 - 100, y + gap * 7 + 4, 200, h).build());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFFFF);
		if (this.client != null && !this.client.isInSingleplayer()) {
			context.drawCenteredTextWithShadow(this.textRenderer,
					Text.literal("Auf Servern deaktiviert – funktioniert nur in Einzelspieler-Welten."),
					this.width / 2, 21, 0xFFFFAA00);
		}
		context.drawTextWithShadow(this.textRenderer, Text.literal("§lZiele"), colA + 2, headerY, 0xFFFFFF55);
		context.drawTextWithShadow(this.textRenderer, Text.literal("§lAuto-Schlagen"), colB + 2, headerY, 0xFFFFFF55);
		context.drawTextWithShadow(this.textRenderer, Text.literal("§lZielhilfe"), colC + 2, headerY, 0xFFFFFF55);
	}

	@Override
	public void close() {
		KillAuraMod.CONFIG.save();
		if (this.client != null) this.client.setScreen(parent);
	}

	// ---------- Hilfsfunktionen fuer Knoepfe ----------

	private static Text onOff(String label, boolean v) {
		return Text.literal(label + ": " + (v ? "§aAN" : "§cAUS"));
	}

	private static ButtonWidget toggle(int x, int y, int w, int h, String label,
									   Supplier<Boolean> get, Consumer<Boolean> set) {
		return ButtonWidget.builder(onOff(label, get.get()), b -> {
			set.accept(!get.get());
			b.setMessage(onOff(label, get.get()));
		}).dimensions(x, y, w, h).build();
	}

	private static ButtonWidget cycle(int x, int y, int w, int h, String label,
									  Supplier<String> get, Runnable next) {
		return ButtonWidget.builder(Text.literal(label + ": " + get.get()), b -> {
			next.run();
			b.setMessage(Text.literal(label + ": " + get.get()));
		}).dimensions(x, y, w, h).build();
	}

	private static class ValueSlider extends SliderWidget {
		private final double min, max;
		private final Consumer<Double> set;
		private final DoubleFunction<Text> label;

		ValueSlider(int x, int y, int w, int h, double min, double max,
					Supplier<Number> get, Consumer<Double> set, DoubleFunction<Text> label) {
			super(x, y, w, h, Text.empty(), (get.get().doubleValue() - min) / (max - min));
			this.min = min;
			this.max = max;
			this.set = set;
			this.label = label;
			updateMessage();
		}

		private double current() {
			return min + this.value * (max - min);
		}

		@Override
		protected void updateMessage() {
			if (label != null) setMessage(label.apply(current()));
		}

		@Override
		protected void applyValue() {
			set.accept(current());
		}
	}
}
