package com.seuprojeto;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import androidx.annotation.Nullable;
import androidx.media3.ui.PlayerControlView;

public class CustomPlayerControlView extends PlayerControlView {

    private PlayerOvelayListener overlayListener;

    public void setOverlayListener(PlayerOvelayListener listener) {
        this.overlayListener = listener;
    }

    public CustomPlayerControlView(Context context) {
        super(context);
    }

    public CustomPlayerControlView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Se o botão OK/Enter for pressionado
        if (event.getAction() == KeyEvent.ACTION_DOWN &&
           (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER || event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
            
            // Apenas avisa a PlayerActivity para executar a ação
            if (overlayListener != null) {
                overlayListener.onToggleOverlay();
            }
            // Retorna 'true' para consumir o evento e impedir a ação padrão (play/pause)
            return true;
        }
        
        // Para todas as outras teclas, deixa o comportamento padrão do player funcionar
        return super.dispatchKeyEvent(event);
    }
}