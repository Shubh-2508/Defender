package com.inse.concordia.defender.collector;


import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;

import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;

import com.inse.concordia.defender.defender.DefenderDBHelper;
import com.inse.concordia.defender.defender.DefenderTask;
import com.inse.concordia.defender.model.Process;

public class ProcessCollector extends DefenderTask {

    public ProcessCollector(){
        this.RunEvery = 5;
    }

    public void doWork(Context context) {
        ActivityManager actManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> runningProcessList = actManager
                .getRunningAppProcesses();
        DefenderDBHelper defDBHelper = DefenderDBHelper.getInstance(context);

        // get running processes
        for (RunningAppProcessInfo pInfo : runningProcessList) {
            Process p = new Process();

            p.TimeStamp = System.currentTimeMillis();
            p.Uid = String.valueOf(pInfo.uid);
            p.Pid = String.valueOf(pInfo.pid);
            p.Name = pInfo.processName;

            defDBHelper.insertProcess(p);
        }
    }
}