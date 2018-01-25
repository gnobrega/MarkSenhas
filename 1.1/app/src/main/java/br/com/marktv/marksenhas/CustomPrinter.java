package br.com.marktv.marksenhas;

import android.content.Context;
import android.os.Handler;
import android.view.View;

import com.epson.epos2.Epos2Exception;
import com.epson.epos2.discovery.DeviceInfo;
import com.epson.epos2.discovery.Discovery;
import com.epson.epos2.discovery.DiscoveryListener;
import com.epson.epos2.discovery.FilterOption;
import com.epson.epos2.printer.Printer;
import com.epson.epos2.printer.PrinterStatusInfo;
import com.epson.epos2.printer.ReceiveListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Gustavo on 23/11/2017.
 */

public class CustomPrinter implements View.OnClickListener, ReceiveListener {
    private Printer mPrinter = null;
    private String printerTarget = "USB:/dev/bus/usb/001/002";
    protected MainActivity mainActivity;
    private Context mContext = null;

    public CustomPrinter(MainActivity mContext) {
        this.mContext = mContext;
        this.mainActivity = mContext;
    }

    protected void start() {

        //Seleciona a impressora
        selectPrinter();

        //Inicia o objeto
        createObject();
    }

    //Chamado após a busca de impressoras
    DiscoveryListener mDiscoveryListener = new DiscoveryListener() {
        @Override
        public void onDiscovery(final DeviceInfo deviceInfo) {
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public synchronized void run() {
                    printerTarget = deviceInfo.getTarget();
                    Util.toastMainThread(mainActivity, "Impressora conectada: " + printerTarget);
                }
            });
        }
    };

    /**
     * Seleciona a impressora
     */
    protected void selectPrinter() {

        //Busca as impressoras
        FilterOption mFilterOption = new FilterOption();
        mFilterOption.setDeviceType(Discovery.TYPE_PRINTER);
        mFilterOption.setEpsonFilter(Discovery.FILTER_NAME);
        try {
            Discovery.start(this.mainActivity, mFilterOption, mDiscoveryListener);
        } catch (Epos2Exception e) {
            e.printStackTrace();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        Discovery.stop();
                    } catch (Epos2Exception e1) {
                        e1.printStackTrace();
                    }
                    selectPrinter();
                }
            }, 5000);
        }
    }

    /**
     * Inicia o objeto printer
     */
    protected void createObject() {
        if( mPrinter == null ) {
            try {
                SpnModelsItem modelLang = new SpnModelsItem(this.mainActivity.getString(R.string.lang_ank), Printer.MODEL_ANK);
                SpnModelsItem modelSerie = new SpnModelsItem(this.mainActivity.getString(R.string.printerseries_t20), Printer.TM_T20);

                mPrinter = new Printer(
                        modelSerie.getModelConstant(),
                        modelLang.getModelConstant(),
                        mContext);
            }
            catch (Exception e) {
                e.printStackTrace();
                return;
            }
            mPrinter.setReceiveEventListener(this);
        }
    }

    /**
     * Cria o conteúdo que será imprimido
     */
    private boolean createData(String currentServiceLabel, String  currentTypeLabel, String  currentPassword) {
        String method = "CustomPrinter.createData()";
        //Bitmap logoData = BitmapFactory.decodeResource(this.mainActivity.getResources(), R.mipmap.ic_marktv);
        StringBuilder textData = new StringBuilder();

        if (mPrinter == null) {
            return false;
        }

        try {
            mPrinter.addTextAlign(Printer.ALIGN_CENTER);

            //Topo
            mPrinter.addFeedLine(1);
            textData.append("MarkTv Senhas\n");
            textData.append("------------------------------\n");
            mPrinter.addText(textData.toString());
            textData.delete(0, textData.length());

            //Número da senha
            mPrinter.addTextSize(2, 2);
            mPrinter.addText("SENHA    " + currentPassword + "\n");

            //Detalhes
            mPrinter.addTextSize(2, 1);
            mPrinter.addFeedLine(1);
            textData.append(currentTypeLabel + "\n");
            textData.append(currentServiceLabel + "\n");
            mPrinter.addText(textData.toString());
            textData.delete(0, textData.length());

            //Horário
            mPrinter.addTextSize(1, 1);
            mPrinter.addFeedLine(1);
            Date todayDate = Calendar.getInstance().getTime();
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
            String todayString = formatter.format(todayDate);
            textData.append(todayString + "\n");

            //Rodapé
            textData.append("------------------------------\n\n");
            textData.append("http://www.marktv.com.br");
            mPrinter.addText(textData.toString());
            textData.delete(0, textData.length());
            mPrinter.addFeedLine(2);

            mPrinter.addCut(Printer.CUT_FEED);
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private boolean printData() {
        if (mPrinter == null) {
            return false;
        }

        //Estabelece a conexão com a impressora
        if ( !connectPrinter()) {
            return false;
        }

        PrinterStatusInfo status = mPrinter.getStatus();
        dispPrinterWarnings(status);
        if (!isPrintable(status)) {
            try {
                mPrinter.disconnect();
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
            return false;
        }

        try {
            //Envia o conteúdo para a impressora
            mPrinter.sendData(Printer.PARAM_DEFAULT);
        }
        catch (Exception e) {
            e.printStackTrace();
            try {
                mPrinter.disconnect();
            }
            catch (Exception ex) {
            }
            return false;
        }

        return true;
    }

    private void finalizeObject() {
        if (mPrinter == null) {
            return;
        }
        mPrinter.clearCommandBuffer();
        mPrinter.setReceiveEventListener(null);
        mPrinter = null;
    }

    private boolean connectPrinter() {
        boolean isBeginTransaction = false;
        if (mPrinter == null) {
            return false;
        }

        //Verifica se a impressora está disponível
        PrinterStatusInfo status = mPrinter.getStatus();
        if( isPrintable(status) ) {
            return  true;
        }

        try {
            mPrinter.connect(printerTarget, Printer.PARAM_DEFAULT);
        }
        catch (Exception e) {
            e.printStackTrace();
            Util.toastMainThread(mainActivity, "Impressora offline");
            return false;
        }

        try {
            mPrinter.beginTransaction();
            isBeginTransaction = true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        if (isBeginTransaction == false) {
            try {
                mPrinter.disconnect();
            }
            catch (Epos2Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    private void disconnectPrinter() {
        if (mPrinter == null) {
            return;
        }

        try {
            mPrinter.endTransaction();
        }
        catch (final Exception e) {
            e.printStackTrace();
            /*this.mainActivity.runOnUiThread(new Runnable() {
                @Override
                public synchronized void run() {
                    ShowMsg.showException(e, "endTransaction", mContext);
                }
            });*/
        }

        try {
            mPrinter.disconnect();
        }
        catch (final Exception e) {
            //e.printStackTrace();
        }

        finalizeObject();
    }

    private boolean isPrintable(PrinterStatusInfo status) {
        if (status == null) {
            return false;
        }
        if (status.getConnection() == Printer.FALSE) {
            return false;
        }
        else if (status.getOnline() == Printer.FALSE) {
            return false;
        }
        return true;
    }

    private String makeErrorMessage(PrinterStatusInfo status) {
        String msg = "";

        if (status.getOnline() == Printer.FALSE) {
            msg += this.mainActivity.getString(R.string.handlingmsg_err_offline);
        }
        if (status.getConnection() == Printer.FALSE) {
            msg += this.mainActivity.getString(R.string.handlingmsg_err_no_response);
        }
        if (status.getCoverOpen() == Printer.TRUE) {
            msg += this.mainActivity.getString(R.string.handlingmsg_err_cover_open);
        }
        if (status.getPaper() == Printer.PAPER_EMPTY) {
            msg += this.mainActivity.getString(R.string.handlingmsg_err_receipt_end);
        }
        if (status.getPaperFeed() == Printer.TRUE || status.getPanelSwitch() == Printer.SWITCH_ON) {
            msg += this.mainActivity.getString(R.string.handlingmsg_err_paper_feed);
        }
        if (status.getErrorStatus() == Printer.MECHANICAL_ERR || status.getErrorStatus() == Printer.AUTOCUTTER_ERR) {
            msg += this.mainActivity.getString(R.string.handlingmsg_err_autocutter);
            msg += this.mainActivity.getString(R.string.handlingmsg_err_need_recover);
        }
        if (status.getErrorStatus() == Printer.UNRECOVER_ERR) {
            msg += this.mainActivity.getString(R.string.handlingmsg_err_unrecover);
        }
        if (status.getErrorStatus() == Printer.AUTORECOVER_ERR) {
            if (status.getAutoRecoverError() == Printer.HEAD_OVERHEAT) {
                msg += this.mainActivity.getString(R.string.handlingmsg_err_overheat);
                msg += this.mainActivity.getString(R.string.handlingmsg_err_head);
            }
            if (status.getAutoRecoverError() == Printer.MOTOR_OVERHEAT) {
                msg += this.mainActivity.getString(R.string.handlingmsg_err_overheat);
                msg += this.mainActivity.getString(R.string.handlingmsg_err_motor);
            }
            if (status.getAutoRecoverError() == Printer.BATTERY_OVERHEAT) {
                msg += this.mainActivity.getString(R.string.handlingmsg_err_overheat);
                msg += this.mainActivity.getString(R.string.handlingmsg_err_battery);
            }
            if (status.getAutoRecoverError() == Printer.WRONG_PAPER) {
                msg += this.mainActivity.getString(R.string.handlingmsg_err_wrong_paper);
            }
        }
        if (status.getBatteryLevel() == Printer.BATTERY_LEVEL_0) {
            msg += this.mainActivity.getString(R.string.handlingmsg_err_battery_real_end);
        }

        return msg;
    }

    private void dispPrinterWarnings(PrinterStatusInfo status) {
        //EditText edtWarnings = (EditText)findViewById(R.id.edtWarnings);
        String warningsMsg = "";

        if (status == null) {
            return;
        }

        if (status.getPaper() == Printer.PAPER_NEAR_END) {
            warningsMsg += this.mainActivity.getString(R.string.handlingmsg_warn_receipt_near_end);
        }

        if (status.getBatteryLevel() == Printer.BATTERY_LEVEL_1) {
            warningsMsg += this.mainActivity.getString(R.string.handlingmsg_warn_battery_near_end);
        }
    }

    /**
     * Recria o objeto
     */
    protected void reconnectPrinter() {
        disconnectPrinter();
        finalizeObject();
        createObject();
    }

    /**
     * Imprime a senha
     */
    protected boolean printPassword(String currentServiceLabel, String  currentTypeLabel, String  currentPassword) {

        //Adiciona os "000" à senha
        if( Integer.parseInt(currentPassword) < 10 ) {
            currentPassword = "00" + currentPassword;
        } else if( Integer.parseInt(currentPassword) < 100 ) {
            currentPassword = "0" + currentPassword;
        }

        //Verifica a conexão com a impressora
        while( mPrinter == null ) {
            Util.log("Falha ao iniciar o Objeto");
            reconnectPrinter();
            if( mPrinter != null ) {
                break;
            } else {
                Util.sleep(5000);
            }
        }

        //Cria o conteúdo
        if (!createData(currentServiceLabel, currentTypeLabel, currentPassword)) {
            finalizeObject();
            return false;
        }

        //Imprime o conteúdo
        if (!printData()) {
            finalizeObject();
            return false;
        }

        return true;
    }

    /********************************************
     ************* Listener ********************
     ******************************************/
    @Override
    public void onPtrReceive(final Printer printerObj, final int code, final PrinterStatusInfo status, final String printJobId) {
        this.mainActivity.runOnUiThread(new Runnable() {
            @Override
            public synchronized void run() {
                //ShowMsg.showResult(code, makeErrorMessage(status), mContext);
                Util.log(makeErrorMessage(status));
                dispPrinterWarnings(status);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        disconnectPrinter();
                    }
                }).start();
            }
        });
    }

    @Override
    public void onClick(View view) {

    }
}
