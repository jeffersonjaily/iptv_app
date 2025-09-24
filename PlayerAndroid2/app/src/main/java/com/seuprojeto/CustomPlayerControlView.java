package com.seuprojeto;

import android.content.Context;
import android.media.AudioManager;
import android.util.AttributeSet;
import android.view.KeyEvent;
import androidx.annotation.Nullable;
import androidx.media3.common.Player;
import androidx.media3.ui.PlayerControlView;

public class CustomPlayerControlView extends PlayerControlView {

    private PlayerOvelayListener overlayListener;
    private AudioManager audioManager;

    public void setOverlayListener(PlayerOvelayListener listener) {
        this.overlayListener = listener;
    }

    public CustomPlayerControlView(Context context) {
        super(context);
        init(context);
    }

    public CustomPlayerControlView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        int action = event.getAction();

        // Se o overlay estiver visível, deixa o comportamento padrão acontecer
        if (overlayListener != null && overlayListener.isOverlayVisible()) {
            return super.dispatchKeyEvent(event);
        }

        // Se o overlay estiver oculto, assume o controle das teclas
        if (action == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    if (overlayListener != null) {
                        overlayListener.onToggleOverlay();
                    }
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                    return true;
                case KeyEvent.KEYCODE_DPAD_UP:
                    Player player = getPlayer();
                    if (player != null) {
                        player.seekToPreviousMediaItem();
                    }
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    Player player_down = getPlayer();
                    if (player_down != null) {
                        player_down.seekToNextMediaItem();
                    }
                    return true;
            }
        }
        
        // Para todas as outras teclas, deixa o comportamento padrão do player funcionar
        return super.dispatchKeyEvent(event);
    }
}