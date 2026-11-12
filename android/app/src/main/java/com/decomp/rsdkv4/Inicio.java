package com.decomp.rsdkv4;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;

public class Inicio extends Activity {

    private static final String RAIZ = "SonicOriginsMOBILE";
    private static final String[] JOGOS = {"Sonic the Hedgehog", "Sonic the Hedgehog 2", "Sonic CD"};
    private static final String[] PASTAS = {RAIZ + "/Sonic1", RAIZ + "/Sonic2", RAIZ + "/SonicCD"};
    private static final String PASTA_VISUAL = RAIZ + "/Visual";
    private static final int REQ_CAPA = 100;   // + indice do jogo (100, 101, 102)
    private static final int REQ_LOGO = 150;   // + indice do jogo (150, 151, 152)
    private static final int REQ_MUSICA = 200;
    private static final int REQ_FUNDO = 300;
    private static final int REQ_AVISO = 400;

    private int atual = 0;
    private boolean pediuAcesso = false;
    private boolean animando = false;
    private boolean musicaLigada = true;
    private MediaPlayer player;
    private SharedPreferences prefs;
    private GestureDetector gestos;

    // vídeo de fundo
    private TextureView fundo;
    private Surface superficie;
    private MediaPlayer videoPlayer;
    private int videoW = 0;
    private int videoH = 0;

    private LinearLayout pagina;
    private ImageView capa;
    private ImageView logo;
    private TextView titulo;
    private TextView status;
    private TextView pontos;
    private Button botaoJogar;
    private Button botaoEsq;
    private Button botaoDir;
    private Button botaoAcesso;
    private Button botaoMusica;

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    // ------------------------------------------------------------------
    // Montagem da tela (horizontal): fundo em vídeo, capa + informações, setas
    // ------------------------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("origins", MODE_PRIVATE);
        musicaLigada = prefs.getBoolean("musica", true);

        FrameLayout raiz = new FrameLayout(this);
        raiz.setBackgroundColor(Color.parseColor("#0B1A6B"));

