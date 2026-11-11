package com.decomp.rsdkv4;

import android.os.Bundle;
import android.os.Environment;

import java.io.File;

import org.libsdl.app.*;

public class RSDKv4 extends SDLActivity {

    // Pasta do jogo (dentro do armazenamento). O menu manda a pasta certa pelo Intent.
    private String pastaJogo = "SonicOriginsMOBILE/Sonic1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Precisa ser lido ANTES do super.onCreate, que inicia o motor
        String p = (getIntent() != null) ? getIntent().getStringExtra("pasta") : null;
        if (p != null && p.length() > 0) {
            pastaJogo = p;
        }
        super.onCreate(savedInstanceState);
    }

    // Pausa/retoma a thread nativa quando nao ha multi-janela
    @Override
    protected void onPause() {
        super.onPause();
        if (mHasMultiWindow) {
            pauseNativeThread();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mHasMultiWindow) {
            resumeNativeThread();
        }
    }

    // O motor guarda estado global: ao sair do jogo, encerra o processo do jogo
    // para o proximo jogo comecar limpo (o menu fica em outro processo).
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

    // Chamado pelo motor (C++) via JNI: devolve a pasta do jogo
    public String getBasePath() {
        String p = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + pastaJogo;
        new File(p).mkdirs();
        new File(p + "/mods").mkdirs();
        return p + "/";
    }
}
