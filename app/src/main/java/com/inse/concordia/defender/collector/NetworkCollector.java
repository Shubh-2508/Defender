package com.inse.concordia.defender.collector;

import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;

import com.inse.concordia.defender.defender.DefenderDBHelper;
import com.inse.concordia.defender.defender.DefenderTask;
import com.inse.concordia.defender.model.NetworkUsage;

public class NetworkCollector extends DefenderTask {

    public NetworkCollector(){
        this.RunEvery = 5;
    }

    public void doWork(Context context) {
        DefenderDBHelper defDBHelper = DefenderDBHelper.getInstance(context);

        ConnectivityManager conMgr =  (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();

        if (activeNetwork == null || !activeNetwork.isConnected()) {
            //no active network found, abort network stats collection
            return;
        }

        ActivityManager actManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> runningProcessList = actManager
                .getRunningAppProcesses();

        //TODO this only gets usage for currently running process, what if it ran and got killed before IDS runs?
        // get running processes and ask about their network usage
        for (RunningAppProcessInfo pInfo : runningProcessList) {
            NetworkUsage nu = new NetworkUsage();

            nu.TimeStamp = System.currentTimeMillis();
            nu.Uid = String.valueOf(pInfo.uid);
            nu.TxBytes = TrafficStats.getUidTxBytes(pInfo.uid);
            nu.RxBytes = TrafficStats.getUidRxBytes(pInfo.uid);

            nu.TxPackets = TrafficStats.getUidTxPackets(pInfo.uid);
            nu.TxTcpPackets = TrafficStats.getUidTcpTxSegments(pInfo.uid);
            nu.TxUdpPackets = TrafficStats.getUidUdpTxPackets(pInfo.uid);

            nu.RxPackets = TrafficStats.getUidRxPackets(pInfo.uid);
            nu.RxTcpPackets = TrafficStats.getUidTcpRxSegments(pInfo.uid);
            nu.RxUdpPackets = TrafficStats.getUidUdpRxPackets(pInfo.uid);

            defDBHelper.insertNetworkUsage(nu);
        }
    }

}