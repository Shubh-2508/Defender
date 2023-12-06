package com.inse.concordia.defender.collector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.content.Context;
import android.nfc.Tag;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.inse.concordia.defender.defender.DefenderDBHelper;
import com.inse.concordia.defender.defender.DefenderTask;
import com.inse.concordia.defender.model.CPUUsage;
public class CPUUsageCollector extends DefenderTask {
    Pattern pidPattern = Pattern.compile("^\\s*(\\d+)\\s*");
    Pattern cpuUsagePattern = Pattern.compile("(\\d+)%");

    public CPUUsageCollector() {
        this.RunEvery = 5;
    }

    public void doWork(Context context) {
        DefenderDBHelper defDBHelper = DefenderDBHelper.getInstance(context);
        //AIDSDBHelper aidsDBHelper = AIDSDBHelper.getInstance(context);
        boolean bandera=true;
        // get cpu usage for processes
        try {

            Process topProcess = Runtime.getRuntime().exec("top -n 1");
            //Process topProcess = Runtime.getRuntime().exec("top -n 1 -d 0");

            BufferedReader bufferedStream = new BufferedReader(
                    new InputStreamReader(topProcess.getInputStream()));
            String tLine;
            bufferedStream.readLine(); // skip over 1st empty line
            bufferedStream.readLine(); // second empty line
            bufferedStream.readLine(); // skip over USER and SYSTEM CPU
            bufferedStream.readLine(); // skip over USER and NICE
            //bufferedStream.readLine(); // skip over column titles

            boolean cabe=false;
            while ((tLine = bufferedStream.readLine()) != null) {

                if (tLine.contains("CPU")){
                    cabe=true;
                    continue;
                }

                if (cabe){
                    String[] lista =tLine.split(" ");
                    List<String> lis=new ArrayList<>();
                    for (int i=0;i<lista.length;i++){
                        if (!lista[i].equals("") && lista[i] !=null && !lista[i].equals("R") && !lista[i].contains("[1m")){ //modifiq aca
                            lis.add(lista[i]);
                            Log.d("HORROR1","lista["+i+"]= "+ lista[i]);
                        }
                    }
                    String cpuUsage=lis.get(8);
                    String cpuUsa=lis.get(8);
                    String pid = lis.get(0);

                    CPUUsage cu = new CPUUsage();
                    cu.TimeStamp = System.currentTimeMillis();
                    cu.Pid = pid;
                    cu.CPUUsage = cpuUsage;
                    Log.e("HORROR2", "esteee cu.toString() " + cu.toString());

                    defDBHelper.insertCPUUsage(cu);
                    cabe = false;
                    //cpuUsage.replaceAll("a","");
                    Double  valor= Double.valueOf(lis.get(8));
                    if(valor>5.0){
                        Log.d("HORROR3", "bandera= : "+ valor + " - "+ bandera);
                        bandera=false;

                    }
                }
            }


        } catch (IOException e) {
            // TODO Auto-generated catch block
            //Log.d("HORROR", "ESTE ES EL CATCH");
            e.printStackTrace();
        }
    }

}