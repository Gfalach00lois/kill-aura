package de.killaura;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Alle Einstellungen. Werden in .minecraft/config/killaura.json gespeichert. */
public class KillAuraConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("killaura.json");

	public boolean enabled = false;
	public double range = 3.5;            // 2.0 - 6.0 Bloecke
	public boolean hostile = true;        // Zombies, Skelette, Creeper, Spinnen ...
	public boolean neutral = false;       // Enderman, Zombie-Piglins, Woelfe, Bienen, Eisengolems ...
	public boolean animals = false;       // Kuehe, Schweine, Schafe ...
	public boolean villagers = false;     // Dorfbewohner, fahrende Haendler
	public boolean pets = false;          // gezaehmte Tiere (Hunde, Katzen, Papageien ...)
	public boolean players = false;       // andere Spieler (nur in deiner eigenen LAN-Welt moeglich)
	public boolean autoAttack = true;     // automatisch schlagen (aus = du schlaegst selbst)
	public boolean aimAssist = false;     // Visier folgt dem naechsten Ziel
	public double aimRange = 6.0;         // 3.0 - 10.0 Bloecke
	public double aimSpeed = 4.0;         // 1 (sanft) - 10 (schnell)
	public boolean lineOfSight = true;    // nur angreifen, wenn das Ziel sichtbar ist
	public boolean rotate = false;        // Blick zum Ziel drehen
	public boolean waitCooldown = true;   // auf vollen Schlag warten (maximaler Schaden)
	public int extraDelay = 0;            // zusaetzliche Ticks zwischen Angriffen (0 - 20)
	public boolean lowestHealthFirst = false; // false = naechstes Ziel zuerst
	public boolean showHud = true;        // Anzeige oben links

	public static KillAuraConfig load() {
		try {
			if (Files.exists(FILE)) {
				try (Reader r = Files.newBufferedReader(FILE)) {
					KillAuraConfig cfg = GSON.fromJson(r, KillAuraConfig.class);
					if (cfg != null) {
						cfg.clamp();
						return cfg;
					}
				}
			}
		} catch (Exception e) {
			System.err.println("[KillAura] Konnte Einstellungen nicht laden: " + e);
		}
		return new KillAuraConfig();
	}

	public void save() {
		try {
			clamp();
			Files.createDirectories(FILE.getParent());
			try (Writer w = Files.newBufferedWriter(FILE)) {
				GSON.toJson(this, w);
			}
		} catch (Exception e) {
			System.err.println("[KillAura] Konnte Einstellungen nicht speichern: " + e);
		}
	}

	private void clamp() {
		range = Math.max(2.0, Math.min(6.0, range));
		extraDelay = Math.max(0, Math.min(20, extraDelay));
		aimRange = Math.max(3.0, Math.min(10.0, aimRange));
		aimSpeed = Math.max(1.0, Math.min(10.0, aimSpeed));
	}
}
