package com.decomp.rsdkv4;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class Inicio extends Activity {

    private static final String RAIZ = "SonicCollectionLauncher";
    private static final String[] NOMES = {"Sonic 1", "Sonic 2", "Sonic CD (em breve)"};
    private static final String[] JOGOS = {"Sonic 1", "Sonic 2", "Sonic CD"};
    private static final String[] PASTAS = {RAIZ + "/Sonic1", RAIZ + "/Sonic2", RAIZ + "/SonicCD"};
    private static final boolean[] DISPONIVEL = {true, true, false};

    private final TextView[] status = new TextView[3];
    private final Button[] botoes = new Button[3];
    private Button botaoAcesso;
    private boolean pediuAcesso = false;

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout raiz = new LinearLayout(this);
        raiz.setOrientation(LinearLayout.VERTICAL);
        raiz.setGravity(Gravity.CENTER_VERTICAL);
        raiz.setPadding(dp(24), dp(14), dp(24), dp(14));

        TextView titulo = new TextView(this);
        titulo.setText("SONIC ORIGINS (UNOFFICIAL)");
        titulo.setTextColor(Color.WHITE);
        titulo.setTextSize(24);
        titulo.setTypeface(Typeface.DEFAULT_BOLD);
        titulo.setGravity(Gravity.CENTER);
        raiz.addView(titulo);

        TextView sub = new TextView(this);
        sub.setText("Coloque o seu Data.rsdk na pasta de cada jogo");
        sub.setTextColor(Color.rgb(170, 180, 220));
        sub.setTextSize(13);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(4), 0, dp(8));
        raiz.addView(sub);

        botaoAcesso = new Button(this);
        botaoAcesso.setText("Permitir acesso aos arquivos");
        botaoAcesso.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                pedirAcesso();
            }
        });
        raiz.addView(botaoAcesso);

        // Os 3 jogos lado a lado (tela na horizontal)
        LinearLayout linha = new LinearLayout(this);
        linha.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lpLinha = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpLinha.topMargin = dp(10);
        raiz.addView(linha, lpLinha);

        for (int i = 0; i < 3; i++) {
            final int idx = i;

            LinearLayout coluna = new LinearLayout(this);
            coluna.setOrientation(LinearLayout.VERTICAL);

            Button b = new Button(this);
            b.setText(NOMES[i]);
            b.setTextSize(20);
            b.setAllCaps(false);
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    jogar(idx);
                }
            });
            botoes[i] = b;
            coluna.addView(b, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(84)));

            TextView st = new TextView(this);
            st.setTextSize(12);
            st.setGravity(Gravity.CENTER);
            st.setPadding(0, dp(4), 0, 0);
            status[i] = st;
            coluna.addView(st);

            LinearLayout.LayoutParams lpCol = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            lpCol.setMargins(dp(6), 0, dp(6), 0);
            linha.addView(coluna, lpCol);
        }

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setBackgroundColor(Color.parseColor("#0B1A6B"));
        sv.addView(raiz);
        setContentView(sv);
    }

    @Override
    protected void onResume() {
        super.onResume();
        atualizar();
    }

    private File pasta(int i) {
        return new File(Environment.getExternalStorageDirectory(), PASTAS[i]);
    }

    private File arquivoData(int i) {
        return new File(pasta(i), "Data.rsdk");
    }

    private boolean temAcesso() {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                Object r = Environment.class.getMethod("isExternalStorageManager").invoke(null);
                return (r instanceof Boolean) && ((Boolean) r).booleanValue();
            } catch (Exception e) {
                return false;
            }
        }
        return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void pedirAcesso() {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                Intent i = new Intent("android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION",
                        Uri.parse("package:" + getPackageName()));
                startActivity(i);
            } catch (Exception e) {
                try {
                    startActivity(new Intent("android.settings.MANAGE_ALL_FILES_ACCESS_PERMISSION"));
                } catch (Exception e2) {
                    Toast.makeText(this, "Abra as configurações do app e ative o acesso a todos os arquivos",
                            Toast.LENGTH_LONG).show();
                }
            }
        } else {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
    }

    private void atualizar() {
        boolean ok = temAcesso();
        botaoAcesso.setVisibility(ok ? View.GONE : View.VISIBLE);

        if (!ok) {
            for (int i = 0; i < 3; i++) {
                status[i].setText("Sem permissão de acesso aos arquivos");
                status[i].setTextColor(Color.rgb(230, 170, 90));
            }
            if (!pediuAcesso) {
                pediuAcesso = true;
                pedirAcesso();
            }
            return;
        }

        for (int i = 0; i < 3; i++) {
            if (!DISPONIVEL[i]) {
                status[i].setText("Ainda não disponível");
                status[i].setTextColor(Color.rgb(170, 180, 220));
                continue;
            }
            pasta(i).mkdirs();
            new File(pasta(i), "mods").mkdirs();

            if (arquivoData(i).exists()) {
                status[i].setText("Data.rsdk: OK");
                status[i].setTextColor(Color.rgb(120, 220, 140));
            } else {
                status[i].setText("Data.rsdk faltando em " + PASTAS[i]);
                status[i].setTextColor(Color.rgb(230, 130, 120));
            }
        }
    }

    private void jogar(int i) {
        if (!DISPONIVEL[i]) {
            Toast.makeText(this, JOGOS[i] + " ainda não está disponível", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!temAcesso()) {
            pedirAcesso();
            return;
        }
        if (!arquivoData(i).exists()) {
            new AlertDialog.Builder(this)
                    .setTitle(JOGOS[i])
                    .setMessage("Coloque o Data.rsdk de " + JOGOS[i] + " na pasta " + PASTAS[i]
                            + " para o jogo rodar!\n\nPasta: " + pasta(i).getAbsolutePath() + "/")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        Intent it = new Intent(this, RSDKv4.class);
        it.putExtra("pasta", PASTAS[i]);
        startActivity(it);
    }
}