        // Vídeo de fundo (o arquivo é o que VOCÊ escolhe) + camada escura para dar leitura
        fundo = new TextureView(this);
        fundo.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture st, int w, int h) {
                superficie = new Surface(st);
                iniciarVideo();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture st, int w, int h) {
                ajustarVideo();
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture st) {
                pararVideo();
                if (superficie != null) {
                    superficie.release();
                    superficie = null;
                }
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture st) {
            }
        });
        raiz.addView(fundo, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        View sombra = new View(this);
        sombra.setBackgroundColor(Color.parseColor("#55000000"));
        raiz.addView(sombra, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

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

        // Logo do jogo (imagem que VOCÊ escolhe); sem logo, aparece o nome em texto
        logo = new ImageView(this);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        info.addView(logo, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(80)));

        titulo = new TextView(this);
        titulo.setTextColor(Color.WHITE);
        titulo.setTextSize(26);
        titulo.setTypeface(Typeface.DEFAULT_BOLD);
        titulo.setGravity(Gravity.CENTER);
        info.addView(titulo);

        status = new TextView(this);
        status.setTextSize(13);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(6), 0, dp(12));
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
                ViewGroup.LayoutParams.MATCH_PARENT, dp(60)));

        // Linha de botões pequenos: Mods | Trocar capa | Trocar logo
        LinearLayout linhaBotoes = new LinearLayout(this);
        linhaBotoes.setOrientation(LinearLayout.HORIZONTAL);
        Button bMods = pequeno(new View.OnClickListener() {
            public void onClick(View v) {
                abrirMods();
            }
        });
        bMods.setText("Mods");
        Button bCapa = pequeno(new View.OnClickListener() {
            public void onClick(View v) {
                escolherArquivo("image/*", REQ_CAPA + atual);
            }
        });
        bCapa.setText("Trocar capa");
        Button bLogo = pequeno(new View.OnClickListener() {
            public void onClick(View v) {
                escolherArquivo("image/*", REQ_LOGO + atual);
            }
        });
        bLogo.setText("Trocar logo");
        LinearLayout.LayoutParams lpPeq = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lpPeq.setMargins(dp(3), 0, dp(3), 0);
        linhaBotoes.addView(bMods, lpPeq);
        linhaBotoes.addView(bCapa, lpPeq);
        linhaBotoes.addView(bLogo, lpPeq);
        LinearLayout.LayoutParams lpLinha = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpLinha.topMargin = dp(8);
        info.addView(linhaBotoes, lpLinha);

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
        topo.setTextColor(Color.rgb(200, 210, 240));
        topo.setTextSize(13);
        topo.setTypeface(Typeface.DEFAULT_BOLD);
        FrameLayout.LayoutParams lpTopo = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.LEFT);
        lpTopo.topMargin = dp(10);
        lpTopo.leftMargin = dp(12);
        raiz.addView(topo, lpTopo);

        // Botões de música e fundo (topo, direita)
        LinearLayout barraTopo = new LinearLayout(this);
        barraTopo.setOrientation(LinearLayout.HORIZONTAL);
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
        Button trocarFundo = pequeno(new View.OnClickListener() {
            public void onClick(View v) {
                escolherArquivo("video/*", REQ_FUNDO);
            }
        });
        trocarFundo.setText("Trocar fundo");
        barraTopo.addView(botaoMusica);
        barraTopo.addView(trocarMusica);
        Button logoAviso = pequeno(new View.OnClickListener() {
            public void onClick(View v) {
                escolherArquivo("image/*", REQ_AVISO);
            }
        });
        logoAviso.setText("Logo do aviso");
        barraTopo.addView(trocarFundo);
        barraTopo.addView(logoAviso);
        FrameLayout.LayoutParams lpBarra = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.RIGHT);
        lpBarra.topMargin = dp(4);
        lpBarra.rightMargin = dp(8);
        raiz.addView(barraTopo, lpBarra);

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
        b.setPadding(dp(8), dp(6), dp(8), dp(6));
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
        iniciarVideo();
    }

    @Override
    protected void onPause() {
        super.onPause();
        pararMusica();
        pararVideo();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pararMusica();
        pararVideo();
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

    private File arquivoLogo(int i) {
        return new File(pastaVisual(), "logo" + i + ".img");
    }

    private File arquivoMusica() {
        return new File(pastaVisual(), "musica.audio");
    }

    private File arquivoFundo() {
        return new File(pastaVisual(), "fundo.video");
    }

    private File arquivoAviso() {
        return new File(pastaVisual(), "aviso.img");
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
    // settings.ini do jogo (usado para ligar o HardwareRenderer e o Dev Menu)
    // ------------------------------------------------------------------
    private void definirIni(File ini, String secao, String chave, String valor) {
        try {
            ArrayList<String> linhas = new ArrayList<String>();
            if (ini.exists()) {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(new FileInputStream(ini), "UTF-8"));
                String l;
                while ((l = br.readLine()) != null) {
                    linhas.add(l);
                }
                br.close();
            }

            int iSecao = -1;
            for (int i = 0; i < linhas.size(); i++) {
                if (linhas.get(i).trim().equalsIgnoreCase("[" + secao + "]")) {
                    iSecao = i;
                    break;
                }
            }

            if (iSecao < 0) {
                if (!linhas.isEmpty() && linhas.get(linhas.size() - 1).trim().length() > 0) {
                    linhas.add("");
                }
                linhas.add("[" + secao + "]");
                linhas.add(chave + "=" + valor);
            } else {
                int fim = linhas.size();
                for (int i = iSecao + 1; i < linhas.size(); i++) {
                    if (linhas.get(i).trim().startsWith("[")) {
                        fim = i;
                        break;
                    }
                }
                boolean achou = false;
                for (int i = iSecao + 1; i < fim; i++) {
                    String t = linhas.get(i).trim();
                    int eq = t.indexOf('=');
                    if (eq > 0 && t.substring(0, eq).trim().equalsIgnoreCase(chave)) {
                        // mantém a grafia da chave que já estava no arquivo
                        linhas.set(i, t.substring(0, eq).trim() + "=" + valor);
                        achou = true;
                        break;
                    }
                }
                if (!achou) {
                    linhas.add(iSecao + 1, chave + "=" + valor);
                }
            }

            ini.getParentFile().mkdirs();
            OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(ini), "UTF-8");
            for (int i = 0; i < linhas.size(); i++) {
                w.write(linhas.get(i));
                w.write("\n");
            }
            w.close();
        } catch (Exception e) {
            Toast.makeText(this, "Não consegui editar o settings.ini", Toast.LENGTH_LONG).show();
        }
    }

    // ------------------------------------------------------------------
    // Mods
    // ------------------------------------------------------------------
    private void abrirMods() {
        if (!temAcesso()) {
            pedirAcesso();
            return;
        }
        File dir = new File(pasta(atual), "mods");
        dir.mkdirs();

        StringBuilder lista = new StringBuilder();
        int n = 0;
        File[] subs = dir.listFiles();
        if (subs != null) {
            Arrays.sort(subs);
            for (int i = 0; i < subs.length; i++) {
                if (subs[i].isDirectory()) {
                    lista.append("\u2022 ").append(subs[i].getName()).append("\n");
                    n++;
                }
            }
        }

        String msg = "Pasta dos mods:\n" + dir.getAbsolutePath() + "/\n\n";
        if (n == 0) {
            msg += "Nenhum mod encontrado. Coloque cada mod numa pasta aqui dentro (a pasta do mod tem o mod.ini).\n";
        } else {
            msg += "Mods encontrados:\n" + lista;
        }
        msg += "\nPara ligar ou desligar um mod, ative o Dev Menu (botão abaixo), abra o jogo, "
                + "abra o Dev Menu e escolha Mods.";

        new AlertDialog.Builder(this)
                .setTitle("Mods - " + JOGOS[atual])
                .setMessage(msg)
                .setPositiveButton("Ativar Dev Menu", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        definirIni(new File(pasta(atual), "settings.ini"), "Dev", "DevMenu", "y");
                        Toast.makeText(Inicio.this, "Dev Menu ativado", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Fechar", null)
                .show();
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
        carregarLogo();
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

        // Sonic CD: liga o HardwareRenderer (necessário para os dados de celular)
        if (atual == 2) {
            definirIni(new File(pasta(2), "settings.ini"), "Window", "HardwareRenderer", "y");
        }

        Intent it = new Intent(this, (atual == 2) ? RSDKv3.class : RSDKv4.class);
        it.putExtra("pasta", PASTAS[atual]);
        startActivity(it);
    }

    // ------------------------------------------------------------------
    // Capas e logos (imagens que VOCÊ escolhe; ficam na pasta Visual)
    // ------------------------------------------------------------------
    private void carregarCapa() {
        Bitmap bmp = null;
        if (temAcesso()) {
            File f = arquivoCapa(atual);
            if (f.exists()) {
                bmp = decodificar(f.getAbsolutePath(), 1);
            }
        }
        if (bmp != null) {
            capa.setBackgroundColor(Color.TRANSPARENT);
            capa.setImageBitmap(bmp);
        } else {
            capa.setImageDrawable(null);
            capa.setBackgroundColor(Color.parseColor("#9914309E"));
        }
    }

    private void carregarLogo() {
        Bitmap bmp = null;
        if (temAcesso()) {
            File f = arquivoLogo(atual);
            if (f.exists()) {
                bmp = decodificar(f.getAbsolutePath(), 2);
            }
        }
        if (bmp != null) {
            logo.setImageBitmap(bmp);
            logo.setVisibility(View.VISIBLE);
            titulo.setVisibility(View.GONE);
        } else {
            logo.setImageDrawable(null);
            logo.setVisibility(View.GONE);
            titulo.setVisibility(View.VISIBLE);
        }
    }

    // reducao: 1 = altura da tela; 2 = metade da altura da tela
    private Bitmap decodificar(String caminho, int reducao) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(caminho, o);
            int alvo = Math.max(300, getResources().getDisplayMetrics().heightPixels / reducao);
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
            copiarEm(uri, arquivoCapa(req - REQ_CAPA), new Runnable() {
                public void run() {
                    carregarCapa();
                }
            });
        } else if (req >= REQ_LOGO && req < REQ_LOGO + JOGOS.length) {
            copiarEm(uri, arquivoLogo(req - REQ_LOGO), new Runnable() {
                public void run() {
                    carregarLogo();
                }
            });
        } else if (req == REQ_MUSICA) {
            copiarEm(uri, arquivoMusica(), new Runnable() {
                public void run() {
                    musicaLigada = true;
                    prefs.edit().putBoolean("musica", true).apply();
                    atualizarBotaoMusica();
                    tocarMusica();
                }
            });
        } else if (req == REQ_FUNDO) {
            copiarEm(uri, arquivoFundo(), new Runnable() {
                public void run() {
                    iniciarVideo();
                }
            });
        } else if (req == REQ_AVISO) {
            copiarEm(uri, arquivoAviso(), new Runnable() {
                public void run() {
                    Toast.makeText(Inicio.this, "Logo do aviso salvo", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // Copia em segundo plano (vídeos podem ser grandes) e depois executa "depois" na tela
    private void copiarEm(final Uri origem, final File destino, final Runnable depois) {
        Toast.makeText(this, "Copiando...", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            public void run() {
                final boolean ok = copiar(origem, destino);
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (ok) {
                            depois.run();
                        } else {
                            Toast.makeText(Inicio.this, "Não consegui copiar o arquivo",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }).start();
    }

    private boolean copiar(Uri origem, File destino) {
        InputStream in = null;
        OutputStream out = null;
        boolean ok = false;
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
            ok = true;
        } catch (Exception e) {
            ok = false;
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
        if (!ok) {
            destino.delete();
        }
        return ok;
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

    // ------------------------------------------------------------------
    // Vídeo de fundo (arquivo que VOCÊ escolhe; toca em loop e sem som)
    // ------------------------------------------------------------------
    private void iniciarVideo() {
        pararVideo();
        if (superficie == null || !temAcesso()) {
            return;
        }
        File f = arquivoFundo();
        if (!f.exists()) {
            return;
        }
        try {
            videoPlayer = new MediaPlayer();
            videoPlayer.setSurface(superficie);
            videoPlayer.setDataSource(f.getAbsolutePath());
            videoPlayer.setLooping(true);
            videoPlayer.setVolume(0f, 0f);
            videoPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                public void onVideoSizeChanged(MediaPlayer mp, int w, int h) {
                    videoW = w;
                    videoH = h;
                    ajustarVideo();
                }
            });
            videoPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                }
            });
            videoPlayer.prepareAsync();
        } catch (Exception e) {
            pararVideo();
        }
    }

    private void pararVideo() {
        if (videoPlayer != null) {
            try {
                videoPlayer.stop();
            } catch (Exception e) {
            }
            try {
                videoPlayer.release();
            } catch (Exception e) {
            }
            videoPlayer = null;
        }
    }

    // Preenche a tela inteira cortando as sobras (sem esticar o vídeo)
    private void ajustarVideo() {
        int vw = fundo.getWidth();
        int vh = fundo.getHeight();
        if (videoW <= 0 || videoH <= 0 || vw <= 0 || vh <= 0) {
            return;
        }
        float escala = Math.max((float) vw / videoW, (float) vh / videoH);
        float sx = (videoW * escala) / vw;
        float sy = (videoH * escala) / vh;
        Matrix m = new Matrix();
        m.setScale(sx, sy, vw / 2f, vh / 2f);
        fundo.setTransform(m);
    }
}
