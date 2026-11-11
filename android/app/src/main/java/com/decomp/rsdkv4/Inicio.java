package com.decomp.rsdkv4;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class Inicio extends Activity {

    private static final String RAIZ = "SonicOriginsMOBILE";
    private static final String[] JOGOS = {"Sonic the Hedgehog", "Sonic the Hedgehog 2", "Sonic CD"};
    private static final String[] PASTAS = {RAIZ + "/Sonic1", RAIZ + "/Sonic2", RAIZ + "/SonicCD"};
    private static final String PASTA_VISUAL = RAIZ + "/Visual";
    private static final int REQ_CAPA = 100;   // + indice do jogo (100, 101, 102)
    private static final int REQ_MUSICA = 200;

    private int atual = 0;
    private boolean pediuAcesso = false;
    private boolean animando = false;
    private boolean musicaLigada = true;
    private MediaPlayer player;
    private SharedPreferences prefs;
    private GestureDetector gestos;

    private LinearLayout pagina;
    private ImageView capa;
    private TextView titulo;
    private TextView status;
    private TextView pontos;
    private Button botaoJogar;
    private Button botaoCapa;
    private Button botaoEsq;
    private Button botaoDir;
    private Button botaoAcesso;
    private Button botaoMusica;

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    // ------------------------------------------------------------------
    // Montagem da tela (horizontal): capa + informações, setas dos lados
    // ------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("origins", MODE_PRIVATE);
        musicaLigada = prefs.getBoolean("musica", true);

        FrameLayout raiz = new FrameLayout(this);
        raiz.setBackgroundColor(Color.parseColor("#0B1A6B"));

        // Página do jogo atual
        pagina = new LinearLayout(this);
        pagina.setOrientation(LinearLayout.HORIZONTAL);
        pagina.setGravity(Gravity.CENTER);
        pagina.setPadding(dp(76), dp(40), dp(76), dp(30));
        raiz.addView(pagina, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        capa = new ImageView(this);
        capa.setScaleType(ImageView.ScaleType.FIT_CENTER);
        pagina.addView(capa, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setGravity(Gravity.CENTER);
        info.setPadding(dp(20), 0, 0, 0);
        pagina.addView(info, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        titulo = new TextView(this);
        titulo.setTextColor(Color.WHITE);
        titulo.setTextSize(26);
        titulo.setTypeface(Typeface.DEFAULT_BOLD);
        titulo.setGravity(Gravity.CENTER);
        info.addView(titulo);

        status = new TextView(this);
        status.setTextSize(13);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(6), 0, dp(14));
        info.addView(status);

        botaoJogar = new Button(this);
        botaoJogar.setText("JOGAR");
        botaoJogar.setTextSize(22);
        botaoJogar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                jogar();
            }
        });
        info.addView(botaoJogar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(64)));

        botaoCapa = new Button(this);
        botaoCapa.setTextSize(13);
        botaoCapa.setAllCaps(false);
        botaoCapa.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                escolherArquivo("image/*", REQ_CAPA + atual);
            }
        });
        LinearLayout.LayoutParams lpCapa = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        lpCapa.topMargin = dp(10);
        info.addView(botaoCapa, lpCapa);

        // Setas dos lados
        botaoEsq = seta("<", -1);
        FrameLayout.LayoutParams lpEsq = new FrameLayout.LayoutParams(
                dp(56), dp(120), Gravity.LEFT | Gravity.CENTER_VERTICAL);
        lpEsq.leftMargin = dp(10);
        raiz.addView(botaoEsq, lpEsq);

        botaoDir = seta(">", 1);
        FrameLayout.LayoutParams lpDir = new FrameLayout.LayoutParams(
                dp(56), dp(120), Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        lpDir.rightMargin = dp(10);
        raiz.addView(botaoDir, lpDir);

        // Título do app (topo)
        TextView topo = new TextView(this);
        topo.setText("SONIC ORIGINS (UNOFFICIAL)");
        topo.setTextColor(Color.rgb(170, 180, 220));
        topo.setTextSize(13);
        topo.setTypeface(Typeface.DEFAULT_BOLD);
        FrameLayout.LayoutParams lpTopo = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        lpTopo.topMargin = dp(10);
        raiz.addView(topo, lpTopo);

        // Botões de música (topo, direita)
        LinearLayout barraMusica = new LinearLayout(this);
        barraMusica.setOrientation(LinearLayout.HORIZONTAL);
        botaoMusica = pequeno(new View.OnClickListener() {
            public void onClick(View v) {
                alternarMusica();
            }
        });
        Button trocarMusica = pequeno(new View.OnClickListener() {
            public void onClick(View v) {
                escolherArquivo("audio/*", REQ_MUSICA);
            }
        });
        trocarMusica.setText("Trocar música");
        barraMusica.addView(botaoMusica);
        barraMusica.addView(trocarMusica);
        FrameLayout.LayoutParams lpMus = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.RIGHT);
        lpMus.topMargin = dp(4);
        lpMus.rightMargin = dp(8);
        raiz.addView(barraMusica, lpMus);

        // Pontinhos (qual jogo está na tela)
        pontos = new TextView(this);
        pontos.setTextColor(Color.WHITE);
        pontos.setTextSize(16);
        FrameLayout.LayoutParams lpPontos = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        lpPontos.bottomMargin = dp(6);
        raiz.addView(pontos, lpPontos);

        // Botão de permissão (só aparece sem acesso aos arquivos)
        botaoAcesso = new Button(this);
        botaoAcesso.setText("Permitir acesso aos arquivos");
        botaoAcesso.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                pedirAcesso();
            }
        });
        FrameLayout.LayoutParams lpAcesso = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        lpAcesso.topMargin = dp(34);
        raiz.addView(botaoAcesso, lpAcesso);

        // Deslizar para o lado também troca de jogo
        gestos = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
                if (e1 == null || e2 == null) {
                    return false;
                }
                float dx = e2.getX() - e1.getX();
                if (Math.abs(dx) > dp(80) && Math.abs(vx) > Math.abs(vy)) {
                    if (dx < 0) {
                        ir(1);
                    } else {
                        ir(-1);
                    }
                    return true;
                }
                return false;
            }
        });

        setContentView(raiz);
        mostrarJogo();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        gestos.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    private Button seta(String texto, final int delta) {
        Button b = new Button(this);
        b.setText(texto);
        b.setTextSize(26);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ir(delta);
            }
        });
        return b;
    }

    private Button pequeno(View.OnClickListener l) {
        Button b = new Button(this);
        b.setTextSize(11);
        b.setAllCaps(false);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setPadding(dp(10), dp(6), dp(10), dp(6));
        b.setOnClickListener(l);
        return b;
    }

    // ------------------------------------------------------------------
    // Ciclo de vida
    // ------------------------------------------------------------------
    @Override
    protected void onResume() {
        super.onResume();
        boolean ok = temAcesso();
        botaoAcesso.setVisibility(ok ? View.GONE : View.VISIBLE);
        if (!ok && !pediuAcesso) {
            pediuAcesso = true;
            pedirAcesso();
        }
        mostrarJogo();
        tocarMusica();
    }

    @Override
    protected void onPause() {
        super.onPause();
        pararMusica();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pararMusica();
    }

    // ------------------------------------------------------------------
    // Pastas e arquivos
    // ------------------------------------------------------------------
    private File raizArmazenamento() {
        return Environment.getExternalStorageDirectory();
    }

    private File pasta(int i) {
        return new File(raizArmazenamento(), PASTAS[i]);
    }

    private File arquivoData(int i) {
        return new File(pasta(i), "Data.rsdk");
    }

    private File pastaVisual() {
        return new File(raizArmazenamento(), PASTA_VISUAL);
    }

    private File arquivoCapa(int i) {
        return new File(pastaVisual(), "capa" + i + ".img");
    }

    private File arquivoMusica() {
        return new File(pastaVisual(), "musica.audio");
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

    // ------------------------------------------------------------------
    // Navegação entre os jogos
    // ------------------------------------------------------------------
    private void ir(final int delta) {
        final int novo = atual + delta;
        if (novo < 0 || novo >= JOGOS.length || animando) {
            return;
        }
        animando = true;
        final float largura = pagina.getWidth();
        final float sinal = (delta > 0) ? -1f : 1f;

        pagina.animate().translationX(sinal * largura).alpha(0f).setDuration(180)
                .withEndAction(new Runnable() {
                    public void run() {
                        atual = novo;
                        mostrarJogo();
                        pagina.setTranslationX(-sinal * largura);
                        pagina.animate().translationX(0f).alpha(1f).setDuration(180)
                                .withEndAction(new Runnable() {
                                    public void run() {
                                        animando = false;
                                    }
                                }).start();
                    }
                }).start();
    }

    private void mostrarJogo() {
        titulo.setText(JOGOS[atual]);
        botaoEsq.setVisibility(atual > 0 ? View.VISIBLE : View.INVISIBLE);
        botaoDir.setVisibility(atual < JOGOS.length - 1 ? View.VISIBLE : View.INVISIBLE);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < JOGOS.length; i++) {
            sb.append(i == atual ? "\u25CF" : "\u25CB");
            if (i < JOGOS.length - 1) {
                sb.append("  ");
            }
        }
        pontos.setText(sb.toString());

        carregarCapa();
        atualizarStatus();
        atualizarBotaoMusica();
    }

    private void atualizarStatus() {
        if (!temAcesso()) {
            status.setText("Sem permissão de acesso aos arquivos");
            status.setTextColor(Color.rgb(230, 170, 90));
            return;
        }
        pasta(atual).mkdirs();
        new File(pasta(atual), "mods").mkdirs();

        if (arquivoData(atual).exists()) {
            status.setText("Data.rsdk: OK");
            status.setTextColor(Color.rgb(120, 220, 140));
        } else {
            status.setText("Data.rsdk faltando em " + PASTAS[atual]);
            status.setTextColor(Color.rgb(230, 130, 120));
        }
    }

    private void jogar() {
        if (!temAcesso()) {
            pedirAcesso();
            return;
        }
        if (!arquivoData(atual).exists()) {
            new AlertDialog.Builder(this)
                    .setTitle(JOGOS[atual])
                    .setMessage("Coloque o Data.rsdk de " + JOGOS[atual] + " na pasta " + PASTAS[atual]
                            + " para o jogo rodar!\n\nPasta: " + pasta(atual).getAbsolutePath() + "/")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        Intent it = new Intent(this, (atual == 2) ? RSDKv3.class : RSDKv4.class);
        it.putExtra("pasta", PASTAS[atual]);
        startActivity(it);
    }

    // ------------------------------------------------------------------
    // Capas (imagens que VOCÊ escolhe; ficam na pasta Visual, fora do APK)
    // ------------------------------------------------------------------
    private void carregarCapa() {
        Bitmap bmp = null;
        if (temAcesso()) {
            File f = arquivoCapa(atual);
            if (f.exists()) {
                bmp = decodificar(f.getAbsolutePath());
            }
        }
        if (bmp != null) {
            capa.setBackgroundColor(Color.TRANSPARENT);
            capa.setImageBitmap(bmp);
            botaoCapa.setText("Trocar capa");
        } else {
            capa.setImageDrawable(null);
            capa.setBackgroundColor(Color.parseColor("#14309E"));
            botaoCapa.setText("Escolher capa");
        }
    }

    private Bitmap decodificar(String caminho) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(caminho, o);
            int alvo = Math.max(600, getResources().getDisplayMetrics().heightPixels);
            int amostra = 1;
            while (o.outHeight / (amostra * 2) >= alvo) {
                amostra *= 2;
            }
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = amostra;
            return BitmapFactory.decodeFile(caminho, o2);
        } catch (Throwable t) {
            return null;
        }
    }

    private void escolherArquivo(String tipo, int codigo) {
        if (!temAcesso()) {
            pedirAcesso();
            return;
        }
        try {
            Intent it = new Intent(Intent.ACTION_GET_CONTENT);
            it.setType(tipo);
            it.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(it, "Escolher arquivo"), codigo);
        } catch (Exception e) {
            Toast.makeText(this, "Não consegui abrir o seletor de arquivos", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null || data.getData() == null) {
            return;
        }
        Uri uri = data.getData();
        if (req >= REQ_CAPA && req < REQ_CAPA + JOGOS.length) {
            int jogo = req - REQ_CAPA;
            if (copiar(uri, arquivoCapa(jogo)) && jogo == atual) {
                carregarCapa();
            }
        } else if (req == REQ_MUSICA) {
            if (copiar(uri, arquivoMusica())) {
                musicaLigada = true;
                prefs.edit().putBoolean("musica", true).apply();
                atualizarBotaoMusica();
                tocarMusica();
            }
        }
    }

    private boolean copiar(Uri origem, File destino) {
        InputStream in = null;
        OutputStream out = null;
        try {
            destino.getParentFile().mkdirs();
            in = getContentResolver().openInputStream(origem);
            out = new FileOutputStream(destino);
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            out.flush();
            return true;
        } catch (Exception e) {
            Toast.makeText(this, "Não consegui copiar o arquivo", Toast.LENGTH_LONG).show();
            return false;
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e) {
            }
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Exception e) {
            }
        }
    }

    // ------------------------------------------------------------------
    // Música do menu (arquivo que VOCÊ escolhe)
    // ------------------------------------------------------------------
    private void atualizarBotaoMusica() {
        botaoMusica.setText(musicaLigada ? "Música: ON" : "Música: OFF");
    }

    private void alternarMusica() {
        if (!arquivoMusica().exists()) {
            escolherArquivo("audio/*", REQ_MUSICA);
            return;
        }
        musicaLigada = !musicaLigada;
        prefs.edit().putBoolean("musica", musicaLigada).apply();
        atualizarBotaoMusica();
        if (musicaLigada) {
            tocarMusica();
        } else {
            pararMusica();
        }
    }

    private void tocarMusica() {
        pararMusica();
        if (!musicaLigada || !temAcesso()) {
            return;
        }
        File f = arquivoMusica();
        if (!f.exists()) {
            return;
        }
        try {
            player = new MediaPlayer();
            player.setDataSource(f.getAbsolutePath());
            player.setLooping(true);
            player.setVolume(0.8f, 0.8f);
            player.prepare();
            player.start();
        } catch (Exception e) {
            pararMusica();
        }
    }

    private void pararMusica() {
        if (player != null) {
            try {
                player.stop();
            } catch (Exception e) {
            }
            try {
                player.release();
            } catch (Exception e) {
            }
            player = null;
        }
    }
}
