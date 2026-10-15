package de.killaura;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
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

	@Override
	protected void init() {
		KillAuraConfig cfg = KillAuraMod.CONFIG;
		int w = 150, h = 20, gap = 22;
		int left = this.width / 2 - w - 5;
		int right = this.width / 2 + 5;
		int y = 36;

		// Grosser An/Aus-Schalter oben
		addDrawableChild(toggle(this.width / 2 - 155, y, 310, h, "Kill Aura",
				() -> cfg.enabled, v -> cfg.enabled = v));
		y += gap + 6;

		// Linke Spalte: Ziele
		addDrawableChild(toggle(left, y, w, h, "Feindliche Mobs", () -> cfg.hostile, v -> cfg.hostile = v));
		addDrawableChild(toggle(left, y + gap, w, h, "Neutrale Mobs", () -> cfg.neutral, v -> cfg.neutral = v));
		addDrawableChild(toggle(left, y + gap * 2, w, h, "Tiere", () -> cfg.animals, v -> cfg.animals = v));
		addDrawableChild(toggle(left, y + gap * 3, w, h, "Dorfbewohner", () -> cfg.villagers, v -> cfg.villagers = v));
		addDrawableChild(toggle(left, y + gap * 4, w, h, "Haustiere", () -> cfg.pets, v -> cfg.pets = v));
		addDrawableChild(toggle(left, y + gap * 5, w, h, "Spieler", () -> cfg.players, v -> cfg.players = v));
		addDrawableChild(cycle(right, y + gap * 5, w, h, "Ziel",
				() -> cfg.lowestHealthFirst ? "Wenigste Leben" : "Nächstes",
				() -> cfg.lowestHealthFirst = !cfg.lowestHealthFirst));
		addDrawableChild(toggle(left, y + gap * 6, w, h, "HUD-Anzeige", () -> cfg.showHud, v -> cfg.showHud = v));

		// Rechte Spalte: Verhalten
		addDrawableChild(new ValueSlider(right, y, w, h, 2.0, 6.0,
				() -> cfg.range,
				v -> cfg.range = Math.round(v * 10.0) / 10.0,
				v -> Text.literal(String.format("Reichweite: %.1f", v))));
		addDrawableChild(toggle(right, y + gap, w, h, "Voller Schlag", () -> cfg.waitCooldown, v -> cfg.waitCooldown = v));
		addDrawableChild(new ValueSlider(right, y + gap * 2, w, h, 0, 20,
				() -> cfg.extraDelay,
				v -> cfg.extraDelay = (int) Math.round(v),
				v -> Text.literal("Extra-Pause: " + Math.round(v) + " Ticks")));
		addDrawableChild(toggle(right, y + gap * 3, w, h, "Nur Sichtlinie", () -> cfg.lineOfSight, v -> cfg.lineOfSight = v));
		addDrawableChild(toggle(right, y + gap * 4, w, h, "Zum Ziel drehen", () -> cfg.rotate, v -> cfg.rotate = v));

		// Fertig
		addDrawableChild(ButtonWidget.builder(Text.literal("Fertig"), b -> close())
				.dimensions(this.width / 2 - 100, y + gap * 7 + 6, 200, h).build());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFFFF);
		if (this.client != null && !this.client.isInSingleplayer()) {
			context.drawCenteredTextWithShadow(this.textRenderer,
					Text.literal("Auf Servern deaktiviert – funktioniert nur in Einzelspieler-Welten."),
					this.width / 2, 27, 0xFFFFAA00);
		}
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
