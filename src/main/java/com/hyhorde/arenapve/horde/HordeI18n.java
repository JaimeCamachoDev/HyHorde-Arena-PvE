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
        return HordeI18n.translateLegacyAttempt(HordeI18n.normalizeLanguage(language), text).text;
    }

    public static String translateUi(String language, String englishText, String spanishText) {
        String normalized = HordeI18n.normalizeLanguage(language);
        if (LANGUAGE_ENGLISH.equals(normalized)) {
            return englishText == null ? "" : englishText;
        }
        if (LANGUAGE_SPANISH.equals(normalized)) {
            return spanishText == null ? "" : spanishText;
        }
        TranslationAttempt englishAttempt = HordeI18n.translateLegacyAttempt(normalized, englishText);
        TranslationAttempt spanishAttempt = HordeI18n.translateLegacyAttempt(normalized, spanishText);
        boolean englishChanged = englishText != null && !englishText.equals(englishAttempt.text);
        boolean spanishChanged = spanishText != null && !spanishText.equals(spanishAttempt.text);
        if (spanishAttempt.replacements > englishAttempt.replacements) {
            return spanishAttempt.text;
        }
        if (englishAttempt.replacements > spanishAttempt.replacements) {
            return englishAttempt.text;
        }
        if (spanishChanged && !englishChanged) {
            return spanishAttempt.text;
        }
        if (englishChanged && !spanishChanged) {
            return englishAttempt.text;
        }
        if (spanishChanged) {
            return spanishAttempt.text;
        }
        return englishAttempt.text;
    }

    private static TranslationAttempt translateLegacyAttempt(String normalizedLanguage, String text) {
        if (text == null || text.isBlank()) {
            return new TranslationAttempt(text == null ? "" : text, 0);
        }
        if (LANGUAGE_ENGLISH.equals(normalizedLanguage) || LANGUAGE_SPANISH.equals(normalizedLanguage)) {
            return new TranslationAttempt(text, 0);
        }
        if (LANGUAGE_PORTUGUESE.equals(normalizedLanguage)) {
            TranslationAttempt spanish = HordeI18n.applyReplacementsAttempt(text, SPANISH_TO_PORTUGUESE);
            TranslationAttempt english = HordeI18n.applyReplacementsAttempt(spanish.text, ENGLISH_TO_PORTUGUESE);
            return new TranslationAttempt(english.text, spanish.replacements + english.replacements);
        }
        if (LANGUAGE_FRENCH.equals(normalizedLanguage)) {
            TranslationAttempt spanish = HordeI18n.applyReplacementsAttempt(text, SPANISH_TO_FRENCH);
            TranslationAttempt english = HordeI18n.applyReplacementsAttempt(spanish.text, ENGLISH_TO_FRENCH);
            return new TranslationAttempt(english.text, spanish.replacements + english.replacements);
        }
        if (LANGUAGE_GERMAN.equals(normalizedLanguage)) {
            TranslationAttempt spanish = HordeI18n.applyReplacementsAttempt(text, SPANISH_TO_GERMAN);
            TranslationAttempt english = HordeI18n.applyReplacementsAttempt(spanish.text, ENGLISH_TO_GERMAN);
            return new TranslationAttempt(english.text, spanish.replacements + english.replacements);
        }
        return new TranslationAttempt(text, 0);
    }

    private static String applyReplacements(String input, Map<String, String> replacements) {
        return HordeI18n.applyReplacementsAttempt(input, replacements).text;
    }

    private static TranslationAttempt applyReplacementsAttempt(String input, Map<String, String> replacements) {
        String output = input;
        int replacementsApplied = 0;
        ArrayList<Map.Entry<String, String>> entries = new ArrayList<Map.Entry<String, String>>(replacements.entrySet());
        entries.sort((left, right) -> Integer.compare(right.getKey().length(), left.getKey().length()));
        for (Map.Entry<String, String> entry : entries) {
            String key = entry.getKey();
            if (key == null || key.isEmpty()) {
                continue;
            }
            int occurrences = HordeI18n.countOccurrences(output, key);
            if (occurrences <= 0) {
                continue;
            }
            replacementsApplied += occurrences;
            output = output.replace(key, entry.getValue());
        }
        return new TranslationAttempt(output, replacementsApplied);
    }

    private static int countOccurrences(String input, String token) {
        if (input == null || token == null || token.isEmpty()) {
            return 0;
        }
        int count = 0;
        int index = 0;
        while ((index = input.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }

    private static final class TranslationAttempt {
        private final String text;
        private final int replacements;

        private TranslationAttempt(String text, int replacements) {
            this.text = text;
            this.replacements = replacements;
        }
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
        map.put("Actions", "Acoes");
        map.put("Add", "Adicionar");
        map.put("Add arena", "Adicionar arena");
        map.put("Add boss", "Adicionar boss");
        map.put("Add category", "Adicionar categoria");
        map.put("Add horde", "Adicionar horda");
        map.put("Amount", "Quantidade");
        map.put("Amt", "Qtd");
        map.put("Apply auto mode", "Aplicar modo auto");
        map.put("Arena definitions", "Definicoes de arenas");
        map.put("Arena editor", "Editor de arena");
        map.put("Arena ID", "Arena ID");
        map.put("Arena players radius", "Raio dos jogadores da arena");
        map.put("Arenas", "Arenas");
        map.put("Attack rate x", "Velocidade de ataque x");
        map.put("Audience mode", "Modo de audiencia");
        map.put("Auto (recommended)", "Auto (recomendado)");
        map.put("Automatic horde mode", "Modo horda automatica");
        map.put("Boss definitions", "Definicoes de bosses");
        map.put("Boss editor", "Editor de boss");
        map.put("Boss ID", "Boss ID");
        map.put("Bosses", "Bosses");
        map.put("Category ID", "Categoria ID");
        map.put("Center (X Y Z)", "Centro (X Y Z)");
        map.put("Current horde", "Horda atual");
        map.put("Current horde arena", "Arena atual da horda");
        map.put("Current horde boss", "Boss atual da horda");
        map.put("Current reward category", "Categoria de recompensa atual");
        map.put("Damage multiplier", "Multiplicador de dano");
        map.put("Elapsed", "Tempo total");
        map.put("Enemies", "Inimigos");
        map.put("Enemy category definitions", "Definicoes de categorias de inimigos");
        map.put("Enemy category editor", "Editor de categoria de inimigos");
        map.put("Enemy ID", "Enemy ID");
        map.put("Enemy IDs", "Enemy IDs");
        map.put("Enemy IDs in category", "Enemy IDs na categoria");
        map.put("Enemy type", "Tipo inimigo");
        map.put("Event", "Evento");
        map.put("Exit", "Sair");
        map.put("Exit *", "Sair *");
        map.put("Final boss", "Boss final");
        map.put("Horde", "Horda");
        map.put("Horde definitions", "Definicoes de hordas");
        map.put("Horde editor", "Editor de horda");
        map.put("Horde ID", "Horde ID");
        map.put("HP multiplier", "Multiplicador HP");
        map.put("Items", "Itens");
        map.put("Items in category", "Itens na categoria");
        map.put("Kills detected", "Kills detectadas");
        map.put("Main player commands", "Comandos principais de jogador");
        map.put("Next round", "Proxima rodada");
        map.put("No arenas yet. Use Add Arena to create one from your position.", "Ainda nao ha arenas. Use Adicionar arena para criar uma da sua posicao.");
        map.put("No bosses yet. Press Add Boss to create one.", "Ainda nao ha bosses. Pressione Adicionar boss para criar um.");
        map.put("No enemy categories yet. Press Add category to create one.", "Ainda nao ha categorias de inimigos. Pressione Adicionar categoria para criar uma.");
        map.put("No horde definitions yet. Press Add horde to create one.", "Ainda nao ha definicoes de horda. Pressione Adicionar horda para criar uma.");
        map.put("No reward categories yet. Press Add category to create one.", "Ainda nao ha categorias de recompensa. Pressione Adicionar categoria para criar uma.");
        map.put("None", "Vazio");
        map.put("Player *", "Jogador *");
        map.put("Player audience definitions", "Definicoes de audiencia de jogadores");
        map.put("Player editor", "Editor de jogador");
        map.put("Player deaths", "Mortes de jogadores");
        map.put("Quick guide for Horde PVE Config (v1.3.0)", "Guia rapida para Horde PVE Config (v1.3.0)");
        map.put("Refresh players", "Atualizar jogadores");
        map.put("Reload and deployment", "Recarga e deploy");
        map.put("Reward category definitions", "Definicoes de categorias de recompensa");
        map.put("Reward category editor", "Editor de categoria de recompensa");
        map.put("Reward item ID", "Reward item ID");
        map.put("Rewards and sounds (start/victory ID and volume) are also persisted.", "Recompensas e sons (ID/volume inicio-vitoria) tambem sao guardados.");
        map.put("Round start", "Inicio rodada");
        map.put("Round victory", "Vitoria rodada");
        map.put("Save arena", "Guardar arena");
        map.put("Save boss", "Guardar boss");
        map.put("Save category", "Guardar categoria");
        map.put("Save horde", "Guardar horda");
        map.put("Save player mode", "Guardar modo jogador");
        map.put("Save sounds", "Guardar sons");
        map.put("Size multiplier", "Multiplicador tamanho");
        map.put("Sound configuration", "Configuracao de sons");
        map.put("Sound editor", "Editor de sons");
        map.put("Sound ID", "Sound ID");
        map.put("Spectator *", "Espectador *");
        map.put("Start every (minutes)", "Iniciar a cada (minutos)");
        map.put("Tier", "Tier");
        map.put("Tip: save sounds here or with Save config.", "Dica: guarde sons aqui ou com Guardar config.");
        map.put("Total spawned", "Spawn total");
        map.put("Volume", "Volume");
        map.put("What Save Config stores", "O que Guardar config salva");
        map.put("Center/world, min-max spawn radius, players area radius and interface language.", "Centro/mundo, raio min-max de spawn, raio de area de jogadores e idioma da interface.");
        map.put("Horde definitions by ID (enemy type, radii, rounds, scaling) plus selected arena/boss/horde.", "Definicoes de horda por ID (tipo inimigo, raios, rodadas, escala) mais arena/boss/horda selecionados.");
        map.put("enemy-categories.json and reward-items.json: enemy categories and reward catalogs.", "enemy-categories.json e reward-items.json: categorias de inimigos e catalogos de recompensa.");
        map.put("horde-definitions.json: editable horde presets shown in the Horde tab.", "horde-definitions.json: presets editaveis de horda exibidos na aba Horda.");
        map.put("horde-sounds.json plus arenas.json/bosses.json: sounds and arena/boss catalogs.", "horde-sounds.json mais arenas.json/bosses.json: catalogos de sons e arena/boss.");
        map.put("'Reload config' or /hordareload config reloads all JSON config files without restart.", "'Recarregar config' ou /hordareload config recarrega todos os JSON sem reiniciar.");
        map.put("Replacing the mod .jar still requires a full server restart.", "Substituir o .jar do mod ainda exige reinicio completo do servidor.");
        map.put("World", "Mundo");
        map.put(" kills", " baixas");
        map.put(" deaths", " mortes");
        map.put("/hordeconfig (aliases: /hconfig /hordecfg /hordepve /spawnve /spawnpve): open config UI.", "/hordeconfig (aliases: /hconfig /hordecfg /hordepve /spawnve /spawnpve): abrir UI de configuracao.");
        map.put("/hordeconfig enemy <type> | enemytypes | role <npcRole|auto> | roles | reward <rounds> | spectator <on|off> | player | arearadius <blocks>.", "/hordeconfig enemy <type> | enemytypes | role <npcRole|auto> | roles | reward <rounds> | spectator <on|off> | player | arearadius <blocks>.");
        map.put("/hordeconfig start | stop | status | logs | setspawn | reload.", "/hordeconfig start | stop | status | logs | setspawn | reload.");
        map.put("Coordinates", "Coordenadas");
        map.put("Elementals", "Elementais");
        map.put("Gems", "Gemas");
        map.put("General", "Geral");
        map.put("Goblins", "Goblins");
        map.put("LEADERBOARD", "CLASSIFICACAO");
        map.put("Metals", "Metais");
        map.put("Mithril", "Mithril");
        map.put("Random by category", "Aleatorio por categoria");
        map.put("Random from all", "Aleatorio total");
        map.put("Rare materials", "Materiais raros");
        map.put("Scarak", "Scarak");
        map.put("Special items", "Itens especiais");
        map.put("Special weapons", "Armas especiais");
        map.put("Undead", "Nao mortos");
        map.put("Void", "Vazio");
        map.put("Wild creatures", "Criaturas agressivas");
        map.put("No active horde. Use /hordeconfig to open the interface.", "Nao ha horda ativa. Use /hordeconfig para abrir a interface.");
        map.put("Could not parse the UI event payload.", "Nao foi possivel interpretar o evento da UI.");
        map.put("Could not access the active world to process this UI action.", "Nao foi possivel acessar o mundo ativo para processar esta acao da UI.");
        map.put("Internal error while processing horde UI. Check server logs and try again.", "Erro interno ao processar a UI da horda. Verifique os logs do servidor e tente novamente.");
        map.put("HORDE PVE", "HORDA PVE");
        map.put("State: ", "Estado: ");
        map.put(" | World: ", " | Mundo: ");
        map.put("Round: ", "Rodada: ");
        map.put("Enemies alive: ", "Inimigos vivos: ");
        map.put("Kills: ", "Abates: ");
        map.put(" | Deaths: ", " | Mortes: ");
        map.put("Next round: ", "Proxima rodada: ");
        map.put("Reward: ", "Recompensa: ");
        map.put(" | Every ", " | Cada ");
        map.put(" round(s)", " rodada(s)");
        map.put("Horde active | Round ", "Horda ativa | Rodada ");
        map.put(" | Remaining enemies: ", " | Inimigos restantes: ");
        map.put(" | Total spawned: ", " | Spawn total: ");
        map.put(" | Kills detected: ", " | Kills detectadas: ");
        map.put(" | Player deaths: ", " | Mortes de jogadores: ");
        map.put(" | Type: ", " | Tipo: ");
        map.put(" | Real role: ", " | Role real: ");
        map.put(" | Players x", " | Jogadores x");
        map.put(" | Locked players: ", " | Jogadores bloqueados: ");
        map.put(" | Spectators: ", " | Espectadores: ");
        map.put(" | Levels: ", " | Niveis: ");
        map.put(" | Final boss: ", " | Chefe final: ");
        map.put(" | Reward every: ", " | Recompensa a cada: ");
        map.put(" | Item: ", " | Item: ");
        map.put("The /horda command does not use subcommands. Use /hordahelp.", "O comando /horda nao usa subcomandos. Use /hordahelp.");
        map.put("No NPC roles are available to spawn enemies.", "Nao ha roles de NPC disponiveis para spawnar inimigos.");
        map.put("Could not create horde (role used: ", "Nao foi possivel criar a horda (role usado: ");
        map.put("Horde created: ", "Horda criada: ");
        map.put("/12 enemies (role: ", "/12 inimigos (role: ");
        map.put("[Horde PVE] Help", "[Horde PVE] Ajuda");
        map.put("/hordahelp -> show this help", "/hordahelp -> mostrar esta ajuda");
        map.put("/hordeconfig -> open configuration (aliases: /hconfig /hordecfg /hordepve /spawnve /spawnpve)", "/hordeconfig -> abrir configuracao (aliases: /hconfig /hordecfg /hordepve /spawnve /spawnpve)");
        map.put("/hordeconfig enemy <category> | enemytypes", "/hordeconfig enemy <categoria> | tipos");
        map.put("/hordeconfig role <npcRole|auto> | roles", "/hordeconfig role <npcRole|auto> | roles");
        map.put("/hordeconfig reward <rounds>", "/hordeconfig reward <rodadas>");
        map.put("/hordeconfig spectator <on|off> | player", "/hordeconfig spectator <on|off> | jogador");
        map.put("/hordeconfig arearadius <blocks>", "/hordeconfig arearadius <blocos>");
        map.put("/hordareload [config] (mod/jar requires restart)", "/hordareload [config] (mod/jar exige reinicio)");
        map.put("Plugin reload in progress. Try again in a few seconds.", "Recarga do plugin em andamento. Tente novamente em alguns segundos.");
        map.put("Logs path: ", "Caminho dos logs: ");
        map.put("Hot-reload of .jar mods is not supported. Replace the file and restart the server.", "Hot-reload de mods .jar nao e suportado. Substitua o arquivo e reinicie o servidor.");
        map.put("Invalid subcommand: ", "Subcomando invalido: ");
        map.put(". Use /hordahelp.", ". Use /hordahelp.");
        map.put("Could not open the interface right now. Use /hordahelp.", "Nao foi possivel abrir a interface agora. Use /hordahelp.");
        map.put("The interface failed to open. Check server logs.", "A interface falhou ao abrir. Verifique os logs do servidor.");
        map.put("Usage: /hordeconfig enemy <", "Uso: /hordeconfig enemy <");
        map.put("Detected horde categories and roles:", "Categorias e roles de horda detectados:");
        map.put("Current NPC role: ", "Role NPC atual: ");
        map.put("Usage: /hordeconfig role <npcRole|auto>", "Uso: /hordeconfig role <npcRole|auto>");
        map.put("No NPC roles available.", "Nao ha roles NPC disponiveis.");
        map.put("Available NPC roles (", "Roles NPC disponiveis (");
        map.put("Usage: /hordeconfig reward <rounds>", "Uso: /hordeconfig reward <rodadas>");
        map.put("Reward value must be a positive integer.", "O valor de reward deve ser um numero inteiro positivo.");
        map.put("Current pre-start role: ", "Role previo ao inicio: ");
        map.put("Usage: /hordeconfig spectator <on|off>", "Uso: /hordeconfig spectator <on|off>");
        map.put("Current arena radius: %.2f blocks. Usage: /hordeconfig arearadius <value>", "Raio atual da arena: %.2f blocos. Uso: /hordeconfig arearadius <valor>");
        map.put("Arena radius must be a valid number.", "O raio da arena deve ser um numero valido.");
        map.put("Usage: /hordareload [config]", "Uso: /hordareload [config]");
        map.put("config: reload horde-config.json + enemy-categories.json + reward-items.json + horde-sounds.json", "config: recarrega horde-config.json + enemy-categories.json + reward-items.json + horde-sounds.json");
        map.put("mod/jar/plugin: requires server restart after replacing the .jar", "mod/jar/plugin: exige reiniciar o servidor apos substituir o .jar");
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
        map.put("Actions", "Actions");
        map.put("Add", "Ajouter");
        map.put("Add arena", "Ajouter arene");
        map.put("Add boss", "Ajouter boss");
        map.put("Add category", "Ajouter categorie");
        map.put("Add horde", "Ajouter horde");
        map.put("Amount", "Quantite");
        map.put("Amt", "Qte");
        map.put("Apply auto mode", "Appliquer mode auto");
        map.put("Arena definitions", "Definitions d arenes");
        map.put("Arena editor", "Editeur d arene");
        map.put("Arena ID", "Arena ID");
        map.put("Arena players radius", "Rayon des joueurs d arene");
        map.put("Arenas", "Arenes");
        map.put("Attack rate x", "Vitesse d attaque x");
        map.put("Audience mode", "Mode audience");
        map.put("Auto (recommended)", "Auto (recommande)");
        map.put("Automatic horde mode", "Mode horde automatique");
        map.put("Boss definitions", "Definitions de boss");
        map.put("Boss editor", "Editeur de boss");
        map.put("Boss ID", "Boss ID");
        map.put("Bosses", "Bosses");
        map.put("Category ID", "Categorie ID");
        map.put("Center (X Y Z)", "Centre (X Y Z)");
        map.put("Current horde", "Horde actuelle");
        map.put("Current horde arena", "Arene actuelle de la horde");
        map.put("Current horde boss", "Boss actuel de la horde");
        map.put("Current reward category", "Categorie de recompense actuelle");
        map.put("Damage multiplier", "Multiplicateur de degats");
        map.put("Elapsed", "Temps total");
        map.put("Enemies", "Ennemis");
        map.put("Enemy category definitions", "Definitions de categories d ennemis");
        map.put("Enemy category editor", "Editeur de categorie d ennemis");
        map.put("Enemy ID", "Enemy ID");
        map.put("Enemy IDs", "Enemy IDs");
        map.put("Enemy IDs in category", "Enemy IDs dans la categorie");
        map.put("Enemy type", "Type d ennemi");
        map.put("Event", "Evenement");
        map.put("Exit", "Sortir");
        map.put("Exit *", "Sortir *");
        map.put("Final boss", "Boss final");
        map.put("Horde", "Horde");
        map.put("Horde definitions", "Definitions de hordes");
        map.put("Horde editor", "Editeur de horde");
        map.put("Horde ID", "Horde ID");
        map.put("HP multiplier", "Multiplicateur HP");
        map.put("Items", "Items");
        map.put("Items in category", "Items dans la categorie");
        map.put("Kills detected", "Kills detectees");
        map.put("Main player commands", "Commandes principales du joueur");
        map.put("Next round", "Prochaine manche");
        map.put("No arenas yet. Use Add Arena to create one from your position.", "Pas encore d arenes. Utilisez Ajouter arene depuis votre position.");
        map.put("No bosses yet. Press Add Boss to create one.", "Pas encore de bosses. Appuyez sur Ajouter boss pour en creer un.");
        map.put("No enemy categories yet. Press Add category to create one.", "Pas encore de categories d ennemis. Appuyez sur Ajouter categorie.");
        map.put("No horde definitions yet. Press Add horde to create one.", "Pas encore de definitions de horde. Appuyez sur Ajouter horde.");
        map.put("No reward categories yet. Press Add category to create one.", "Pas encore de categories de recompense. Appuyez sur Ajouter categorie.");
        map.put("None", "Vide");
        map.put("Player *", "Joueur *");
        map.put("Player audience definitions", "Definitions d audience des joueurs");
        map.put("Player editor", "Editeur de joueur");
        map.put("Player deaths", "Morts des joueurs");
        map.put("Quick guide for Horde PVE Config (v1.3.0)", "Guide rapide pour Horde PVE Config (v1.3.0)");
        map.put("Refresh players", "Actualiser joueurs");
        map.put("Reload and deployment", "Rechargement et deploiement");
        map.put("Reward category definitions", "Definitions de categories de recompense");
        map.put("Reward category editor", "Editeur de categorie de recompense");
        map.put("Reward item ID", "Reward item ID");
        map.put("Rewards and sounds (start/victory ID and volume) are also persisted.", "Recompenses et sons (ID/volume debut-victoire) sont aussi sauvegardes.");
        map.put("Round start", "Debut manche");
        map.put("Round victory", "Victoire manche");
        map.put("Save arena", "Sauvegarder arene");
        map.put("Save boss", "Sauvegarder boss");
        map.put("Save category", "Sauvegarder categorie");
        map.put("Save horde", "Sauvegarder horde");
        map.put("Save player mode", "Sauvegarder mode joueur");
        map.put("Save sounds", "Sauvegarder sons");
        map.put("Size multiplier", "Multiplicateur taille");
        map.put("Sound configuration", "Configuration des sons");
        map.put("Sound editor", "Editeur de sons");
        map.put("Sound ID", "Sound ID");
        map.put("Spectator *", "Spectateur *");
        map.put("Start every (minutes)", "Demarrer toutes les (minutes)");
        map.put("Tier", "Tier");
        map.put("Tip: save sounds here or with Save config.", "Astuce: sauvegardez les sons ici ou avec Sauvegarder config.");
        map.put("Total spawned", "Spawn total");
        map.put("Volume", "Volume");
        map.put("What Save Config stores", "Ce que Sauvegarder config stocke");
        map.put("Center/world, min-max spawn radius, players area radius and interface language.", "Centre/monde, rayon min-max de spawn, rayon des joueurs et langue de l interface.");
        map.put("Horde definitions by ID (enemy type, radii, rounds, scaling) plus selected arena/boss/horde.", "Definitions de horde par ID (type ennemi, rayons, manches, echelle) plus arene/boss/horde selectionnes.");
        map.put("enemy-categories.json and reward-items.json: enemy categories and reward catalogs.", "enemy-categories.json et reward-items.json: categories d ennemis et catalogues de recompense.");
        map.put("horde-definitions.json: editable horde presets shown in the Horde tab.", "horde-definitions.json: presets de horde modifiables affiches dans l onglet Horde.");
        map.put("horde-sounds.json plus arenas.json/bosses.json: sounds and arena/boss catalogs.", "horde-sounds.json plus arenas.json/bosses.json: catalogues de sons et d arene/boss.");
        map.put("'Reload config' or /hordareload config reloads all JSON config files without restart.", "'Recharger config' ou /hordareload config recharge tous les JSON sans redemarrage.");
        map.put("Replacing the mod .jar still requires a full server restart.", "Remplacer le .jar du mod exige toujours un redemarrage complet du serveur.");
        map.put("World", "Monde");
        map.put(" kills", " victimes");
        map.put(" deaths", " morts");
        map.put("/hordeconfig (aliases: /hconfig /hordecfg /hordepve /spawnve /spawnpve): open config UI.", "/hordeconfig (aliases: /hconfig /hordecfg /hordepve /spawnve /spawnpve): ouvrir l UI de configuration.");
        map.put("/hordeconfig enemy <type> | enemytypes | role <npcRole|auto> | roles | reward <rounds> | spectator <on|off> | player | arearadius <blocks>.", "/hordeconfig enemy <type> | enemytypes | role <npcRole|auto> | roles | reward <rounds> | spectator <on|off> | player | arearadius <blocks>.");
        map.put("/hordeconfig start | stop | status | logs | setspawn | reload.", "/hordeconfig start | stop | status | logs | setspawn | reload.");
        map.put("Coordinates", "Coordonnees");
        map.put("Elementals", "Elementaires");
        map.put("Gems", "Gemmes");
        map.put("General", "General");
        map.put("Goblins", "Gobelins");
        map.put("LEADERBOARD", "CLASSEMENT");
        map.put("Metals", "Metaux");
        map.put("Mithril", "Mithril");
        map.put("Random by category", "Aleatoire par categorie");
        map.put("Random from all", "Aleatoire total");
        map.put("Rare materials", "Materiaux rares");
        map.put("Scarak", "Scarak");
        map.put("Special items", "Items speciaux");
        map.put("Special weapons", "Armes speciales");
        map.put("Undead", "Morts-vivants");
        map.put("Void", "Vide");
        map.put("Wild creatures", "Creatures agressives");
        map.put("No active horde. Use /hordeconfig to open the interface.", "Aucune horde active. Utilisez /hordeconfig pour ouvrir l interface.");
        map.put("Could not parse the UI event payload.", "Impossible d interpreter l evenement UI.");
        map.put("Could not access the active world to process this UI action.", "Impossible d acceder au monde actif pour traiter cette action UI.");
        map.put("Internal error while processing horde UI. Check server logs and try again.", "Erreur interne lors du traitement de l UI de horde. Verifiez les logs serveur puis reessayez.");
        map.put("HORDE PVE", "HORDE PVE");
        map.put("State: ", "Etat: ");
        map.put(" | World: ", " | Monde: ");
        map.put("Round: ", "Manche: ");
        map.put("Enemies alive: ", "Ennemis vivants: ");
        map.put("Kills: ", "Eliminations: ");
        map.put(" | Deaths: ", " | Morts: ");
        map.put("Next round: ", "Prochaine manche: ");
        map.put("Reward: ", "Recompense: ");
        map.put(" | Every ", " | Chaque ");
        map.put(" round(s)", " manche(s)");
        map.put("Horde active | Round ", "Horde active | Manche ");
        map.put(" | Remaining enemies: ", " | Ennemis restants: ");
        map.put(" | Total spawned: ", " | Spawn total: ");
        map.put(" | Kills detected: ", " | Eliminations detectees: ");
        map.put(" | Player deaths: ", " | Morts des joueurs: ");
        map.put(" | Type: ", " | Type: ");
        map.put(" | Real role: ", " | Role reel: ");
        map.put(" | Players x", " | Joueurs x");
        map.put(" | Locked players: ", " | Joueurs verrouilles: ");
        map.put(" | Spectators: ", " | Spectateurs: ");
        map.put(" | Levels: ", " | Niveaux: ");
        map.put(" | Final boss: ", " | Boss final: ");
        map.put(" | Reward every: ", " | Recompense chaque: ");
        map.put(" | Item: ", " | Item: ");
        map.put("The /horda command does not use subcommands. Use /hordahelp.", "La commande /horda n utilise pas de sous-commandes. Utilisez /hordahelp.");
        map.put("No NPC roles are available to spawn enemies.", "Aucun role NPC disponible pour faire apparaitre des ennemis.");
        map.put("Could not create horde (role used: ", "Impossible de creer la horde (role utilise: ");
        map.put("Horde created: ", "Horde creee: ");
        map.put("/12 enemies (role: ", "/12 ennemis (role: ");
        map.put("[Horde PVE] Help", "[Horde PVE] Aide");
        map.put("/hordahelp -> show this help", "/hordahelp -> afficher cette aide");
        map.put("/hordeconfig -> open configuration (aliases: /hconfig /hordecfg /hordepve /spawnve /spawnpve)", "/hordeconfig -> ouvrir la configuration (aliases: /hconfig /hordecfg /hordepve /spawnve /spawnpve)");
        map.put("/hordeconfig enemy <category> | enemytypes", "/hordeconfig enemy <categorie> | types");
        map.put("/hordeconfig role <npcRole|auto> | roles", "/hordeconfig role <npcRole|auto> | roles");
        map.put("/hordeconfig reward <rounds>", "/hordeconfig reward <manches>");
        map.put("/hordeconfig spectator <on|off> | player", "/hordeconfig spectator <on|off> | joueur");
        map.put("/hordeconfig arearadius <blocks>", "/hordeconfig arearadius <blocs>");
        map.put("/hordareload [config] (mod/jar requires restart)", "/hordareload [config] (mod/jar requiert redemarrage)");
        map.put("Plugin reload in progress. Try again in a few seconds.", "Rechargement du plugin en cours. Reessayez dans quelques secondes.");
        map.put("Logs path: ", "Chemin des logs: ");
        map.put("Hot-reload of .jar mods is not supported. Replace the file and restart the server.", "Le hot-reload des mods .jar n est pas supporte. Remplacez le fichier et redemarrez le serveur.");
        map.put("Invalid subcommand: ", "Sous-commande invalide: ");
        map.put(". Use /hordahelp.", ". Utilisez /hordahelp.");
        map.put("Could not open the interface right now. Use /hordahelp.", "Impossible d ouvrir l interface maintenant. Utilisez /hordahelp.");
        map.put("The interface failed to open. Check server logs.", "L interface n a pas pu s ouvrir. Verifiez les logs serveur.");
        map.put("Usage: /hordeconfig enemy <", "Usage: /hordeconfig enemy <");
        map.put("Detected horde categories and roles:", "Categories et roles de horde detectes:");
        map.put("Current NPC role: ", "Role NPC actuel: ");
        map.put("Usage: /hordeconfig role <npcRole|auto>", "Usage: /hordeconfig role <npcRole|auto>");
        map.put("No NPC roles available.", "Aucun role NPC disponible.");
        map.put("Available NPC roles (", "Roles NPC disponibles (");
        map.put("Usage: /hordeconfig reward <rounds>", "Usage: /hordeconfig reward <manches>");
        map.put("Reward value must be a positive integer.", "La valeur reward doit etre un entier positif.");
        map.put("Current pre-start role: ", "Role avant depart: ");
        map.put("Usage: /hordeconfig spectator <on|off>", "Usage: /hordeconfig spectator <on|off>");
        map.put("Current arena radius: %.2f blocks. Usage: /hordeconfig arearadius <value>", "Rayon actuel de l arene: %.2f blocs. Usage: /hordeconfig arearadius <valeur>");
        map.put("Arena radius must be a valid number.", "Le rayon d arene doit etre un nombre valide.");
        map.put("Usage: /hordareload [config]", "Usage: /hordareload [config]");
        map.put("config: reload horde-config.json + enemy-categories.json + reward-items.json + horde-sounds.json", "config: recharge horde-config.json + enemy-categories.json + reward-items.json + horde-sounds.json");
        map.put("mod/jar/plugin: requires server restart after replacing the .jar", "mod/jar/plugin: requiert un redemarrage serveur apres remplacement du .jar");
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
        map.put("Actions", "Aktionen");
        map.put("Add", "Hinzufugen");
        map.put("Add arena", "Arena hinzufugen");
        map.put("Add boss", "Boss hinzufugen");
        map.put("Add category", "Kategorie hinzufugen");
        map.put("Add horde", "Horde hinzufugen");
        map.put("Amount", "Menge");
        map.put("Amt", "Menge");
        map.put("Apply auto mode", "Auto-Modus anwenden");
        map.put("Arena definitions", "Arena-Definitionen");
        map.put("Arena editor", "Arena-Editor");
        map.put("Arena ID", "Arena ID");
        map.put("Arena players radius", "Arena-Spieler-Radius");
        map.put("Arenas", "Arenen");
        map.put("Attack rate x", "Angriffsgeschwindigkeit x");
        map.put("Audience mode", "Publikumsmodus");
        map.put("Auto (recommended)", "Auto (empfohlen)");
        map.put("Automatic horde mode", "Automatischer Horde-Modus");
        map.put("Boss definitions", "Boss-Definitionen");
        map.put("Boss editor", "Boss-Editor");
        map.put("Boss ID", "Boss ID");
        map.put("Bosses", "Bosses");
        map.put("Category ID", "Kategorie ID");
        map.put("Center (X Y Z)", "Zentrum (X Y Z)");
        map.put("Current horde", "Aktuelle Horde");
        map.put("Current horde arena", "Aktuelle Horde-Arena");
        map.put("Current horde boss", "Aktueller Horde-Boss");
        map.put("Current reward category", "Aktuelle Belohnungskategorie");
        map.put("Damage multiplier", "Schaden-Multiplikator");
        map.put("Elapsed", "Gesamtzeit");
        map.put("Enemies", "Gegner");
        map.put("Enemy category definitions", "Gegnerkategorie-Definitionen");
        map.put("Enemy category editor", "Gegnerkategorie-Editor");
        map.put("Enemy ID", "Enemy ID");
        map.put("Enemy IDs", "Enemy IDs");
        map.put("Enemy IDs in category", "Enemy IDs in Kategorie");
        map.put("Enemy type", "Gegnertyp");
        map.put("Event", "Ereignis");
        map.put("Exit", "Verlassen");
        map.put("Exit *", "Verlassen *");
        map.put("Final boss", "Endboss");
        map.put("Horde", "Horde");
        map.put("Horde definitions", "Horde-Definitionen");
        map.put("Horde editor", "Horde-Editor");
        map.put("Horde ID", "Horde ID");
        map.put("HP multiplier", "HP-Multiplikator");
        map.put("Items", "Items");
        map.put("Items in category", "Items in Kategorie");
        map.put("Kills detected", "Erkannte Kills");
        map.put("Main player commands", "Hauptbefehle fur Spieler");
        map.put("Next round", "Nachste Runde");
        map.put("No arenas yet. Use Add Arena to create one from your position.", "Noch keine Arenen. Nutze Arena hinzufugen von deiner Position.");
        map.put("No bosses yet. Press Add Boss to create one.", "Noch keine Bosses. Drucke Boss hinzufugen.");
        map.put("No enemy categories yet. Press Add category to create one.", "Noch keine Gegnerkategorien. Drucke Kategorie hinzufugen.");
        map.put("No horde definitions yet. Press Add horde to create one.", "Noch keine Horde-Definitionen. Drucke Horde hinzufugen.");
        map.put("No reward categories yet. Press Add category to create one.", "Noch keine Belohnungskategorien. Drucke Kategorie hinzufugen.");
        map.put("None", "Leer");
        map.put("Player *", "Spieler *");
        map.put("Player audience definitions", "Publikumsdefinitionen fur Spieler");
        map.put("Player editor", "Spieler-Editor");
        map.put("Player deaths", "Spieler-Tode");
        map.put("Quick guide for Horde PVE Config (v1.3.0)", "Kurzanleitung fur Horde PVE Config (v1.3.0)");
        map.put("Refresh players", "Spieler aktualisieren");
        map.put("Reload and deployment", "Reload und Deployment");
        map.put("Reward category definitions", "Belohnungskategorie-Definitionen");
        map.put("Reward category editor", "Belohnungskategorie-Editor");
        map.put("Reward item ID", "Reward item ID");
        map.put("Rewards and sounds (start/victory ID and volume) are also persisted.", "Belohnungen und Sounds (Start/Sieg ID und Lautstarke) werden ebenfalls gespeichert.");
        map.put("Round start", "Rundenstart");
        map.put("Round victory", "Rundensieg");
        map.put("Save arena", "Arena speichern");
        map.put("Save boss", "Boss speichern");
        map.put("Save category", "Kategorie speichern");
        map.put("Save horde", "Horde speichern");
        map.put("Save player mode", "Spielermodus speichern");
        map.put("Save sounds", "Sounds speichern");
        map.put("Size multiplier", "Groessen-Multiplikator");
        map.put("Sound configuration", "Sound-Konfiguration");
        map.put("Sound editor", "Sound-Editor");
        map.put("Sound ID", "Sound ID");
        map.put("Spectator *", "Zuschauer *");
        map.put("Start every (minutes)", "Start alle (Minuten)");
        map.put("Tier", "Tier");
        map.put("Tip: save sounds here or with Save config.", "Tipp: Speichere Sounds hier oder mit Konfig speichern.");
        map.put("Total spawned", "Gesamt gespawnt");
        map.put("Volume", "Lautstarke");
        map.put("What Save Config stores", "Was Konfig speichern speichert");
        map.put("Center/world, min-max spawn radius, players area radius and interface language.", "Zentrum/Welt, Min-Max Spawn-Radius, Spielerbereich-Radius und Interface-Sprache.");
        map.put("Horde definitions by ID (enemy type, radii, rounds, scaling) plus selected arena/boss/horde.", "Horde-Definitionen pro ID (Gegnertyp, Radien, Runden, Skalierung) plus gewahlte Arena/Boss/Horde.");
        map.put("enemy-categories.json and reward-items.json: enemy categories and reward catalogs.", "enemy-categories.json und reward-items.json: Gegnerkategorien und Belohnungskataloge.");
        map.put("horde-definitions.json: editable horde presets shown in the Horde tab.", "horde-definitions.json: bearbeitbare Horde-Presets im Horde-Tab.");
        map.put("horde-sounds.json plus arenas.json/bosses.json: sounds and arena/boss catalogs.", "horde-sounds.json plus arenas.json/bosses.json: Sound- und Arena/Boss-Kataloge.");
        map.put("'Reload config' or /hordareload config reloads all JSON config files without restart.", "'Konfig neu laden' oder /hordareload config laedt alle JSONs ohne Neustart.");
        map.put("Replacing the mod .jar still requires a full server restart.", "Das Ersetzen der Mod-.jar erfordert weiterhin einen kompletten Server-Neustart.");
        map.put("World", "Welt");
        map.put(" kills", " Abschusse");
        map.put(" deaths", " Tode");
        map.put("/hordeconfig (aliases: /hconfig /hordecfg /hordepve /spawnve /spawnpve): open config UI.", "/hordeconfig (aliases: /hconfig /hordecfg /hordepve /spawnve /spawnpve): Konfig-UI offnen.");
        map.put("/hordeconfig enemy <type> | enemytypes | role <npcRole|auto> | roles | reward <rounds> | spectator <on|off> | player | arearadius <blocks>.", "/hordeconfig enemy <type> | enemytypes | role <npcRole|auto> | roles | reward <rounds> | spectator <on|off> | player | arearadius <blocks>.");
        map.put("/hordeconfig start | stop | status | logs | setspawn | reload.", "/hordeconfig start | stop | status | logs | setspawn | reload.");
        map.put("Coordinates", "Koordinaten");
        map.put("Elementals", "Elementare");
        map.put("Gems", "Edelsteine");
        map.put("General", "Allgemein");
        map.put("Goblins", "Goblins");
        map.put("LEADERBOARD", "BESTENLISTE");
        map.put("Metals", "Metalle");
        map.put("Mithril", "Mithril");
        map.put("Random by category", "Zufallig nach Kategorie");
        map.put("Random from all", "Zufallig aus allen");
        map.put("Rare materials", "Seltene Materialien");
        map.put("Scarak", "Scarak");
        map.put("Special items", "Spezial-Items");
        map.put("Special weapons", "Spezialwaffen");
        map.put("Undead", "Untote");
        map.put("Void", "Leere");
        map.put("Wild creatures", "Aggressive Kreaturen");
        map.put("No active horde. Use /hordeconfig to open the interface.", "Keine aktive Horde. Nutze /hordeconfig, um die Oberflache zu offnen.");
        map.put("Could not parse the UI event payload.", "Das UI-Ereignis konnte nicht gelesen werden.");
        map.put("Could not access the active world to process this UI action.", "Auf die aktive Welt konnte fur diese UI-Aktion nicht zugegriffen werden.");
        map.put("Internal error while processing horde UI. Check server logs and try again.", "Interner Fehler bei der Verarbeitung der Horde-UI. Prufe Server-Logs und versuche es erneut.");
        map.put("HORDE PVE", "HORDE PVE");
        map.put("State: ", "Status: ");
        map.put(" | World: ", " | Welt: ");
        map.put("Round: ", "Runde: ");
        map.put("Enemies alive: ", "Lebende Gegner: ");
        map.put("Kills: ", "Kills: ");
        map.put(" | Deaths: ", " | Tode: ");
        map.put("Next round: ", "Nachste Runde: ");
        map.put("Reward: ", "Belohnung: ");
        map.put(" | Every ", " | Jede ");
        map.put(" round(s)", " Runde(n)");
        map.put("Horde active | Round ", "Horde aktiv | Runde ");
        map.put(" | Remaining enemies: ", " | Verbleibende Gegner: ");
        map.put(" | Total spawned: ", " | Gesamt gespawnt: ");
        map.put(" | Kills detected: ", " | Erkannte Kills: ");
        map.put(" | Player deaths: ", " | Spieler-Tode: ");
        map.put(" | Type: ", " | Typ: ");
        map.put(" | Real role: ", " | Echte Rolle: ");
        map.put(" | Players x", " | Spieler x");
        map.put(" | Locked players: ", " | Gesperrte Spieler: ");
        map.put(" | Spectators: ", " | Zuschauer: ");
        map.put(" | Levels: ", " | Level: ");
        map.put(" | Final boss: ", " | Endboss: ");
        map.put(" | Reward every: ", " | Belohnung alle: ");
        map.put(" | Item: ", " | Item: ");
        map.put("The /horda command does not use subcommands. Use /hordahelp.", "Der Befehl /horda nutzt keine Unterbefehle. Nutze /hordahelp.");
        map.put("No NPC roles are available to spawn enemies.", "Keine NPC-Rollen zum Spawnen von Gegnern verfugbar.");
        map.put("Could not create horde (role used: ", "Horde konnte nicht erstellt werden (verwendete Rolle: ");
        map.put("Horde created: ", "Horde erstellt: ");
        map.put("/12 enemies (role: ", "/12 Gegner (Rolle: ");
        map.put("[Horde PVE] Help", "[Horde PVE] Hilfe");
        map.put("/hordahelp -> show this help", "/hordahelp -> diese Hilfe anzeigen");
        map.put("/hordeconfig -> open configuration (aliases: /hconfig /hordecfg /hordepve /spawnve /spawnpve)", "/hordeconfig -> Konfiguration offnen (aliases: /hconfig /hordecfg /hordepve /spawnve /spawnpve)");
        map.put("/hordeconfig enemy <category> | enemytypes", "/hordeconfig enemy <kategorie> | enemytypes");
        map.put("/hordeconfig role <npcRole|auto> | roles", "/hordeconfig role <npcRole|auto> | roles");
        map.put("/hordeconfig reward <rounds>", "/hordeconfig reward <runden>");
        map.put("/hordeconfig spectator <on|off> | player", "/hordeconfig spectator <on|off> | spieler");
        map.put("/hordeconfig arearadius <blocks>", "/hordeconfig arearadius <blocke>");
        map.put("/hordareload [config] (mod/jar requires restart)", "/hordareload [config] (mod/jar erfordert Neustart)");
        map.put("Plugin reload in progress. Try again in a few seconds.", "Plugin-Neuladen lauft. Versuche es in ein paar Sekunden erneut.");
        map.put("Logs path: ", "Log-Pfad: ");
        map.put("Hot-reload of .jar mods is not supported. Replace the file and restart the server.", "Hot-Reload von .jar Mods wird nicht unterstutzt. Ersetze die Datei und starte den Server neu.");
        map.put("Invalid subcommand: ", "Ungueltiger Unterbefehl: ");
        map.put(". Use /hordahelp.", ". Nutze /hordahelp.");
        map.put("Could not open the interface right now. Use /hordahelp.", "Die Oberflache konnte gerade nicht geoffnet werden. Nutze /hordahelp.");
        map.put("The interface failed to open. Check server logs.", "Die Oberflache konnte nicht geoffnet werden. Prufe die Server-Logs.");
        map.put("Usage: /hordeconfig enemy <", "Usage: /hordeconfig enemy <");
        map.put("Detected horde categories and roles:", "Erkannte Horde-Kategorien und Rollen:");
        map.put("Current NPC role: ", "Aktuelle NPC-Rolle: ");
        map.put("Usage: /hordeconfig role <npcRole|auto>", "Usage: /hordeconfig role <npcRole|auto>");
        map.put("No NPC roles available.", "Keine NPC-Rollen verfugbar.");
        map.put("Available NPC roles (", "Verfugbare NPC-Rollen (");
        map.put("Usage: /hordeconfig reward <rounds>", "Usage: /hordeconfig reward <rounds>");
        map.put("Reward value must be a positive integer.", "Der Reward-Wert muss eine positive Ganzzahl sein.");
        map.put("Current pre-start role: ", "Aktuelle Vorstart-Rolle: ");
        map.put("Usage: /hordeconfig spectator <on|off>", "Usage: /hordeconfig spectator <on|off>");
        map.put("Current arena radius: %.2f blocks. Usage: /hordeconfig arearadius <value>", "Aktueller Arena-Radius: %.2f Blocke. Usage: /hordeconfig arearadius <value>");
        map.put("Arena radius must be a valid number.", "Der Arena-Radius muss eine gueltige Zahl sein.");
        map.put("Usage: /hordareload [config]", "Usage: /hordareload [config]");
        map.put("config: reload horde-config.json + enemy-categories.json + reward-items.json + horde-sounds.json", "config: lade horde-config.json + enemy-categories.json + reward-items.json + horde-sounds.json neu");
        map.put("mod/jar/plugin: requires server restart after replacing the .jar", "mod/jar/plugin: erfordert Server-Neustart nach dem Ersetzen der .jar");
        return map;
    }
}
