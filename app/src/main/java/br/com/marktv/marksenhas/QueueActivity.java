package br.com.marktv.marksenhas;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.epson.eposprint.BatteryStatusChangeEventListener;
import com.epson.eposprint.Builder;
import com.epson.eposprint.EposException;
import com.epson.eposprint.Print;
import com.epson.eposprint.StatusChangeEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import static br.com.marktv.marksenhas.App.URL_GET_NEXT;
import static br.com.marktv.marksenhas.App.URL_SERVICES;
import static br.com.marktv.marksenhas.R.id.containerServices;

public class QueueActivity extends Activity implements StatusChangeEventListener,
                                                BatteryStatusChangeEventListener {

    static Print printer = null;
    int PRINT_ST_SUCCESS = 16777220;
    String PRINTER_IP;
    TextView txtDesc;
    private Map<String, Button> buttonsServices;
    public String currentService;
    public String currentServiceLabel;
    protected LinearLayout.LayoutParams btnParams;
    public String currentType;
    public String currentTypeLabel;
    public Thread threadPrinter = null;
    public String currentPassword;
    public ProgressDialog progressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_queue);

        RelativeLayout body = (RelativeLayout) findViewById(R.id.body);
        App.BODY = body;
        Util.context = this.getApplicationContext();
        Util.ACTIVITY = this;
        txtDesc = (TextView) findViewById(R.id.txtDesc);
        txtDesc.setText(App.MSG_DESC_SERVICE);

        if( progressDialog == null ) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setIndeterminate(false);
        }

        persistFullscreen(body);
        dontSleep();

        //Carrega os dados do servidor
        App.URL_SERVER = Util.loadUrlServer();
        URL_SERVICES = App.URL_SERVER + "api.php?action=getServices";
        URL_GET_NEXT = App.URL_SERVER + "api.php?action=getNext";
        PRINTER_IP = Util.getPrinterIp();

        //Inicia a execução
        initialize();

        //Monitora a comunicação com a impressora
        if( !printConnected() ) {
            openPrinter();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                while( Thread.currentThread().isAlive() ) {
                    if( printer != null ) {
                        int[] status = new int[1];
                        int[] batt = new int[1];
                        try {
                            printer.getStatus(status, batt);
                            String txtStatus = ShowMsg.getEposStatusText(status[0]);
                            Log.e("XXXXXXXX", txtStatus);
                        } catch (EposException e) {
                            e.printStackTrace();
                        }

                    } else {
                        Log.e("XXXXXXXX", "PRINTER IS NULL");
                    }
                    Util.sleep(10000);
                }
            }
        }).start();
    }

    /**
     * Carrega toda a estrutura
     */
    protected void initialize() {

        //Formata o campo de texto
        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/MuseoSans-100.otf");
        txtDesc.setTypeface(typeface);

        //Cria os botões
        createButtonsServices();
        createButtonsTypes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        RelativeLayout body = (RelativeLayout) findViewById(R.id.body);
        Util.setFullscreen(this, body);
    }

    /**
     * Cria a lista de botões de serviços
     */
    private void createButtonsServices() {
        final LinearLayout containerServices = (LinearLayout) findViewById(R.id.containerServices);
        buttonsServices = new TreeMap<String, Button>();
        final QueueActivity mainActivity = this;
        final boolean[] buttonsServicesCreated = new boolean[1];
        buttonsServicesCreated[0] = false;

        //Busca os serviços no servidor
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                String json = null;
                while ( json == null ) {
                    json = Util.requestHttp(URL_SERVICES);
                    if( json == null ) {
                        Util.toastMainThread(mainActivity, "Falha na requisição da consulta dos serviços\n Url: "+URL_SERVICES);
                        Util.sleep(10000);
                    }
                }
                try {
                    JSONObject jsonResp = new JSONObject(json);
                    JSONArray jsonData = jsonResp.getJSONArray("data");
                    if (jsonData.length() > 0) {

                        if( jsonData.length() > 1 ) {
                            for (int i = 0; i < jsonData.length(); i++) {
                                JSONObject service = jsonData.getJSONObject(i);
                                Integer serviceId = service.getInt("id");
                                String serviceName = service.getString("name");
                                String serviceKey = service.getString("key");

                                //Cria os botões
                                final Button button = new Button(getApplicationContext());
                                formatButton(button, 1);
                                button.setText(serviceName);
                                button.setTag(serviceKey);
                                buttonsServices.put(serviceKey, button);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public synchronized void run() {
                                        containerServices.addView(button);
                                    }
                                });

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
                        } else {

                            //Possui apenas 1 serviço
                            JSONObject service = jsonData.getJSONObject(0);
                            currentService = service.getString("key");
                            currentServiceLabel = service.getString("name");
                            Util.ACTIVITY.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showPage("types");
                                }
                            });
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });
        thread.start();
    }

    /**
     * Cria a lista de botões de tipos de atendimentos
     */
    private void createButtonsTypes() {
        LinearLayout containerTypes = (LinearLayout) findViewById(R.id.containerTypes);

        //Evento click
        View.OnClickListener eventClick = new View.OnClickListener() {
            public void onClick(View v) {
                Util.log("btnType click()");
                Button target = (Button) v;
                currentType = target.getTag().toString();
                currentTypeLabel = target.getText().toString();

                //Solicita senha no servidor
                loadDataPrint();
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
        buttonV.setVisibility(View.INVISIBLE);
        buttonV.setBackgroundResource(R.drawable.botao3);
        LinearLayout.LayoutParams btnParamsV = new LinearLayout.LayoutParams(265, 140);
        btnParamsV.setMargins(0, 30, 0, 0);
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
     * Carrega a próxima senha e envia para a impressora
     */
    private void loadDataPrint  () {
        Util.log("loadDataPrint()");
        final String url = App.URL_GET_NEXT + "&service=" + currentService + "&type=" +currentType;

        //Exibe a mensagem de espera
        showWaitMessage("Obtendo nova senha...");

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
                    Boolean success = printPassword(currentServiceLabel, currentTypeLabel, currentPassword);
                    closeWaitMessage();
                    if( !success ) {
                        Util.ACTIVITY.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ShowMsg.show("Falha na impressão", Util.ACTIVITY, "Fechar");
                            }
                        });

                    }
                } catch (JSONException e) {
                    Util.log(e.getMessage());
                    closeWaitMessage();
                    e.printStackTrace();
                }
            }
        });
        threadPrinter.start();

        //Retorna a tela inicial
        toHome();
    }

    /**
     * Imprime a senha
     */
    protected boolean printPassword(String currentServiceLabel, String  currentTypeLabel, String  currentPassword) {

        //Adiciona os "000" à senha
        if (Integer.parseInt(currentPassword) < 10) {
            currentPassword = "00" + currentPassword;
        } else if (Integer.parseInt(currentPassword) < 100) {
            currentPassword = "0" + currentPassword;
        }

        //Imprime o conteúdo (3 tentativas)
        Boolean success = false;
        updateWaitMessage("Imprimindo...");
        for( int i = 0; i < 3; i++ ) {
            int n = i + 1;
            Util.log("Tentativa de impressão nº " + n);
            success = sendToPrinter(currentServiceLabel, currentTypeLabel, currentPassword);

            if( success ) {
                break;
            } else {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if( i >= 2 ) {
                Log.e("ERRO", "Falha na impressão");
            }
        }

        return success;
    }

    /**
     * Retorna para a tela inicial
     */
    protected void toHome() {
        showPage("types");
    }

    /**
     * Imprime o texto
     */
    private Boolean sendToPrinter(String currentServiceLabel, String  currentTypeLabel, String  currentPassword){
        Boolean connected = false;
        Boolean rs = false;

        //Inicia a comunicação com a impressora
        if( printConnected() ) {
            connected = true;
        } else {
            connected = openPrinter();
        }

        if( connected ) {
            Builder builder = null;
            String method = "";
            try {
                //create builder
                Intent intent = getIntent();
                method = "Builder";
                builder = new Builder(
                        intent.getStringExtra("printername"), intent.getIntExtra("language", 0), getApplicationContext());

                //Align center
                method = "AlignCenter";
                builder.addTextAlign(Builder.ALIGN_CENTER);

                //Build text
                method = "AddText";
                String text = "MarkTv Senhas\n";
                text += "------------------------------\n\n";
                builder.addText(text);

                //Senha
                method = "SetSize";
                builder.addTextSize(2, 2);
                text = "SENHA " + currentPassword + "\n\n";
                builder.addText(text);

                //Detalhes
                text = currentTypeLabel + "\n\n";
                builder.addText(text);
                builder.addTextSize(2, 1);
                text = currentServiceLabel + "\n\n";
                builder.addText(text);

                //Horário
                builder.addTextSize(1, 1);
                Date todayDate = Calendar.getInstance().getTime();
                SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy H:mm:ss");
                String todayString = formatter.format(todayDate);
                text = todayString + "\n";
                builder.addText(text);

                //Rodapé
                text = "------------------------------\n";
                text += "http://www.marktv.com.br\n\n\n";
                builder.addText(text);
                builder.addCut(Builder.CUT_FEED);

                //send builder data
                int[] status = new int[1];
                int[] battery = new int[1];
                printer.sendData(builder, 1000 * 10, status, battery);
                String strStatus = status[0] + ": " + ShowMsg.getEposStatusText(status[0]);
                Util.log(strStatus);
                if( status[0] == App.PRINT_SUCCESS ) {
                    rs = true;
                }
            } catch (Exception e) {
                Util.log("Exception: "+e.getMessage());
                e.printStackTrace();
            }

            //remove builder
            if (builder != null) {
                try {
                    builder.clearCommandBuffer();
                    builder = null;
                } catch (Exception e) {
                    builder = null;
                }
            }
        }

        Util.log("Print return: "+rs);
        return rs;
    }

    /**
     * Abre a conexão com a impressora
     */
    public Boolean openPrinter() {
        Boolean rs = false;

        //Encerra a conexão anterior
        if( printer != null ) {
            try {
                printer.closePrinter();
            } catch (Exception e) {}
        }

        //Inicia a nova conexão
        printer = new Print(getApplicationContext());
        printer.setStatusChangeEventCallback(this);
        printer.setBatteryStatusChangeEventCallback(this);
        try {
            printer.openPrinter(Print.DEVTYPE_TCP, PRINTER_IP, Print.FALSE, 1000);
            rs = true;
        } catch (Exception e) {
            //ShowMsg.showException(e, "openPrinter", this);
            Log.e("ERRO", "Falha ao localizar a impressora");
        }

        return rs;
    }

    /**
     * Verifica se a impressora está conectada
     */
    public boolean printConnected() {
        if( printer != null ) {
            int status[] = new int[1];
            int battery[] = new int[1];
            try {
                printer.getStatus(status, battery);
                if( status[0] == PRINT_ST_SUCCESS ) {
                    return true;
                }
            } catch (Exception e) {
                //e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Manté a tela em fullscreen
     */
    public void persistFullscreen(final View body) {
        final Activity activity = this;
        Util.setFullscreen(this, body);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Util.setFullscreen(activity, body);
                    }
                });
            }
        }, 60000);
    }

    /**
     * Formata o botão
     */
    private void formatButton(Button button, int theme) {
        final Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/MuseoSans-900.otf");
        btnParams = new LinearLayout.LayoutParams(600, 150);
        btnParams.setMargins(0, 0, 0, 25);
        button.setTextSize(24);
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

        if( page.equals("services") ) {
            ctnServices.setVisibility(View.VISIBLE);
            txtDesc.setText(App.MSG_DESC_SERVICE);
        } else if( page.equals("types") ) {
            ctnType.setVisibility(View.VISIBLE);
            txtDesc.setText(currentServiceLabel.toUpperCase());
        }
    }

    /**
     * Exibe a mensagem de espera
     */
    public void showWaitMessage(String message) {
        if( !Util.ACTIVITY.isDestroyed() ) {
            progressDialog.setMessage(message);
            progressDialog.show();
        }
    }

    /**
     * Atualiza a mensagem de espera
     */
    public void updateWaitMessage(final String message) {
        if( !Util.ACTIVITY.isDestroyed() && Looper.getMainLooper().getThread() == Thread.currentThread() ) {
            progressDialog.setMessage(message);
        } else if ( !Util.ACTIVITY.isDestroyed() ) {
            Util.ACTIVITY.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.setMessage(message);
                }
            });
        }
    }

    /**
     * Fecha a mensagem
     */
    public void closeWaitMessage() {
        progressDialog.dismiss();

        Util.ACTIVITY.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Util.setFullscreen(Util.ACTIVITY, App.BODY);
            }
        });
    }

    @Override
    public void onBatteryStatusChangeEvent(String s, int i) {
        System.out.println(s);
    }

    @Override
    public void onStatusChangeEvent(String s, int i) {
        System.out.println(s);
    }
}
