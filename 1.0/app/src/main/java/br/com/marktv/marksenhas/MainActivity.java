package br.com.marktv.marksenhas;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.TreeMap;

import static br.com.marktv.marksenhas.R.id.containerServices;
import static br.com.marktv.marksenhas.Util.context;

public class MainActivity extends AppCompatActivity {

    private Map<String, Button> buttonsServices;
    private Map<String, Button> buttonsTypes;
    protected LinearLayout.LayoutParams btnParams;
    public String currentService;
    public String currentServiceLabel;
    public String currentType;
    public String currentTypeLabel;
    public String currentPassword;
    public CustomPrinter customPrinter;
    public Thread threadPrinter = null;
    public LinearLayout body;
    public ProgressDialog progressDoalog;
    TextView txtDesc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        App.MAIN_ACTIVITY = this;
        body = (LinearLayout) findViewById(R.id.body);
        App.BODY = body;
        txtDesc = (TextView) findViewById(R.id.txtDesc);

        //Carrega o diálogo de espera
        progressDoalog = new ProgressDialog(MainActivity.this);
        progressDoalog.setMax(100);
        progressDoalog.setMessage("Imprimindo...");
        progressDoalog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        //Configurações iniciais
        dontSleep();
        context = getApplicationContext();
        txtDesc.setText(App.MSG_DESC_SERVICE);

        //Carrega os dados do servidor
        App.URL_SERVER = Util.loadUrlServer();

