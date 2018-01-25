package br.com.marktv.marksenhas;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Configurações iniciais
        dontSleep();
        Util.context = getApplicationContext();
        ((TextView) findViewById(R.id.txtDesc)).setText(App.MSG_DESC_SERVICE);
        //Remove title bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

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

        //Recupera a url do servidor
        App.URL_SERVER = Util.getServerUrl();

        //Inicia a conexão com a impressora
        customPrinter = new CustomPrinter(this);
        customPrinter.start();

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
                            int btnHeight = 700 / jsonData.length();
                            btnParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, btnHeight);

                            for (int i = 0; i < jsonData.length(); i++) {
                                JSONObject service = jsonData.getJSONObject(i);
                                Integer serviceId = service.getInt("id");
                                String serviceName = service.getString("name");
                                String serviceKey = service.getString("key");

                                //Cria os botões
                                Button button = new Button(getApplicationContext());
                                button.setText(serviceName);
                                button.setTag(serviceKey);
                                button.setTextSize(30);
                                button.setLayoutParams(btnParams);
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

        int btnHeight = 250;
        btnParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, btnHeight);

        //Evento click
        View.OnClickListener eventClick = new View.OnClickListener() {
            public void onClick(View v) {
                Button target = (Button) v;
                currentType = target.getTag().toString();
                currentTypeLabel = target.getText().toString();
                printPassword();
            }
        };

        //Botão Atendimento Convencional
        Button button = new Button(getApplicationContext());
        button.setText("Convencional");
        button.setTag("TYPE_DEFAULT");
        button.setTextSize(30);
        button.setLayoutParams(btnParams);
        button.setOnClickListener(eventClick);
        containerTypes.addView(button);

        //Botão Atendimento Prioritário
        Button buttonP = new Button(getApplicationContext());
        buttonP.setText("Prioritário");
        buttonP.setTag("TYPE_PRIORITY");
        buttonP.setTextSize(30);
        buttonP.setLayoutParams(btnParams);
        buttonP.setOnClickListener(eventClick);
        containerTypes.addView(buttonP);

        //Botão Voltar
        Button buttonV = new Button(getApplicationContext());
        buttonV.setText("Voltar");
        buttonV.setTextSize(30);
        LinearLayout.LayoutParams btnParamsV = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 100);
        buttonV.setLayoutParams(btnParamsV);
        btnParamsV.setMargins(0, 100, 0, 20);
        buttonV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toHome();
            }
        });
        containerTypes.addView(buttonV);
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
        LinearLayout ctnServices = (LinearLayout)findViewById(R.id.containerServices);
        LinearLayout ctnType = (LinearLayout)findViewById(R.id.containerTypes);
        TextView txtDesc = (TextView) findViewById(R.id.txtDesc);
        ctnServices.setVisibility(View.INVISIBLE);
        ctnType.setVisibility(View.INVISIBLE);

        switch (page) {
            case "services":
                ctnServices.setVisibility(View.VISIBLE);
                txtDesc.setText(App.MSG_DESC_SERVICE);
                break;
            case "types":
                ctnType.setVisibility(View.VISIBLE);
                txtDesc.setText(App.MSG_DESC_TYPE);
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
