package com.hyhorde.arenapve.horde;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class HordeI18n {
    public static final String LANGUAGE_SPANISH = "es";
    public static final String LANGUAGE_ENGLISH = "en";
    public static final String LANGUAGE_PORTUGUESE = "pt";
    public static final String LANGUAGE_FRENCH = "fr";
    public static final String LANGUAGE_GERMAN = "de";
    public static final List<String> LANGUAGE_OPTIONS = List.of(LANGUAGE_SPANISH, LANGUAGE_ENGLISH, LANGUAGE_PORTUGUESE, LANGUAGE_FRENCH, LANGUAGE_GERMAN);
    private static final Map<String, String> SPANISH_TO_PORTUGUESE = HordeI18n.buildSpanishToPortuguese();
    private static final Map<String, String> ENGLISH_TO_PORTUGUESE = HordeI18n.buildEnglishToPortuguese();
    private static final Map<String, String> SPANISH_TO_FRENCH = HordeI18n.buildSpanishToFrench();
    private static final Map<String, String> ENGLISH_TO_FRENCH = HordeI18n.buildEnglishToFrench();
    private static final Map<String, String> SPANISH_TO_GERMAN = HordeI18n.buildSpanishToGerman();
    private static final Map<String, String> ENGLISH_TO_GERMAN = HordeI18n.buildEnglishToGerman();

    private HordeI18n() {
    }

    public static String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return LANGUAGE_SPANISH;
        }
        String normalized = language.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        normalized = normalized.replace('\u00e1', 'a').replace('\u00e9', 'e').replace('\u00ed', 'i').replace('\u00f3', 'o').replace('\u00fa', 'u').replace('\u00f1', 'n').replace('\u00e7', 'c');
        normalized = normalized.replace('á', 'a').replace('é', 'e').replace('í', 'i').replace('ó', 'o').replace('ú', 'u').replace('ñ', 'n').replace('ç', 'c');
        String tokenized = " " + normalized.replace('(', ' ').replace(')', ' ').replace('[', ' ').replace(']', ' ').replace('/', ' ').replace('|', ' ').replace('_', ' ') + " ";
        normalized = normalized.replace('á', 'a').replace('é', 'e').replace('í', 'i').replace('ó', 'o').replace('ú', 'u').replace('ñ', 'n').replace('ç', 'c');
        if ("en".equals(normalized) || normalized.startsWith("en_") || normalized.endsWith("_en") || normalized.contains("(en)") || normalized.contains("english") || normalized.contains("ingles") || HordeI18n.hasToken(tokenized, "en") || HordeI18n.hasToken(tokenized, "eng") || HordeI18n.hasToken(tokenized, "ing")) {
            return LANGUAGE_ENGLISH;
        }
        if ("es".equals(normalized) || normalized.startsWith("es_") || normalized.endsWith("_es") || normalized.contains("(es)") || normalized.contains("spanish") || normalized.contains("espanol") || HordeI18n.hasToken(tokenized, "es") || HordeI18n.hasToken(tokenized, "spa")) {
            return LANGUAGE_SPANISH;
        }
        if ("pt".equals(normalized) || normalized.startsWith("pt_") || normalized.endsWith("_pt") || normalized.contains("(pt)") || normalized.contains("portugues") || normalized.contains("portuguese") || normalized.contains("brazilian") || normalized.contains("brasil") || HordeI18n.hasToken(tokenized, "pt") || HordeI18n.hasToken(tokenized, "br")) {
            return LANGUAGE_PORTUGUESE;
        }
        if ("fr".equals(normalized) || normalized.startsWith("fr_") || normalized.endsWith("_fr") || normalized.contains("(fr)") || normalized.contains("french") || normalized.contains("francais") || normalized.contains("france") || HordeI18n.hasToken(tokenized, "fr")) {
            return LANGUAGE_FRENCH;
        }
        if ("de".equals(normalized) || normalized.startsWith("de_") || normalized.endsWith("_de") || normalized.contains("(de)") || normalized.contains("german") || normalized.contains("deutsch") || normalized.contains("aleman") || HordeI18n.hasToken(tokenized, "de")) {
            return LANGUAGE_GERMAN;
        }
        return LANGUAGE_SPANISH;
    }

    public static boolean isEnglish(String language) {
        return LANGUAGE_ENGLISH.equals(HordeI18n.normalizeLanguage(language));
    }

    public static String getLanguageDisplay(String language) {
        String normalized = HordeI18n.normalizeLanguage(language);
        switch (normalized) {
            case LANGUAGE_ENGLISH: {
                return "English (en)";
            }
            case LANGUAGE_PORTUGUESE: {
                return "Portugues (pt)";
            }
            case LANGUAGE_FRENCH: {
                return "Francais (fr)";
            }
            case LANGUAGE_GERMAN: {
                return "Deutsch (de)";
            }
        }
        return "Espanol (es)";
    }

    public static String translateLegacy(String language, String text) {
        if (text == null || text.isBlank()) {
            return text == null ? "" : text;
        }
        String normalized = HordeI18n.normalizeLanguage(language);
        if (LANGUAGE_ENGLISH.equals(normalized) || LANGUAGE_SPANISH.equals(normalized)) {
            return text;
        }
        if (LANGUAGE_PORTUGUESE.equals(normalized)) {
            return HordeI18n.applyReplacements(HordeI18n.applyReplacements(text, SPANISH_TO_PORTUGUESE), ENGLISH_TO_PORTUGUESE);
        }
        if (LANGUAGE_FRENCH.equals(normalized)) {
            return HordeI18n.applyReplacements(HordeI18n.applyReplacements(text, SPANISH_TO_FRENCH), ENGLISH_TO_FRENCH);
        }
        if (LANGUAGE_GERMAN.equals(normalized)) {
            return HordeI18n.applyReplacements(HordeI18n.applyReplacements(text, SPANISH_TO_GERMAN), ENGLISH_TO_GERMAN);
        }
        return text;
    }

    private static String applyReplacements(String input, Map<String, String> replacements) {
        String output = input;
        ArrayList<Map.Entry<String, String>> entries = new ArrayList<Map.Entry<String, String>>(replacements.entrySet());
        entries.sort((left, right) -> Integer.compare(right.getKey().length(), left.getKey().length()));
        for (Map.Entry<String, String> entry : entries) {
            output = output.replace(entry.getKey(), entry.getValue());
        }
        return output;
    }

    private static boolean hasToken(String tokenized, String token) {
        if (tokenized == null || token == null || token.isBlank()) {
            return false;
        }
        return tokenized.contains(" " + token + " ");
    }

    private static Map<String, String> buildSpanishToPortuguese() {
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        map.put("Horda PVE Config", "Config Horde PVE");
        map.put("Horda PVE - Estado", "Horde PVE - Estado");
        map.put("CLASIFICACION", "CLASSIFICACAO");
        map.put("Comandos principales", "Comandos principais");
        map.put("Guia rapida para usar Horde PVE", "Guia rapida para usar Horde PVE");
        map.put("Recarga y despliegue", "Recarregamento e deploy");
        map.put("JSON externos (carpeta de datos del plugin)", "JSON externos (pasta de dados do plugin)");
        map.put("config persistente", "config persistente");
        map.put("Guardar config", "Salvar config");
        map.put("Recargar config", "Recarregar config");
        map.put("Iniciar horda", "Iniciar horda");
        map.put("Detener horda", "Parar horda");
        map.put("Pasar ronda", "Pular rodada");
        map.put("Cerrar", "Fechar");
        map.put("Ayuda", "Ajuda");
        map.put("Jugadores", "Jogadores");
        map.put("Jugador", "Jogador");
        map.put("jugadores", "jogadores");
        map.put("jugador", "jogador");
        map.put("Sonidos", "Sons");
        map.put("sonidos", "sons");
        map.put("General", "Geral");
        map.put("Recompensas", "Recompensas");
        map.put("Recompensa", "Recompensa");
        map.put("ronda(s)", "rodada(s)");
        map.put("Rondas", "Rodadas");
        map.put("Ronda", "Rodada");
        map.put("rondas", "rodadas");
        map.put("ronda", "rodada");
        map.put("Siguiente", "Proxima");
        map.put("Siguiente ronda", "Proxima rodada");
        map.put("inicio ronda", "inicio da rodada");
        map.put("victoria ronda", "vitoria da rodada");
        map.put("Boss final", "Chefe final");
        map.put("radio", "raio");
        map.put("Radio", "Raio");
        map.put("Cantidad", "Quantidade");
        map.put("Detectados", "Detectados");
        map.put("No hay", "Nao ha");
        map.put("No se pudo", "Nao foi possivel");
        map.put("No active", "No active");
        map.put("Uso:", "Uso:");
        map.put("invalido", "invalido");
        map.put("actual", "atual");
        map.put("configuracion", "configuracao");
        map.put("Configuracion", "Configuracao");
        map.put("Configuracion del radio de aparicion de enemigos", "Configuracao do raio de aparicao de inimigos");
        map.put("Cantidad de rondas", "Quantidade de rodadas");
        map.put("Cantidad base de enemigos por ronda", "Quantidade base de inimigos por rodada");
        map.put("Incremento de enemigos por ronda", "Incremento de inimigos por rodada");
        map.put("Tiempo de espera entre rondas (s)", "Tempo de espera entre rodadas (s)");
        map.put("Volumen inicio (%)", "Volume inicio (%)");
        map.put("Volumen victoria (%)", "Volume vitoria (%)");
        map.put("Idioma interfaz", "Idioma da interface");
        map.put("Idioma de interfaz", "Idioma da interface");
        map.put("Centro", "Centro");
        map.put("Mundo", "Mundo");
        map.put("Estado", "Estado");
        map.put("Activa", "Ativa");
        map.put("Inactiva", "Inativa");
        map.put("Enemigos vivos", "Inimigos vivos");
        map.put("Muertes jugadores", "Mortes de jogadores");
        map.put("Tiempo total", "Tempo total");
        map.put("Jugadores rastreados", "Jogadores rastreados");
        map.put("Tus estadisticas", "Suas estatisticas");
        map.put("Sin estadisticas personales por ahora.", "Sem estatisticas pessoais por enquanto.");
        map.put("No hay jugadores detectados en el radio actual de arena.", "Nao ha jogadores detectados no raio atual da arena.");
        return map;
    }

    private static Map<String, String> buildEnglishToPortuguese() {
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        map.put("Horde PVE Config", "Config Horde PVE");
        map.put("Horde PVE - Status", "Horde PVE - Estado");
        map.put("Help", "Ajuda");
        map.put("Players", "Jogadores");
        map.put("Sounds", "Sons");
        map.put("Rewards", "Recompensas");
        map.put("Close", "Fechar");
        map.put("Save config", "Salvar config");
        map.put("Reload config", "Recarregar config");
        map.put("Start horde", "Iniciar horda");
        map.put("Stop horde", "Parar horda");
        map.put("Skip round", "Pular rodada");
        map.put("Horde stopped manually.", "Horda parada manualmente.");
        map.put("Horde stopped.", "Horda parada.");
        map.put("Horde starts in ", "Horda comeca em ");
        map.put("Round", "Rodada");
        map.put("Rounds", "Rodadas");
        map.put("Player", "Jogador");
        map.put("Mode", "Modo");
        map.put("State", "Estado");
        map.put("Active", "Ativa");
        map.put("Inactive", "Inativa");
        map.put("Counter", "Contador");
        map.put("Alive enemies", "Inimigos vivos");
        map.put("Type / role", "Tipo / papel");
        map.put("Tracked players", "Jogadores rastreados");
        map.put("Your stats", "Suas estatisticas");
        map.put("No player stats yet.", "Ainda nao ha estatisticas de jogadores.");
        map.put("No personal stats yet.", "Ainda nao ha estatisticas pessoais.");
        map.put("Detected", "Detectados");
        map.put("Language updated.", "Idioma atualizado.");
        map.put("Use my current position", "Usar minha posicao atual");
        map.put("Spawn radius setup", "Configuracao de raio de spawn");
        map.put("Enemy spawn radius setup", "Configuracao do raio de aparicao de inimigos");
        map.put("Round setup", "Configuracao de rodada");
        map.put("Number of rounds", "Quantidade de rodadas");
        map.put("Base enemies per round", "Quantidade base de inimigos por rodada");
        map.put("Enemy increment per round", "Incremento de inimigos por rodada");
        map.put("Delay between rounds (s)", "Tempo de espera entre rodadas (s)");
        map.put("Minimum radius", "Raio minimo");
        map.put("Maximum radius", "Raio maximo");
        map.put("Players area radius", "Raio da area de jogadores");
        map.put("Players inside current area", "Jogadores dentro da area atual");
        map.put("Refresh list", "Atualizar lista");
        map.put("Changes apply to next start. If horde is active, they are applied to current lock immediately.", "Alteracoes aplicam no proximo inicio. Se a horda estiver ativa, aplicam no bloqueio atual.");
        map.put("Base / round", "Base / rodada");
        map.put("Inc. per round", "Inc. por rodada");
        map.put("Delay (s)", "Espera (s)");
        map.put("Horde category", "Categoria da horda");
        map.put("Interface language", "Idioma da interface");
        map.put("Round start sound", "Som de inicio da rodada");
        map.put("Round victory sound", "Som de vitoria da rodada");
        map.put("Start volume", "Volume inicio");
        map.put("Victory volume", "Volume vitoria");
        map.put("Start volume (%)", "Volume inicio (%)");
        map.put("Victory volume (%)", "Volume vitoria (%)");
        map.put("Quick guide for Horde PVE usage", "Guia rapido para usar Horde PVE");
        map.put("Main commands", "Comandos principais");
        map.put("External JSON files (plugin data folder)", "Arquivos JSON externos (pasta de dados do plugin)");
        map.put("Use the buttons below to open the community links.", "Use os botoes abaixo para abrir os links da comunidade.");
        map.put("Open Discord", "Abrir Discord");
        map.put("Open CurseForge", "Abrir CurseForge");
        map.put("Reload and deployment notes", "Notas de recarga e deploy");
        map.put("Link sent to chat: ", "Link enviado no chat: ");
        map.put("Could not resolve external link.", "Nao foi possivel resolver o link externo.");
        map.put("No players detected in the current arena radius.", "Nenhum jogador detectado no raio atual da arena.");
        map.put("Current arena radius: ", "Raio atual da arena: ");
        map.put("Players inside area: ", "Jogadores na area: ");
        map.put("Use each row to set Player, Spectator or Exit mode.", "Use cada linha para definir modo Jogador, Espectador ou Sair.");
        map.put("Move players inside the arena radius to manage them here.", "Mova jogadores para dentro do raio da arena para gerenciar aqui.");
        map.put("Horde center not configured. You can use your current position.", "Centro da horda nao configurado. Voce pode usar sua posicao atual.");
        map.put("Current center: ", "Centro atual: ");
        map.put("World: ", "Mundo: ");
        map.put("Spectator", "Espectador");
        map.put("Exit area", "Sair da area");
        return map;
    }

    private static Map<String, String> buildSpanishToFrench() {
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        map.put("Horda PVE Config", "Configuration Horde PVE");
        map.put("Horda PVE - Estado", "Horde PVE - Statut");
        map.put("CLASIFICACION", "CLASSEMENT");
        map.put("Comandos principales", "Commandes principales");
        map.put("Guardar config", "Sauvegarder config");
        map.put("Recargar config", "Recharger config");
        map.put("Iniciar horda", "Demarrer horde");
        map.put("Detener horda", "Arreter horde");
        map.put("Pasar ronda", "Passer manche");
        map.put("Cerrar", "Fermer");
        map.put("Ayuda", "Aide");
        map.put("General", "General");
        map.put("Jugadores", "Joueurs");
        map.put("Jugador", "Joueur");
        map.put("Sonidos", "Sons");
        map.put("Recompensas", "Recompenses");
        map.put("Rondas", "Manches");
        map.put("Ronda", "Manche");
        map.put("rondas", "manches");
        map.put("ronda", "manche");
        map.put("Siguiente ronda", "Prochaine manche");
        map.put("Boss final", "Boss final");
        map.put("Cantidad", "Quantite");
        map.put("Configuracion del radio de aparicion de enemigos", "Configuration du rayon d apparition des ennemis");
        map.put("Cantidad de rondas", "Nombre de manches");
        map.put("Cantidad base de enemigos por ronda", "Quantite de base d ennemis par manche");
        map.put("Incremento de enemigos por ronda", "Augmentation d ennemis par manche");
        map.put("Tiempo de espera entre rondas (s)", "Temps d attente entre manches (s)");
        map.put("Volumen inicio (%)", "Volume debut (%)");
        map.put("Volumen victoria (%)", "Volume victoire (%)");
        map.put("Idioma de interfaz", "Langue de l interface");
        map.put("No hay", "Il n y a pas");
        map.put("No se pudo", "Impossible de");
        map.put("Centro", "Centre");
        map.put("Mundo", "Monde");
        map.put("Estado", "Etat");
        map.put("Activa", "Active");
        map.put("Inactiva", "Inactive");
        map.put("Enemigos vivos", "Ennemis en vie");
        map.put("Muertes jugadores", "Morts des joueurs");
        map.put("Tiempo total", "Temps total");
        map.put("Jugadores rastreados", "Joueurs suivis");
        map.put("Tus estadisticas", "Vos statistiques");
        map.put("Sin estadisticas personales por ahora.", "Aucune statistique personnelle pour le moment.");
        map.put("No hay jugadores detectados en el radio actual de arena.", "Aucun joueur detecte dans le rayon actuel de l arene.");
        return map;
    }

    private static Map<String, String> buildEnglishToFrench() {
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        map.put("Horde PVE Config", "Configuration Horde PVE");
        map.put("Horde PVE - Status", "Horde PVE - Statut");
        map.put("Help", "Aide");
        map.put("Players", "Joueurs");
        map.put("Sounds", "Sons");
        map.put("Rewards", "Recompenses");
        map.put("Close", "Fermer");
        map.put("Save config", "Sauvegarder config");
        map.put("Reload config", "Recharger config");
        map.put("Start horde", "Demarrer horde");
        map.put("Stop horde", "Arreter horde");
        map.put("Skip round", "Passer manche");
        map.put("Horde stopped manually.", "Horde arretee manuellement.");
        map.put("Horde stopped.", "Horde arretee.");
        map.put("Horde starts in ", "La horde commence dans ");
        map.put("Round", "Manche");
        map.put("Rounds", "Manches");
        map.put("Player", "Joueur");
        map.put("Mode", "Mode");
        map.put("State", "Etat");
        map.put("Active", "Actif");
        map.put("Inactive", "Inactif");
        map.put("Counter", "Compteur");
        map.put("Alive enemies", "Ennemis vivants");
        map.put("Type / role", "Type / role");
        map.put("Tracked players", "Joueurs suivis");
        map.put("Your stats", "Vos statistiques");
        map.put("No player stats yet.", "Aucune statistique joueur pour le moment.");
        map.put("No personal stats yet.", "Aucune statistique personnelle pour le moment.");
        map.put("Detected", "Detectes");
        map.put("Language updated.", "Langue mise a jour.");
        map.put("Use my current position", "Utiliser ma position actuelle");
        map.put("Spawn radius setup", "Configuration du rayon de spawn");
        map.put("Enemy spawn radius setup", "Configuration du rayon d apparition des ennemis");
        map.put("Round setup", "Configuration de manche");
        map.put("Number of rounds", "Nombre de manches");
        map.put("Base enemies per round", "Quantite de base d ennemis par manche");
        map.put("Enemy increment per round", "Augmentation d ennemis par manche");
        map.put("Delay between rounds (s)", "Temps d attente entre manches (s)");
        map.put("Minimum radius", "Rayon minimum");
        map.put("Maximum radius", "Rayon maximum");
        map.put("Players area radius", "Rayon de zone des joueurs");
        map.put("Players inside current area", "Joueurs dans la zone actuelle");
        map.put("Refresh list", "Actualiser la liste");
        map.put("Changes apply to next start. If horde is active, they are applied to current lock immediately.", "Les changements s appliquent au prochain depart. Si la horde est active, ils s appliquent immediatement au verrou actuel.");
        map.put("Base / round", "Base / manche");
        map.put("Inc. per round", "Augm. par manche");
        map.put("Delay (s)", "Attente (s)");
        map.put("Horde category", "Categorie de horde");
        map.put("Interface language", "Langue de l interface");
        map.put("Round start sound", "Son debut de manche");
        map.put("Round victory sound", "Son victoire de manche");
        map.put("Start volume", "Volume debut");
        map.put("Victory volume", "Volume victoire");
        map.put("Start volume (%)", "Volume debut (%)");
        map.put("Victory volume (%)", "Volume victoire (%)");
        map.put("Quick guide for Horde PVE usage", "Guide rapide pour utiliser Horde PVE");
        map.put("Main commands", "Commandes principales");
        map.put("External JSON files (plugin data folder)", "Fichiers JSON externes (dossier de donnees du plugin)");
        map.put("Use the buttons below to open the community links.", "Utilisez les boutons ci-dessous pour ouvrir les liens de la communaute.");
        map.put("Open Discord", "Ouvrir Discord");
        map.put("Open CurseForge", "Ouvrir CurseForge");
        map.put("Reload and deployment notes", "Notes de rechargement et de deploiement");
        map.put("Link sent to chat: ", "Lien envoye au chat: ");
        map.put("Could not resolve external link.", "Impossible de resoudre le lien externe.");
        map.put("No players detected in the current arena radius.", "Aucun joueur detecte dans le rayon actuel de l arene.");
        map.put("Current arena radius: ", "Rayon actuel de l arene: ");
        map.put("Players inside area: ", "Joueurs dans la zone: ");
        map.put("Use each row to set Player, Spectator or Exit mode.", "Utilisez chaque ligne pour definir le mode Joueur, Spectateur ou Sortie.");
        map.put("Move players inside the arena radius to manage them here.", "Placez les joueurs dans le rayon de l arene pour les gerer ici.");
        map.put("Horde center not configured. You can use your current position.", "Centre de horde non configure. Vous pouvez utiliser votre position actuelle.");
        map.put("Current center: ", "Centre actuel: ");
        map.put("World: ", "Monde: ");
        map.put("Spectator", "Spectateur");
        map.put("Exit area", "Sortir de la zone");
        return map;
    }

    private static Map<String, String> buildSpanishToGerman() {
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        map.put("Horda PVE Config", "Horde PVE Konfiguration");
        map.put("Horda PVE - Estado", "Horde PVE - Status");
        map.put("CLASIFICACION", "BESTENLISTE");
        map.put("Comandos principales", "Hauptbefehle");
        map.put("Guardar config", "Konfig speichern");
        map.put("Recargar config", "Konfig neu laden");
        map.put("Iniciar horda", "Horde starten");
        map.put("Detener horda", "Horde stoppen");
        map.put("Pasar ronda", "Runde uberspringen");
        map.put("Cerrar", "Schliessen");
        map.put("Ayuda", "Hilfe");
        map.put("General", "Allgemein");
        map.put("Jugadores", "Spieler");
        map.put("Jugador", "Spieler");
        map.put("Sonidos", "Sounds");
        map.put("Recompensas", "Belohnungen");
        map.put("Rondas", "Runden");
        map.put("Ronda", "Runde");
        map.put("rondas", "runden");
        map.put("ronda", "runde");
        map.put("Siguiente ronda", "Nachste Runde");
        map.put("Boss final", "Endboss");
        map.put("Cantidad", "Menge");
        map.put("Configuracion del radio de aparicion de enemigos", "Konfiguration des Feind-Spawn-Radius");
        map.put("Cantidad de rondas", "Anzahl der Runden");
        map.put("Cantidad base de enemigos por ronda", "Basisanzahl an Gegnern pro Runde");
        map.put("Incremento de enemigos por ronda", "Gegnerzuwachs pro Runde");
        map.put("Tiempo de espera entre rondas (s)", "Wartezeit zwischen Runden (s)");
        map.put("Volumen inicio (%)", "Startlautstarke (%)");
        map.put("Volumen victoria (%)", "Sieglautstarke (%)");
        map.put("Idioma de interfaz", "Interface-Sprache");
        map.put("No hay", "Es gibt keine");
        map.put("No se pudo", "Konnte nicht");
        map.put("Centro", "Zentrum");
        map.put("Mundo", "Welt");
        map.put("Estado", "Status");
        map.put("Activa", "Aktiv");
        map.put("Inactiva", "Inaktiv");
        map.put("Enemigos vivos", "Lebende Gegner");
        map.put("Muertes jugadores", "Spieler-Tode");
        map.put("Tiempo total", "Gesamtzeit");
        map.put("Jugadores rastreados", "Verfolgte Spieler");
        map.put("Tus estadisticas", "Deine Statistiken");
        map.put("Sin estadisticas personales por ahora.", "Noch keine personlichen Statistiken.");
        map.put("No hay jugadores detectados en el radio actual de arena.", "Keine Spieler im aktuellen Arena-Radius erkannt.");
        return map;
    }

    private static Map<String, String> buildEnglishToGerman() {
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        map.put("Horde PVE Config", "Horde PVE Konfiguration");
        map.put("Horde PVE - Status", "Horde PVE - Status");
        map.put("Help", "Hilfe");
        map.put("Players", "Spieler");
        map.put("Sounds", "Sounds");
        map.put("Rewards", "Belohnungen");
        map.put("Close", "Schliessen");
        map.put("Save config", "Konfig speichern");
        map.put("Reload config", "Konfig neu laden");
        map.put("Start horde", "Horde starten");
        map.put("Stop horde", "Horde stoppen");
        map.put("Skip round", "Runde uberspringen");
        map.put("Horde stopped manually.", "Horde manuell gestoppt.");
        map.put("Horde stopped.", "Horde gestoppt.");
        map.put("Horde starts in ", "Horde startet in ");
        map.put("Round", "Runde");
        map.put("Rounds", "Runden");
        map.put("Player", "Spieler");
        map.put("Mode", "Modus");
        map.put("State", "Status");
        map.put("Active", "Aktiv");
        map.put("Inactive", "Inaktiv");
        map.put("Counter", "Zaehler");
        map.put("Alive enemies", "Lebende Gegner");
        map.put("Type / role", "Typ / Rolle");
        map.put("Tracked players", "Verfolgte Spieler");
        map.put("Your stats", "Deine Statistiken");
        map.put("No player stats yet.", "Noch keine Spielerstatistiken.");
        map.put("No personal stats yet.", "Noch keine personlichen Statistiken.");
        map.put("Detected", "Erkannt");
        map.put("Language updated.", "Sprache aktualisiert.");
        map.put("Use my current position", "Eigene aktuelle Position verwenden");
        map.put("Spawn radius setup", "Spawn-Radius Konfiguration");
        map.put("Enemy spawn radius setup", "Konfiguration des Feind-Spawn-Radius");
        map.put("Round setup", "Runden Konfiguration");
        map.put("Number of rounds", "Anzahl der Runden");
        map.put("Base enemies per round", "Basisanzahl an Gegnern pro Runde");
        map.put("Enemy increment per round", "Gegnerzuwachs pro Runde");
        map.put("Delay between rounds (s)", "Wartezeit zwischen Runden (s)");
        map.put("Minimum radius", "Mindest-Radius");
        map.put("Maximum radius", "Maximal-Radius");
        map.put("Players area radius", "Spielerbereich-Radius");
        map.put("Players inside current area", "Spieler im aktuellen Bereich");
        map.put("Refresh list", "Liste aktualisieren");
        map.put("Changes apply to next start. If horde is active, they are applied to current lock immediately.", "Aenderungen gelten fur den nachsten Start. Wenn die Horde aktiv ist, gelten sie sofort fur die aktuelle Sperre.");
        map.put("Base / round", "Basis / Runde");
        map.put("Inc. per round", "Zuwachs pro Runde");
        map.put("Delay (s)", "Wartezeit (s)");
        map.put("Horde category", "Horde-Kategorie");
        map.put("Interface language", "Interface-Sprache");
        map.put("Round start sound", "Sound bei Rundenstart");
        map.put("Round victory sound", "Sound bei Rundensieg");
        map.put("Start volume", "Startlautstarke");
        map.put("Victory volume", "Sieglautstarke");
        map.put("Start volume (%)", "Startlautstarke (%)");
        map.put("Victory volume (%)", "Sieglautstarke (%)");
        map.put("Quick guide for Horde PVE usage", "Kurzanleitung fur Horde PVE");
        map.put("Main commands", "Hauptbefehle");
        map.put("External JSON files (plugin data folder)", "Externe JSON-Dateien (Plugin-Datenordner)");
        map.put("Use the buttons below to open the community links.", "Nutze die Buttons unten, um Community-Links zu offnen.");
        map.put("Open Discord", "Discord offnen");
        map.put("Open CurseForge", "CurseForge offnen");
        map.put("Reload and deployment notes", "Hinweise zu Reload und Deployment");
        map.put("Link sent to chat: ", "Link in den Chat gesendet: ");
        map.put("Could not resolve external link.", "Externer Link konnte nicht aufgelost werden.");
        map.put("No players detected in the current arena radius.", "Keine Spieler im aktuellen Arena-Radius erkannt.");
        map.put("Current arena radius: ", "Aktueller Arena-Radius: ");
        map.put("Players inside area: ", "Spieler im Bereich: ");
        map.put("Use each row to set Player, Spectator or Exit mode.", "Verwende jede Zeile fur Spieler-, Zuschauer- oder Verlassen-Modus.");
        map.put("Move players inside the arena radius to manage them here.", "Bewege Spieler in den Arena-Radius, um sie hier zu verwalten.");
        map.put("Horde center not configured. You can use your current position.", "Horde-Zentrum nicht konfiguriert. Du kannst deine aktuelle Position verwenden.");
        map.put("Current center: ", "Aktuelles Zentrum: ");
        map.put("World: ", "Welt: ");
        map.put("Spectator", "Zuschauer");
        map.put("Exit area", "Bereich verlassen");
        return map;
    }
}
