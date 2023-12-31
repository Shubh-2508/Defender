package com.inse.concordia.defender.analyzer;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.inse.concordia.defender.defender.DefenderDBHelper;
import com.inse.concordia.defender.defender.DefenderTask;
import com.inse.concordia.defender.model.Alert;
import com.inse.concordia.defender.model.CPUUsage;
import com.inse.concordia.defender.model.IEModel;
import com.inse.concordia.defender.model.Process;

/*
 * Resource usage global for entire device. Uses unique IEModel for that since
 * we follow the same structure.
 */
public class GAnalyzer extends DefenderTask {
    private final String TAG = GAnalyzer.class.getName();

    public GAnalyzer() {
        this.RunEvery = 60;
    }

    public void doWork(Context context) {
        DefenderDBHelper defDBHelper = DefenderDBHelper.getInstance(context);

        Calendar calendar = Calendar.getInstance();
        long currentTimeMillis = calendar.getTimeInMillis();
        Calendar prevCalendar = Calendar.getInstance();
        prevCalendar.add(Calendar.SECOND, -1 * RunEvery);
        long prevTimeMillis = prevCalendar.getTimeInMillis();

        defDBHelper.insertLog(String.format("Running GAnalyzer for %s-%s",
                calendar.toString(), prevCalendar.toString()));
        String gModelName = "_global" + calendar.get(Calendar.HOUR_OF_DAY);

        // get global model
        IEModel gModel = defDBHelper.getIEModel(gModelName);

        if (gModel == null) {
            // first invocation
            gModel = new IEModel();
            gModel.ProcessName = gModelName;
            gModel.FromTimeStamp = prevTimeMillis;
            gModel.ToTimeStamp = currentTimeMillis;
            gModel.Age = 1;

            defDBHelper.insertIEModel(gModel);
        }

        // get IEModels of processes for past hour
        HashMap<String, IEModel> processMap = getIEModelsForProcesses(
                defDBHelper, prevTimeMillis, currentTimeMillis);

        for (String pName : processMap.keySet()) {
            IEModel newModel = processMap.get(pName);

            // update the model with current generation
            gModel.ToTimeStamp = currentTimeMillis;
            gModel.CPULow += newModel.CPULow;
            gModel.CPUMid += newModel.CPUMid;
            gModel.CPUHigh += newModel.CPUHigh;
            gModel.CPUCounter += newModel.CPUCounter;
            gModel.Age = gModel.Age + 1; // and increment the age

            defDBHelper.updateIEModel(gModel);
        }
    }

    // return hashmap of IEModel keyed by process name
    public HashMap<String, IEModel> getIEModelsForProcesses(
            DefenderDBHelper aidsDBHelper, long fromTimeMillis, long toTimeMillis) {
        // hashmap keyed by process name and then attributes
        HashMap<String, IEModel> processMap = new HashMap<String, IEModel>();

        // get processes for specified period
        List<Process> processListForPeriod = aidsDBHelper.getProcesses(
                fromTimeMillis, toTimeMillis);
        // i have to iterate over them all because some could have different
        // PIDs
        // so i group them under process name
        for (Process p : processListForPeriod) {
            if (!processMap.containsKey(p.Name)) {
                processMap.put(p.Name, new IEModel());
            }

            IEModel pIEModel = processMap.get(p.Name);

            // get cpuusage for each process
            List<CPUUsage> cpuUsageForProcessList = aidsDBHelper.getCPUUsage(
                    p.Pid, fromTimeMillis, toTimeMillis);
            Log.i("hash", "" + cpuUsageForProcessList.size());
            for (CPUUsage cpu : cpuUsageForProcessList) {
                double cpuUsageInt = Double.parseDouble(cpu.CPUUsage);

                if (cpuUsageInt < 2) {
                    pIEModel.CPULow = pIEModel.CPULow + 1;
                } else if (cpuUsageInt < 11) {
                    pIEModel.CPUMid = pIEModel.CPUMid + 1;
                } else {
                    pIEModel.CPUHigh = pIEModel.CPUHigh + 1;
                }

                pIEModel.CPUCounter = pIEModel.CPUCounter + 1;
            }

            // bandwidth usage for each process
            HashMap<String, String> bandwidthUse = aidsDBHelper
                    .getBandwidthUsage(p.Uid, fromTimeMillis, toTimeMillis);
            // bandwidth is calculated per uid not pid, so its already
            // cumulative
            pIEModel.RxBytes = Integer.parseInt(bandwidthUse.get("rx"));
            pIEModel.TxBytes = Integer.parseInt(bandwidthUse.get("tx"));
        }

        return processMap;
    }
}
