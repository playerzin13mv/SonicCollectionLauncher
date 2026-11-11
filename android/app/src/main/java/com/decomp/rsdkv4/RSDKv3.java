package com.decomp.rsdkv4;

import android.os.Bundle;
import android.os.Environment;

import java.io.File;

import org.libsdl.app.*;

public class RSDKv3 extends SDLActivity {

    // Pasta do jogo (dentro do armazenamento). O menu manda a pasta certa pelo Intent.
    private String pastaJogo = "SonicOriginsMOBILE/SonicCD";

    // O motor do Sonic CD fica na biblioteca libmainv3.so
    @Override
    protected String[] getLibraries() {
        return new String[] { "SDL2", "mainv3" };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Precisa ser lido ANTES do super.onCreate, que inicia o motor
        String p = (getIntent() != null) ? getIntent().getStringExtra("pasta") : null;
        if (p != null && p.length() > 0) {
            pastaJogo = p;
        }
        super.onCreate(savedInstanceState);
    }

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

    // Ao sair do jogo, encerra o processo dele para o proximo comecar limpo
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
