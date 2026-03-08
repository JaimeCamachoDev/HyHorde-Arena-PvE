/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.server.core.Message
 *  com.hypixel.hytale.server.core.command.system.AbstractCommand
 *  com.hypixel.hytale.server.core.command.system.CommandContext
 *  com.hypixel.hytale.server.core.command.system.arguments.system.Argument
 *  com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg
 *  com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg
 *  com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
 *  com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType
 *  com.hypixel.hytale.server.core.util.EventTitleUtil
 *  javax.annotation.Nonnull
 *  javax.annotation.Nullable
 */
package dev.scripting.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.Argument;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AnnouncementCommand
extends AbstractCommand {
    private final RequiredArg<String> titleArg = this.withRequiredArg("title", "Title Announcement", (ArgumentType)ArgTypes.STRING);
    private final OptionalArg<String> subTitleArg = this.withOptionalArg("subtitle", "Subtitle Announcement", (ArgumentType)ArgTypes.STRING);

    public AnnouncementCommand(String name, String description) {
        super(name, description);
    }

    @Nullable
    protected CompletableFuture<Void> execute(@Nonnull CommandContext ctx) {
        String title = AnnouncementCommand.readArgumentText(ctx, this.titleArg, null);
        String subtitle = AnnouncementCommand.readArgumentText(ctx, this.subTitleArg, "--subtitle");
        if (title == null || title.isBlank()) {
            ctx.sendMessage(Message.raw((String)"Title null"));
            return null;
        }
        if (subtitle == null || subtitle.isBlank()) {
            ctx.sendMessage(Message.raw((String)"Title null"));
            return null;
        }
        EventTitleUtil.showEventTitleToUniverse((Message)Message.raw((String)title), (Message)Message.raw((String)subtitle), (boolean)true, (String)"", (float)4.0f, (float)1.0f, (float)1.0f);
        return null;
    }

    public static String stripQuotes(String input) {
        if (input == null) {
            return null;
        }
        if (input.length() >= 2 && input.startsWith("\"") && input.endsWith("\"")) {
            return input.substring(1, input.length() - 1);
        }
        return input;
    }

    public static String readArgumentText(CommandContext ctx, Argument<?, ?> arg, String flagName) {
        CharSequence[] input = ctx.getInput(arg);
        if (input == null || input.length == 0) {
            return null;
        }
        String joined = String.join((CharSequence)" ", input).trim();
        if (flagName != null && joined.startsWith(flagName + "=")) {
            joined = joined.substring((flagName + "=").length());
        }
        if ((joined = joined.trim()).startsWith("\"")) {
            joined = joined.substring(1);
        }
        if (joined.endsWith("\"")) {
            joined = joined.substring(0, joined.length() - 1);
        }
        return joined.trim();
    }
}

