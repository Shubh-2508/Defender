package com.inse.concordia.defender.detector;

import java.util.Calendar;
import java.util.HashMap;

import android.content.Context;
import android.util.Log;

import com.inse.concordia.defender.defender.DefenderDBHelper;
import com.inse.concordia.defender.defender.DefenderTask;
import com.inse.concordia.defender.analyzer.IEAnalyzer;
import com.inse.concordia.defender.model.APackage;
import com.inse.concordia.defender.model.Alert;
import com.inse.concordia.defender.model.IEModel;

/*
 * Threat detector for IEModels. It compares process activity models to
 * their learned models and bumps up package threat when it execeeds it.
 *
 */
public class IEDetector extends DefenderTask {
    private final String TAG = IEDetector.class.getName();

    public IEDetector() {
        this.RunEvery = 120;
    }

    public void doWork(Context context) {
        DefenderDBHelper defDBHelper = DefenderDBHelper.getInstance(context);

        Calendar calendar = Calendar.getInstance();
        long currentTimeMillis = calendar.getTimeInMillis();
        Calendar prevCalendar = Calendar.getInstance();
        prevCalendar.add(Calendar.SECOND, -1 * RunEvery);
        long prevTimeMillis = prevCalendar.getTimeInMillis();

        HashMap<String, IEModel> previousProcessesModels = IEAnalyzer
                .getIEModelsForProcesses(defDBHelper, prevTimeMillis,
                        currentTimeMillis);

        defDBHelper.insertLog(String.format("Running IEDetector for %s-%s",
                calendar.toString(), prevCalendar.toString()));

        for (String pName : previousProcessesModels.keySet()) {
            IEModel learnedModel = defDBHelper.getIEModel(pName);

            if (learnedModel == null || learnedModel.Age < 24) {
                // model is not old enough or doesn't exist, catch it on next
                // iteration
                continue;
            }

            IEModel activityModel = previousProcessesModels.get(pName);

            float learnedCPULow = learnedModel.CPULow / learnedModel.CPUCounter;
            float learnedCPUMid = learnedModel.CPUMid / learnedModel.CPUCounter;
            float learnedCPUHigh = learnedModel.CPUHigh
                    / learnedModel.CPUCounter;

            // check if activcity model shows activity similar to learned, if
            // not bump package threat
            float cpuLow = activityModel.CPULow / activityModel.CPUCounter;
            float cpuMid = activityModel.CPUMid / activityModel.CPUCounter;
            float cpuHigh = activityModel.CPUHigh / activityModel.CPUCounter;

            if (cpuLow > learnedCPULow || cpuMid > learnedCPUMid
                    || cpuHigh > learnedCPUHigh
                    || activityModel.RxBytes > learnedModel.RxBytes
                    || activityModel.TxBytes > learnedModel.TxBytes) {
                // process behavior does not follow learned model

                // increase the threat of the package
                APackage pkg = defDBHelper.getPackage(pName);
                pkg.Threat_Numeric += 0.2;
                defDBHelper.updatePackage(pkg);

                defDBHelper
                        .insertLog(String
                                .format("Process %s consumed more resources than IEModel %s. Package %s acitivty %s",
                                        activityModel.ProcessName,
                                        learnedModel, pkg, activityModel));
            }
        }
    }
}