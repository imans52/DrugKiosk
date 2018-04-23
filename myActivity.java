package com.rkara.sample;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import com.printer.command.EscCommand;
import com.rkara.PosBank.Mellat;
import com.rkara.printer.Printer;

import java.util.ArrayList;
import java.util.List;

public class myActivity extends AppCompatActivity
{
    Printer printer = null;
    SharedPreferences sharedPref;
    String portName = null;
    String portAddress = null;
    private List<FactorItems> factorItems;
    private WebView wvMain;
    Mellat pcPos;
    String resultMsg = null;
    String resultCode = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        wvMain = (WebView) findViewById(R.id.wvMain);
        wvMain.getSettings().setJavaScriptEnabled(true); // enable javascript
        wvMain.addJavascriptInterface(new WebViewJavaScriptInterface(this), "app");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        wvMain.loadUrl("http://192.168.10.122:24435/drugkiosk/default/index");

        //wvMain.addJavascriptInterface(myJavaScriptInterface, "AndroidFunction");

        printer = new Printer(this);
        sharedPref = getSharedPreferences("printerSetting", this.MODE_PRIVATE);
        factorItems = new ArrayList<FactorItems>();
        loadFactorDetails();
    }

    public void loadFactorDetails()
    {
        FactorItems items = new FactorItems();
        items.factorId = 1520;
        items.rowIndex = 1;
        items.goodsCode = "6666666673947";
        items.goodsName = "کاپسيتابين 500 قرص زلودا سفارش ترک";
        items.qty = 150;
        items.salesPrice = 365000;
        items.consumerPrice = 40000;
        factorItems.add(items);

        items = new FactorItems();
        items.factorId = 1520;
        items.rowIndex = 2;
        items.goodsCode = "0200000008086";
        items.goodsName = "چسب ضد حساسيت کاغذي 9*2/5";
        items.qty = 20;
        items.salesPrice = 145000;
        items.consumerPrice = 165000;
        factorItems.add(items);

        items = new FactorItems();
        items.factorId = 1520;
        items.rowIndex = 3;
        items.goodsCode = "5010706002760";
        items.goodsName = "کوتريموکسازول آمپول خارجي المان رشيوفارم";
        items.qty = 13.75;
        items.salesPrice = 35000;
        items.consumerPrice = 45000;
        factorItems.add(items);
    }

    public void btSettingClicked(View view)
    {
        startActivity(new Intent(this, SettingActivity.class));
    }

    public void initializePrinter() throws RemoteException
    {
        int result = 0;
        result = printer.getPrinterStatus();
        if (result == 3)
            return;

        sharedPref = getSharedPreferences("printerSetting", this.MODE_PRIVATE);
        if (sharedPref.contains("portName"))
        {
            portName = sharedPref.getString("portName", null);
            portAddress = sharedPref.getString("portAddress", null);
            if (!portName.equals(null) || !portAddress.equals(null))
                printer.ConnectToPrinter(portName, portAddress);
            else
                Toast.makeText(this, "پورت چاپگر تنظیم نشده است!!!", Toast.LENGTH_SHORT).show();
        }
        else
            Toast.makeText(this, "پورت چاپگر تنظیم نشده است!!!", Toast.LENGTH_SHORT).show();

    }

    public void loadPrinter(View view) throws RemoteException
    {
        initializePrinter();
        printFactorHeader();
        printFactorItems();
        printFooter();
    }

    private void printFooter()
    {
        printer.print(" ------------------------------------------  ", EscCommand.JUSTIFICATION.CENTER, false);
        printer.print(" تعداد کل اقلام : ", EscCommand.JUSTIFICATION.RIGHT, false);
        printer.print(" جمع کل فاکتور :  ", EscCommand.JUSTIFICATION.RIGHT, false);
        printer.print(" ", EscCommand.JUSTIFICATION.RIGHT, true);
    }

    private void printFactorHeader()
    {
        printer.print("فاکتور فروش ", EscCommand.JUSTIFICATION.CENTER, EscCommand.ENABLE.ON, false);
        printer.print(" ------------------------------------------  ", EscCommand.JUSTIFICATION.CENTER, false);
    }

    private void printFactorItems()
    {
        String item = "";
        for (int index = 0; index < factorItems.size(); index++)
        {
            printer.print(String.format("(%d) %s", factorItems.get(index).rowIndex, factorItems.get(index).goodsName), EscCommand.JUSTIFICATION.RIGHT, false);
            printer.print(String.format(" %9.0f  %8.0f  %7s        ", (factorItems.get(index).qty * factorItems.get(index).salesPrice), factorItems.get(index).salesPrice, Double.toString(factorItems.get(index).qty)), EscCommand.JUSTIFICATION.RIGHT, false);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        switch (requestCode)
        {
            case 1:
            {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {

                }
                else
                {
                    Toast.makeText(this, "Permission denied to USB Devices", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    public class WebViewJavaScriptInterface
    {

        private Context context;

        public WebViewJavaScriptInterface(Context context)
        {
            this.context = context;
        }

        @JavascriptInterface
        public void makeToast(String message)
        {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }

        @JavascriptInterface
        public void SendAmountToPCPOS(String amount)
        {
            Thread waitForPos = new Thread(new Runnable()
            {
                public void run()
                {
                    while (true)
                    {
                        resultMsg = pcPos.get_responseMsg();
                        resultCode = pcPos.get_responseCode();
                        if (resultMsg != null)
                            break;
                        try
                        {
                            Thread.sleep(1000);
                        }
                        catch (InterruptedException e)
                        {
                            e.printStackTrace();
                        }
                    }
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            //Toast.makeText(context, resultMsg, Toast.LENGTH_SHORT).show();
                            if (resultCode == "00")
                                wvMain.loadUrl("javascript:submitFactor()");
                            else
                                wvMain.loadUrl("javascript:stopWaitingPos('" + resultMsg + "')");
                        }
                    });
                }
            });
            if (!amount.equals(""))
            {
                pcPos = new Mellat("192.168.10.38", Integer.valueOf(amount));
                pcPos.Send();
                waitForPos.start();
            }
        }
    }


}
