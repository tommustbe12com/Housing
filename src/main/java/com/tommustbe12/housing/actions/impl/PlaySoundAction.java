package com.tommustbe12.housing.actions.impl;

import com.tommustbe12.housing.actions.Action;
import com.tommustbe12.housing.actions.ActionContext;
import org.bukkit.Sound;

public final class PlaySoundAction implements Action {
    private final String sound;
    private final float volume;
    private final float pitch;

    public PlaySoundAction(String sound, float volume, float pitch) {
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
    }

    @Override
    public String type() {
        return "play_sound";
    }

    public String sound() { return sound; }
    public float volume() { return volume; }
    public float pitch() { return pitch; }

    @Override
    public void execute(ActionContext ctx) {
        if (ctx == null || ctx.player() == null) return;
        try {
            Sound s = Sound.valueOf(sound);
            ctx.player().playSound(ctx.player().getLocation(), s, volume, pitch);
        } catch (Exception ignored) {
        }
    }
}