        //Carrega as permissões
        if ( Build.VERSION.SDK_INT >= 23 && !hasPermissions() ) {
            requestPermissions();
        } else {

            //Carrega toda a estrutura
            initialize();
        }
    }

    /**
     * Carrega toda a estrutura
     */
    protected void initialize() {

        //Fullscreen
        Util.setFullscreen(body);

        //Inicia a conexão com a impressora
        customPrinter = new CustomPrinter(this);
        customPrinter.start();

        //Formata o campo de texto
        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/MuseoSans-100.otf");
        txtDesc.setTypeface(typeface);

        //Cria os botões
        createButtonsServices();
        createButtonsTypes();
    }

    /**
     * Cria a lista de botões de serviços
     */
    private void createButtonsServices() {
        final LinearLayout containerServices = (LinearLayout) findViewById(R.id.containerServices);
        buttonsServices = new TreeMap<>();
        final MainActivity mainActivity = this;
        final boolean[] buttonsServicesCreated = new boolean[1];
        buttonsServicesCreated[0] = false;

        //Busca os serviços no servidor
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                String json = Util.requestHttp(App.URL_SERVICES);
                if( json == null ) {
                    Util.toastMainThread(mainActivity, "Falha na requisição da consulta dos serviços");

                } else {
                    try {
                        JSONObject jsonResp = new JSONObject(json);
                        JSONArray jsonData = jsonResp.getJSONArray("data");
                        if (jsonData.length() > 0) {

                            for (int i = 0; i < jsonData.length(); i++) {
                                JSONObject service = jsonData.getJSONObject(i);
                                Integer serviceId = service.getInt("id");
                                String serviceName = service.getString("name");
                                String serviceKey = service.getString("key");

                                //Cria os botões
                                Button button = new Button(getApplicationContext());
                                formatButton(button, 1);
                                button.setText(serviceName);
                                button.setTag(serviceKey);
                                buttonsServices.put(serviceKey, button);
                                containerServices.addView(button);

                                //Evento de click do botão
                                button.setOnClickListener(new View.OnClickListener() {
                                    public void onClick(View v) {
                                        Button target = (Button) v;
                                        currentService = target.getTag().toString();
                                        currentServiceLabel = target.getText().toString();
                                        showPage("types");
                                    }
                                });
                            }
                            buttonsServicesCreated[0] = true;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();
        try {
            thread.join();

            //Verifica se os botões foram criados
            if( !buttonsServicesCreated[0] ) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        createButtonsServices();
                    }
                }, 30000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Cria a lista de botões de tipos de atendimentos
     */
    private void createButtonsTypes() {
        LinearLayout containerTypes = (LinearLayout) findViewById(R.id.containerTypes);

        //Evento click
        View.OnClickListener eventClick = new View.OnClickListener() {
            public void onClick(View v) {
                Button target = (Button) v;
                currentType = target.getTag().toString();
                currentTypeLabel = target.getText().toString();

                //Solicita senha no servidor
                //progressDoalog.show();
                Util.toast("Imprimindo...");
                printPassword();
            }
        };

        //Botão Atendimento Convencional
        Button button = new Button(getApplicationContext());
        formatButton(button, 1);
        button.setText("Convencional");
        button.setTag("TYPE_DEFAULT");
        button.setOnClickListener(eventClick);
        containerTypes.addView(button);

        //Botão Atendimento Prioritário
        Button buttonP = new Button(getApplicationContext());
        formatButton(buttonP, 2);
        buttonP.setText("Prioritário");
        buttonP.setTag("TYPE_PRIORITY");
        buttonP.setOnClickListener(eventClick);
        containerTypes.addView(buttonP);

        //Botão Voltar
        Button buttonV = new Button(getApplicationContext());
        buttonV.setBackgroundResource(R.drawable.botao3);
        LinearLayout.LayoutParams btnParamsV = new LinearLayout.LayoutParams(220, 120);
        btnParamsV.setMargins(0, 130, 0, 0);
        buttonV.setLayoutParams(btnParamsV);
        buttonV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toHome();
            }
        });
        containerTypes.addView(buttonV);
    }

    /**
     * Formata o botão
     */
    private void formatButton(Button button, int theme) {
        final Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/MuseoSans-900.otf");
        btnParams = new LinearLayout.LayoutParams(460, 120);
        btnParams.setMargins(0, 20, 0, 0);
        button.setTextSize(30);
        button.setLayoutParams(btnParams);
        button.setTypeface(typeface);
        if( theme == 1 ) {
            button.setBackgroundResource(R.drawable.botao);
            button.setTextColor(Color.parseColor("#9e9e9e"));
        } else {
            button.setBackgroundResource(R.drawable.botao2);
            button.setTextColor(Color.parseColor("#f7f6f7"));
        }
    }

    /**
     * Carrega a próxima senha e envia para a impressora
     */
    private void printPassword() {
        final String url = App.URL_GET_NEXT + "&service=" + currentService + "&type=" +currentType;

        //Recupera a próxima senha da fila
        threadPrinter = new Thread(new Runnable() {
            @Override
            public void run() {
                String json = Util.requestHttp(url);
                if (json == null) {
                    Util.log("Falha na recuperação da senha");
                    finish();
                }
                try {
                    JSONObject jsonResp = new JSONObject(json);
                    JSONObject jsonData = jsonResp.getJSONObject("data");
                    currentPassword = jsonData.getString("password");

                    //Envia o comando para a impressora
                    customPrinter.printPassword(currentServiceLabel, currentTypeLabel, currentPassword);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        threadPrinter.start();
        try {
            threadPrinter.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Retorna a tela inicial
        toHome();
        //progressDoalog.hide();
    }

    @Override
    protected void onActivityResult(int requestCode, final int resultCode, final Intent data) {
        if (data != null && resultCode == RESULT_OK) {
        }
    }

    /**
     * Retorna para a tela inicial
     */
    protected void toHome() {
        showPage("services");
    }

    /**
     * Mantém o dispositivo ativo
     */
    private void dontSleep() {
        //Mantém a tela acesa
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * Alterna entre as telas
     */
    private void showPage(String page) {
        LinearLayout ctnServices = (LinearLayout)findViewById(containerServices);
        LinearLayout ctnType = (LinearLayout)findViewById(R.id.containerTypes);
        ctnServices.setVisibility(View.INVISIBLE);
        ctnType.setVisibility(View.INVISIBLE);

        switch (page) {
            case "services":
                ctnServices.setVisibility(View.VISIBLE);
                txtDesc.setText(App.MSG_DESC_SERVICE);
                break;
            case "types":
                ctnType.setVisibility(View.VISIBLE);
                txtDesc.setText(currentServiceLabel.toUpperCase());
                break;
        }
    }

    /**
     * Verifica se possui as permissões necessárias
     */
    boolean hasPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            boolean hasPermission = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            if (hasPermission) {
                return true;
            }
        }
        return false;
    }

    /**
     * Carrega as permissões do aplicativo
     */
    void requestPermissions() {

        String[] permissions;
        permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};

        //Solicita a permissao
        ActivityCompat.requestPermissions(MainActivity.this, permissions, App.PERMISSION_READ_EXTERNAL_STORAGE);
    }

    @Override
    public void onDestroy() {
        if( threadPrinter != null && threadPrinter.isAlive() ) {
            threadPrinter.interrupt();
        }
        super.onDestroy();
    }

    /**
     * Resposta do pedido de permissão
     */
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean allPermissions = true;
        for( int grant : grantResults ) {
            if( grant != PackageManager.PERMISSION_GRANTED ) {
                allPermissions = false;
            }
        }
        if( !allPermissions ) {
            Util.log("O aplicativo não recebeu as permissões necessárias para executar a playlist");
        } else {

            //Carrega toda a estrutura
            initialize();
        }
    }
}
